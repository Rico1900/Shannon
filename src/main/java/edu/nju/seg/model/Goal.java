package edu.nju.seg.model;

import edu.nju.seg.exception.ParseException;
import edu.nju.seg.expression.Expr;
import edu.nju.seg.expression.parser.ParserGenerator;
import edu.nju.seg.util.$;
import org.typemeta.funcj.data.Chr;
import org.typemeta.funcj.parser.Input;
import org.typemeta.funcj.parser.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Goal {

    private static final Pattern GOAL_PATTERN = Pattern.compile("^(max|min|MAX|MIN|Max|Min)\\((.*)\\)$");

    private static final Parser<Chr, Expr> expr_parser = ParserGenerator.v_expr();

    private final OptimizationType type;

    private final Expr expression;

    public Goal(OptimizationType type,
                Expr expression) {
        this.type = type;
        this.expression = expression;
    }

    public OptimizationType getType() {
        return type;
    }

    public Expr getExpression() {
        return expression;
    }

    public static List<Goal> parse_goals(List<String> strings)
    {
        if ($.isBlankList(strings)) {
            return new ArrayList<>();
        }
        return strings.stream()
                .map(Goal::convert_to_goal)
                .collect(Collectors.toList());
    }

    private static Goal convert_to_goal(String str)
    {
        Matcher m = GOAL_PATTERN.matcher(str);
        if (m.matches()) {
            if (m.group(1).toUpperCase().equals(OptimizationType.MAX.name())) {
                return new Goal(
                        OptimizationType.MAX,
                        expr_parser.parse(Input.of(m.group(2))).getOrThrow()
                );
            } else {
                return new Goal(
                        OptimizationType.MIN,
                        expr_parser.parse(Input.of(m.group(2))).getOrThrow()
                );
            }
        } else {
            throw new ParseException("wrong goal expression: " + str);
        }
    }

}
