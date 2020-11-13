package edu.nju.seg.encoder;


import com.microsoft.z3.*;
import edu.nju.seg.util.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;


public class SolverManager {

    private final static java.util.regex.Pattern LOC_PATTERN = java.util.regex.Pattern.compile("^(.*)_loc_(.*)$");

    private final static java.util.regex.Pattern DELTA_PATTERN = java.util.regex.Pattern.compile("^(.*)_delta_(.*)$");

    private final Context context;

    private final Solver solver;

    public SolverManager()
    {
        Global.ToggleWarningMessages(true);
        Global.setParameter(":proof", "true");
        this.context = new Context();
        Params p = context.mkParams();
        // time limitation: one hour
        p.add("timeout", 3600 * 1000);
        solver = context.mkSolver();
        solver.setParameters(p);
    }

    public int get_clause_num()
    {
        int sum = 0;
        for (BoolExpr e: solver.getAssertions()) {
            sum += calculate_clause_num(e);
        }
        return sum;
    }

    public Status check()
    {
        return solver.check();
    }

    public Model getModel()
    {
        return solver.getModel();
    }

    public void print_variables()
    {
        Model m = solver.getModel();
        FuncDecl[] func = m.getDecls();
        StringBuilder sb = new StringBuilder();
        for (FuncDecl f: func) {
            sb.append(f.getName()).append(" = ").append(m.getConstInterp(f).toString()).append("\n");
        }
        System.out.println(sb.toString());
    }

    public void print_automata_trace()
    {
        Model m = solver.getModel();
        FuncDecl[] func = m.getDecls();
        Map<String, List<Pair<Integer, String>>> map = new HashMap<>();
        for (FuncDecl f: func) {
            String name = f.getName().toString();
            Matcher mat = LOC_PATTERN.matcher(name);
            if (mat.matches()) {
                String a_name = mat.group(1);
                if (!map.containsKey(a_name)) {
                    map.put(a_name, new ArrayList<>());
                }
                map.get(a_name).add(new Pair<>(Integer.parseInt(mat.group(2)), m.getConstInterp(f).toString()));
            }
        }
        for (String name: map.keySet()) {
            print_trace(name, map.get(name));
        }
    }

    private void print_trace(String name, List<Pair<Integer, String>> trace)
    {
        trace.sort(Comparator.comparingInt(Pair::get_left));
        String sb = name + ": " +
                trace.stream().map(p -> p.get_left() + " " + p.get_right()).collect(Collectors.joining(" -> "));
        System.out.println(sb);
    }


    public void print_proof()
    {
        System.out.println(solver.getProof());
    }

    public Context get_context()
    {
        return context;
    }

    public void add_clause(BoolExpr... boolExprs)
    {
        solver.add(boolExprs);
    }

    private long calculate_clause_num(BoolExpr e)
    {
        if (e.isAdd()
                || e.isOr()
                || e.isNot()
                || e.isApp()) {
            int sum = 1;
            for (Expr sub: e.getArgs()) {
                if (sub instanceof  BoolExpr) {
                    sum += calculate_clause_num((BoolExpr) sub);
                }
            }
            return sum;
        } else {
            return 1;
        }
    }

}
