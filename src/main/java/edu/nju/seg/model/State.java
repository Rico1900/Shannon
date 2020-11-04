package edu.nju.seg.model;

import edu.nju.seg.expression.*;
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

    private Set<Variable> variables;

    private List<DeEquation> deEquations;

    private List<Judgement> constraints;

    private List<Relation> outers;

    public void setDeEquations(List<DeEquation> equations)
    {
        this.deEquations = equations;
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
        deEquations.stream()
                .map(DeEquation::get_left)
                .filter(l -> l.getOp() == UnaryOp.DIFFERENTIAL)
                .map(UnaryExpr::getExpr)
                .filter(e -> e instanceof Variable)
                .map(e -> (Variable) e)
                .forEach(variables::add);
    }

    public List<State> getNext()
    {
        return outers.stream()
                .map(Relation::get_target)
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
