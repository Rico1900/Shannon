package edu.nju.seg.solver;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.RealExpr;
import edu.nju.seg.exception.EncodeException;
import edu.nju.seg.exception.Z3Exception;
import edu.nju.seg.model.AutomatonDiagram;
import edu.nju.seg.model.Relation;
import edu.nju.seg.model.State;
import edu.nju.seg.parser.EquationParser;
import edu.nju.seg.parser.ExprParser;
import edu.nju.seg.solver.model.Block;
import edu.nju.seg.solver.model.Duration;
import edu.nju.seg.solver.model.Instant;
import edu.nju.seg.solver.model.Node;
import edu.nju.seg.util.$;
import edu.nju.seg.util.Pair;
import edu.nju.seg.util.Z3Wrapper;

import java.util.*;
import java.util.stream.Collectors;

public class LocalEncoder {

    private AutomatonDiagram diagram;

    private List<Node> lifetime;

    private SolverManager manager;

    private int bound;

    private Context ctx;

    private ExprParser p;

    private Z3Wrapper w;

    private Map<String, State> stateMap = new HashMap<>();

    private Map<String, Relation> edgeMap= new HashMap<>();

    private int keyIndex;

    public LocalEncoder(AutomatonDiagram d,
                        List<Node> lifetime,
                        SolverManager manager,
                        int bound)
    {
        this.diagram = d;
        this.lifetime = lifetime;
        this.manager = manager;
        this.ctx = manager.getContext();
        this.p = new ExprParser(ctx);
        this.w = new Z3Wrapper(ctx);
        this.bound = bound;
        initStateAndEdgeMap();
        this.keyIndex = 0;
    }

    private void initStateAndEdgeMap()
    {
        for (State s: diagram.getAllStates()) {
            stateMap.put(s.getStateName(), s);
        }
        for (Relation r: diagram.getAllRelations()) {
            edgeMap.put(r.getName(), r);
        }
    }

    public Optional<BoolExpr> encode() throws Z3Exception
    {
        if ($.isBlankList(lifetime)) {
            return Optional.empty();
        }
        List<BoolExpr> exprs = new ArrayList<>();
        exprs.add(encodeInit());
        List<State> initStates = diagram.getInitial().getOuters().stream()
                .map(Relation::getTarget)
                .collect(Collectors.toList());
        Node firstNode = lifetime.get(0);
        State first = getHeadOfNode(firstNode);
        List<BoolExpr> initExprs = new ArrayList<>();
        for (State i: initStates) {
            initExprs.add(encodeSegment(i, first, 0));
        }
        exprs.add(w.mkOrNotEmpty(initExprs));
        for (int i = 0; i < lifetime.size() - 1; i++) {
            Node pre = lifetime.get(i);
            Node succ = lifetime.get(i + 1);
            encodeNode(pre, calculateOffset(i + 1)).ifPresent(exprs::add);
            exprs.add(encodeSegment(getTailOfNode(pre), getHeadOfNode(succ), i + 1));
        }
        encodeNode(lifetime.get(lifetime.size() - 1), calculateOffset(lifetime.size())).ifPresent(exprs::add);
        return Optional.of(w.mkAndNotEmpty(exprs));
    }

    private State getHeadOfNode(Node n)
    {
        if (n instanceof Duration) {
            return stateMap.get(((Duration) n).getName());
        } else if (n instanceof Instant) {
            Relation r = edgeMap.get(((Instant) n).getName());
            return r.getSource();
        } else if (n instanceof Block) {
            Pair<Node, Node> p = ((Block) n).getSubs();
            return getHeadOfNode(p.getLeft());
        } else {
            throw new EncodeException("illegal node type: " + n);
        }
    }

    private State getTailOfNode(Node n)
    {
        if (n instanceof Duration) {
            return stateMap.get(((Duration) n).getName());
        } else if (n instanceof Instant) {
            Relation r = edgeMap.get(((Instant) n).getName());
            return r.getTarget();
        } else if (n instanceof Block) {
            Pair<Node, Node> p = ((Block) n).getSubs();
            return getTailOfNode(p.getRight());
        } else {
            throw new EncodeException("illegal node type: " + n);
        }
    }

