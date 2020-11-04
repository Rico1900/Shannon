package edu.nju.seg.model;

import edu.nju.seg.expression.Judgement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AltFragment extends Fragment {

    private Judgement condition;

    private Judgement else_condition;

    /** else fragment children **/
    private List<SDComponent> else_children;

    public AltFragment(List<SDComponent> children,
                       List<Instance> covered,
                       String raw,
                       List<SDComponent> else_children)
    {
        super(children, covered, raw);
        this.else_children = else_children;
        this.condition = null;
        this.else_condition = null;
    }

    public AltFragment(List<SDComponent> children,
                       List<Instance> covered,
                       String raw,
                       Judgement condition,
                       Judgement else_condition,
                       List<SDComponent> else_children)
    {
        super(children, covered, raw);
        this.condition = condition;
        this.else_condition = else_condition;
        this.else_children = else_children;
    }

    public Judgement get_condition()
    {
        return condition;
    }

    public Judgement get_else_condition()
    {
        return else_condition;
    }

    public List<SDComponent> get_else_children()
    {
        return else_children;
    }

    public void add_to_else(SDComponent c)
    {
        else_children.add(c);
    }

    @Override
    public Set<String> extract_variables() {
        Set<String> result = new HashSet<>();
        for (SDComponent c: else_children) {
            result.addAll(c.extract_variables());
        }
        result.addAll(super.extract_variables());
        return result;
    }

    @Override
    public Set<SDComponent> get_inside_nodes(List<Integer> loop_queue) {
        Set<SDComponent> set = super.get_inside_nodes(loop_queue);
        for (SDComponent c: else_children) {
            set.addAll(c.get_inside_nodes(loop_queue));
        }
        return set;
    }
}
