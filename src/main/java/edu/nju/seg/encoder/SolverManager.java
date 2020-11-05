package edu.nju.seg.encoder;

import com.microsoft.z3.*;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class SolverManager {

    private Context context;

    private Solver solver;

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

    public Status check()
    {
        return solver.check();
    }

    public Model getModel()
    {
        return solver.getModel();
    }

    public String getEventTrace(boolean removeVirtual)
    {
        Model m = solver.getModel();
        FuncDecl[] func = m.getDecls();
        SortedMap<Double, List<String>> map = new TreeMap<>();
        for (FuncDecl f: func) {
            if (f.getRange() instanceof RealSort) {
                Double key = Double.valueOf(m.getConstInterp(f).toString() + ".0");
                if (!map.containsKey(key)) {
                    map.put(key, new ArrayList<>());
                }
                map.get(key).add(f.getName().toString());
            }
        }
        StringBuilder builder = new StringBuilder();
        for (Double d: map.keySet()) {
            for (String var: map.get(d)) {
                if (removeVirtual && (var.contains("head") || var.contains("tail"))) {
                    continue;
                }
                builder.append(var);
                builder.append(": ");
                builder.append(d);
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    public Expr getProof()
    {
        return solver.getProof();
    }

    public Context getContext()
    {
        return context;
    }

    public void addClause(BoolExpr... boolExprs)
    {
        solver.add(boolExprs);
    }

}
