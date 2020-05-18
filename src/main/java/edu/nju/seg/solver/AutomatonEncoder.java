package edu.nju.seg.solver;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.RealExpr;
import edu.nju.seg.exception.Z3Exception;
import edu.nju.seg.model.AutomatonDiagram;
import edu.nju.seg.exception.EncodeException;
import edu.nju.seg.model.Relation;
import edu.nju.seg.model.State;
import edu.nju.seg.parser.EquationParser;
import edu.nju.seg.parser.ExprParser;
import edu.nju.seg.util.$;
import edu.nju.seg.util.Z3Wrapper;

import java.util.*;

public class AutomatonEncoder {

    private AutomatonDiagram diagram;

    private SolverManager manager;

    private Context ctx;

    private ExprParser p;

    private Z3Wrapper w;

    private int bound;

    public AutomatonEncoder(AutomatonDiagram diagram,
                            SolverManager manager,
                            int bound)
    {
        this.diagram = diagram;
        this.manager = manager;
        this.ctx = manager.getContext();
        this.p = new ExprParser(ctx);
        this.w = new Z3Wrapper(ctx);
        this.bound = bound;
    }

    public void encode() throws Z3Exception
    {
        encodeInit();
        encodeTarget();
        encodeTransAndInvar();
    }

    /**
     * encode initial state
     */
    private void encodeInit() throws Z3Exception
    {
        State initial = diagram.getInitial();
        List<Relation> outers = initial.getOuters();
        if (outers != null) {
            List<BoolExpr> inits = new ArrayList<>();
            for (Relation r : outers) {
                String assignment = r.getAssignment();
                List<String> assignments = $.splitExpr(assignment);
                List<BoolExpr> initExpr = new ArrayList<>();
                initExpr.add(encodeCurrentLocExpr(0, r.getTarget()));
                for (String as : assignments) {
                    initExpr.add(p.convertWithBound(as, 0));
                }
                // ensure initial invariant condition
                encodeInvariant(0, r.getTarget()).ifPresent(initExpr::add);
                inits.add(w.mkAndNotEmpty(initExpr));
            }
            manager.addClause(w.mkOrNotEmpty(inits));
        } else {
            throw new EncodeException("wrong initial assignment");
        }
    }

    /**
     * encode target nodes
     */
    private void encodeTarget()
    {
        List<BoolExpr> bools = new ArrayList<>();
        for (State s: diagram.getAllStates()) {
            bools.add(encodeCurrentLocExpr(bound, s));
        }
        manager.addClause(w.mkOrNotEmpty(bools));
    }

    /**
     * encode transition and invariant condition according to the bound and states
     */
    private void encodeTransAndInvar() throws Z3Exception
    {
        Set<String> vars = diagram.getAllVar();
        for (int i = 0; i < bound; i++) {
            for (State s: diagram.getAllStates()) {
                BoolExpr current = encodeCurrentLocExpr(i, s);
                Optional<BoolExpr> jump = encodeJump(i, s, vars);
                BoolExpr trans;
                if (jump.isPresent()) {
                    trans = ctx.mkOr(encodeStutter(i, s, vars),
                            encodeTimed(i, s, vars), jump.get());
                } else {
                    trans = ctx.mkOr(encodeStutter(i, s, vars),
                            encodeTimed(i, s, vars));
                }
                Optional<BoolExpr> invar = encodeInvariant(i + 1, s);
                if (invar.isPresent()) {
                    manager.addClause(ctx.mkAnd(ctx.mkImplies(current, trans), invar.get()));
                } else {
                    manager.addClause(ctx.mkImplies(current, trans));
                }
            }
        }
    }

