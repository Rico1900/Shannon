package edu.nju.seg.model;

import edu.nju.seg.expression.Assignment;
import edu.nju.seg.expression.Judgement;

import java.util.List;

public class Relation {

    private State source;

    private State target;

    private final String name;

    private final List<Judgement> guards;

    private final List<Assignment> assignments;

    public Relation(String name,
                    List<Judgement> guards,
                    List<Assignment> assignments) {
        this.name = name;
        this.guards = guards;
        this.assignments = assignments;
    }

    public State get_source()
    {
        return source;
    }

    public State get_target()
    {
        return target;
    }

    public String get_name()
    {
        return name;
    }

    public List<Judgement> get_guards()
    {
        return guards;
    }

    public List<Assignment> get_assignments()
    {
        return assignments;
    }

    public void set_source(State source)
    {
        this.source = source;
    }

    public void set_target(State target)
    {
        this.target = target;
    }

    @Override
    public String toString()
    {
        return source.getStateName() + " --> " + target.getStateName();
    }

}
