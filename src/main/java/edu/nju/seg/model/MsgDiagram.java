package edu.nju.seg.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class MsgDiagram extends Diagram {

    private SimpleState initial;

    private SimpleState end;

    /** not include initial state **/
    private List<SimpleState> allStates;

    private List<SimpleRelation> allRelations;

    private List<String> global;

}
