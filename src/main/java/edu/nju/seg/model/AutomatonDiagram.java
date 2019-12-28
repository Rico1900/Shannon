package edu.nju.seg.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class AutomatonDiagram extends Diagram {

    private State initial;

    private List<State> allStates;

    private List<Relation> allRelations;

}
