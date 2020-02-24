package edu.nju.seg.parser;

import edu.nju.seg.model.Element;
import edu.nju.seg.model.ParseException;
import edu.nju.seg.model.UMLType;

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
            return new SequenceParser(contents.get(0), contents.get(1));
        } else if(isHybridAutomaton(contents)) {
            return new AutomatonParser(fileName, contents);
        } else  {
            throw new ParseException("wrong uxf file: " + fileName);
        }
    }

    /**
     * check if the contents belong to a sequence diagram; this method has effects
     * @param contents the element list from the XML file
     * @return if contents belong to a sequence diagram
     */
    private static boolean isSequenceDiagram(List<Element> contents) {
        if (contents.size() == 2) {
            if (contents.get(0).getType() == UMLType.UMLNote
                    && contents.get(1).getType() == UMLType.UMLSequenceAllInOne) {
                return true;
            } else if (contents.get(0).getType() == UMLType.UMLSequenceAllInOne
                    && contents.get(1).getType() == UMLType.UMLNote) {
                Element temp = contents.get(0);
                contents.set(0, contents.get(1));
                contents.set(1, temp);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * check if the contents belong to a automaton diagram
     * @param contents the element list from the XML file
     * @return if the contents belong to a automaton diagram
     */
    private static boolean isHybridAutomaton(List<Element> contents) {
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
