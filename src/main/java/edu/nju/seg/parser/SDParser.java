package edu.nju.seg.parser;

import edu.nju.seg.exception.ParseException;
import edu.nju.seg.expression.Judgement;
import edu.nju.seg.model.*;
import edu.nju.seg.util.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.nju.seg.config.Constants.OUTERMOST_FRAGMENT_NAME;

public class SDParser implements DiagramParser {

    private static final Pattern TITLE_PATTERN = Pattern.compile("^title=(.*)$");

    private static final Pattern CLAIM_PATTERN = Pattern.compile("^obj=(.*?)~(.*?)$");

    private static final Pattern FRAGMENT_PATTERN = Pattern.compile("^combinedFragment=(.*?)~(.*?)$");

    private static final Pattern MESSAGE_PATTERN = Pattern.compile("^(.*?)->>>(.*?)\\s*:\\s*(.*)$");

    private final Element element;

    private final List<Element> notes;

    private final Map<String, Instance> instance_map = new HashMap<>();

    private List<Instance> instances;

    public SDParser(List<Element> notes,
                    Element element)
    {
        this.notes = notes;
        this.element = element;
    }

    @Override
    public Diagram parse()
    {
        // parse notes
        Pair<List<Judgement>, List<Judgement>> p = parse_notes(notes);
        List<String> text = parse_diagram_context();
        // parse sequence diagram title
        String title = parse_title(text.get(0));
        Pair<Integer, List<Instance>> dec = parse_declaration(text);
        this.instances = dec.get_right();
        // parse fragment
        List<String> frag_text = trim_blank_line(text.subList(dec.get_left(), text.size()));
        return new SequenceDiagram(
                title,
                p.get_left(),
                parse_sd(frag_text),
                p.get_right());
    }

    private Pair<List<Judgement>, List<Judgement>> parse_notes(List<Element> notes)
    {
        List<Judgement> constraints = null;
        List<Judgement> properties = null;
        for (Element e: notes) {
            Pair<NoteType, List<Judgement>> note = UmlNoteParser.parse_note(e);
            if (note.get_left() == NoteType.CONSTRAINTS) {
                constraints = note.get_right();
            }
            if (note.get_left() == NoteType.PROPERTIES) {
                properties = note.get_right();
            }
        }
        return new Pair<>(constraints, properties);
    }


    /**
     * parse the declaration of instances
     * @param text the input text
     * @return a tuple: (text index, the list of instances)
     */
    private Pair<Integer, List<Instance>> parse_declaration(List<String> text)
    {
        // parse the instances declaration
        int claimIndex = 1;
        int len = text.size();
        List<Instance> instances = new ArrayList<>();
        while (claimIndex < len) {
            String current = text.get(claimIndex);
            Matcher m = CLAIM_PATTERN.matcher(current);
            if (m.find()) {
                Instance i = new Instance(m.group(1), m.group(2));
                instance_map.put(m.group(2), i);
                instances.add(i);
                claimIndex++;
            } else {
                break;
            }
        }
        return new Pair<>(claimIndex, instances);
    }

    private List<String> parse_diagram_context()
    {
        List<String> text = Arrays.asList(element.getContent().split("\n"));
        if (text.size() < 2) {
            throw new ParseException("missing instance claim");
        }
        return text;
    }

    /**
     * parse fragments
     * @param text text
     * @return structure fragment
     */
    private Fragment parse_sd(List<String> text)
    {
        Fragment fragment = new Fragment(new ArrayList<>(), instances, OUTERMOST_FRAGMENT_NAME);
        parse_helper(fragment, text, false);
        return fragment;
    }

    /**
     * parse recursively
     * @param parent parent context
     * @param text text
     * @param elseMode alt fragment else mode
     */
    private void parse_helper(Fragment parent, List<String> text, boolean elseMode)
    {
        if (text.size() == 0) {
            return;
        }
        String current = text.get(0);
        Matcher fragMat = FRAGMENT_PATTERN.matcher(current);
        if (fragMat.find()) {
            int end = search_end_index(text);
            Fragment f = parse_fragment(fragMat.group(1));
            parent.addChild(f);
            parse_helper(f, trim_blank_line(text.subList(1, end)), false);
            parse_helper(parent, trim_blank_line(text.subList(end + 1, text.size())), false);
        } else {
            Matcher mat = MESSAGE_PATTERN.matcher(current);
            if (mat.find()) {
                Message m = new MessageParser(instance_map, mat.group(1), mat.group(2), mat.group(3)).parse_message();
                if (elseMode) {
                    ((AltFragment) parent).add_to_else(m);
                } else {
                    parent.addChild(m);
                }
                parse_helper(parent, trim_blank_line(text.subList(1, text.size())), false);
            } else {
                if (current.trim().equals("..")) {
                    // alt fragment else part
                    parse_helper(parent, trim_blank_line(text.subList(1, text.size())), true);
                } else {
                    throw new ParseException("wrong modeling language, current: " + current);
                }
            }
        }
    }

    /**
     * parse fragment corresponding to the fragment language
     * @param info the fragment information
     * @return fragment
     */
    private Fragment parse_fragment(String info)
    {
        info = info.trim();
        if (info.startsWith(FragmentType.INT.getV())) {
            return new IntFragmentParser(instances, info).parse_int_fragment();
        } else if (info.startsWith(FragmentType.LOOP.getV())) {
            return new LoopFragmentParser(instances, info).parse_loop_fragment();
        } else if (info.startsWith(FragmentType.ALT.getV())) {
            return new AltFragmentParser(instances, info).parse_alt_fragment();
        } else if (info.startsWith(FragmentType.OPT.getV())) {
            return new OptFragmentParser(instances, info).parse_opt_fragment();
        } else {
            throw new ParseException("no corresponding combined fragment");
        }
    }

    /**
     * trim the blank lines of the text list
     * @param text the text list
     * @return the text list without blank lines in the front
     */
    private List<String> trim_blank_line(List<String> text)
    {
        if (text.size() == 0) {
            return text;
        } else {
            if (text.get(0).trim().equals("")) {
                return trim_blank_line(text.subList(1, text.size()));
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
    private int search_end_index(List<String> text)
    {
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
     * parse sequence title
     * @param text text
     * @return title
     */
    private String parse_title(String text)
    {
        Matcher m = TITLE_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1);
        } else {
            throw new ParseException("parse sequence title failed");
        }
    }

}
