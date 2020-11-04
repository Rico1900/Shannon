package edu.nju.seg.model;

import edu.nju.seg.expression.Assignment;
import edu.nju.seg.util.$;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LoopFragment extends Fragment {

    private int min;

    private int max;

    private Assignment interval;

    private Assignment start;

    public LoopFragment(List<SDComponent> children,
                        List<Instance> covered,
                        String raw,
                        int min,
                        int max)
    {
        super(children, covered, raw);
        this.min = min;
        this.max = max;
    }

    public LoopFragment(List<SDComponent> children,
                        List<Instance> covered,
                        String raw,
                        int min,
                        int max,
                        Assignment interval,
                        Assignment start)
    {
        super(children, covered, raw);
        this.min = min;
        this.max = max;
        this.interval = interval;
        this.start = start;
    }

    public int get_min()
    {
        return min;
    }

    public int get_max()
    {
        return max;
    }

    public Assignment get_interval()
    {
        return interval;
    }

    public Assignment get_start()
    {
        return start;
    }

    @Override
    public Set<SDComponent> get_inside_nodes(List<Integer> loop_queue) {
        Set<SDComponent> set = new HashSet<>();
        set.add(virtual_head.attach_loop_queue(loop_queue));
        set.add(virtual_tail.attach_loop_queue(loop_queue));
        for (int i = 0; i < max; i++) {
            List<Integer> next = $.addToList(loop_queue, i);
            for (SDComponent c: children) {
                set.addAll(c.get_inside_nodes(next));
            }
        }
        return set;
    }
}
