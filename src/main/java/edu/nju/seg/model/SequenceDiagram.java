package edu.nju.seg.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SequenceDiagram {

    private String title;

    private Fragment container;

    private List<String> constraints;

}
