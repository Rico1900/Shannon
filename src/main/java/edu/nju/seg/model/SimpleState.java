package edu.nju.seg.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class SimpleState {

    private StateType type;

    private String stateName;

    private boolean loop = false;

    private List<SimpleRelation> outers;

    public void addEdge(SimpleRelation r)
    {
        if (outers == null) {
            outers = new ArrayList<>();
        }
        outers.add(r);
    }

}
