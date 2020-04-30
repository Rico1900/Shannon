package edu.nju.seg.solver.tassat;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.RealExpr;
import edu.nju.seg.exception.EncodeException;
import edu.nju.seg.exception.Z3Exception;
import edu.nju.seg.model.*;
import edu.nju.seg.parser.ExprParser;
import edu.nju.seg.solver.SolverManager;
import edu.nju.seg.util.Z3Util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SimpleMSCEncoder {

    private static final Pattern GLOBAL_PATTERN = Pattern.compile("^(.*)\\s*<->\\s*(.*)\\s*:\\s*(.*)$");

    private int boundIndex;

    private SequenceDiagram sd;

    private List<String> global;

    private SolverManager manager;

    private Context ctx;

    private ExprParser p;

    private Map<Instance, List<Event>> lifeSpanMap;

    private Set<String> varSet;

    private String prefix;

    private Map<Set<String>, String> miniMap;

    public SimpleMSCEncoder(SequenceDiagram sd,
                            List<String> global,
                            int boundIndex,
                            SolverManager manager)
    {
        this.sd = sd;
        this.global = global;
        this.boundIndex = boundIndex;
        this.manager = manager;
        this.ctx = manager.getContext();
        this.p = new ExprParser(ctx);
        this.prefix = Z3Util.indexPrefix(boundIndex);
        this.miniMap = consMiniMap(global);
    }

    public BoolExpr encode() throws Z3Exception
    {
        this.lifeSpanMap = consLifeSpanMap(sd.getContainer().getCovered(), sd.getContainer().getChildren());
        this.varSet = collectVars(sd.getContainer().getChildren());
        List<BoolExpr> exprs = new ArrayList<>();
        exprs.add(encodeStartEnd());
        encodeLifeSpan().ifPresent(exprs::add);
        encodeMessage(sd.getContainer().getChildren()).ifPresent(exprs::add);
        encodeConstraints().ifPresent(exprs::add);
        return Z3Util.mkAndNotEmpty(exprs, ctx);
    }

    private Map<Set<String>, String> consMiniMap(List<String> globals)
    {
        Map<Set<String>, String> map = new HashMap<>();
        for (String g: globals) {
            Matcher m = GLOBAL_PATTERN.matcher(g);
            if (m.matches()) {
                Set<String> key = new HashSet<>();
                key.add(m.group(1).trim());
                key.add(m.group(2).trim());
                map.put(key, m.group(3).trim());
            } else {
                throw new EncodeException("wrong global: " + g);
            }
        }
        return map;
    }

    private Map<Instance, List<Event>> consLifeSpanMap(List<Instance> covered,
                                                       List<SDComponent> children)
    {
        Map<Instance, List<Event>> result = new HashMap<>();
        for (Instance i: covered) {
            result.put(i, new ArrayList<>());
        }
        for (SDComponent c: children) {
            if (c instanceof Message) {
                Message m = (Message) c;
                Event from = m.getFrom();
                result.get(from.getBelongTo()).add(from);
                Event to = m.getTo();
                result.get(to.getBelongTo()).add(to);
            } else {
                throw new EncodeException("wrong component");
            }
        }
        return result;
    }

    private Set<String> collectVars(List<SDComponent> children)
    {
        Set<String> set = new HashSet<>();
        set.add(sd.getStart());
        set.add(sd.getEnd());
        for (SDComponent c: children) {
            if (c instanceof Message) {
                Message m = (Message) c;
                set.add(m.getFrom().getName());
                set.add(m.getTo().getName());
            } else {
                throw new EncodeException("wrong component");
            }
        }
        return set;
    }

    private BoolExpr encodeStartEnd() throws Z3Exception
    {
        List<BoolExpr> exprs = new ArrayList<>();
        exprs.add(ctx.mkGe(Z3Util.mkSub(prefix + sd.getEnd(), prefix + sd.getStart(), ctx),
                Z3Util.mkRealExpr("0", ctx)));
        for (Instance ins: lifeSpanMap.keySet()) {
            List<Event> events = lifeSpanMap.get(ins);
            int len = events.size();
            if (len > 0) {
                exprs.add(ctx.mkLe(Z3Util.mkRealVar(prefix + sd.getStart(), ctx),
                        Z3Util.mkRealVar(prefix + events.get(0).getName(), ctx)));
                exprs.add(ctx.mkGe(Z3Util.mkRealVar(prefix + sd.getEnd(), ctx),
                        Z3Util.mkRealVar(prefix + events.get(len - 1).getName(), ctx)));
            }

        }
        return Z3Util.mkAndNotEmpty(exprs, ctx);
    }

    private Optional<BoolExpr> encodeLifeSpan()
    {
        List<BoolExpr> exprs = new ArrayList<>();
        for (Instance ins: lifeSpanMap.keySet()) {
            List<Event> events = lifeSpanMap.get(ins);
            int len = events.size();
            if (len > 1) {
                for (int i = 0; i < len - 1; i++) {
                    Event pre = events.get(i);
                    Event suc = events.get(i + 1);
                    exprs.add(ctx.mkLe(Z3Util.mkRealVar(prefix + pre.getName(), ctx),
                            Z3Util.mkRealVar(prefix + suc.getName(), ctx)));
                }
            }
        }
        return Z3Util.mkAnd(exprs, ctx);
    }

    private Optional<BoolExpr> encodeMessage(List<SDComponent> children)
    {
        List<BoolExpr> exprs = new ArrayList<>();
        for (SDComponent c: children) {
            if (c instanceof Message) {
                Message m = (Message) c;
                List<BoolExpr> subs = new ArrayList<>();
                RealExpr start = Z3Util.mkRealVar(prefix + m.getFrom().getName(), ctx);
                RealExpr end = Z3Util.mkRealVar(prefix + m.getTo().getName(), ctx);
                subs.add(ctx.mkLe(start, end));
                Set<String> key = new HashSet<>();
                key.add(m.getFrom().getBelongTo().getVariable());
                key.add(m.getTo().getBelongTo().getVariable());
                if (miniMap.containsKey(key)) {
                    subs.add(ctx.mkGe(ctx.mkSub(end, start), ctx.mkReal(miniMap.get(key))));
                }
                exprs.add(Z3Util.mkAndNotEmpty(subs, ctx));
            } else {
                throw new EncodeException("wrong component");
            }
        }
        return Z3Util.mkAnd(exprs, ctx);
    }

    private Optional<BoolExpr> encodeConstraints()
    {
        List<BoolExpr> exprs = sd.getConstraints().stream()
                .map(this::mapConstraint)
                .map(expr -> {
                    try {
                        return p.convert(expr);
                    } catch (Z3Exception e) {
                        throw new EncodeException("wrong expr: " + expr);
                    }
                })
                .collect(Collectors.toList());
        return Z3Util.mkAnd(exprs, ctx);
    }

    private String mapConstraint(String constraint)
    {
        String result = constraint;
        for (String v: varSet) {
            result = result.replace(v, prefix + v);
        }
        return result;
    }

}
