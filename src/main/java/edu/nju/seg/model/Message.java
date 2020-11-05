package edu.nju.seg.model;

import edu.nju.seg.expression.Assignment;
import edu.nju.seg.util.$;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Message extends SDComponent {

    private final String name;

    private final List<Assignment> assignments;

    private final Assignment mask_instruction;

    private final Instance source;

    private final Instance target;

    private int source_index;

    private int target_index;

    public Message(String name,
                   List<Assignment> assignments,
                   Assignment mask_instruction,
                   Instance source,
                   Instance target)
    {
        this.name = name;
        this.assignments = assignments;
        this.mask_instruction = mask_instruction;
        this.source = source;
        this.target = target;
    }

    public String get_name()
    {
        return name;
    }

    public List<Assignment> get_assignments()
    {
        return assignments;
    }

    public Instance get_source()
    {
        return source;
    }

    public Instance get_target()
    {
        return target;
    }

    public Assignment get_mask_instruction()
    {
        return mask_instruction;
    }

    public int get_source_index() {
        return source_index;
    }

    public void set_source_index(int source_index) {
        this.source_index = source_index;
    }

    public int get_target_index() {
        return target_index;
    }

    public void set_target_index(int target_index) {
        this.target_index = target_index;
    }

    public Message attach_loop_queue(List<Integer> loop_queue)
    {
        Message m = new Message($.loop_queue_prefix(loop_queue) + name,
                assignments, mask_instruction, source, target);
        m.set_source_index(source_index);
        m.set_target_index(target_index);
        return m;
    }

    @Override
    public Set<String> extract_variables() {
        Set<String> result = new HashSet<>();
        result.add(name);
        return result;
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
        Message message = (Message) o;
        return source_index == message.source_index &&
                target_index == message.target_index &&
                name.equals(message.name) &&
                Objects.equals(assignments, message.assignments) &&
                Objects.equals(mask_instruction, message.mask_instruction) &&
                source.equals(message.source) &&
                target.equals(message.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, assignments, mask_instruction, source, target, source_index, target_index);
    }

}
