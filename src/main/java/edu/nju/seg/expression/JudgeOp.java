package edu.nju.seg.expression;

public enum JudgeOp {

    EQ, LT, GT, LE, GE;

    public JudgeOp revert() {
        if (this == LT) {
            return GT;
        } else if (this == GT) {
            return LT;
        } else if (this == LE) {
            return GE;
        } else if (this == GE) {
            return LE;
        } else {
            return EQ;
        }
    }

}
