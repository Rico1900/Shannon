package edu.nju.seg.parser;

import edu.nju.seg.model.Element;
import edu.nju.seg.model.Pair;
import edu.nju.seg.model.ParseException;
import edu.nju.seg.model.UMLType;
import edu.nju.seg.util.$;

import java.util.ArrayList;
import java.util.List;

public class ParserDispatcher {

    /**
     * dispatch the contents to the appropriate parser
     * @param fileName the file name
     * @param contents the UMLet model language
     */
    public static Parser dispatch(String fileName,
                                  List<Element> contents) {
        if (isSequenceDiagram(contents)) {
            Pair<Element, List<Element>> p = partition(contents);
            return new SequenceParser(p.getRight(), p.getLeft());
        } else if(isHybridAutomaton(contents)) {
            return new AutomatonParser(fileName, contents);
        } else  {
            throw new ParseException("wrong uxf file: " + fileName);
        }
    }

    /**
     * check if the contents belong to a sequence diagram
     * @param contents the element list from the XML file
     * @return if contents belong to a sequence diagram
     */
    private static boolean isSequenceDiagram(List<Element> contents) {
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

    private static Pair<Element, List<Element>> partition(List<Element> contents) {
        Pair<Element, List<Element>> result = new Pair<>();
        for (Element e: contents) {
            if (e.getType() == UMLType.UMLNote) {
                if (result.getRight() == null) {
                    result.setRight(new ArrayList<>());
                }
                result.getRight().add(e);
            }
            if (e.getType() == UMLType.UMLSequenceAllInOne) {
                result.setLeft(e);
            }
        }
        return result;
    }

    /**
     * check if the contents belong to a automaton diagram
     * @param contents the element list from the XML file
     * @return if the contents belong to a automaton diagram
     */
    private static boolean isHybridAutomaton(List<Element> contents) {
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
