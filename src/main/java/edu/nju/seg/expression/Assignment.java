package edu.nju.seg.expression;

import java.util.Objects;

public class Assignment {

    private final Variable left;

    private final Expr right;

    public Assignment(Variable left, Expr right) {
        this.left = left;
        this.right = right;
    }

    public Variable getLeft() {
        return left;
    }

    public Expr getRight() {
        return right;
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
        return left + " = " + right;
    }
}
