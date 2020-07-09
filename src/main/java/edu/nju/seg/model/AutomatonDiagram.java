package edu.nju.seg.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class AutomatonDiagram extends Diagram {

    private State initial;

    /** not include initial state **/
    private List<State> allStates;

    private Set<String> allVar;

    private List<Relation> allRelations;

    public void setAllStates(List<State> allStates)
    {
        this.allStates = allStates;
        if (allVar == null) {
            this.allVar = new HashSet<>();
        }
        for (State s: allStates) {
            allVar.addAll(s.getVariables());
        }
    }

    public List<State> getAllStatesIncludeInit()
    {
        List<State> set = new ArrayList<>(getAllStates());
        set.add(initial);
        return set;
    }

}
