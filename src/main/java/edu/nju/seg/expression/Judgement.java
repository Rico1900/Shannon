package edu.nju.seg.expression;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Judgement {

    private JudgeOp op;

    private final Expr left;

    private final Expr right;

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

    public Judgement attach_bound(int k)
    {
        return new Judgement(op, left.attach_bound(k), right.attach_bound(k));
    }

    public Judgement mark_seq_index(int k)
    {
        return new Judgement(op, left.mark_seq_index(k), right.mark_seq_index(k));
    }

    public Judgement attach_loop_queue(List<Integer> loop_queue)
    {
        return new Judgement(op, left.attach_loop_queue(loop_queue), right.attach_loop_queue(loop_queue));
    }

    public Set<String> extract_variables()
    {
        Set<String> result = new HashSet<>();
        result.addAll(left.extract_variables());
        result.addAll(right.extract_variables());
        return result;
    }

    public boolean is_task_judgement()
    {
        if (left instanceof UnaryExpr) {
            UnaryExpr ue = (UnaryExpr) left;
            return ue.get_op() == UnaryOp.TASK_TIME;
        }
        return false;
    }

    public void replace_variable(String source, String target)
    {
        left.replace_variable(source, target);
        right.replace_variable(source, target);
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
