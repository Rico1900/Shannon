package edu.nju.seg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Element {

    protected UMLType type;

    /**
     * x coordinate
     */
    protected int x;

    /**
     * y coordinate
     */
    protected int y;

    /**
     * width
     */
    protected int w;

    /**
     * height
     */
    protected int h;

    protected String content;

}
