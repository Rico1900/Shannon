package edu.nju.seg.solver.model;

import edu.nju.seg.model.Event;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class Instant extends Node {

    private String prefix;

    private String name;

    public Instant(Event e,
                   String prefix)
    {
        this.name = e.getName();
        this.prefix = prefix;
    }

    public String getFullName()
    {
        return prefix + name;
    }

}
