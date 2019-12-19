package edu.nju.seg.model;

import java.util.List;

public class AltFragment extends Fragment {

    private String condition;

    /** else fragment children **/
    private List<SDComponent> elseChildren;

    public AltFragment(String condition,
                       List<SDComponent> children,
                       List<SDComponent> elseChildren) {
        super(children);
        this.condition = condition;
        this.elseChildren = elseChildren;
    }

}
