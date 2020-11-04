package edu.nju.seg.model;

import edu.nju.seg.expression.Assignment;
import edu.nju.seg.util.$;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IntFragment extends Fragment {

    private int priority = 0;

    private int min = 1;

    private int max = 1;

    private Assignment instruction = null;

    private String mask_var = null;

    public IntFragment(List<SDComponent> children,
                       List<Instance> covered,
                       String raw,
                       int priority)
    {
        super(children, covered, raw);
        this.priority = priority;
    }

    public IntFragment(List<SDComponent> children,
                       List<Instance> covered,
                       String raw,
                       int priority,
                       int min,
                       int max)
    {
        super(children, covered, raw);
        this.priority = priority;
        this.min = min;
        this.max = max;
    }

    public IntFragment(List<SDComponent> children,
                       List<Instance> covered,
                       String raw,
                       int priority,
                       int min,
                       int max,
                       Assignment instruction)
    {
        super(children, covered, raw);
        this.priority = priority;
        this.min = min;
        this.max = max;
        this.instruction = instruction;
        init_var();
    }

    private void init_var()
    {
        mask_var = instruction.getLeft().getName();
    }

    public int get_priority()
    {
        return priority;
    }

    public int get_min()
    {
        return min;
    }

    public int get_max()
    {
        return max;
    }

    public Assignment get_instruction()
    {
        return instruction;
    }

    public String get_mask_var()
    {
        return mask_var;
    }

    @Override
    public Set<SDComponent> get_inside_nodes(List<Integer> loop_queue) {
        Set<SDComponent> set = new HashSet<>();
        for (int i = 0; i < max; i++) {
            List<Integer> next = $.addToList(loop_queue, i);
            set.add(virtual_head.attach_loop_queue(next));
            set.add(virtual_tail.attach_loop_queue(next));
            for (SDComponent c: children) {
                set.addAll(c.get_inside_nodes(next));
            }
        }
        return set;
    }
}
