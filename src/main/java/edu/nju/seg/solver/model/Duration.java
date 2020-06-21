package edu.nju.seg.solver.model;

import edu.nju.seg.model.Message;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class Duration extends Node {

    private String name;

    private String prefix;

    public Duration(Message m,
                    String prefix)
    {
        this.name = m.getName();
        this.prefix = prefix;
    }

}
