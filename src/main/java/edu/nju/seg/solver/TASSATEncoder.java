package edu.nju.seg.solver;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import edu.nju.seg.exception.EncodeException;
import edu.nju.seg.exception.Z3Exception;
import edu.nju.seg.model.*;
import edu.nju.seg.parser.ExprParser;
import edu.nju.seg.util.Pair;
import edu.nju.seg.util.Z3Util;

import java.util.*;

public class TASSATEncoder {

    private int bound;

    private MsgDiagram md;

    private List<String> targets;

    private List<SequenceDiagram> mscs;

    private SolverManager manager;

    private Context ctx;

    private ExprParser p;

    private Map<String, SequenceDiagram> mscMap;

    private Set<String> symbolSet;

    private Map<Pair<String, String>, String> constraintMap;

    public TASSATEncoder(List<Diagram> diagrams,
                         int bound,
                         List<String> targets,
                         SolverManager manager)
    {
        this.bound = bound;
        this.manager = manager;
        partition(diagrams);
        this.targets = targets;
        this.ctx = manager.getContext();
        this.p = new ExprParser(ctx);
        consMscMap();
        collectSymbol();
        constructConstraintMap();
    }

    public void encode() throws Z3Exception
    {
        manager.addClause(encodeStartEnd());
        manager.addClause(encodeConnection());
        manager.addClause(encodeTargets());
        encodeNonTerminated().ifPresent(manager::addClause);
    }

    private BoolExpr encodeStartEnd() throws Z3Exception
    {
        SimpleState start = md.getInitial();
        SimpleState end = md.getEnd();
        List<BoolExpr> exprs = new ArrayList<>();
        BoolExpr startExper = ctx.mkEq(mkLocVar(0), ctx.mkString(start.getType().name()));
        exprs.add(startExper);
        exprs.add(ctx.mkGe(Z3Util.mkRealVar(Z3Util.indexPrefix(0) + start.getType().name(), ctx),
                ctx.mkReal(0)));
        exprs.add(ctx.mkEq(mkLocVar(bound), ctx.mkString(end.getType().name())));
        List<BoolExpr> next = new ArrayList<>();
        for (SimpleRelation r: start.getOuters()) {
            List<BoolExpr> subs = new ArrayList<>();
            subs.add(encodeLocExpr(1, r.getTarget()));
            encodeMsc(1, r.getTarget().getStateName()).ifPresent(subs::add);
            encodeMscOrder(0, start, 1, r.getTarget()).ifPresent(subs::add);
            next.add(ctx.mkImplies(startExper, Z3Util.mkAndNotEmpty(subs, ctx)));
        }
        exprs.add(Z3Util.mkOrNotEmpty(next, ctx));
        return Z3Util.mkAndNotEmpty(exprs, ctx);
    }

    private BoolExpr encodeConnection() throws Z3Exception
    {
        List<BoolExpr> exprs = new ArrayList<>();
        for (int i = 1; i < bound; i++) {
            for (SimpleState s: md.getAllStates()) {
                BoolExpr current = encodeLocExpr(i, s);
                List<BoolExpr> next = new ArrayList<>();
                for (SimpleRelation r: s.getOuters()) {
                    List<BoolExpr> subs = new ArrayList<>();
                    subs.add(encodeLocExpr(i + 1, r.getTarget()));
                    encodeMsc(i + 1, r.getTarget().getStateName()).ifPresent(subs::add);
                    encodeMscOrder(i, s, i + 1, r.getTarget())
                            .ifPresent(subs::add);
                    encodeGlobalConstraints(i, s.getStateName(), i + 1, r.getTarget().getStateName())
                            .ifPresent(subs::add);
                    next.add(Z3Util.mkAndNotEmpty(subs, ctx));
                }
                exprs.add(ctx.mkImplies(current, Z3Util.mkOrNotEmpty(next, ctx)));
            }
        }
        return Z3Util.mkAndNotEmpty(exprs, ctx);
    }

    private BoolExpr encodeTargets()
    {
        List<BoolExpr> expers = new ArrayList<>();
        for (String t: targets) {
            List<BoolExpr> subs = new ArrayList<>();
            for (int i = 1; i < bound; i++) {
                subs.add(ctx.mkEq(mkLocVar(i), ctx.mkString(t)));
            }
            expers.add(Z3Util.mkOrNotEmpty(subs, ctx));
        }
        return Z3Util.mkAndNotEmpty(expers, ctx);
    }

    private Optional<BoolExpr> encodeNonTerminated()
    {
        List<BoolExpr> exprs = new ArrayList<>();
        for (int i = 1; i < bound; i++) {
            exprs.add(ctx.mkNot(ctx.mkEq(mkLocVar(i), ctx.mkString(StateType.FINAL.name()))));
        }
        return Z3Util.mkAnd(exprs, ctx);
    }

