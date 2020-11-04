package edu.nju.seg.model;

import edu.nju.seg.util.$;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class VirtualNode extends SDComponent {

    private final String name;

    public VirtualNode(String name)
    {
        this.name = "virtual_" + name;
    }

    public String get_name()
    {
        return name;
    }

    public VirtualNode attach_loop_queue(List<Integer> loop_queue)
    {
        return new VirtualNode($.loop_queue_prefix(loop_queue) + name);
    }

    @Override
    public Set<String> extract_variables() {
        return new HashSet<>(0);
    }

    @Override
    public Set<SDComponent> get_inside_nodes(List<Integer> loop_queue) {
        Set<SDComponent> set = new HashSet<>();
        set.add(attach_loop_queue(loop_queue));
        return set;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirtualNode that = (VirtualNode) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

}
