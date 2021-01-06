package edu.nju.seg.expression;


import java.util.List;
import java.util.Set;

public abstract class Expr {

    public abstract Expr attach_bound(int k);

    public abstract Expr mark_seq_index(int k);

    public abstract Expr attach_loop_queue(List<Integer> loop_queue);

    public abstract Set<String> extract_variables();

    public abstract Expr replace_variable(String source, String target);

}
