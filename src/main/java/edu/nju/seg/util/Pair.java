package edu.nju.seg.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pair<L, R> {

    private L l;

    private R r;

    public L getLeft()
    {
        return l;
    }

    public R getRight()
    {
        return r;
    }

    public void setLeft(L l)
    {
        this.l = l;
    }

    public void setRight(R r)
    {
        this.r = r;
    }

    public String unpackToStr()
    {
        return l.toString() + "-" + r.toString();
    }
}
