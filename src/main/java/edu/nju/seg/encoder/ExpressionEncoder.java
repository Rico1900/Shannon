package edu.nju.seg.encoder;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.RealExpr;
import edu.nju.seg.exception.EncodeException;
import edu.nju.seg.exception.Z3Exception;
import edu.nju.seg.expression.*;
import edu.nju.seg.expression.Number;
import edu.nju.seg.util.Z3Wrapper;

import java.util.ArrayList;
import java.util.List;

public class ExpressionEncoder {

    private final Z3Wrapper w;

    public ExpressionEncoder(Z3Wrapper w)
    {
        this.w = w;
    }

    public BoolExpr encode_assignment_with_index(Assignment a, int index)
    {
        return encode_binary_judgement(
                JudgeOp.EQ,
                encode_expression(a.get_left().attach_bound(index)),
                encode_expression(a.get_right().attach_bound(index - 1))
        );
    }

    public BoolExpr encode_deequation_with_index(DeEquation e, int index, RealExpr delta)
    {
        return encode_binary_judgement(
                e.get_op(),
                encode_de_expression(e.get_left(), index, delta),
                encode_de_expression(e.get_right(), index, delta)
        );
    }

    private ArithExpr encode_de_expression(Expr e, int index, RealExpr delta)
    {
        if (e instanceof Number) {
            return encode_de_number((Number) e, delta);
        } else if (e instanceof Variable) {
            return encode_de_variable(((Variable) e).attach_bound(index), delta);
        } else if (e instanceof UnaryExpr) {
            return encode_de_unary((UnaryExpr) e, index);
        } else {
            return encode_de_binary((BinaryExpr) e, index, delta);
        }
    }

    private ArithExpr encode_de_unary(UnaryExpr e, int index)
    {
        if (e.get_op() == UnaryOp.DIFFERENTIAL) {
            if (e.get_expr() instanceof Variable) {
                String v = ((Variable) e.get_expr()).getName();
                int next = index + 1;
                return w.mk_sub(
                        w.mk_real_var(v + "_" + next),
                        w.mk_real_var(v + "_" + index)
                );
            }
        }
        throw new EncodeException("Wrong unary operation");
    }

    private ArithExpr encode_de_binary(BinaryExpr e, int index, RealExpr delta)
    {
        switch (e.getOp()) {
            case ADD:
                return w.mk_add(
                        encode_de_expression(e.getLeft(), index, delta),
                        encode_de_expression(e.getRight(), index, delta));
            case SUB:
                return w.mk_sub(
                        encode_de_expression(e.getLeft(), index, delta),
                        encode_de_expression(e.getRight(), index, delta)
                );
            case MUL:
                return w.mk_mul(
                        encode_de_expression(e.getLeft(), index, delta),
                        encode_de_expression(e.getRight(), index, delta)
                );
            case DIV:
                return w.mk_div(
                        encode_de_expression(e.getLeft(), index, delta),
                        encode_de_expression(e.getRight(), index, delta)
                );
            default:
                throw new EncodeException("Wrong operation");
        }
    }

    private ArithExpr encode_de_number(Number n, RealExpr delta)
    {
        try {
            ArithExpr real = w.mk_real(n.getValue().toString());
            return w.mk_mul(real, delta);
        } catch (Z3Exception e) {
            throw new EncodeException("wrong number: " + n.toString());
        }
    }

    private ArithExpr encode_de_variable(Variable v, RealExpr delta)
    {
        return w.mk_mul(w.mk_real_var(v.getName()), delta);
    }

    public BoolExpr encode_assignment_with_double_index(Assignment a, int l, int r)
    {
        return encode_binary_judgement(
                JudgeOp.EQ,
                encode_expression(a.get_left().attach_bound(l)),
                encode_expression(a.get_right().attach_bound(r))
        );
    }

    public BoolExpr encode_judgement_with_index(Judgement j, int index)
    {
        return encode_judgement(j.attach_bound(index));
    }

    public BoolExpr encode_judgement_with_loop_queue(Judgement j, List<Integer> loop_queue)
    {
        return encode_judgement(j.attach_loop_queue(loop_queue));
    }

    public BoolExpr encode_judgement(Judgement j)
    {
        if (j.getLeft() instanceof UnaryExpr) {
            UnaryExpr left = (UnaryExpr) j.getLeft();
            if (left.get_op() == UnaryOp.ABS) {
                return encode_abs_judgement(j, left);
            }
        }
        return encode_binary_judgement(j.getOp(), encode_expression(j.getLeft()), encode_expression(j.getRight()));
    }

    public ArithExpr encode_expression_with_loop_queue(Expr e, List<Integer> loop_queue)
    {
        return encode_expression(e.attach_loop_queue(loop_queue));
    }

    private BoolExpr encode_abs_judgement(Judgement j, UnaryExpr left)
    {
        List<BoolExpr> es = new ArrayList<>();
        ArithExpr left_expr = encode_expression(left.get_expr());
        es.add(w.get_ctx().mkAnd(
                encode_binary_judgement(j.getOp(), left_expr, encode_expression(j.getRight())),
                w.mk_ge(left_expr, w.get_ctx().mkReal(0))
        ));
        es.add(w.get_ctx().mkAnd(
                encode_binary_judgement(j.getOp(), w.mk_neg(left_expr), encode_expression(j.getRight())),
                w.mk_lt(left_expr, w.get_ctx().mkReal(0))
        ));
        return w.mk_or_not_empty(es);
    }

    public BoolExpr encode_binary_judgement(JudgeOp op, ArithExpr left, ArithExpr right)
    {
        switch (op) {
            case GT:
                return w.mk_gt(left, right);
            case GE:
                return w.mk_ge(left, right);
            case LT:
                return w.mk_lt(left, right);
            case LE:
                return w.mk_le(left, right);
            case EQ:
                return w.mk_eq(left, right);
            default:
                throw new EncodeException("Wrong judgement operation: " + op.toString());
        }
    }

    private ArithExpr encode_expression(Expr e)
    {
        if (e instanceof Number) {
            return encode_number((Number) e);
        } else if (e instanceof Variable) {
            return encode_variable((Variable) e);
        } else if (e instanceof UnaryExpr) {
            throw new EncodeException("wrong unary operation: " + e);
        } else {
            return encode_binary_expr((BinaryExpr) e);
        }
    }

    private ArithExpr encode_binary_expr(BinaryExpr be)
    {
        switch (be.getOp()) {
            case ADD:
                return w.mk_add(encode_expression(be.getLeft()), encode_expression(be.getRight()));
            case SUB:
                return w.mk_sub(encode_expression(be.getLeft()), encode_expression(be.getRight()));
            case DIV:
                return w.mk_div(encode_expression(be.getLeft()), encode_expression(be.getRight()));
            case MUL:
                return w.mk_mul(encode_expression(be.getLeft()), encode_expression(be.getRight()));
            default:
                throw new EncodeException("wrong binary operation: " + be.getOp());
        }
    }

    /**
     * encode number expression
     * @param n the number
     * @return arithmetic expression in Z3 real sort
     */
    private ArithExpr encode_number(Number n)
    {
        try {
            return w.mk_real(n.getValue().toString());
        } catch (Z3Exception e) {
            throw new EncodeException("wrong number: " + n.toString());
        }
    }

    /**
     * encode variable expression
     * @param v the variable
     * @return arithmetic expression in Z3 real sort
     */
    private ArithExpr encode_variable(Variable v)
    {
        return w.mk_real_var(v.getName());
    }

}
