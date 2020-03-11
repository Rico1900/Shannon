package edu.nju.seg.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class OptFragment extends Fragment {

    private String condition;

    public OptFragment(List<SDComponent> children, String condition, String raw)
    {
        super(children, new ArrayList<>(), raw);
        this.condition = condition;
    }

}
