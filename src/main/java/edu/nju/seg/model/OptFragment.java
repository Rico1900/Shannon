package edu.nju.seg.model;

import edu.nju.seg.expression.Judgement;

import java.util.List;

public class OptFragment extends Fragment {

    private Judgement condition;

    public OptFragment(List<SDComponent> children,
                       List<Instance> covered,
                       String raw)
    {
        super(children, covered, raw);
    }

    public OptFragment(List<SDComponent> children,
                       List<Instance> covered,
                       String raw,
                       Judgement condition)
    {
        super(children, covered, raw);
        this.condition = condition;
    }

    public Judgement get_condition()
    {
        return condition;
    }

}
