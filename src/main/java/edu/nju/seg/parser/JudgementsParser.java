package edu.nju.seg.parser;

import edu.nju.seg.expression.Judgement;
import edu.nju.seg.expression.parser.ParserGenerator;
import edu.nju.seg.util.$;
import org.typemeta.funcj.data.Chr;
import org.typemeta.funcj.parser.Input;
import org.typemeta.funcj.parser.Parser;
import org.typemeta.funcj.parser.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JudgementsParser {

    private final static Parser<Chr, Judgement> JUDGEMENT_PARSER = ParserGenerator.judgement();

    /**
     * parse judgements on the edge
     * @param sl the judgement string
     * @return the judgements
     */
    public static List<Judgement> parse_judgements(String sl)
    {
        if ($.isBlank(sl)) {
            return new ArrayList<>(0);
        }
        return Arrays.stream(sl.split(","))
                .map($::remove_whitespace)
                .map(Input::of)
                .map(JUDGEMENT_PARSER::parse)
                .map(Result::getOrThrow)
                .collect(Collectors.toList());
    }

    public static Judgement parse_judgement(String s)
    {
        return JUDGEMENT_PARSER.parse(Input.of($.remove_whitespace(s))).getOrThrow();
    }

}
