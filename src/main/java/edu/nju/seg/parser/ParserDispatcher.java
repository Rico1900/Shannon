package edu.nju.seg.parser;

import edu.nju.seg.model.Element;
import edu.nju.seg.util.Pair;
import edu.nju.seg.exception.ParseException;
import edu.nju.seg.model.UMLType;
import edu.nju.seg.util.$;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserDispatcher {

    private static final Pattern SD_PATTERN = Pattern.compile("^.*_sd\\.uxf$");

    private static final Pattern HA_PATTERN = Pattern.compile("^.*_ha\\.uxf$");

    /**
     * dispatch the contents to the appropriate parser,
     * according to the file name
     * @param fn the file name
     * @param contents the UMLet model language
     */
    public static DiagramParser dispatch_uxf(String fn,
                                             List<Element> contents)
    {
        if (SD_PATTERN.matcher(fn).matches()) {
            Pair<Element, List<Element>> p = partition(contents);
            return new SDParser(p.get_right(), p.get_left());
        } else if (HA_PATTERN.matcher(fn).matches()) {
            return new AutomatonParser(fn, contents);
        } else  {
            throw new ParseException("Wrong uxf file: " + fn + ", please check the file name.");
        }
    }

    /**
     * partition elements in an uxf file
     * @param contents the elements in the uxf file
     * @return a tuple: (sequence diagram element, note elements)
     */
    private static Pair<Element, List<Element>> partition(List<Element> contents)
    {
        List<Element> notes = new ArrayList<>();
        Element sd = null;
        for (Element e: contents) {
            if (e.getType() == UMLType.UMLNote) {
                notes.add(e);
            }
            if (e.getType() == UMLType.UMLSequenceAllInOne) {
                sd = e;
            }
        }
        return new Pair<>(sd, notes);
    }

    /**
     * check if the contents belong to a sequence diagram
     * @param contents the element list from the XML file
     * @return if contents belong to a sequence diagram
     */
    private static boolean is_sequence_diagram(List<Element> contents)
    {
        if (contents.size() == 3) {
            boolean hasNote = false;
            boolean hasSequence = false;
            for (Element e: contents) {
                if (e.getType() == UMLType.UMLNote) {
                    hasNote = true;
                }
                if (e.getType() == UMLType.UMLSequenceAllInOne) {
                    hasSequence = true;
                }
            }
            return hasNote & hasSequence;
        } else {
            return false;
        }
    }

    /**
     * check if the contents belong to a automaton diagram
     * @param contents the element list from the XML file
     * @return if the contents belong to a automaton diagram
     */
    private static boolean is_hybrid_automaton(List<Element> contents)
    {
        if ($.isBlankList(contents)) {
            return false;
        }
        if (contents.size() > 2) {
            boolean hasInit = false;
            boolean hasState = false;
            for (Element e: contents) {
                if (e.getType() == UMLType.UMLSpecialState) {
                    hasInit = true;
                }
                if (e.getType() == UMLType.UMLState && e.getContent().contains("valign")) {
                    hasState = true;
                }
            }
            return hasInit && hasState;
        } else {
            return false;
        }
    }

    /**
     * judge if the graph is a message graph
     * @param contents the elements
     * @return if the contents form a message graph
     */
    private static boolean is_MSG(List<Element> contents)
    {
        if ($.isBlankList(contents)) {
            return false;
        }
        if (contents.size() > 2) {
            boolean hasInit = false;
            boolean hasState = false;
            for (Element e: contents) {
                if (e.getType() == UMLType.UMLSpecialState) {
                    hasInit = true;
                }
                if (e.getType() == UMLType.UMLState) {
                    hasState = true;
                }
            }
            return hasInit && hasState;
        } else {
            return false;
        }
    }

}
