package edu.nju.seg.parser;

import edu.nju.seg.model.*;
import edu.nju.seg.util.$;

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
                           List<Element> elements) {
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
    public Diagram parse() {
        AutomatonDiagram ad = new AutomatonDiagram();
        ad.setTitle(name);
        parseAutomaton(ad);
        ad.setConstraints(mergeConstraints(ad));
        return ad;
    }

    private void parseAutomaton(AutomatonDiagram ad) {
        List<Element> relationEles = partitionRelation();
        List<Element> stateEles = partitionState();
        List<Element> specialEles = partitionSpecial();
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

    private State consSpecial(Element e) {
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

    private Relation consRelation(RelationElement e) {
        Relation r = new Relation();
        Matcher m = RELATION_PATTERN.matcher(e.getContent());
        if (m.matches()) {
            String cond = m.group(1);
            r.setCondition(cond.trim());
            Pair<Integer, Integer> sourceCoord = e.getSource();
            Pair<Integer, Integer> targetCoord = e.getTarget();
            Optional<State> maybeSource = searchState(sourceCoord);
            if (maybeSource.isPresent()) {
                State source = maybeSource.get();
                source.addEdge(r);
                r.setSource(source);
            } else {
                throw new ParseException("wrong relation source, condition: " + cond);
            }
            Optional<State> maybeTarget = searchState(targetCoord);
            if (maybeTarget.isPresent()) {
                r.setTarget(maybeTarget.get());
            } else {
                throw new ParseException("wrong relation target, condition: " + cond);
            }
        } else {
            throw new ParseException("wrong relation format");
        }
        return r;
    }

    private State consState(Element e) {
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

    private Optional<State> searchState(Pair<Integer, Integer> coord) {
        for (Pair<List<Integer>, State> p: locationList) {
            List<Integer> loc = p.getLeft();
            if (coord.getLeft() >= loc.get(0)
                    && coord.getLeft() <= loc.get(1)
                    && coord.getRight() >= loc.get(2)
                    && coord.getRight() <= loc.get(3)) {
                return Optional.of(p.getRight());
            }
        }
        return Optional.empty();
    }

    private void addToLocList(Element e, State s) {
        List<Integer> loc = new ArrayList<>();
        loc.add(e.getX());
        loc.add(e.getX() + e.getW());
        loc.add(e.getY());
        loc.add(e.getY() + e.getH());
        Pair<List<Integer>, State> p = new Pair<>();
        p.setLeft(loc);
        p.setRight(s);
        locationList.add(p);
    }

    /**
     * pick up relation elements
     * @return relation elements
     */
    private List<Element> partitionRelation() {
        return elements.stream()
                .filter(e -> e.getType() == UMLType.Relation)
                .collect(Collectors.toList());
    }

    /**
     * pick up state elements
     * @return state elements
     */
    private List<Element> partitionState() {
        return elements.stream()
                .filter(e -> e.getType() == UMLType.UMLState)
                .collect(Collectors.toList());
    }

    /**
     * pick up special elements
     * @return special elements
     */
    private List<Element> partitionSpecial() {
        return elements.stream()
                .filter(e -> e.getType() == UMLType.UMLSpecialState)
                .collect(Collectors.toList());
    }

    /**
     * merge constraints of
     * @param ad automaton
     * @return merged constraints
     */
    private List<String> mergeConstraints(AutomatonDiagram ad) {
        List<String> constraints = new ArrayList<>();
        for (State s: ad.getAllStates()) {
            constraints.addAll(s.getConstraints());
        }
        return constraints;
    }

}
