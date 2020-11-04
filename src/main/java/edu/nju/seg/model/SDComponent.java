package edu.nju.seg.model;

import java.util.List;
import java.util.Set;

public abstract class SDComponent {

    public abstract Set<String> extract_variables();

    public abstract String get_name();

    public abstract Set<SDComponent> get_inside_nodes(List<Integer> loop_queue);

}
