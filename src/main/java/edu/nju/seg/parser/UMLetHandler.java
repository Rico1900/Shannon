package edu.nju.seg.parser;

import edu.nju.seg.model.ElementContent;
import edu.nju.seg.model.UMLType;
import lombok.Getter;
import lombok.Setter;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

public class UMLetHandler extends DefaultHandler {

    @Getter
    @Setter
    private List<ElementContent> result;

    private boolean betweenDiagram;

    private boolean betweenElement;

    private boolean betweenId;

    private boolean betweenPanelAttributes;

    private String elementId;

    private String elementContent;

    public UMLetHandler() {
        this.result = new ArrayList<>();
        this.betweenDiagram = false;
        this.betweenElement = false;
    }


    @Override
    public void startElement(String uri,
                             String localName,
                             String qName,
                             Attributes attributes) {
        if (qName.equalsIgnoreCase("diagram")) {
            betweenDiagram = true;
        }

        if (qName.equalsIgnoreCase("element")) {
            betweenElement = true;
        }

        if (qName.equalsIgnoreCase("id")) {
            betweenId = true;
        }

        if (qName.equalsIgnoreCase("panel_attributes")) {
            betweenPanelAttributes = true;
        }
    }

    @Override
    public void characters(char[] ch, int start, int len) {
        if (betweenDiagram && betweenElement && betweenId) {
            elementId = new String(ch, start, len);
            betweenId = false;
        }

        if (betweenDiagram && betweenElement && betweenPanelAttributes) {
            elementContent = new String(ch, start, len);
            betweenPanelAttributes = false;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equalsIgnoreCase("diagram")) {
            betweenDiagram = false;
        }

        if (qName.equalsIgnoreCase("element")) {
            betweenElement = false;
            result.add(new ElementContent(UMLType.valueOf(elementId), elementContent));
        }

        if (qName.equalsIgnoreCase("id")) {
            betweenId = false;
        }

        if (qName.equalsIgnoreCase("panel_attributes")) {
            betweenPanelAttributes = false;
        }
    }

}
