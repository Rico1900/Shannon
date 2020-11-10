package edu.nju.seg.parser;

import edu.nju.seg.expression.AdJudgement;
import edu.nju.seg.expression.Judgement;
import edu.nju.seg.expression.parser.ParserGenerator;
import edu.nju.seg.util.$;
import org.typemeta.funcj.data.Chr;
import org.typemeta.funcj.parser.Input;
import org.typemeta.funcj.parser.Parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JudgementsParser {

    private final static Parser<Chr, Judgement> JUDGEMENT_PARSER = ParserGenerator.judgement();

    private final static Parser<Chr, AdJudgement> AD_JUDGEMENT_PARSER = ParserGenerator.general_judgement();

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
        return parse_judgements(Arrays.asList(sl.split(",")));
    }

    public static List<Judgement> parse_judgements(List<String> list)
    {
        List<Judgement> result = new ArrayList<>();
        for (String s: list) {
            String clean = $.remove_whitespace(s);
            if ($.is_ad_judgement(clean)) {
                result.add(AD_JUDGEMENT_PARSER.parse(Input.of(clean)).getOrThrow());
            } else {
                result.add(JUDGEMENT_PARSER.parse(Input.of(clean)).getOrThrow());
            }
        }
        return result;
    }

    public static Judgement parse_judgement(String s)
    {
        return JUDGEMENT_PARSER.parse(Input.of($.remove_whitespace(s))).getOrThrow();
    }

}
