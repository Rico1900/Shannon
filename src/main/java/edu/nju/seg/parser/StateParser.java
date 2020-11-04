package edu.nju.seg.parser;

import edu.nju.seg.exception.ParseException;
import edu.nju.seg.expression.DeEquation;
import edu.nju.seg.expression.Judgement;
import edu.nju.seg.expression.parser.ParserGenerator;
import edu.nju.seg.model.Element;
import edu.nju.seg.model.State;
import edu.nju.seg.model.StateType;
import edu.nju.seg.model.UMLType;
import edu.nju.seg.util.$;
import org.typemeta.funcj.data.Chr;
import org.typemeta.funcj.parser.Input;
import org.typemeta.funcj.parser.Parser;
import org.typemeta.funcj.parser.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StateParser {

    private static final Pattern SPECIAL_STATE_PATTERN = Pattern.compile("^type=(.*)$");

    private static final Pattern STATE_PATTERN = Pattern.compile("^(.*)--(.*)-\\.(.*)valign=top.*$", Pattern.DOTALL);

    private static final Parser<Chr, Judgement> CONSTRAINT_PARSER = ParserGenerator.judgement();

    private static final Parser<Chr, DeEquation> EQUATION_PARSER = ParserGenerator.differential_equation();

    private final Element e;

    public StateParser(Element e)
    {
        this.e = e;
    }

    public State parse()
    {
        if (e.getType() == UMLType.UMLSpecialState) {
            return parse_special_state();
        } else if (e.getType() == UMLType.UMLState) {
            return parse_state();
        } else {
            throw new ParseException("the given element is not state node, the content of the element: " + e.getContent());
        }
    }

    private State parse_state()
    {
        State s = new State();
        Matcher m = STATE_PATTERN.matcher(e.getContent());
        if (m.matches()) {
            s.setType(StateType.NORMAL);
            s.setStateName(m.group(1).trim().replace("\n", "").replace("\r", ""));
            s.setDeEquations(parse_equations(get_meaningful_str_list(m.group(2))));
            s.setConstraints(parse_constraints(get_meaningful_str_list(m.group(3))));
            s.setOuters(new ArrayList<>());
        } else {
            throw new ParseException("wrong state element content");
        }
        return s;
    }

    private State parse_special_state()
    {
        State s = new State();
        Matcher m = SPECIAL_STATE_PATTERN.matcher(e.getContent());
        if (m.matches()) {
            String t = m.group(1);
            if (t.equals("initial")) {
                s.setType(StateType.INITIAL);
                s.setStateName("INIT");
            } else if (t.equals("final")) {
                s.setType(StateType.FINAL);
                s.setStateName("FINAL");
            } else {
                throw new ParseException("wrong special node type");
            }
        } else {
            throw new ParseException("wrong special node");
        }
        s.setDeEquations(new ArrayList<>());
        s.setConstraints(new ArrayList<>());
        return s;
    }

    private List<DeEquation> parse_equations(List<String> sl)
    {
        return sl.stream()
                .map(Input::of)
                .map(EQUATION_PARSER::parse)
                .map(Result::getOrThrow)
                .collect(Collectors.toList());
    }

    private List<Judgement> parse_constraints(List<String> sl)
    {
        return sl.stream()
                .map(Input::of)
                .map(CONSTRAINT_PARSER::parse)
                .map(Result::getOrThrow)
                .collect(Collectors.toList());
    }

    private List<String> get_meaningful_str_list(String s)
    {
        return $.filter_blank_str(Arrays.asList(s.split("\n")));
    }

}
