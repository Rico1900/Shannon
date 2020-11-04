package edu.nju.seg.parser;

import edu.nju.seg.expression.Assignment;
import edu.nju.seg.expression.parser.ParserGenerator;
import edu.nju.seg.util.$;
import org.typemeta.funcj.data.Chr;
import org.typemeta.funcj.parser.Input;
import org.typemeta.funcj.parser.Parser;
import org.typemeta.funcj.parser.Result;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AssignmentsParser {

    private final static Parser<Chr, Assignment> p = ParserGenerator.assignment();

    /**
     * parse assignments on the edge
     * @param sl the assignment string
     * @return the assignments
     */
    public static List<Assignment> parse_assignments(String sl)
    {
        return Arrays.stream(sl.split(","))
                .map($::remove_whitespace)
                .map(Input::of)
                .map(p::parse)
                .map(Result::getOrThrow)
                .collect(Collectors.toList());
    }

    public static Assignment parse_assignment(String s)
    {
        return p.parse(Input.of($.remove_whitespace(s))).getOrThrow();
    }

}
