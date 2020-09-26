package edu.nju.seg.expression;

import java.util.Objects;

public class Judgement extends Expr {

    private JudgeOp op;

    private Expr left;

    private Expr right;

    public Judgement(JudgeOp op, Expr left, Expr right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public JudgeOp getOp() {
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
        Judgement judgement = (Judgement) o;
        return op == judgement.op &&
                left.equals(judgement.left) &&
                right.equals(judgement.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, left, right);
    }

    @Override
    public String toString() {
        return "Judgement{" +
                "op=" + op +
                ", left=" + left +
                ", right=" + right +
                '}';
    }

}
