package edu.nju.seg.util;

import com.microsoft.z3.*;

import java.util.List;
import java.util.Optional;

public class Z3Util {

    public static Optional<BoolExpr> mkAnd(List<BoolExpr> list,
                                           Context ctx) {
        if ($.isBlankList(list)) {
            return Optional.empty();
        }
        return Optional.of(ctx.mkAnd(list.toArray(new BoolExpr[0])));
    }

    public static BoolExpr mkAndNotEmpty(List<BoolExpr> list,
                                         Context ctx) {
        return ctx.mkAnd(list.toArray(list.toArray(new BoolExpr[0])));
    }

    public static Optional<BoolExpr> mkOr(List<BoolExpr> list,
                                          Context ctx) {
        if ($.isBlankList(list)) {
            return Optional.empty();
        }
        return Optional.of(ctx.mkOr(list.toArray(new BoolExpr[0])));
    }

    public static ArithExpr mkSub(String minuend, String subtrahend, Context ctx) {
        RealSort rs = ctx.mkRealSort();
        RealExpr v1 = (RealExpr) ctx.mkConst(ctx.mkSymbol(minuend), rs);
        RealExpr v2 = (RealExpr) ctx.mkConst(ctx.mkSymbol(subtrahend), rs);
        return ctx.mkSub(v1, v2);
    }

}
