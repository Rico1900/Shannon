package edu.nju.seg.model;

import edu.nju.seg.expression.Judgement;
import edu.nju.seg.expression.Variable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AutomatonDiagram extends Diagram {

    private final State initial;

    /** not include initial state **/
    private final List<State> states_exclude_initial;

    private Set<Variable> variables;

    private final List<Relation> relations;

    private final List<Judgement> properties;

    public AutomatonDiagram(String title,
                            List<Judgement> constraints,
                            State initial,
                            List<State> all_states_exclude_initial,
                            List<Relation> relations,
                            List<Judgement> properties)
    {
        super(title, constraints);
        this.initial = initial;
        this.states_exclude_initial = all_states_exclude_initial;
        this.relations = relations;
        this.properties = properties;
        init_variables();
    }

    private void init_variables()
    {
        if (variables == null) {
            this.variables = new HashSet<>();
        }
        for (State s: states_exclude_initial) {
            variables.addAll(s.getVariables());
        }
    }

    public State get_initial()
    {
        return initial;
    }

    public List<State> get_states_exclude_initial()
    {
        return states_exclude_initial;
    }

    public Set<Variable> get_variables()
    {
        return variables;
    }

    public Set<String> get_var_str()
    {
        return variables.stream()
                .map(Variable::getName)
                .collect(Collectors.toSet());
    }

    public List<Relation> get_relations()
    {
        return relations;
    }

    public List<Judgement> get_properties()
    {
        return properties;
    }

    public List<State> get_all_states()
    {
        List<State> set = new ArrayList<>(states_exclude_initial);
        set.add(initial);
        return set;
    }

}
