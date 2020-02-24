package edu.nju.seg.solver;

import com.microsoft.z3.Context;
import edu.nju.seg.model.SequenceDiagram;

public class SequenceEncoder {

    private SequenceDiagram diagram;

    private SolverManager manager;

    private Context ctx;

    private int bound;

    public SequenceEncoder(SequenceDiagram diagram,
                           SolverManager manager,
                           int bound) {
        this.diagram = diagram;
        this.manager = manager;
        this.ctx = manager.getContext();
        this.bound = bound;
    }



}
