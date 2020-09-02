package edu.nju.seg.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SequenceDiagram extends Diagram {

    private Fragment container;

    /**
     * the properties that need to be checked
     */
    private List<String> properties;

    private List<Goal> goals;

    private String start;

    private String end;

    private String symbol;

    private Fragment cleanFragment;

    private List<IntFragment> flow;

    public void initSymbol()
    {
        if (end != null
                && start != null
                && start.startsWith("s")
                && end.startsWith("w")) {
            if (start.substring(1).equals(end.substring(1))) {
                symbol = start.substring(1);
            }
        }
    }

    public Fragment getCleanFragment()
    {
        if (cleanFragment == null) {
            cleanFragment = extractIntFrag(getContainer());
        }
        return cleanFragment;
    }

    public List<IntFragment> getIntFragments()
    {
        if (flow == null) {
            cleanFragment = extractIntFrag(getContainer());
        }
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
            List<SDComponent> children = filterChildren(container.getChildren());
            if (container instanceof LoopFragment) {
                LoopFragment origin = (LoopFragment) container;
                LoopFragment result = new LoopFragment(origin.getMin(), origin.getMax(),
                        children, origin.getRaw());
                result.setCovered(container.getCovered());
                return result;
            } else if (container instanceof AltFragment) {
                AltFragment origin = (AltFragment) container;
                List<SDComponent> elseChildren = filterChildren(origin.getElseChildren());
                AltFragment result = new AltFragment(origin.getCondition(), origin.getAltCondition(),
                        children, elseChildren, origin.getRaw());
                result.setCovered(container.getCovered());
                return result;
            } else if (container instanceof OptFragment) {
                OptFragment origin = (OptFragment) container;
                OptFragment result = new OptFragment(children, origin.getCondition(), origin.getRaw());
                result.setCovered(container.getCovered());
                return result;
            } else {
                return new Fragment(children, container.getCovered(), "container");
            }
        }
    }

    /**
     * filter component list, extract all the int fragments
     * @param origin the original list
     * @return the list without int fragments
     */
    private List<SDComponent> filterChildren(List<SDComponent> origin)
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
