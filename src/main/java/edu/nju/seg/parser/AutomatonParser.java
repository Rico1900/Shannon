package edu.nju.seg.parser;

import edu.nju.seg.exception.ParseException;
import edu.nju.seg.expression.Judgement;
import edu.nju.seg.model.*;
import edu.nju.seg.util.Pair;
import edu.nju.seg.util.UmlUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutomatonParser implements DiagramParser {

    private static final Pattern FILENAME_PATTERN = Pattern.compile("^(.*)_ha\\.uxf$");

    private final String file_name;

    private final List<Element> elements;

    private final List<Pair<List<Integer>, State>> locations;

    public AutomatonParser(String file_name,
                           List<Element> elements)
    {
        this.file_name = file_name;
        this.elements = elements;
        this.locations = new ArrayList<>();
    }

    private String parse_file_name(String file_name)
    {
        Matcher m = FILENAME_PATTERN.matcher(file_name);
        if (m.matches()) {
            return m.group(1);
        } else {
            throw new ParseException("wrong file name: " + file_name);
        }
    }

    @Override
    public Diagram parse()
    {
        String name = parse_file_name(file_name);
        State init = parse_initial_node();
        List<State> states = parse_states();
        List<Relation> relations = parse_relations();
        List<Judgement> properties = parse_properties_from_notes();
        return new AutomatonDiagram(name,
                merge_constraints(states),
                init,
                states,
                relations,
                properties);
    }


    /**
     * parse the relation on the graph
     * @return the relations
     */
    private List<Relation> parse_relations()
    {
        return UmlUtil.pickup_relation(elements)
                .stream()
                .map(e -> (RelationElement) e)
                .map(this::parse_relation)
                .collect(Collectors.toList());
    }

    /**
     * get all states excluding the initial node
     * @return the list of states
     */
    private List<State> parse_states()
    {
        return Stream.concat(
                UmlUtil.pickup_state(elements).stream().map(this::parse_state),
                UmlUtil.pickup_special(elements).stream().map(this::parse_special_node))
                .filter(s -> s.getType() != StateType.INITIAL)
                .collect(Collectors.toList());
    }

    /**
     * parse the initial node
     * @return the initial node if there exist one
     */
    private State parse_initial_node()
    {
        return UmlUtil.pickup_special(elements)
                .stream()
                .map(this::parse_special_node)
                .filter(s -> s.getType() == StateType.INITIAL)
                .findFirst()
                .orElseThrow();
    }

    /**
     * parse properties from the uml notes
     * @return the judgements
     */
    private List<Judgement> parse_properties_from_notes()
    {
         return UmlUtil.pick_UML_notes(elements)
                 .stream()
                 .map(UmlNoteParser::parse_note)
                 .filter(p -> p.get_left() == NoteType.PROPERTIES)
                 .map(Pair::get_right)
                 .findFirst()
                 .orElse(new ArrayList<>());
    }

    private State parse_special_node(Element e)
    {
        State s = new StateParser(e).parse();
        record_location(e, s);
        return s;
    }

    /**
     * parse relation according to the XML element
     * @param e element
     * @return relation
     */
    private Relation parse_relation(RelationElement e)
    {
        Relation r = new RelationParser(e).parse();
        Pair<Integer, Integer> source_coord = e.getSource();
        Pair<Integer, Integer> target_coord = e.getTarget();
        Optional<State> maybe_source = search_state(source_coord);
        if (maybe_source.isPresent()) {
            State source = maybe_source.get();
            source.addEdge(r);
            r.set_source(source);
        } else {
            throw new ParseException("wrong relation source, name: " + r.get_name());
        }
        Optional<State> maybe_target = search_state(target_coord);
        if (maybe_target.isPresent()) {
            r.set_target(maybe_target.get());
        } else {
            throw new ParseException("wrong relation target, name: " + r.get_name());
        }
        if (r.get_source() == r.get_target()) {
            r.get_source().setLoop(true);
        }
        return r;
    }

    private State parse_state(Element e)
    {
        State s = new StateParser(e).parse();
        record_location(e, s);
        return s;
    }

    private Optional<State> search_state(Pair<Integer, Integer> coord)
    {
        for (Pair<List<Integer>, State> p: locations) {
            List<Integer> loc = p.get_left();
            if (UmlUtil.in_square(coord, loc)) {
                return Optional.of(p.get_right());
            }
        }
        return Optional.empty();
    }

    /**
     * add state to the location list
     * @param e XML element
     * @param s state
     */
    private void record_location(Element e, State s)
    {
        List<Integer> loc = UmlUtil.parse_location(e);
        locations.add(new Pair<>(loc, s));
    }

    /**
     * merge all constraints from the states
     * @param states the states in the automaton diagram
     * @return the constraints
     */
    private List<Judgement> merge_constraints(List<State> states)
    {
        return states.stream()
                .map(State::getConstraints)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

}
