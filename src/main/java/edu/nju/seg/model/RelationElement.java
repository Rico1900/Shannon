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
        return new Pair<>(x + sourceX, y + sourceY);
    }

    public Pair<Integer, Integer> getTarget()
    {
        return new Pair<>(x + targetX, y + targetY);
    }

}
