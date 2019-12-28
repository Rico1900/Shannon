package edu.nju.seg.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class State {

    private StateType type;

    private String stateName;

    private boolean loop = false;

    private List<String> equations;

    private List<String> constraints;

    private List<Relation> outers;

    public List<State> getNext() {
        return outers.stream()
                .map(Relation::getTarget)
                .collect(Collectors.toList());
    }

    public void addEdge(Relation r) {
        if (outers == null) {
            outers = new ArrayList<>();
        }
        outers.add(r);
    }

    @Override
    public String toString() {
        return type.name() + ": " + stateName;
    }

}
