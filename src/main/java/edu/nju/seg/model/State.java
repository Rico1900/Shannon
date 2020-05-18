package edu.nju.seg.model;

import edu.nju.seg.exception.ParseException;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class State {

    private StateType type;

    private String stateName;

    private boolean loop = false;

    private Set<String> variables;

    private List<String> equations;

    private List<String> constraints;

    private List<Relation> outers;

    public void setEquations(List<String> equations)
    {
        this.equations = equations;
        calVariables();
    }

    /**
     * summary variables from equations
     */
    private void calVariables()
    {
        if (variables == null) {
            variables = new HashSet<>();
        }
        for (String e: equations) {
            if (e.contains("=")) {
                String v = e.split("=")[0].trim();
                variables.add(v.replace("\'", ""));
            } else {
                throw new ParseException("wrong equation: " + e);
            }
        }
    }

    public List<State> getNext()
    {
        return outers.stream()
                .map(Relation::getTarget)
                .collect(Collectors.toList());
    }

    public void addEdge(Relation r)
    {
        if (outers == null) {
            outers = new ArrayList<>();
        }
        outers.add(r);
    }

    @Override
    public String toString()
    {
        return type.name() + ": " + stateName;
    }

}
