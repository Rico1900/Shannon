package edu.nju.seg.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class LoopFragment extends Fragment {

    private int min;

    private int max;

    public LoopFragment(int min, int max, List<SDComponent> children, String raw)
    {
        super(children, new ArrayList<>(), raw);
        this.min = min;
        this.max = max;
    }

}