    /**
     * encode stutter state
     * @param k the bound
     * @param current the current state
     * @param vars the full variable set
     * @return the bool expression that represent the current location information
     */
    private BoolExpr encodeStutter(int k,
                                   State current,
                                   Set<String> vars) throws Z3Exception
    {
        String prefix = current.getStateName() + "_" + k + "_";
        BoolExpr time = p.convert(prefix + "delta = 0");
        BoolExpr loc = ctx.mkEq(mkLocVar(k + 1), mkLocVar(k));
        BoolExpr label = encodeLabel(k, "S");
        BoolExpr result = ctx.mkAnd(time, loc, label);
        List<BoolExpr> bools = new ArrayList<>();
        for (String v: vars) {
            bools.add(ctx.mkEq(mkVarVar(v, k + 1), mkVarVar(v, k)));
        }
        return ctx.mkAnd(result, w.mkAndNotEmpty(bools));
    }

    /**
     * encode timed transitions
     * @param k the bound
     * @param current the current state
     * @param vars the full variable set
     * @return the bool expression that represent the current location information
     */
    private BoolExpr encodeTimed(int k,
                                 State current,
                                 Set<String> vars)
    {
        String prefix = current.getStateName() + "_" + k + "_";
        RealExpr delta = (RealExpr) ctx.mkConst(ctx.mkSymbol(prefix + "delta"), ctx.mkRealSort());
        BoolExpr time = ctx.mkGt(delta, ctx.mkReal(0));
        BoolExpr loc = ctx.mkEq(mkLocVar(k + 1), mkLocVar(k));
        BoolExpr label = encodeLabel(k, "T");
        BoolExpr result = ctx.mkAnd(time, loc, label);
        Map<String, Double> map = EquationParser.parseEquations(current.getEquations());
        for (Map.Entry<String, Double> pair: map.entrySet()) {
            String v = pair.getKey();
            // add differential equation to the result, for example
            // f'(x) = 1, make sure that x' - x = 1 * delta
            result = ctx.mkAnd(result,
                    ctx.mkEq(ctx.mkMul(delta, ctx.mkReal(pair.getValue().toString())),
                            ctx.mkSub(mkVarVar(v, k + 1), mkVarVar(v, k))));
        }
        return result;
    }

    /**
     * encode jump transition
     * @param k the bound
     * @param current the current state
     * @param vars the full variable set
     * @return the bool expression that represent the jump transition
     */
    private Optional<BoolExpr> encodeJump(int k,
                                          State current,
                                          Set<String> vars) throws Z3Exception
    {
        List<BoolExpr> nexts = new ArrayList<>();
        for (Relation r: current.getOuters()) {
            String prefix = current.getStateName() + "_" + k + "_";
            BoolExpr time = p.convert(prefix + "delta = 0");
            BoolExpr loc = ctx.mkEq(mkLocVar(k + 1), ctx.mkString(r.getTarget().getStateName()));
            BoolExpr label = encodeLabel(k, "J-" + r.getName());
            BoolExpr single = ctx.mkAnd(time, loc, label);
            for (String c: $.splitExpr(r.getCondition())) {
                single = ctx.mkAnd(single, p.convertWithBound(c, k));
            }
            List<String> assignments = $.splitExpr(r.getAssignment());
            if ($.isNotBlankList(assignments)) {
                // encode jump assignment
                for (String assign: assignments) {
                    single = ctx.mkAnd(single, p.convertWithBound(assign, k + 1));
                }
            } else {
                // variables remain unchanged
                for (String v: vars) {
                    single = ctx.mkAnd(single, ctx.mkEq(mkVarVar(v, k), mkVarVar(v, k + 1)));
                }
            }
            nexts.add(single);
        }
        return w.mkOr(nexts);
    }

    /**
     * encode invariant condition
     * @param k the bound
     * @param current the current state
     * @return the bool expression that represent the invariant conditions
     */
    private Optional<BoolExpr> encodeInvariant(int k, State current) throws Z3Exception
    {
        if ($.isBlankList(current.getConstraints())) {
            return Optional.empty();
        }
        BoolExpr currentExpr = encodeCurrentLocExpr(k, current);
        List<BoolExpr> invars = new ArrayList<>();
        for (String c: current.getConstraints()) {
            invars.add(p.convertWithBound(c.trim(), k));
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

    private BoolExpr encodeLabel(int k, String label)
    {
        return ctx.mkEq(mkLabelVar(k), ctx.mkString(label));
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

}
