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
import java.util.stream.Collectors;

public class AutomatonParser implements Parser {

    private static Pattern STATE_PATTERN = Pattern.compile("^(.*)--(.*)-\\.(.*)valign=top.*$", Pattern.DOTALL);

    private static Pattern SPECIAL_STATE_PATTERN = Pattern.compile("^type=(.*)$");

    private static Pattern RELATION_PATTERN = Pattern.compile("^lt=->(.*)$", Pattern.DOTALL);

    private static Pattern FILENAME_PATTERN = Pattern.compile("^(.*)\\.uxf$");

    private String name;

    private List<Element> elements;

    private List<Pair<List<Integer>, State>> locationList;

    public AutomatonParser(String fileName,
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
        AutomatonDiagram ad = new AutomatonDiagram();
        ad.setTitle(name);
        parseAutomaton(ad);
        ad.setConstraints(mergeConstraints(ad));
        return ad;
    }

    /**
     * parse automaton
     * @param ad the structure automaton
     */
    private void parseAutomaton(AutomatonDiagram ad)
    {
        List<Element> relationEles = UmlUtil.partitionRelation(elements);
        List<Element> stateEles = UmlUtil.partitionState(elements);
        List<Element> specialEles = UmlUtil.partitionSpecial(elements);
        List<State> states = new ArrayList<>();
        State initial = null;
        List<Relation> relations = new ArrayList<>();
        for (Element e: stateEles) {
            states.add(consState(e));
        }
        for (Element e: specialEles) {
            State s = consSpecial(e);
            if (s.getType() == StateType.INITIAL) {
                initial = s;
            } else {
                states.add(s);
            }
        }
        for (Element e: relationEles) {
            relations.add(consRelation((RelationElement) e));
        }
        if (initial == null) {
            throw new ParseException("missing initial node");
        }
        ad.setInitial(initial);
        ad.setAllStates(states);
        ad.setAllRelations(relations);
    }

    /**
     * construct special element
     * @param e element
     * @return state
     */
    private State consSpecial(Element e)
    {
        State s = new State();
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
        s.setEquations(new ArrayList<>());
        s.setConstraints(new ArrayList<>());
        addToLocList(e, s);
        return s;
    }

    /**
     * construct relation according to the XML element
     * @param e element
     * @return relation
     */
    private Relation consRelation(RelationElement e)
    {
        Relation r = new Relation();
        Matcher m = RELATION_PATTERN.matcher(e.getContent());
        if (m.matches()) {
            String expr = m.group(1).trim();
            if (expr.contains(":")) {
                String[] nameSplits = expr.split(":");
                r.setName(nameSplits[0]);
                expr = nameSplits[1];
            }
            if (expr.contains(";")) {
                String[] splits = expr.split(";");
                r.setCondition(splits[0]);
                r.setAssignment(splits[1]);
            } else {
                r.setCondition(expr);
            }

            Pair<Integer, Integer> sourceCoord = e.getSource();
            Pair<Integer, Integer> targetCoord = e.getTarget();
            Optional<State> maybeSource = searchState(sourceCoord);
            if (maybeSource.isPresent()) {
                State source = maybeSource.get();
                source.addEdge(r);
                r.setSource(source);
            } else {
                throw new ParseException("wrong relation source, condition: " + expr);
            }
            Optional<State> maybeTarget = searchState(targetCoord);
            if (maybeTarget.isPresent()) {
                r.setTarget(maybeTarget.get());
            } else {
                throw new ParseException("wrong relation target, condition: " + expr);
            }
            if (r.getSource() == r.getTarget()) {
                r.getSource().setLoop(true);
            }
        } else {
            throw new ParseException("wrong relation format");
        }
        return r;
    }

    /**
     * construct state according to the XML element
     * @param e element
     * @return state
     */
    private State consState(Element e)
    {
        State s = new State();
        if (e.getType() == UMLType.UMLState) {
            s.setType(StateType.NORMAL);
        } else {
            throw new ParseException("wrong state type");
        }
        Matcher m = STATE_PATTERN.matcher(e.getContent());
        if (m.matches()) {
            s.setStateName(m.group(1).trim());
            s.setEquations($.filterStrList(Arrays.asList(m.group(2).split("\n"))));
            s.setConstraints($.filterStrList(Arrays.asList(m.group(3).split("\n"))));
            s.setOuters(new ArrayList<>());
        } else {
            throw new ParseException("wrong state element content");
        }
        addToLocList(e, s);
        return s;
    }

    /**
     * search state by the given coordinate
     * @param coord coordinate
     * @return maybe state
     */
    private Optional<State> searchState(Pair<Integer, Integer> coord)
    {
        for (Pair<List<Integer>, State> p: locationList) {
            List<Integer> loc = p.getLeft();
            if (UmlUtil.inSquare(coord, loc)) {
                return Optional.of(p.getRight());
            }
        }
        return Optional.empty();
    }

    /**
     * add state to the location list
     * @param e XML element
     * @param s state
     */
    private void addToLocList(Element e, State s)
    {
        List<Integer> loc = UmlUtil.consLocation(e);
        Pair<List<Integer>, State> p = new Pair<>();
        p.setLeft(loc);
        p.setRight(s);
        locationList.add(p);
    }

    /**
     * merge constraints of
     * @param ad automaton
     * @return merged constraints
     */
    private List<String> mergeConstraints(AutomatonDiagram ad)
    {
        List<String> constraints = new ArrayList<>();
        for (State s: ad.getAllStates()) {
            constraints.addAll(s.getConstraints());
        }
        return constraints;
    }

}
