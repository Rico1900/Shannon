package edu.nju.seg.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class IntFragment extends Fragment {

    private int priority = 0;

    private int min = 1;

    private int max = 1;

    private String instruction;

    private String name;

    private String maskVar;

    public IntFragment(List<SDComponent> children,
                       int priority,
                       int min,
                       int max,
                       String instruction,
                       String name,
                       String raw)
    {
        super(children, new ArrayList<>(), raw);
        this.priority = priority;
        this.min = min;
        this.max = max;
        this.instruction = instruction;
        this.name = name;
        init();
    }

    private void init()
    {
        if (instruction != null) {
            String[] split = instruction.split("=");
            maskVar = split[0].trim();
        }
    }

}
