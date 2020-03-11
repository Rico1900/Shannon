package edu.nju.seg.util;

import com.microsoft.z3.*;
import edu.nju.seg.exception.Z3Exception;

import java.util.List;
import java.util.Optional;

public class Z3Util {

    /**
     * make and expression for the bool expression list
     * @param list the bool expression that may be blank
     * @param ctx Z3 context
     * @return maybe and expression
     */
    public static Optional<BoolExpr> mkAnd(List<BoolExpr> list,
                                           Context ctx)
    {
        if ($.isBlankList(list)) {
            return Optional.empty();
        }
        return Optional.of(mkAndNotEmpty(list, ctx));
    }

    /**
     * make and expression for non-empty list
     * @param list the bool expression
     * @param ctx Z3 context
     * @return the and bool expression
     */
    public static BoolExpr mkAndNotEmpty(List<BoolExpr> list,
                                         Context ctx)
    {
        return ctx.mkAnd(list.toArray(new BoolExpr[0]));
    }

    /**
     * make or expression for bool expression
     * @param list the bool expression that may be blank
     * @param ctx Z3 context
     * @return maybe bool expression
     */
    public static Optional<BoolExpr> mkOr(List<BoolExpr> list,
                                          Context ctx)
    {
        if ($.isBlankList(list)) {
            return Optional.empty();
        }
        return Optional.of(mkOrNotEmpty(list, ctx));
    }

    /**
     * make or bool expression for non-empty list
     * @param list bool expression list
     * @param ctx Z3 context
     * @return the or bool expression
     */
    public static BoolExpr mkOrNotEmpty(List<BoolExpr> list,
                                        Context ctx)
    {
        return ctx.mkOr(list.toArray(new BoolExpr[0]));
    }

    /**
     * make sub expression
     * @param minuend the minuend
     * @param subtrahend the subtrahend
     * @param ctx Z3 context
     * @return arithmetic expression
     */
    public static ArithExpr mkSub(String minuend,
                                  String subtrahend,
                                  Context ctx)
    {
        RealSort rs = ctx.mkRealSort();
        RealExpr v1 = (RealExpr) ctx.mkConst(ctx.mkSymbol(minuend), rs);
        RealExpr v2 = (RealExpr) ctx.mkConst(ctx.mkSymbol(subtrahend), rs);
        return ctx.mkSub(v1, v2);
    }

    /**
     * make real expression according to the string
     * @param s the string that needs to be
     * @param ctx Z3 context
     * @return the arithmetic expression
     * @throws Z3Exception throws when the given string is null
     */
    public static ArithExpr mkRealExpr(String s,
                                       Context ctx) throws Z3Exception
    {
        if (s == null) {
            throw new Z3Exception();
        }
        return ctx.mkReal(s);
    }

    /**
     * make bool expression according to the operator
     * @param operator the operator string
     * @param left the left expression
     * @param right the right expression
     * @param ctx Z3 context
     * @return bool expression
     * @throws Z3Exception when the operator is not supported
     */
    public static BoolExpr mkOperatorExpr(String operator,
                                          ArithExpr left,
                                          ArithExpr right,
                                          Context ctx) throws Z3Exception
    {
        switch (operator) {
            case "<":
                return ctx.mkLt(left, right);
            case "<=":
                return ctx.mkLe(left, right);
            case ">":
                return ctx.mkGt(left, right);
            case ">=":
                return ctx.mkGe(left, right);
            case "=":
                return ctx.mkEq(left, right);
            case "!=":
                return ctx.mkNot(ctx.mkEq(left, right));
            default:
                throw new Z3Exception();
        }
    }

}
