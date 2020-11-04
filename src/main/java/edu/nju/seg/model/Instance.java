package edu.nju.seg.model;

import java.util.Objects;

public class Instance {

    private String name;

    private String variable;

    public Instance(String name, String variable)
    {
        this.name = name;
        this.variable = variable;
    }

    public String get_name()
    {
        return name;
    }

    public String get_variable()
    {
        return variable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Instance instance = (Instance) o;
        return name.equals(instance.name) &&
                variable.equals(instance.variable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, variable);
    }

}
