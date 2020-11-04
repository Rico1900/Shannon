package edu.nju.seg.expression;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Number extends Expr {

    private final Double value;

    public Number(Double value)
    {
        this.value = value;
    }

    public Double getValue()
    {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Number number = (Number) o;
        return value.equals(number.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public Expr attach_bound(int k) {
        return this;
    }

    @Override
    public Expr attach_loop_queue(List<Integer> loop_queue) {
        return this;
    }

    @Override
    public Set<String> extract_variables() {
        return new HashSet<>(0);
    }

}
