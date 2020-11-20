package edu.nju.seg.expression;

import java.util.Objects;

public class Assignment {

    private final Variable left;

    private final Expr right;

    public Assignment(Variable left, Expr right) {
        this.left = left;
        this.right = right;
    }

    public Variable get_left() {
        return left;
    }

    public Expr get_right() {
        return right;
    }

    public Assignment mark_seq_index(int k)
    {
        return new Assignment((Variable) left.mark_seq_index(k), right.mark_seq_index(k));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assignment that = (Assignment) o;
        return left.equals(that.left) &&
                right.equals(that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public String toString() {
        return left + " := " + right;
    }
}
