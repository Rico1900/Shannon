package edu.nju.seg.parser;

import edu.nju.seg.model.Element;
import edu.nju.seg.model.RelationElement;
import edu.nju.seg.model.UMLType;
import edu.nju.seg.util.$;
import lombok.Getter;
import lombok.Setter;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

public class UMLetHandler extends DefaultHandler {

    @Getter
    @Setter
    private List<Element> result;

    private boolean betweenDiagram = false;

    private boolean betweenElement = false;

    private boolean betweenId = false;

    private boolean betweenPanelAttributes = false;

    private boolean betweenX = false;

    private boolean betweenY = false;

    private boolean betweenW = false;

    private boolean betweenH = false;

    private boolean betweenAdditional = false;

    private String elementId = "";

    private String elementContent = "";

    private String x = "";

    private String y = "";

    private String w = "";

    private String h = "";

    private String additional = "";

    public UMLetHandler()
    {
        this.result = new ArrayList<>();
    }

    @Override
    public void startElement(String uri,
                             String localName,
                             String qName,
                             Attributes attributes)
    {
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
        if (qName.equalsIgnoreCase("x")) {
            betweenX = true;
        }
        if (qName.equalsIgnoreCase("y")) {
            betweenY = true;
        }
        if (qName.equalsIgnoreCase("w")) {
            betweenW = true;
        }
        if (qName.equalsIgnoreCase("h")) {
            betweenH = true;
        }
        if (qName.equalsIgnoreCase("additional_attributes")) {
            betweenAdditional = true;
        }
    }

    @Override
    public void characters(char[] ch, int start, int len)
    {
        if (betweenDiagram && betweenElement && betweenId) {
            elementId += new String(ch, start, len);
        }
        if (betweenDiagram && betweenElement && betweenPanelAttributes) {
            elementContent += new String(ch, start, len);
        }
        if (betweenDiagram && betweenElement && betweenX) {
            x += new String(ch, start, len);
        }
        if (betweenDiagram && betweenElement && betweenY) {
            y += new String(ch, start, len);
        }
        if (betweenDiagram && betweenElement && betweenW) {
            w += new String(ch, start, len);
        }
        if (betweenDiagram && betweenElement && betweenH) {
            h += new String(ch, start, len);
        }
        if (betweenDiagram && betweenElement && betweenAdditional) {
            additional += new String(ch, start, len);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
    {
        if (qName.equalsIgnoreCase("diagram")) {
            betweenDiagram = false;
        }
        if (qName.equalsIgnoreCase("element")) {
            betweenElement = false;
            Element e;
            if ($.isBlank(additional)) {
                e = new Element();
            } else {
                e = new RelationElement();
                String[] splits = parseAdditional();
                int len = splits.length;
                if (elementContent.equals("lt=->")) {
                    ((RelationElement) e).setSourceX((int) Float.parseFloat(splits[0]));
                    ((RelationElement) e).setSourceY((int) Float.parseFloat(splits[1]));
                    ((RelationElement) e).setTargetX((int) Float.parseFloat(splits[len - 2]));
                    ((RelationElement) e).setTargetY((int) Float.parseFloat(splits[len - 1]));
                } else {
                    ((RelationElement) e).setSourceX((int) Float.parseFloat(splits[len - 2]));
                    ((RelationElement) e).setSourceY((int) Float.parseFloat(splits[len - 1]));
                    ((RelationElement) e).setTargetX((int) Float.parseFloat(splits[0]));
                    ((RelationElement) e).setTargetY((int) Float.parseFloat(splits[1]));
                }
            }
            e.setType(UMLType.valueOf(elementId));
            e.setContent(elementContent);
            e.setX(Integer.parseInt(x));
            e.setY(Integer.parseInt(y));
            e.setW(Integer.parseInt(w));
            e.setH(Integer.parseInt(h));
            result.add(e);
            elementId = "";
            elementContent = "";
            x = "";
            y = "";
            w = "";
            h = "";
            additional = "";
        }
        if (qName.equalsIgnoreCase("id")) {
            betweenId = false;
        }
        if (qName.equalsIgnoreCase("panel_attributes")) {
            betweenPanelAttributes = false;
        }
        if (qName.equalsIgnoreCase("x")) {
            betweenX = false;
        }
        if (qName.equalsIgnoreCase("y")) {
            betweenY = false;
        }
        if (qName.equalsIgnoreCase("w")) {
            betweenW = false;
        }
        if (qName.equalsIgnoreCase("h")) {
            betweenH = false;
        }
        if (qName.equalsIgnoreCase("additional_attributes")) {
            betweenAdditional = false;
        }
    }

    private String[] parseAdditional()
    {
        return additional.split(";");
    }

}
