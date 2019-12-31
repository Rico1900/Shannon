package edu.nju.seg.parser;

import edu.nju.seg.model.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SequenceParser implements Parser {

    private static Pattern TITLE_PATTERN = Pattern.compile("^title=(.*)$");

    private static Pattern CLAIM_PATTERN = Pattern.compile("^obj=(.*?)~(.*?)$");

    private static Pattern FRAGMENT_PATTERN = Pattern.compile("^combinedFragment=(.*?)~(.*?)$");

    private static Pattern MESSAGE_PATTERN = Pattern.compile("^(.*?)->>>(.*?)[\\s*]:[\\s.*](.*?)[\\s*;]$");

    private static Pattern FULL_INT_PATTERN = Pattern.compile("^int\\s*\\(p=(.*)\\)\\s*\\((.*),(.*)\\)\\s*\\[(.*)\\]\\s*(.*)$");

    private static Pattern LIMIT_INT_PATTERN = Pattern.compile("^int\\s*\\(p=(.*)\\)\\s*\\((.*),(.*)\\)\\s*(.*)$");

    private static Pattern INSTRUCTION_INT_PATTERN = Pattern.compile("^int\\s*\\(p=(.*)\\)\\s*\\[(.*)\\]\\s*(.*)$");

    private static Pattern SIMPLE_INT_PATTERN = Pattern.compile("^int\\s*\\(p=(.*)\\)\\s*(.*)$");

    private static Pattern MESSAGE_INFO_PATTERN = Pattern.compile("^\\((.*),(.*),(.*)\\)$");

    private static Pattern MESSAGE_INSTRUCTION_PATTERN = Pattern.compile("^(.*)\\{(.*)\\}$");

    private Element note;

    private Element diagram;

    private Map<String, String> instanceMap;

    public SequenceParser(Element note, Element diagram) {
        this.note = note;
        this.diagram = diagram;
        this.instanceMap = new HashMap<>();
    }

    @Override
    public Diagram parse() {
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

    /**
     * parse fragments
     * @param text text
     * @return structure fragment
     */
    private Fragment parseFragment(List<String> text) {
        Fragment fragment = new Fragment(new ArrayList<>());
        parseHelper(fragment, text);
        return fragment;
    }

    /**
     * parse recursively
     * @param parent parent context
     * @param text text
     */
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

    /**
     * construct fragment corresponding to the fragment language
     * @param info the fragment information
     * @return fragment
     */
    private Fragment consFragment(String info) {
        info = info.trim();
        if (info.startsWith("int")) {
            return consIntFragment(info);
        } else if (info.startsWith("loop")) {
            // TODO
            return null;
        } else if (info.startsWith("alt")) {
            // TODO
            return null;
        } else {
            throw new ParseException("no corresponding combined fragment");
        }
    }

    /**
     * construct interrupt fragment
     * @param info interrupt fragment information
     * @return structure fragment
     */
    private IntFragment consIntFragment(String info) {
        int bracketCount = 0;
        int parenthesisCount = 0;
        for (char c: info.toCharArray()) {
            if (c == '[') {
                bracketCount++;
            }
            if (c == '(') {
                parenthesisCount++;
            }
        }
        if (bracketCount == 1 && parenthesisCount == 2) {
            Matcher m = FULL_INT_PATTERN.matcher(info);
            checkIntFragMat(m);
            return new IntFragment(new ArrayList<>(),
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)),
                    m.group(4),
                    m.group(5));
        } else if (bracketCount == 0 && parenthesisCount == 2) {
            Matcher m = LIMIT_INT_PATTERN.matcher(info);
            checkIntFragMat(m);
            return new IntFragment(new ArrayList<>(),
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)),
                    "",
                    m.group(4));
        } else if (bracketCount == 1 && parenthesisCount == 1) {
            Matcher m = INSTRUCTION_INT_PATTERN.matcher(info);
            checkIntFragMat(m);
            return new IntFragment(new ArrayList<>(),
                    Integer.parseInt(m.group(1)),
                    1,
                    1,
                    m.group(2),
                    m.group(3));
        } else if (bracketCount == 0 && parenthesisCount == 1) {
            Matcher m = SIMPLE_INT_PATTERN.matcher(info);
            checkIntFragMat(m);
            return new IntFragment(new ArrayList<>(),
                    Integer.parseInt(m.group(1)),
                    1,
                    1,
                    "",
                    m.group(2));
        } else {
            throw new ParseException("wrong int fragment modeling language");
        }
    }

    /**
     * check if the interrupt fragment is right
     * @param m match status
     */
    private void checkIntFragMat(Matcher m) {
        if (!m.find()) {
            throw new ParseException("wrong int fragment modeling language");
        }
    }

    /**
     * construct message
     * @param fromVar start instance variable
     * @param toVar ending instance variable
     * @param info message information
     * @return the message
     */
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

    /**
     * construct event
     * @param var event variable
     * @param eventStr event string
     * @return structure event
     */
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
            if (text.get(0).trim().equals("")) {
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
        int fragCount = 0;
        int endCount = 0;
        for (int i = 0; i < text.size(); i++) {
            String current = text.get(i);
            if (current.startsWith("combinedFragment")) {
                fragCount++;
            }
            if (current.startsWith("--")) {
                endCount++;
                if (fragCount == endCount) {
                    return i;
                }
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