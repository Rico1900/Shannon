package edu.nju.seg.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Relation {

    private String condition;

    private State source;

    private State target;

    @Override
    public String toString() {
        return source.getStateName() + " --> " + target.getStateName();
    }

}