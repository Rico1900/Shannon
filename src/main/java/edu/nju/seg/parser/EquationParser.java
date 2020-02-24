package edu.nju.seg.parser;

import edu.nju.seg.model.ParseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquationParser {

    /**
     * parse simple differential equations
     * @param equations equations
     * @return equation map
     */
    public static Map<String, Double> parseEquations(List<String> equations) {
        Map<String, Double> map = new HashMap<>();
        for (String e: equations) {
            if (e.contains("=")) {
                String[] splits = e.split("=");
                map.put(splits[0].trim().replace("\'", ""), Double.valueOf(splits[1].trim()));
            } else {
                throw new ParseException("wrong equations: " + e);
            }
        }
        return map;
    }

}
