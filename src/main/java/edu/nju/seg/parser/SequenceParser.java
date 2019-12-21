package edu.nju.seg.parser;

import edu.nju.seg.model.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SequenceParser {

    private static Pattern TITLE_PATTERN = Pattern.compile("^title=(.*)$");

    private static Pattern CLAIM_PATTERN = Pattern.compile("^obj=(.*?)~(.*?)$");

    private static Pattern FRAGMENT_PATTERN = Pattern.compile("^combinedFragment=(.*?)~(.*?)$");

    private static Pattern MESSAGE_PATTERN = Pattern.compile("^(.*?)->>>(.*?)[\\s.*]:[\\s.*](.*?)[\\s;*]$");

    private static Pattern INT_PATTERN = Pattern.compile("^int[\\s.*]\\(p=(.*)\\)[\\s.*]$");

    private static Pattern MESSAGE_INFO_PATTERN = Pattern.compile("^\\((.*),(.*),(.*)\\)$");

    private static Pattern MESSAGE_INSTRUCTION_PATTERN = Pattern.compile("^(.*)\\{(.*)\\}$");

    private ElementContent note;

    private ElementContent diagram;

    private Map<String, String> instanceMap;

    public SequenceParser(ElementContent note, ElementContent diagram) {
        this.note = note;
        this.diagram = diagram;
        this.instanceMap = new HashMap<>();
    }

    /**
     * parse sequence diagram from the raw text
     * @return the sequence diagram
     */
    public SequenceDiagram parseSequenceDiagram() {
        SequenceDiagram sd = new SequenceDiagram();
        // parse constraints in the note element
        sd.setConstraints(parseConstraints());

        List<String> text = Arrays.asList(diagram.getContent().split("\\n"));
        if (text.size() < 2) {
            throw new ParseException("missing instance claim");
        }
        // parse sequence diagram title
        sd.setTitle(parseTitle(text.get(0)));
        // parse instance claim
        int claimIndex = 1;
        while (true) {
            String current = text.get(claimIndex);
            Matcher m = CLAIM_PATTERN.matcher(current);
            if (m.find()) {
                instanceMap.put(m.group(2), m.group(1));
                claimIndex++;
            } else {
                break;
            }
        }
        // parse fragment
        List<String> fragments = trimBlankLine(text.subList(claimIndex, text.size()));
        sd.setContainer(parseFragment(fragments));
        return sd;
    }

    private Fragment parseFragment(List<String> text) {
        Fragment fragment = new Fragment(new ArrayList<>());
        parseHelper(fragment, text);
        return fragment;
    }

    private void parseHelper(Fragment parent, List<String> text) {
        if (text.size() == 0) {
            return;
        }
        String current = text.get(0);
        Matcher fragMat = FRAGMENT_PATTERN.matcher(current);
        if (fragMat.find()) {
            int end = searchEndIndex(text);
            Fragment f = consFragment(fragMat.group(1));
            parent.addChild(f);
            parseHelper(f, trimBlankLine(text.subList(1, end)));
            parseHelper(parent, trimBlankLine(text.subList(end + 1, text.size())));
        } else {
            Matcher msgMat = MESSAGE_PATTERN.matcher(current);
            if (msgMat.find()) {
                Message m = consMessage(msgMat.group(1), msgMat.group(2), msgMat.group(3));
                parent.addChild(m);
                parseHelper(parent, trimBlankLine(text.subList(1, text.size())));
            } else {
                throw new ParseException("wrong modeling language");
            }
        }
    }

    private Fragment consFragment(String message) {
        Matcher intMat = INT_PATTERN.matcher(message);
        if (intMat.find()) {

        }
        throw new ParseException("no corresponding combined fragment");
    }

    private Message consMessage(String fromVar, String toVar, String info) {
        Message m = new Message();
        Matcher infoMat = MESSAGE_INFO_PATTERN.matcher(info);
        if (infoMat.find()) {
            String fromEventStr = infoMat.group(1);
            String toEventStr = infoMat.group(2);
            String msgName = infoMat.group(3);
            m.setName(msgName);
            m.setFrom(consEvent(fromVar, fromEventStr));
            m.setTo(consEvent(toVar, toEventStr));
            return m;
        } else {
            throw new ParseException("wrong message information");
        }
    }

    private Event consEvent(String var, String eventStr) {
        Instance ins = new Instance();
        ins.setName(instanceMap.get(var));
        Event event = new Event();
        event.setBelongTo(ins);
        if (eventStr.contains("[") || eventStr.contains("]")) {
            Matcher mat = MESSAGE_INSTRUCTION_PATTERN.matcher(eventStr);
            if (mat.find()) {
                event.setName(mat.group(1));
                event.setInstruction(mat.group(2));
            } else {
                throw new ParseException("wrong event instruction");
            }
        } else {
            event.setName(eventStr);
        }
        return event;
    }

    /**
     * trim the blank lines of the text list
     * @param text the text list
     * @return the text list without blank lines in the front
     */
    private List<String> trimBlankLine(List<String> text) {
        if (text.size() == 0) {
            return text;
        } else {
            if (text.get(0).equals("")) {
                return trimBlankLine(text.subList(1, text.size()));
            } else {
                return text;
            }
        }
    }

    /**
     * search fragment ending symbols from the tail
     * @param text the text list
     * @return the index of ending symbols or throw exception
     */
    private int searchEndIndex(List<String> text) {
        for (int i = text.size() - 1; i >= 0; i--) {
            if (text.get(i).equals("--")) {
                return i;
            }
        }
        throw new ParseException("missing fragment ending symbols --");
    }

    /**
     * split constraints from text
     * @return the constraints list
     */
    private List<String> parseConstraints() {
        return Arrays.asList(note.getContent().split("\\n"));
    }

    /**
     * parse sequence title
     * @param text text
     * @return title
     */
    private String parseTitle(String text) {
        Matcher m = TITLE_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1);
        } else {
            throw new ParseException("parse sequence title failed");
        }
    }

}
