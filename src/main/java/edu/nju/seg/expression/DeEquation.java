package edu.nju.seg.expression;

import java.util.Objects;

public class DeEquation {

    private final UnaryExpr left;

    private final JudgeOp op;

    private final Expr right;

    public DeEquation(UnaryExpr left, JudgeOp op, Expr right)
    {
        this.left = left;
        this.op = op;
        this.right = right;
    }

    public UnaryExpr get_left() {
        return left;
    }

    public JudgeOp get_op()
    {
        return op;
    }

    public Expr get_right() {
        return right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeEquation that = (DeEquation) o;
        return left.equals(that.left) &&
                op == that.op &&
                right.equals(that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, op, right);
    }

    @Override
    public String toString()
    {
        return left.toString() + op.toString() + right.toString();
    }

}
