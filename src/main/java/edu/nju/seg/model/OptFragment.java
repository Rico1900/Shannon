package edu.nju.seg.model;

import java.util.List;

public class OptFragment extends Fragment {

    private String condition;

    public OptFragment(List<SDComponent> children, String condition) {
        super(children);
        this.condition = condition;
    }

}
