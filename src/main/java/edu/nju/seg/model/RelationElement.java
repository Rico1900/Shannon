package edu.nju.seg.model;

import edu.nju.seg.util.Pair;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class RelationElement extends Element {

    private static int padding = 10;

    private int sourceX;

    private int sourceY;

    private int targetX;

    private int targetY;

    public Pair<Integer, Integer> getSource()
    {
        Pair<Integer, Integer> pair = new Pair<>();
        pair.setLeft(x + sourceX);
        pair.setRight(y + sourceY);
        return pair;
    }

    public Pair<Integer, Integer> getTarget()
    {
        Pair<Integer, Integer> pair = new Pair<>();
        pair.setLeft(x + targetX);
        pair.setRight(y + targetY);
        return pair;
    }

}