    private Optional<BoolExpr> encodeNode(Node n,
                                          int index) throws Z3Exception
    {
        if (n instanceof Duration) {
            return Optional.empty();
        } else if (n instanceof Instant) {
            this.keyIndex += 1;
            Relation r = edgeMap.get(((Instant) n).getName());
            List<BoolExpr> exprs = new ArrayList<>();
            exprs.add(encodeCurrentLocExpr(index, r.getSource()));
            exprs.add(encodeCurrentLocExpr(index + 1, r.getTarget()));
            exprs.add(encodeTimeExpr(index, 0));
            exprs.add(encodeCurrentTime(((Instant) n).getPrefix() + ((Instant) n).getName(), index));
            encodeInvariant(index, r.getSource()).ifPresent(exprs::add);
            return Optional.of(w.mkAndNotEmpty(exprs));
        } else if (n instanceof Block) {
            Pair<Node, Node> p = ((Block) n).getSubs();
            // TODO: verify the block
            if (p.getLeft() instanceof Instant) {
                return encodeNode(p.getLeft(), index);
            } else {
                return encodeNode(p.getRight(), index);
            }
        } else  {
            throw new EncodeException("illegal node type: " + n);
        }
    }

    private BoolExpr encodeInit() throws Z3Exception
    {
        State initial = diagram.getInitial();
        List<Relation> outers = initial.getOuters();
        if (outers != null) {
            List<BoolExpr> inits = new ArrayList<>();
            for (Relation r : outers) {
                String assignment = r.getAssignment();
                List<String> assignments = $.splitExpr(assignment);
                List<BoolExpr> subs = new ArrayList<>();
                subs.add(encodeCurrentLocExpr(0, r.getTarget()));
                for (String as : assignments) {
                    subs.add(p.convertWithBound(as, 0));
                }
                // ensure initial invariant condition
                encodeInvariant(0, r.getTarget()).ifPresent(subs::add);
                inits.add(w.mkAndNotEmpty(subs));
            }
            return w.mkOrNotEmpty(inits);
        } else {
            throw new EncodeException("wrong initial assignment");
        }
    }

    private BoolExpr encodeCurrentTime(String eventName,
                                       int index) throws Z3Exception
    {
        List<RealExpr> reals = new ArrayList<>();
        for (int i = 0; i < index; i++) {
            reals.add(mkTimeVar(i));
        }
        return ctx.mkEq(w.mkRealExpr(eventName), w.sumUpReals(reals));
    }

    private BoolExpr encodeSingleRelation(int index,
                                         Relation r) throws Z3Exception
    {
        State target = r.getTarget();
        BoolExpr time = encodeTimeExpr(index, 0);
        BoolExpr loc = ctx.mkEq(mkLocVar(index + 1), ctx.mkString(target.getStateName()));
        List<BoolExpr> exprs = new ArrayList<>();
        exprs.add(ctx.mkAnd(time, loc));
        // encode jump condition
        for (String c: $.splitExpr(r.getCondition())) {
            exprs.add(p.convertWithBound(c, index));
        }
        List<String> assignments = $.splitExpr(r.getAssignment());
        Set<String> vars = diagram.getAllVar();
        if ($.isNotBlankList(assignments)) {
            // encode jump assignment
            Set<String> changedVars = new HashSet<>();
            for (String assign: assignments) {
                changedVars.add(extractVar(assign));
                exprs.add(p.convertWithBound(assign, index + 1));
            }
            // encode unchanged variables
            for (String v: vars) {
                if (!changedVars.contains(v)) {
                    exprs.add(ctx.mkEq(mkVarVar(v, index), mkVarVar(v, index + 1)));
                }
            }
        } else {
            // variables remain unchanged
            for (String v: vars) {
                exprs.add(ctx.mkEq(mkVarVar(v, index), mkVarVar(v, index + 1)));
            }
        }
        return w.mkAndNotEmpty(exprs);
    }

    /**
     * encode the segment according to the bound b
     * @param start the start state
     * @param end the end state
     * @param segIndex the index of the segment
     * @return the bool expression
     */
    private BoolExpr encodeSegment(State start,
                                  State end,
                                  int segIndex) throws Z3Exception
    {
        int offset = calculateOffset(segIndex);
        List<BoolExpr> exprs = new ArrayList<>();
        for (int i = offset; i < offset + bound; i++) {
            for (State s: diagram.getAllStates()) {
                BoolExpr current = encodeCurrentLocExpr(i, s);
                List<BoolExpr> trans = new ArrayList<>();
                encodeJump(i, s).ifPresent(trans::add);
                trans.add(encodeStutter(i));
                trans.add(encodeTimed(i, s));
                exprs.add(ctx.mkImplies(current, w.mkOrNotEmpty(trans)));
                encodeInvariant(i + 1, s).ifPresent(exprs::add);
            }
        }
        // encode head and tail of the segment
        exprs.add(ctx.mkAnd(encodeTarget(offset, start), encodeTarget(offset + bound, end)));
        return w.mkAndNotEmpty(exprs);
    }

