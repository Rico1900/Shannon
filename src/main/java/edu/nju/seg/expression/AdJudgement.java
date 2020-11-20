package edu.nju.seg.expression;

public class AdJudgement extends Judgement {

    private final UnaryOp qualifier;

    public AdJudgement(UnaryOp qualifier, Judgement j) {
        super(j.getOp(), j.getLeft(), j.getRight());
        this.qualifier = qualifier;
    }

    public UnaryOp get_qualifier()
    {
        return qualifier;
    }

    @Override
    public AdJudgement mark_seq_index(int k) {
        return new AdJudgement(qualifier, super.mark_seq_index(k));
    }
}
