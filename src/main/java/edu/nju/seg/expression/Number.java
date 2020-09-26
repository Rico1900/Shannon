package edu.nju.seg.expression;

import java.util.Objects;

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
}
