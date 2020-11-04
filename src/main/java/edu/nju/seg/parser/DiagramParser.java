package edu.nju.seg.parser;

import edu.nju.seg.model.Diagram;

public interface DiagramParser {

    /**
     * parse sequence diagram from the raw text
     * @return the sequence diagram
     */
    Diagram parse();

}
