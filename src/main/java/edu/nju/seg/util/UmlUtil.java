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
    public static List<Element> pickup_relation(List<Element> elements)
    {
        return elements.stream()
                .filter(e -> e.getType() == UMLType.Relation)
                .collect(Collectors.toList());
    }

    /**
     * pick up state elements
     * @return the state elements
     */
    public static List<Element> pickup_state(List<Element> elements)
    {
        return elements.stream()
                .filter(e -> e.getType() == UMLType.UMLState)
                .collect(Collectors.toList());
    }

    /**
     * pick up special elements
     * @return the special elements
     */
    public static List<Element> pickup_special(List<Element> elements)
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
    public static List<Element> pick_UML_notes(List<Element> elements)
    {
        return elements.stream()
                .filter(e -> e.getType() == UMLType.UMLNote)
                .collect(Collectors.toList());
    }

    /**
     * parse location according to an element
     * @param e the element
     * @return the location
     */
    public static List<Integer> parse_location(Element e)
    {
        List<Integer> loc = new ArrayList<>();
        loc.add(e.getX());
        loc.add(e.getX() + e.getW());
        loc.add(e.getY());
        loc.add(e.getY() + e.getH());
        return loc;
    }

    public static boolean in_square(Pair<Integer, Integer> coord,
                                    List<Integer> square)
    {
        return coord.get_left() >= square.get(0)
                && coord.get_left() <= square.get(1)
                && coord.get_right() >= square.get(2)
                && coord.get_right() <= square.get(3);
    }

}
