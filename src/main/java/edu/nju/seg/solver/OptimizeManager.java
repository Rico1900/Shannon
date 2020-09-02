package edu.nju.seg.solver;

import com.microsoft.z3.*;

public class OptimizeManager {

    private Context context;

    private Optimize optimize;

    public OptimizeManager()
    {
        Global.ToggleWarningMessages(true);
        Global.setParameter(":proof", "true");
        this.context = new Context();
        Params p = context.mkParams();
        // time limitation: one hour
        p.add("timeout", 3600 * 1000);
        optimize = context.mkOptimize();
        optimize.setParameters(p);
    }

    public void addConstraint(BoolExpr... exprs)
    {
        optimize.Add(exprs);
    }

    public Optimize.Handle mkMaximize(Expr expr)
    {
        return optimize.MkMaximize(expr);
    }

    public Optimize.Handle mkMinimize(Expr expr)
    {
        return optimize.MkMinimize(expr);
    }

    public Status check()
    {
        return optimize.Check();
    }

    public Context getContext()
    {
        return context;
    }

    /**
     * record current optimization point
     */
    public void checkPoint()
    {
        optimize.Push();
    }

    /**
     * go back to the check point
     */
    public void backToCheckPoint()
    {
        optimize.Pop();
    }

}
