package edu.nju.seg.util;

import edu.nju.seg.model.Element;
import edu.nju.seg.model.UMLType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UmlUtil {

    /**
     * pick up relation elements
     * @return the relation elements
     */
    public static List<Element> pickupRelation(List<Element> elements)
    {
        return elements.stream()
                .filter(e -> e.getType() == UMLType.Relation)
                .collect(Collectors.toList());
    }

    /**
     * pick up state elements
     * @return the state elements
     */
    public static List<Element> pickupState(List<Element> elements)
    {
        return elements.stream()
                .filter(e -> e.getType() == UMLType.UMLState)
                .collect(Collectors.toList());
    }

    /**
     * pick up special elements
     * @return the special elements
     */
    public static List<Element> pickupSpecial(List<Element> elements)
    {
        return elements.stream()
                .filter(e -> e.getType() == UMLType.UMLSpecialState)
                .collect(Collectors.toList());
    }

    /**
     * pick up uml notes
     * @param elements the whole elements
     * @return the uml notes
     */
    public static List<Element> pickUmlNote(List<Element> elements)
    {
        return elements.stream()
                .filter(e -> e.getType() == UMLType.UMLNote)
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
