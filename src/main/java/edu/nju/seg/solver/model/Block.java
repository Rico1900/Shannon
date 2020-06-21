package edu.nju.seg.solver.model;

import edu.nju.seg.util.Pair;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class Block extends Node {

    private Pair<Node, Node> subs;

    public Block(Node l, Node r)
    {
        this.subs = new Pair<>(l, r);
    }

}
