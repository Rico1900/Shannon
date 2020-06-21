package edu.nju.seg.model;

import edu.nju.seg.solver.model.Node;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class Sequel {

    private String label = "";

    private Map<Instance, List<Node>> sequel = new HashMap<>();

    public void init(List<Instance> covered)
    {
        for (Instance i: covered) {
            sequel.put(i, new ArrayList<>());
        }
    }

    public static Sequel clone(Sequel self)
    {
        Sequel c = new Sequel();
        c.setLabel(self.getLabel());
        c.setSequel(new HashMap<>(self.getSequel()));
        return c;
    }

}
