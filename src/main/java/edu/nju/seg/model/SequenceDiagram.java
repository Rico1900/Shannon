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

}
