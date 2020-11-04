package edu.nju.seg.expression;

import java.util.*;

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

    @Override
    public Expr attach_bound(int k) {
        return new BinaryExpr(op, left.attach_bound(k), right.attach_bound(k));
    }

    @Override
    public Expr attach_loop_queue(List<Integer> loop_queue) {
        return new BinaryExpr(op, left.attach_loop_queue(loop_queue), right.attach_loop_queue(loop_queue));
    }

    @Override
    public Set<String> extract_variables() {
        Set<String> result = new HashSet<>();
        result.addAll(left.extract_variables());
        result.addAll(right.extract_variables());
        return result;
    }

}
