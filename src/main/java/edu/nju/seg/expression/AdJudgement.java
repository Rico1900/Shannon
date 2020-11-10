package edu.nju.seg.expression;

public class AdJudgement extends Judgement {

    private final UnaryOp op;

    public AdJudgement(UnaryOp op, Judgement j) {
        super(j.getOp(), j.getLeft(), j.getRight());
        this.op = op;
    }

    public UnaryOp get_op()
    {
        return op;
    }

}
