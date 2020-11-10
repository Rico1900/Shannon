package edu.nju.seg.parser;

import edu.nju.seg.expression.DeEquation;
import edu.nju.seg.expression.Number;
import edu.nju.seg.expression.UnaryOp;
import edu.nju.seg.expression.Variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquationParser {

    /**
     * parse simple differential equations
     * @param equations equations
     * @return equation map
     */
    public static Map<String, Double> parseEquations(List<DeEquation> equations)
    {
        Map<String, Double> map = new HashMap<>();
        equations.stream()
                .filter(e -> e.get_left().get_op() == UnaryOp.DIFFERENTIAL)
                .filter(e -> e.get_right() instanceof Number)
                .forEach(e -> {
                   String v = ((Variable) e.get_left().get_expr()).getName();
                   Double d = ((Number) e.get_right()).getValue();
                   map.put(v, d);
                });
        return map;
    }

}
