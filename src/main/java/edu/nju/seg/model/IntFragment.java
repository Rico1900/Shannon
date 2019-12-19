package edu.nju.seg.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class IntFragment extends Fragment {

    private int priority;

    private int min;

    private int max;

    private String instruction;

    private String name;

    public IntFragment(List<SDComponent> children,
                       int priority,
                       int min,
                       int max,
                       String instruction,
                       String name) {
        super(children);
        this.priority = priority;
        this.min = min;
        this.max = max;
        this.instruction = instruction;
        this.name = name;
    }

}
