package edu.nju.seg.model;


import edu.nju.seg.expression.Judgement;

import java.util.ArrayList;
import java.util.List;

import static edu.nju.seg.config.Constants.OUTERMOST_FRAGMENT_NAME;

public class SequenceDiagram extends Diagram {

    private final Fragment container;

    /**
     * the properties that need to be checked
     */
    private final List<Judgement> properties;

    private final Fragment clean;

    private List<IntFragment> flow;

    public SequenceDiagram(String title,
                           List<Judgement> constraints,
                           Fragment container,
                           List<Judgement> properties) {
        super(title, constraints);
        this.container = container;
        this.properties = properties;
        this.clean = extractIntFrag(container);
    }

    public Fragment get_container()
    {
        return container;
    }

    public List<Judgement> get_properties() {
        return properties;
    }

    public Fragment get_clean()
    {
        return clean;
    }

    public List<IntFragment> get_flow()
    {
        return flow;
    }

    /**
     * extract interrupt fragment from the original diagram
     * @param container the container fragment
     */
    private Fragment extractIntFrag(Fragment container)
    {
        if (flow == null) {
            flow = new ArrayList<>();
        }
        if (container instanceof IntFragment) {
            flow.add((IntFragment) container);
            return null;
        } else {
            List<SDComponent> children = filter_children(container.get_children());
            List<Instance> covered = container.get_covered();
            String raw = container.get_raw();
            if (container instanceof LoopFragment) {
                LoopFragment origin = (LoopFragment) container;
                return new LoopFragment(children, covered, raw, origin.get_min(), origin.get_max(),
                        origin.get_interval(), origin.get_start());
            } else if (container instanceof AltFragment) {
                AltFragment origin = (AltFragment) container;
                List<SDComponent> elseChildren = filter_children(origin.get_else_children());
                return new AltFragment(children, covered, raw, origin.get_condition(),
                        origin.get_else_condition(), elseChildren);
            } else if (container instanceof OptFragment) {
                OptFragment origin = (OptFragment) container;
                return new OptFragment(children, covered, raw, origin.get_condition());
            } else {
                return new Fragment(children, container.get_covered(), OUTERMOST_FRAGMENT_NAME);
            }
        }
    }

    /**
     * filter component list, extract all the int fragments
     * @param origin the original list
     * @return the list without int fragments
     */
    private List<SDComponent> filter_children(List<SDComponent> origin)
    {
        List<SDComponent> children = new ArrayList<>();
        for (SDComponent child: origin) {
            if (child instanceof Message) {
                children.add(child);
            } else {
                Fragment sub = extractIntFrag((Fragment) child);
                if (sub != null) {
                    children.add(sub);
                }
            }
        }
        return children;
    }

}
