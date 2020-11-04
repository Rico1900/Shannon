package edu.nju.seg.parser;

import edu.nju.seg.exception.ParseException;
import edu.nju.seg.expression.Assignment;
import edu.nju.seg.expression.Judgement;
import edu.nju.seg.model.Relation;
import edu.nju.seg.model.RelationElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RelationParser {

    private static final Pattern RELATION_PATTERN = Pattern.compile("^lt=->(.*)$", Pattern.DOTALL);

    private static final Pattern RELATION_DETAIL = Pattern.compile("^(.*?):(.*);(.*);?$", Pattern.DOTALL);

    private final RelationElement re;

    public RelationParser(RelationElement re)
    {
        this.re = re;
    }

    public Relation parse()
    {
        Matcher m = RELATION_PATTERN.matcher(re.getContent());
        if (m.matches()) {
            Matcher dm = RELATION_DETAIL.matcher(m.group(1));
            if (dm.matches()) {
                String name = dm.group(1).trim().replace("\n", "").replace("\r", "");
                List<Judgement> judgements = JudgementsParser.parse_judgements(dm.group(2));
                List<Assignment> assignments = AssignmentsParser.parse_assignments(dm.group(3));
                return new Relation(name, judgements, assignments);
            } else {
                return new Relation(m.group(1).trim().replace("\n", "").replace("\r", ""),
                        new ArrayList<>(0), new ArrayList<>(0));
            }
        } else {
            throw new ParseException("Wrong relation element: " + re.getContent());
        }
    }

}
