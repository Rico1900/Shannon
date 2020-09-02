package edu.nju.seg.solver;

import com.microsoft.z3.Optimize;
import edu.nju.seg.exception.EncodeException;
import edu.nju.seg.exception.Z3Exception;
import edu.nju.seg.model.*;
import edu.nju.seg.parser.ExprParser;
import edu.nju.seg.util.$;
import edu.nju.seg.util.Z3Wrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptimizeEncoder {

    private final SequenceDiagram sd;

    private final List<AutomatonDiagram> list;

    private final OptimizeManager om;

    private final int bound;

    private final List<IntFragment> flow;

    /** the container fragment without int fragment **/
    private final Fragment clean;

    private final Z3Wrapper w;

    private final ExprParser p;

    public OptimizeEncoder(SequenceDiagram sd,
                           List<AutomatonDiagram> list,
                           OptimizeManager om,
                           int bound)
    {
        this.sd = sd;
        this.list = list;
        this.om = om;
        this.bound = bound;
        this.clean = sd.getCleanFragment();
        this.flow = sd.getIntFragments();
        this.w = new Z3Wrapper(om.getContext());
        this.p = new ExprParser(om.getContext());
    }

    public void optimize() throws Z3Exception
    {
        // set the optimization goal
        Optimize.Handle handle = encodeSingleGoal();
        om.checkPoint();
        // visit all path
        List<List<Event>> collector = generateCleanPaths();
        Map<IntFragment, List<List<Event>>> map = generateIntPathMap();

    }

    private List<List<Event>> generateCleanPaths()
    {
        List<List<Event>> collector = new ArrayList<>();
        generateCleanPathsRec(sd.getContainer().getChildren(), new ArrayList<>(), collector, true);
        return collector;
    }

    private Map<IntFragment, List<List<Event>>> generateIntPathMap()
    {
        Map<IntFragment, List<List<Event>>> map = new HashMap<>();
        for (IntFragment f: flow) {
            List<List<Event>> collector = new ArrayList<>();
            generateCleanPathsRec(f.getChildren(), new ArrayList<>(), collector, true);
            map.put(f, collector);
        }
        return map;
    }

    private void generateCleanPathsRec(List<SDComponent> children,
                                       List<Event> current,
                                       List<List<Event>> collector,
                                       boolean isOuter)
    {
        for (SDComponent c: children) {
            if (c instanceof Message) {
                Message m = (Message) c;
                current.add(m.getFrom());
                current.add(m.getTo());
            } else if (c instanceof AltFragment) {
                AltFragment af = (AltFragment) c;
                generateCleanPathsRec(af.getChildren(), current, collector, false);
                generateCleanPathsRec(af.getElseChildren(), new ArrayList<>(current), collector, false);
            } else if (c instanceof LoopFragment) {
                LoopFragment lf = (LoopFragment) c;
                int min = lf.getMin();
                int max = lf.getMax();
                for (int i = min; i <= max; i++) {
                    List<Event> checkPoint = new ArrayList<>(current);
                    for (int j = 0; j < i; j++) {
                        generateCleanPathsRec(lf.getChildren(), checkPoint, collector, false);
                    }
                }
            } else {
                throw new EncodeException("wrong component");
            }
        }
        if (isOuter) {
            collector.add(current);
        }
    }

    /**
     * encode single goal
     * @return optimization handle
     * @throws Z3Exception when the goal is wrong
     */
    private Optimize.Handle encodeSingleGoal() throws Z3Exception
    {
        List<Goal> goals = sd.getGoals();
        if ($.isNotBlankList(goals) && goals.size() == 1) {
            Goal g = goals.get(0);
            switch (g.getType()) {
                case MAX:
                    return om.mkMaximize(p.encodeOptimizeGoal(g.getExpression()));
                case MIN:
                    return om.mkMinimize(p.encodeOptimizeGoal(g.getExpression()));
                default:
                    throw new EncodeException("wrong goal: " + g);
            }
        } else {
            throw new EncodeException("wrong goals");
        }
    }

}
