package edu.nju.seg.expression;

import java.util.*;

public class UnaryExpr extends Expr {

    private final UnaryOp op;

    private final Expr expr;

    public UnaryExpr(UnaryOp op, Expr expr)
    {
        this.op = op;
        this.expr = expr;
    }

    public UnaryOp get_op()
    {
        return op;
    }

    public Expr get_expr()
    {
        return expr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnaryExpr unaryExpr = (UnaryExpr) o;
        return op == unaryExpr.op &&
                expr.equals(unaryExpr.expr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, expr);
    }

    @Override
    public String toString() {
        return "UnaryExpr{" +
                "op=" + op +
                ", expr=" + expr +
                '}';
    }

    @Override
    public Expr attach_bound(int k) {
        return new UnaryExpr(op, expr.attach_bound(k));
    }

    @Override
    public Expr mark_seq_index(int k) {
        return new UnaryExpr(op, expr.mark_seq_index(k));
    }

    @Override
    public Expr attach_loop_queue(List<Integer> loop_queue) {
        return new UnaryExpr(op, expr.attach_loop_queue(loop_queue));
    }

    @Override
    public Set<String> extract_variables() {
        return new HashSet<>(expr.extract_variables());
    }
}
