package edu.nju.seg.model;

import edu.nju.seg.expression.Judgement;

import java.util.List;

public class Diagram {

    protected String title;

    /**
     * the constraints that describe the diagram
     */
    protected List<Judgement> constraints;

    public Diagram(String title, List<Judgement> constraints) {
        this.title = title;
        this.constraints = constraints;
    }

    public String getTitle() {
        return title;
    }

    public List<Judgement> getConstraints() {
        return constraints;
    }
}
