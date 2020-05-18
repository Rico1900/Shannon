package edu.nju.seg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fragment extends SDComponent {

    /**
     * the children which belong to the fragment
     */
    protected List<SDComponent> children;

    /**
     * the instances covered by the
     */
    protected List<Instance> covered;

    protected Event virtualHead;

    protected Event virtualTail;

    protected String raw;

    public Fragment(List<SDComponent> children, List<Instance> covered, String raw)
    {
        this.children = children;
        this.covered = covered;
        this.raw = raw;
        this.virtualHead = new Event("virtual_head_" + yieldMark(),
                null, null, true);
        this.virtualTail = new Event("virtual_tail_" + yieldMark(),
                null, null, true);
    }

    private String yieldMark()
    {
        return "<" + this.raw + ">";
    }

    public void addChild(SDComponent component)
    {
        children.add(component);
    }

    public void addInstance(Instance i)
    {
        covered.add(i);
    }

}
