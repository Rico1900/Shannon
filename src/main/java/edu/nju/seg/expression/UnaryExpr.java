package edu.nju.seg.expression;

import java.util.Objects;

public class UnaryExpr extends Expr {

    private UnaryOp op;

    private Expr expr;

    public UnaryExpr(UnaryOp op, Expr expr)
    {
        this.op = op;
        this.expr = expr;
    }

    public UnaryOp getOp()
    {
        return op;
    }

    public Expr getExpr()
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

}
