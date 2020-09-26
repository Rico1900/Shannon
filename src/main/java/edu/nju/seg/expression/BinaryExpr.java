package edu.nju.seg.expression;

import java.util.Objects;

public class BinaryExpr extends Expr {

    private BinaryOp op;

    private Expr left;

    private Expr right;

    public BinaryExpr(BinaryOp op, Expr left, Expr right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public BinaryOp getOp() {
        return op;
    }

    public Expr getLeft() {
        return left;
    }

    public Expr getRight() {
        return right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BinaryExpr that = (BinaryExpr) o;
        return op == that.op &&
                left.equals(that.left) &&
                right.equals(that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, left, right);
    }

    @Override
    public String toString() {
        return "BinaryExpr{" +
                "op=" + op +
                ", left=" + left +
                ", right=" + right +
                '}';
    }

}
