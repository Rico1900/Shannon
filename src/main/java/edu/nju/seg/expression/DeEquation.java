package edu.nju.seg.expression;

import java.util.Objects;

public class DeEquation {

    private final UnaryExpr left;

    private final Expr right;

    public DeEquation(UnaryExpr left, Expr right)
    {
        this.left = left;
        this.right = right;
    }

    public UnaryExpr get_left() {
        return left;
    }

    public Expr get_right() {
        return right;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeEquation deEquation = (DeEquation) o;
        return left.equals(deEquation.left) &&
                right.equals(deEquation.right);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(left, right);
    }

    @Override
    public String toString()
    {
        return left.toString() + " = " + right.toString();
    }

}
