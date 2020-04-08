package edu.nju.seg.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SimpleRelation {

    private SimpleState source;

    private SimpleState target;

    @Override
    public String toString()
    {
        return source.getStateName() + " --> " + target.getStateName();
    }

}
