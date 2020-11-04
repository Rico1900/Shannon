package edu.nju.seg.util;

import com.microsoft.z3.*;
import edu.nju.seg.exception.Z3Exception;

import java.util.List;
import java.util.Optional;

public class Z3Wrapper {

    private final Context ctx;

    public Z3Wrapper(Context ctx) {
        this.ctx = ctx;
    }

    public Context get_ctx() {
        return ctx;
    }

    /**
     * make and expression for the bool expression list
     * @param list the bool expression that may be blank
     * @return maybe and expression
     */
    public Optional<BoolExpr> mk_and(List<BoolExpr> list)
    {
        if ($.isBlankList(list)) {
            return Optional.empty();
        }
        return Optional.of(mk_and_not_empty(list));
    }

    /**
     * make and expression for non-empty list
     * @param list the bool expression
     * @return the and bool expression
     */
    public BoolExpr mk_and_not_empty(List<BoolExpr> list)
    {
        if (list.size() == 1) {
            return list.get(0);
        }
        return ctx.mkAnd(list.toArray(new BoolExpr[0]));
    }

    /**
     * make or expression for bool expression
     * @param list the bool expression that may be blank
     * @return maybe bool expression
     */
    public Optional<BoolExpr> mk_or(List<BoolExpr> list)
    {
        if ($.isBlankList(list)) {
            return Optional.empty();
        }
        return Optional.of(mk_or_not_empty(list));
    }

    /**
     * make or bool expression for non-empty list
     * @param list bool expression list
     * @return the or bool expression
     */
    public BoolExpr mk_or_not_empty(List<BoolExpr> list)
    {
        if (list.size() == 1) {
            return list.get(0);
        }
        return ctx.mkOr(list.toArray(new BoolExpr[0]));
    }

    public ArithExpr sum_reals(List<RealExpr> list)
    {
        return ctx.mkAdd(list.toArray(new RealExpr[0]));
    }

    /**
     * make sub expression
     * @param minuend the minuend
     * @param subtrahend the subtrahend
     * @return arithmetic expression
     */
    public ArithExpr mk_sub(String minuend,
                            String subtrahend)
    {
        RealSort rs = ctx.mkRealSort();
        RealExpr v1 = (RealExpr) ctx.mkConst(ctx.mkSymbol(minuend), rs);
        RealExpr v2 = (RealExpr) ctx.mkConst(ctx.mkSymbol(subtrahend), rs);
        return ctx.mkSub(v1, v2);
    }

    /**
     * make real expression according to the string
     * @param s the string that needs to be
     * @return the arithmetic expression
     * @throws Z3Exception throws when the given string is null
     */
    public ArithExpr mk_real(String s) throws Z3Exception
    {
        if (s == null) {
            throw new Z3Exception();
        }
        return ctx.mkReal(s);
    }

    public ArithExpr mk_real(int i)
    {
        return ctx.mkReal(i);
    }

    public RealExpr mk_real_var(String s)
    {
        return (RealExpr) ctx.mkConst(ctx.mkSymbol(s), ctx.mkRealSort());
    }

    public IntExpr mk_int_var(String s)
    {
        return (IntExpr) ctx.mkConst(ctx.mkSymbol(s), ctx.mkIntSort());
    }

    public Expr mk_string_var(String s)
    {
        return ctx.mkConst(ctx.mkSymbol(s), ctx.mkStringSort());
    }

    public SeqExpr mk_string(String s)
    {
        return ctx.mkString(s);
    }

    /**
     * make bool expression according to the operator
     * @param operator the operator string
     * @param left the left expression
     * @param right the right expression
     * @return bool expression
     * @throws Z3Exception when the operator is not supported
     */
    public BoolExpr mk_judgement(String operator,
                                 ArithExpr left,
                                 ArithExpr right) throws Z3Exception
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

    public BoolExpr mk_lt(ArithExpr left, ArithExpr right)
    {
        return ctx.mkLt(left, right);
    }

    public BoolExpr mk_le(ArithExpr left, ArithExpr right)
    {
        return ctx.mkLe(left, right);
    }

    public BoolExpr mk_gt(ArithExpr left, ArithExpr right)
    {
        return ctx.mkGt(left, right);
    }

    public BoolExpr mk_ge(ArithExpr left, ArithExpr right)
    {
        return ctx.mkGe(left, right);
    }

    public BoolExpr mk_eq(Expr left, Expr right)
    {
        return ctx.mkEq(left, right);
    }

    public ArithExpr mkOperationExpr(String op,
                                     ArithExpr left,
                                     ArithExpr right) throws Z3Exception
    {
        switch (op) {
            case "+":
                return ctx.mkAdd(left, right);
            case "-":
                return ctx.mkSub(left, right);
            case "*":
                return ctx.mkMul(left, right);
            case "/":
                return ctx.mkDiv(left, right);
            default:
                throw new Z3Exception();
        }
    }

    public ArithExpr mk_neg(ArithExpr e)
    {

        return mk_sub(mk_real(0), e);
    }

    public ArithExpr mk_add(ArithExpr left, ArithExpr right)
    {
        return ctx.mkAdd(left, right);
    }

    public ArithExpr mk_sub(ArithExpr left, ArithExpr right)
    {
        return ctx.mkSub(left, right);
    }

    public ArithExpr mk_mul(ArithExpr left, ArithExpr right)
    {
        return ctx.mkMul(left, right);
    }

    public ArithExpr mk_div(ArithExpr left, ArithExpr right)
    {
        return ctx.mkDiv(left, right);
    }

}
