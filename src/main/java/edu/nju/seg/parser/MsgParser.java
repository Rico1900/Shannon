package edu.nju.seg.parser;

import edu.nju.seg.exception.ParseException;
import edu.nju.seg.model.*;
import edu.nju.seg.util.$;
import edu.nju.seg.util.Pair;
import edu.nju.seg.util.UmlUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MsgParser implements Parser {

    private static Pattern FILENAME_PATTERN = Pattern.compile("^(.*)\\.uxf$");

    private static Pattern CONSTRAINTS_PATTERN = Pattern.compile("^Constraints(.*)$", Pattern.DOTALL);

    private static Pattern GLOBAL_PATTERN = Pattern.compile("^Global(.*)$", Pattern.DOTALL);

    private static Pattern SPECIAL_STATE_PATTERN = Pattern.compile("^type=(.*)$");

    private String name;

    private List<Element> elements;

    private List<Pair<List<Integer>, SimpleState>> locationList;

    public MsgParser(String fileName,
                     List<Element> elements)
    {
        Matcher m = FILENAME_PATTERN.matcher(fileName);
        if (m.matches()) {
            this.name = m.group(1);
        } else {
            throw new ParseException("wrong file name: " + fileName);
        }
        this.elements = elements;
        this.locationList = new ArrayList<>();
    }

    @Override
    public Diagram parse()
    {
        MsgDiagram md = new MsgDiagram();
        md.setTitle(name);
        parseMsg(md);
        md.setConstraints(parseConstraints());
        md.setGlobal(parseGlobal());
        return md;
    }

    private void parseMsg(MsgDiagram md)
    {
        List<Element> relationEles = UmlUtil.pickupRelation(elements);
        List<Element> stateEles = UmlUtil.pickupState(elements);
        List<Element> specialEles = UmlUtil.pickupSpecial(elements);
        List<SimpleState> states = new ArrayList<>();
        SimpleState initial = null;
        SimpleState end = null;
        List<SimpleRelation> relations = new ArrayList<>();
        for (Element e: stateEles) {
            states.add(consState(e));
        }
        for (Element e: specialEles) {
            SimpleState s = consSpecial(e);
            if (s.getType() == StateType.INITIAL) {
                initial = s;
            } else if (s.getType() == StateType.FINAL) {
                end = s;
            } else {
                states.add(s);
            }
        }
        for (Element e: relationEles) {
            relations.add(consRelation((RelationElement) e));
        }
        md.setInitial(initial);
        md.setEnd(end);
        md.setAllStates(states);
        md.setAllRelations(relations);
    }

    private SimpleState consSpecial(Element e)
    {
        SimpleState s = new SimpleState();
        Matcher m = SPECIAL_STATE_PATTERN.matcher(e.getContent());
        if (m.matches()) {
            String t = m.group(1);
            if (t.equals("initial")) {
                s.setType(StateType.INITIAL);
            } else if (t.equals("final")) {
                s.setType(StateType.FINAL);
            } else {
                throw new ParseException("wrong special node type");
            }
        } else {
            throw new ParseException("wrong special node");
        }
        s.setStateName("");
        addToLocList(e, s);
        return s;
    }

    private SimpleState consState(Element e)
    {
        SimpleState s = new SimpleState();
        if (e.getType() == UMLType.UMLState) {
            s.setType(StateType.NORMAL);
        } else {
            throw new ParseException("wrong state type");
        }
        s.setStateName(e.getContent());
        addToLocList(e, s);
        return s;
    }

    private SimpleRelation consRelation(RelationElement e)
    {
        SimpleRelation r = new SimpleRelation();
        Pair<Integer, Integer> sourceCoord = e.getSource();
        Pair<Integer, Integer> targetCoord = e.getTarget();
        Optional<SimpleState> maybeSource = searchState(sourceCoord);
        if (maybeSource.isPresent()) {
            SimpleState source = maybeSource.get();
            source.addEdge(r);
            r.setSource(source);
        } else {
            throw new ParseException("wrong relation source, coord: " + sourceCoord);
        }
        Optional<SimpleState> maybeTarget = searchState(targetCoord);
        if (maybeTarget.isPresent()) {
            r.setTarget(maybeTarget.get());
        } else {
            throw new ParseException("wrong relation target, coord: " + targetCoord);
        }
        if (r.getSource() == r.getTarget()) {
            r.getSource().setLoop(true);
        }
        return r;
    }

    private List<String> parseConstraints()
    {
        List<String> constraints = new ArrayList<>();
        for (Element e: elements) {
            if (e.getType() == UMLType.UMLNote) {
                Matcher m = CONSTRAINTS_PATTERN.matcher(e.getContent());
                if (m.matches()) {
                    constraints.addAll($.filterStrList(Arrays.asList(m.group(1).split("\n"))));
                }
            }
        }
        return constraints;
    }

    private List<String> parseGlobal()
    {
        List<String> constraints = new ArrayList<>();
        for (Element e: elements) {
            if (e.getType() == UMLType.UMLNote) {
                Matcher m = GLOBAL_PATTERN.matcher(e.getContent());
                if (m.matches()) {
                    constraints.addAll($.filterStrList(Arrays.asList(m.group(1).split("\n"))));
                }
            }
        }
        return constraints;
    }

    /**
     * add state to the location list
     * @param e XML element
     * @param s state
     */
    private void addToLocList(Element e, SimpleState s)
    {
        List<Integer> loc = UmlUtil.consLocation(e);
        Pair<List<Integer>, SimpleState> p = new Pair<>();
        p.setLeft(loc);
        p.setRight(s);
        locationList.add(p);
    }

    /**
     * search state by the given coordinate
     * @param coord coordinate
     * @return maybe state
     */
    private Optional<SimpleState> searchState(Pair<Integer, Integer> coord)
    {
        for (Pair<List<Integer>, SimpleState> p: locationList) {
            List<Integer> loc = p.getLeft();
            if (UmlUtil.inSquare(coord, loc)) {
                return Optional.of(p.getRight());
            }
        }
        return Optional.empty();
    }

}
