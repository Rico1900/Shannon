package edu.nju.seg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fragment extends SDComponent {

    /**
     * the children which belong to the fragment
     */
    protected List<SDComponent> children;

    public void addChild(SDComponent component) {
        children.add(component);
    }

}
