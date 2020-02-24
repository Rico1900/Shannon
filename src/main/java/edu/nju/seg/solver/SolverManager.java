package edu.nju.seg.solver;

import com.microsoft.z3.*;

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
