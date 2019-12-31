package edu.nju.seg.model;

import java.util.List;

public class AltFragment extends Fragment {

    private String condition;

    private String altCondition;

    /** else fragment children **/
    private List<SDComponent> elseChildren;

    public AltFragment(String condition,
                       String altCondition,
                       List<SDComponent> children,
                       List<SDComponent> elseChildren) {
        super(children);
        this.condition = condition;
        this.altCondition = altCondition;
        this.elseChildren = elseChildren;
    }

    public void addToElse(SDComponent c) {
        elseChildren.add(c);
    }

}
