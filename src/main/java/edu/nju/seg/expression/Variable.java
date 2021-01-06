package edu.nju.seg.expression;

import edu.nju.seg.util.$;

import java.util.*;

public class Variable extends Expr {

    private String name;

    public Variable(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variable variable = (Variable) o;
        return name.equals(variable.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Variable attach_bound(int k) {
        return new Variable(name + "_" + k);
    }

    @Override
    public Expr mark_seq_index(int k) {
        return new Variable(k + "_" + name);
    }

    @Override
    public Expr attach_loop_queue(List<Integer> loop_queue) {
        return new Variable($.loop_queue_prefix(loop_queue) + name);
    }

    @Override
    public Set<String> extract_variables() {
        Set<String> result = new HashSet<>();
        result.add(name);
        return result;
    }

    @Override
    public Variable replace_variable(String source, String target) {
        if (name.equals(source)) {
            return new Variable(target);
        } else {
            return this;
        }
    }

}