    private BoolExpr encodeTarget(int index,
                                 State t)
    {
        return encodeCurrentLocExpr(index, t);
    }

    private Optional<BoolExpr> encodeJump(int index,
                                         State s) throws Z3Exception
    {
        List<BoolExpr> nexts = new ArrayList<>();
        for (Relation r: s.getOuters()) {
            nexts.add(encodeSingleRelation(index, r));
        }
        return w.mkOr(nexts);
    }

    private BoolExpr encodeStutter(int index)
    {
        BoolExpr time = encodeTimeExpr(index, 0);
        BoolExpr loc = ctx.mkEq(mkLabelVar(index + 1), mkLocVar(index));
        BoolExpr unchanged = encodeVariableUnchanged(index);
        return ctx.mkAnd(time, loc, unchanged);
    }

    private BoolExpr encodeTimed(int index,
                                 State s)
    {
        RealExpr delta = mkTimeVar(index);
        BoolExpr time = ctx.mkGt(delta, ctx.mkReal(0));
        BoolExpr loc = ctx.mkEq(mkLocVar(index + 1), mkLocVar(index));
        List<BoolExpr> exprs = new ArrayList<>();
        exprs.add(time);
        exprs.add(loc);
        Map<String, Double> map = EquationParser.parseEquations(s.getEquations());
        for (Map.Entry<String, Double> pair: map.entrySet()) {
            String v = pair.getKey();
            // add differential equation to the result, for example
            // f'(x) = 1, make sure that x' - x = 1 * delta
            exprs.add(ctx.mkEq(ctx.mkMul(delta, ctx.mkReal(pair.getValue().toString())),
                    ctx.mkSub(mkVarVar(v, index + 1), mkVarVar(v, index))));
        }
        // rest variables remain unchanged
        Set<String> vars = diagram.getAllVar();
        for (String v: vars) {
            if (!map.containsKey(v)) {
                exprs.add(ctx.mkEq(mkVarVar(v, index + 1), mkVarVar(v, index)));
            }
        }
        return w.mkAndNotEmpty(exprs);
    }

    /**
     * encode invariant condition
     * @param index the bound index
     * @param current the current state
     * @return the bool expression that represent the invariant conditions
     */
    private Optional<BoolExpr> encodeInvariant(int index, State current) throws Z3Exception
    {
        if ($.isBlankList(current.getConstraints())) {
            return Optional.empty();
        }
        BoolExpr currentExpr = encodeCurrentLocExpr(index, current);
        List<BoolExpr> invars = new ArrayList<>();
        for (String c: current.getConstraints()) {
            invars.add(p.convertWithBound(c.trim(), index));
        }
        return Optional.of(ctx.mkImplies(currentExpr, w.mkAndNotEmpty(invars)));
    }

    /**
     * encode current location information
     * @param k the bound
     * @param current the current state
     * @return current location bool expression
     */
    private BoolExpr encodeCurrentLocExpr(int k, State current)
    {
        return ctx.mkEq(mkLocVar(k), ctx.mkString(current.getStateName()));
    }

    /**
     * make location symbol
     * @param k bound
     * @return the location symbol
     */
    private Expr mkLocVar(int k)
    {
        return w.mkStringVar("loc_" + k);
    }

    private RealExpr mkTimeVar(int k)
    {
        return w.mkRealVar("delta_" + k);
    }

    /**
     * make variable symbol
     * @param var variable name
     * @param k bound
     * @return the variable symbol
     */
    private RealExpr mkVarVar(String var, int k)
    {
        return w.mkRealVar(var + "_" + k);
    }

    private Expr mkLabelVar(int k)
    {
        return w.mkStringVar("label_" + k);
    }

    private String extractVar(String assign)
    {
        return assign.split("=")[0].trim();
    }

    /**
     * calculate total offset for the segment encoding
     * @param segIndex the segment index, start from 0
     * @return the total offset
     */
    private int calculateOffset(int segIndex)
    {
        return segIndex * bound + keyIndex;
    }

    private BoolExpr encodeTimeExpr(int index,
                                    int time)
    {
        return ctx.mkEq(mkTimeVar(index), ctx.mkReal(time));
    }

    private BoolExpr encodeVariableUnchanged(int index)
    {
        Set<String> vars = diagram.getAllVar();
        if (vars == null || vars.size() == 0) {
            throw new EncodeException("empty variable set");
        }
        List<BoolExpr> expers = new ArrayList<>();
        for (String v: vars) {
            expers.add(ctx.mkEq(mkVarVar(v, index + 1), mkVarVar(v, index)));
        }
        return w.mkAndNotEmpty(expers);
    }

}
