package edu.nju.seg.model;

import edu.nju.seg.util.Pair;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class RelationElement extends Element {

    private int sourceX;

    private int sourceY;

    private int targetX;

    private int targetY;

    public Pair<Integer, Integer> getSource()
    {
        Pair<Integer, Integer> pair = new Pair<>();
        if(sourceX > targetX) {
            pair.setLeft(x + sourceX + targetX);
        } else {
            pair.setLeft(x + sourceX);
        }
        if (sourceY + targetY < h) {
            if (sourceY > targetY) {
                pair.setRight(y + sourceY + targetY);
            } else {
                pair.setRight(y + sourceY);
            }
        } else {
            pair.setRight(y + sourceY);
        }
        return pair;
    }

    public Pair<Integer, Integer> getTarget()
    {
        Pair<Integer, Integer> pair = new Pair<>();
        if (targetX > sourceX) {
            pair.setLeft(x + sourceX + targetX);
        } else {
            pair.setLeft(x + targetX);
        }
        if (sourceY + targetY < h) {
            if (targetY > sourceY) {
                pair.setRight(y + sourceY + targetY);
            } else {
                pair.setRight(y + targetY);
            }
        } else {
            pair.setRight(y + targetY);
        }
        return pair;
    }

}
