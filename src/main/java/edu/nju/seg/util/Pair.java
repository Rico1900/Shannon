package edu.nju.seg.util;

import java.util.Objects;

public class Pair<L, R> {

    private final L left;

    private final R right;

    public Pair(L left, R right)
    {
        this.left = left;
        this.right = right;
    }

    public L get_left() {
        return left;
    }

    public R get_right() {
        return right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return left.equals(pair.left) &&
                right.equals(pair.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

}
