package edu.nju.seg.model;

import edu.nju.seg.util.$;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Fragment extends SDComponent {

    /**
     * the children which belong to the fragment
     */
    protected List<SDComponent> children;

    /**
     * the instances covered by the
     */
    protected List<Instance> covered;

    /**
     * the unparsed fragment string, the outermost fragment is named "container"
     */
    protected String raw;

    protected VirtualNode virtual_head;

    protected VirtualNode virtual_tail;

    public Fragment(List<SDComponent> children,
                    List<Instance> covered,
                    String raw)
    {
        this.children = children;
        this.covered = covered;
        this.raw = raw;
        this.virtual_head = new VirtualNode("head_" + yield_raw_name());
        this.virtual_tail = new VirtualNode("tail_" + yield_raw_name());
    }

    public String yield_raw_name()
    {
        return "<" + this.raw + this.hashCode() + ">";
    }

    public String get_frag_tag(List<Integer> loop_queue)
    {
        return $.loop_queue_prefix(loop_queue) + yield_raw_name();
    }

    public void addChild(SDComponent component)
    {
        children.add(component);
    }

    public List<SDComponent> get_children()
    {
        return children;
    }

    public List<Instance> get_covered()
    {
        return covered;
    }

    public String get_raw()
    {
        return raw;
    }

    public VirtualNode get_virtual_head()
    {
        return virtual_head;
    }

    public VirtualNode get_virtual_tail()
    {
        return virtual_tail;
    }

    @Override
    public Set<String> extract_variables() {
        Set<String> result = new HashSet<>();
        for (SDComponent c: children) {
            if (c instanceof Message) {
                result.addAll(c.extract_variables());
            }
        }
        return result;
    }

    @Override
    public Set<SDComponent> get_inside_nodes(List<Integer> loop_queue) {
        Set<SDComponent> set = new HashSet<>();
        set.add(virtual_head.attach_loop_queue(loop_queue));
        set.add(virtual_tail.attach_loop_queue(loop_queue));
        for (SDComponent c: children) {
            set.addAll(c.get_inside_nodes(loop_queue));
        }
        return set;
    }

    @Override
    public String get_name() {
        return yield_raw_name();
    }

}
