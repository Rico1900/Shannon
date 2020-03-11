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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutomatonEncoder {

    private AutomatonDiagram diagram;

    private SolverManager manager;

    private Context ctx;

    private ExprParser p;

    private int bound;

    public AutomatonEncoder(AutomatonDiagram diagram,
                            SolverManager manager,
                            int bound)
    {
        this.diagram = diagram;
        this.manager = manager;
        this.ctx = manager.getContext();
        this.p = new ExprParser(ctx);
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
    private void encodeInit() throws Z3Exception {
        State initial = diagram.getInitial();
        List<Relation> outers = initial.getOuters();
        if (outers != null) {
            BoolExpr[] inits = new BoolExpr[outers.size()];
            for (int i = 0; i < outers.size(); i++) {
                Relation r = outers.get(i);
                String assignment = r.getAssignment();
                List<String> assignments = $.splitExpr(assignment);
                BoolExpr sub = encodeCurrentLocExpr(0, r.getTarget());
                for (String as: assignments) {
                    sub = ctx.mkAnd(sub, p.convertK(as, 0));
                }
                // ensure initial invariant condition
                inits[i] = ctx.mkAnd(sub, encodeInvariant(0, r.getTarget()));
            }
            manager.addClause(ctx.mkOr(inits));
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
        BoolExpr[] boolArray = new BoolExpr[diagram.getAllStates().size()];
        manager.addClause(ctx.mkOr(bools.toArray(boolArray)));
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
                BoolExpr trans = ctx.mkOr(encodeStutter(i, s, vars),
                        encodeTimed(i, s, vars), encodeJump(i, s, vars));
                manager.addClause(ctx.mkAnd(ctx.mkImplies(current, trans), encodeInvariant(i + 1, s)));
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
    private BoolExpr encodeStutter(int k, State current, Set<String> vars) throws Z3Exception
    {
        String prefix = current.getStateName() + "_" + k + "_";
        BoolExpr time = p.convert(prefix + "delta = 0");
        BoolExpr loc = ctx.mkEq(mkLoc(k + 1), mkLoc(k));
        BoolExpr label = encodeLabel(k, "S");
        BoolExpr result = ctx.mkAnd(time, loc, label);
        for (String v: vars) {
            result = ctx.mkAnd(result, ctx.mkEq(mkVar(v, k + 1), mkVar(v, k)));
        }
        return result;
    }

    /**
     * encode timed transitions
     * @param k the bound
     * @param current the current state
     * @param vars the full variable set
     * @return the bool expression that represent the current location information
     */
    private BoolExpr encodeTimed(int k, State current, Set<String> vars)
    {
        String prefix = current.getStateName() + "_" + k + "_";
        RealExpr delta = (RealExpr) ctx.mkConst(ctx.mkSymbol(prefix + "delta"), ctx.mkRealSort());
        BoolExpr time = ctx.mkGt(delta, ctx.mkReal(0));
        BoolExpr loc = ctx.mkEq(mkLoc(k + 1), mkLoc(k));
        BoolExpr label = encodeLabel(k, "T");
        BoolExpr result = ctx.mkAnd(time, loc, label);
        Map<String, Double> map = EquationParser.parseEquations(current.getEquations());
        for (Map.Entry<String, Double> pair: map.entrySet()) {
            String v = pair.getKey();
            // add differential equation to the result, for example
            // f'(x) = 1, make sure that x' - x = 1 * delta
            result = ctx.mkAnd(result,
                    ctx.mkEq(ctx.mkMul(delta, ctx.mkReal(pair.getValue().toString())),
                            ctx.mkSub(mkVar(v, k + 1), mkVar(v, k))));
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
    private BoolExpr encodeJump(int k, State current, Set<String> vars) throws Z3Exception
    {
        BoolExpr[] nexts = new BoolExpr[current.getOuters().size()];
        for (int i = 0; i < nexts.length; i++) {
            Relation r = current.getOuters().get(i);
            String prefix = current.getStateName() + "_" + k + "_";
            BoolExpr time = p.convert(prefix + "delta = 0");
            BoolExpr loc = ctx.mkEq(mkLoc(k + 1), ctx.mkString(r.getTarget().getStateName()));
            BoolExpr label = encodeLabel(k, "J-" + r.getName());
            BoolExpr single = ctx.mkAnd(time, loc, label);
            // TODO: jump condition, double check
            for (String c: $.splitExpr(r.getCondition())) {
                single = ctx.mkAnd(single, p.convertK(c, k));
            }
            for (String v: vars) {
                single = ctx.mkAnd(single, ctx.mkEq(mkVar(v, k), mkVar(v, k + 1)));
            }
            nexts[i] = single;
        }
        return ctx.mkOr(nexts);
    }

    /**
     * encode invariant condition
     * @param k the bound
     * @param current the current state
     * @return the bool expression that represent the invariant conditions
     */
    private BoolExpr encodeInvariant(int k, State current) throws Z3Exception
    {
        // TODO: double check what to do if there is no invariant conditions
        if ($.isBlankList(current.getConstraints())) {
            return ctx.mkBoolConst(current.getStateName() + "_" + k + "_dummy");
        }
        BoolExpr currentExpr = encodeCurrentLocExpr(k, current);
        BoolExpr[] invars = new BoolExpr[current.getConstraints().size()];
        for (int i = 0; i < current.getConstraints().size(); i++) {
            String c = current.getConstraints().get(i).trim();
            invars[i] = p.convertK(c, k);
        }
        return ctx.mkImplies(currentExpr, ctx.mkAnd(invars));
    }

    /**
     * encode current location information
     * @param k the bound
     * @param current the current state
     * @return current location bool expression
     */
    private BoolExpr encodeCurrentLocExpr(int k, State current)
    {
        return ctx.mkEq(mkLoc(k), ctx.mkString(current.getStateName()));
    }

    private BoolExpr encodeLabel(int k, String label)
    {
        return ctx.mkEq(mkLabel(k), ctx.mkString(label));
    }

    /**
     * make location symbol
     * @param k bound
     * @return the location symbol
     */
    private Expr mkLoc(int k)
    {
        return ctx.mkConst(ctx.mkSymbol("loc_" + k), ctx.mkStringSort());
    }

    /**
     * make variable symbol
     * @param var variable name
     * @param k bound
     * @return the variable symbol
     */
    private RealExpr mkVar(String var, int k)
    {
        return (RealExpr) ctx.mkConst(ctx.mkSymbol(var + "_" + k), ctx.mkRealSort());
    }

    private Expr mkLabel(int k)
    {
        return ctx.mkConst(ctx.mkSymbol("label_" + k), ctx.mkStringSort());
    }

}
