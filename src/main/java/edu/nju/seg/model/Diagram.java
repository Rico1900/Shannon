package edu.nju.seg.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Diagram {

    protected String title;

    protected List<String> constraints;

}