    private void consMscMap()
    {
        mscMap = new HashMap<>();
        for (SequenceDiagram m: mscs) {
            mscMap.put(m.getTitle(), m);
        }
    }

    private void collectSymbol()
    {
        symbolSet = new HashSet<>();
        for (SequenceDiagram d: mscs) {
            symbolSet.add(d.getSymbol());
        }
    }

    private void constructConstraintMap()
    {
        constraintMap = new HashMap<>();
        for (String c: md.getConstraints()) {
            Pair<String, String> vs = p.getVarFromCP(c);
            String pre = searchSymbol(vs.getRight());
            String suc = searchSymbol(vs.getLeft());
            constraintMap.put(new Pair<>(pre, suc), c);
        }
    }

    private String searchSymbol(String var)
    {
        for (String s: symbolSet) {
            if (var.startsWith(s)) {
                return s;
            }
        }
        throw new EncodeException("failed to find symbol: " + var);
    }

    private void partition(List<Diagram> diagrams)
    {
        List<SequenceDiagram> mscs = new ArrayList<>();
        for (Diagram d: diagrams) {
            if (d instanceof MsgDiagram) {
                md = (MsgDiagram) d;
            } else if (d instanceof SequenceDiagram) {
                mscs.add((SequenceDiagram) d);
            } else {
                throw new EncodeException("wrong diagram type: " + d.getTitle());
            }
        }
        this.mscs = mscs;
    }

    /**
     * make location symbol
     * @param k bound
     * @return the location symbol
     */
    private Expr mkLocVar(int k)
    {
        return Z3Util.mkStringVar("loc_" + k, ctx);
    }

    private BoolExpr encodeLocExpr(int k, SimpleState state)
    {
        if (state.getType() == StateType.NORMAL) {
            return ctx.mkEq(mkLocVar(k), ctx.mkString(state.getStateName()));
        } else {
            return ctx.mkEq(mkLocVar(k), ctx.mkString(state.getType().name()));
        }

    }

    private Optional<BoolExpr> encodeMsc(int k, String name) throws Z3Exception
    {
        if (mscMap.containsKey(name)) {
            SimpleMSCEncoder encoder = new SimpleMSCEncoder(mscMap.get(name), md.getGlobal(), k, manager);
            return Optional.of(encoder.encode());
        } else {
            return Optional.empty();
        }
    }

    private Optional<BoolExpr> encodeMscOrder(int preIndex,
                                              SimpleState pre,
                                              int sucIndex,
                                              SimpleState suc)
    {
        String prePrefix = Z3Util.indexPrefix(preIndex);
        String sucPrefix = Z3Util.indexPrefix(sucIndex);
        if (mscMap.containsKey(suc.getStateName()) && mscMap.containsKey(pre.getStateName())) {
            return Optional.of(ctx.mkLe(Z3Util.mkRealVar(prePrefix + mscMap.get(pre.getStateName()).getEnd(), ctx),
                    Z3Util.mkRealVar(sucPrefix + mscMap.get(suc.getStateName()).getStart(), ctx)));
        } else if (mscMap.containsKey(suc.getStateName()) && pre.getType() == StateType.INITIAL) {
            return Optional.of(ctx.mkLe(Z3Util.mkRealVar(Z3Util.indexPrefix(0) + StateType.INITIAL.name(), ctx),
                    Z3Util.mkRealVar(sucPrefix + mscMap.get(suc.getStateName()).getStart(), ctx)));
        } else if (mscMap.containsKey(pre.getStateName()) && suc.getType() == StateType.FINAL) {
            return Optional.of(ctx.mkLe(Z3Util.mkRealVar(prePrefix + mscMap.get(pre.getStateName()).getEnd(), ctx),
                    Z3Util.mkRealVar(Z3Util.indexPrefix(bound) + StateType.FINAL.name(), ctx)));
        } else {
            return Optional.empty();
        }
    }

    private Optional<BoolExpr> encodeGlobalConstraints(int preIndex,
                                                       String pre,
                                                       int sucIndex,
                                                       String suc) throws Z3Exception
    {
        if (mscMap.containsKey(suc)) {
            String preS = mscMap.get(pre).getSymbol();
            String sucS = mscMap.get(suc).getSymbol();
            Pair<String, String> key = new Pair<>(preS, sucS);
            if (constraintMap.containsKey(key)) {
                String con = constraintMap.get(key);
                Pair<String, String> vars = p.getVarFromCP(con);
                con = con.replaceAll(vars.getLeft(), Z3Util.indexPrefix(sucIndex) + vars.getLeft())
                        .replaceAll(vars.getRight(), Z3Util.indexPrefix(preIndex) + vars.getRight());
                return Optional.of(p.convert(con));
            }
        }
        return Optional.empty();
    }

}
