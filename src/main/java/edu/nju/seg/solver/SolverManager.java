package edu.nju.seg.solver;

import com.microsoft.z3.*;
import org.scijava.parse.eval.DefaultEvaluator;
import org.scijava.parse.eval.Evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class SolverManager {

    private Context context;

    private Solver solver;

    public SolverManager() {
        Global.ToggleWarningMessages(true);
//        Global.setParameter(":unsat_core", "true");
//        Global.setParameter(":proof", "true");
        this.context = new Context();
        solver = context.mkSolver();
    }

    public Status check() {
        return solver.check();
    }

    public Model getModel() {
        return solver.getModel();
    }

    public String getEventTrace(boolean removeVirtual) {
        Model m = solver.getModel();
        FuncDecl[] func = m.getDecls();
        SortedMap<Double, List<String>> map = new TreeMap<>();
        Evaluator evaluator = new DefaultEvaluator();
        for (FuncDecl f: func) {
            if (f.getRange() instanceof RealSort) {
                Object o = evaluator.evaluate(m.getConstInterp(f).toString() + ".0");
                Double key = Double.valueOf(o.toString());
                if (map.containsKey(key)) {
                    map.get(key).add(f.getName().toString());
                } else {
                    map.put(key, new ArrayList<>());
                    map.get(key).add(f.getName().toString());
                }
            }
        }
        StringBuilder builder = new StringBuilder();
        for (Double d: map.keySet()) {
            for (String var: map.get(d)) {
                if (removeVirtual) {
                    if (!var.contains("virtual")) {
                        builder.append(var);
                        builder.append(": ");
                        builder.append(d);
                        builder.append("\n");
                    }
                } else {
                    builder.append(var);
                    builder.append(": ");
                    builder.append(d);
                    builder.append("\n");
                }
            }
        }
        return builder.toString();
    }

    public BoolExpr[] getUnsatCore() {
        return solver.getUnsatCore();
    }

    public Expr getProof() {
        return solver.getProof();
    }

    public Context getContext() {
        return context;
    }

    public void addClause(BoolExpr... boolExprs) {
        solver.add(boolExprs);
    }

}
