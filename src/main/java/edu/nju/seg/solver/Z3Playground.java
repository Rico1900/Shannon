package edu.nju.seg.solver;

import com.microsoft.z3.*;

public class Z3Playground {

    public static void main(String[] args) {
        Z3Playground p = new Z3Playground();
        com.microsoft.z3.Global.ToggleWarningMessages(true);
        Context ctx = new Context();
        p.testBool(ctx);
        p.testReal(ctx);
        ctx.close();
    }

    private void testBool(Context ctx) {
        Solver s = ctx.mkSolver();
        BoolExpr tie = ctx.mkBoolConst("Tie");
        BoolExpr shirt = ctx.mkBoolConst("Shirt");
        s.add(ctx.mkOr(tie, shirt),
                ctx.mkOr(ctx.mkNot(tie), shirt),
                ctx.mkOr(ctx.mkNot(tie), ctx.mkNot(shirt)));
        System.out.println(s.check());
        System.out.println(s.getModel());
    }

    private void testReal(Context ctx) {
        Solver s = ctx.mkSolver();
        RealSort rs = ctx.mkRealSort();
        RealExpr e1 = (RealExpr) ctx.mkConst(ctx.mkSymbol("e1"), rs);
        RealExpr e2 = (RealExpr) ctx.mkConst(ctx.mkSymbol("e2"), rs);
        RatNum lower = ctx.mkReal("0.5");
        RatNum upper = ctx.mkReal("0.8");
        ArithExpr expr = ctx.mkSub(e2, e1);
        s.add(ctx.mkAnd(ctx.mkLe(lower, expr), ctx.mkLe(expr, upper)));
        System.out.println(s.check());
        System.out.println(s.getModel());
    }

}
