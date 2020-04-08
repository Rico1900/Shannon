package edu.nju.seg.solver;

import com.microsoft.z3.*;

public class Z3Playground {

    public static void main(String[] args)
    {
        Z3Playground p = new Z3Playground();
        com.microsoft.z3.Global.ToggleWarningMessages(true);
        Context ctx = new Context();
        p.testBool(ctx);
//        p.testReal(ctx);
//        p.testComplex(ctx);
//        p.testString(ctx);
        ctx.close();
    }

    private void testBool(Context ctx)
    {
        Solver s = ctx.mkSolver();
        BoolExpr tie = ctx.mkBoolConst("Tie");
        BoolExpr shirt = ctx.mkBoolConst("Shirt");
        s.add(ctx.mkOr(tie, shirt),
                ctx.mkOr(ctx.mkNot(tie), shirt),
                ctx.mkOr(ctx.mkNot(tie), ctx.mkNot(shirt)));
        System.out.println(s.check());
        System.out.println(s.getModel());
    }

    private void testReal(Context ctx)
    {
        Solver s = ctx.mkSolver();
        RealSort rs = ctx.mkRealSort();
        RealExpr e1 = (RealExpr) ctx.mkConst(ctx.mkSymbol("e1"), rs);
        RealExpr e2 = (RealExpr) ctx.mkConst(ctx.mkSymbol("e2"), rs);
        RatNum lower = ctx.mkReal("0.5");
        RatNum upper = ctx.mkReal("0.6");
        ArithExpr expr = ctx.mkSub(e2, e1);
        s.add(ctx.mkLe(lower, expr));
        s.add(ctx.mkLe(expr, upper));
        System.out.println(s.check());
        System.out.println(s.getModel());
    }

    private void testComplex(Context ctx)
    {
        Solver s = ctx.mkSolver();
        RealSort rs = ctx.mkRealSort();
        RealExpr e1 = (RealExpr) ctx.mkConst(ctx.mkSymbol("e1"), rs);
        RatNum lower = ctx.mkReal("0.5");
        BoolExpr tie = ctx.mkBoolConst("Tie");
        BoolExpr shirt = ctx.mkBoolConst("Shirt");
        s.add(ctx.mkOr(tie, shirt));
        s.add(ctx.mkLe(lower, e1));
        System.out.println(s.check());
        System.out.println(s.getModel());
    }

    private void testString(Context ctx)
    {
        Solver s = ctx.mkSolver();
        Expr s1 = ctx.mkConst(ctx.mkSymbol("a"), ctx.mkStringSort());
        Expr s2 = ctx.mkConst(ctx.mkSymbol("a"), ctx.mkStringSort());
        s.add(ctx.mkEq(s1, ctx.mkString("ssss")));
        s.add(ctx.mkEq(s2, ctx.mkString("sss")));
        s.add(ctx.mkEq(s1, s2));
        System.out.println(s.check());
        System.out.println(s.getModel());
    }

}
