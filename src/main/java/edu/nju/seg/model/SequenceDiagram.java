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
public class SequenceDiagram extends Diagram {

    private Fragment container;

    /**
     * the properties that need to be checked
     */
    private List<String> properties;

    private List<Goal> goals;

    private String start;

    private String end;

    private String symbol;

    public void initSymbol()
    {
        if (end != null
                && start != null
                && start.startsWith("s")
                && end.startsWith("w")) {
            if (start.substring(1).equals(end.substring(1))) {
                symbol = start.substring(1);
            }
        }
    }
}
