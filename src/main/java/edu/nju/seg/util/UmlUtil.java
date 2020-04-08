package edu.nju.seg.util;

import edu.nju.seg.model.Element;
import edu.nju.seg.model.UMLType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UmlUtil {

    /**
     * pick up relation elements
     * @return relation elements
     */
    public static List<Element> partitionRelation(List<Element> elements)
    {
        return elements.stream()
                .filter(e -> e.getType() == UMLType.Relation)
                .collect(Collectors.toList());
    }

    /**
     * pick up state elements
     * @return state elements
     */
    public static List<Element> partitionState(List<Element> elements)
    {
        return elements.stream()
                .filter(e -> e.getType() == UMLType.UMLState)
                .collect(Collectors.toList());
    }

    /**
     * pick up special elements
     * @return special elements
     */
    public static List<Element> partitionSpecial(List<Element> elements)
    {
        return elements.stream()
                .filter(e -> e.getType() == UMLType.UMLSpecialState)
                .collect(Collectors.toList());
    }

    /**
     * construct location information for an element
     * @param e the element
     * @return the location information
     */
    public static List<Integer> consLocation(Element e)
    {
        List<Integer> loc = new ArrayList<>();
        loc.add(e.getX());
        loc.add(e.getX() + e.getW());
        loc.add(e.getY());
        loc.add(e.getY() + e.getH());
        return loc;
    }

    public static boolean inSquare(Pair<Integer, Integer> coord, List<Integer> square)
    {
        return coord.getLeft() >= square.get(0)
                && coord.getLeft() <= square.get(1)
                && coord.getRight() >= square.get(2)
                && coord.getRight() <= square.get(3);
    }

}
