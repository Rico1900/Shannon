package edu.nju.seg.parser;

import edu.nju.seg.model.ElementContent;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class UMLetParser {

    /**
     * parse element from file
     * @param f the UMLet .uxf file
     * @return maybe the elements
     */
    public static Optional<List<ElementContent>> parseElement(File f) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            UMLetHandler handler = new UMLetHandler();
            parser.parse(f, handler);
            return Optional.of(handler.getResult());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.err.println(e.toString());
        }
        return Optional.empty();
    }

}
