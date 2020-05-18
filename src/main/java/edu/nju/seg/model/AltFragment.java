package edu.nju.seg.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class AltFragment extends Fragment {

    private String condition;

    private String altCondition;

    /** else fragment children **/
    private List<SDComponent> elseChildren;

    public AltFragment(String condition,
                       String altCondition,
                       List<SDComponent> children,
                       List<SDComponent> elseChildren,
                       String raw)
    {
        super(children, new ArrayList<>(), raw);
        this.condition = condition;
        this.altCondition = altCondition;
        this.elseChildren = elseChildren;
    }

    public void addToElse(SDComponent c)
    {
        elseChildren.add(c);
    }

}
