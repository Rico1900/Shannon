package edu.nju.seg.expression.parser;

import edu.nju.seg.expression.*;
import edu.nju.seg.expression.Number;
import org.typemeta.funcj.data.Chr;
import org.typemeta.funcj.data.IList;
import org.typemeta.funcj.parser.Parser;
import org.typemeta.funcj.parser.Ref;

import java.util.Optional;

import static org.typemeta.funcj.parser.Combinators.choice;
import static org.typemeta.funcj.parser.Text.*;

public class ParserGenerator {

    /**
     * construct a parser which parses expressions
     * @return the parser
     */
    public static Parser<Chr, Expr> expression()
    {
        Ref<Chr, Expr> expr = Parser.ref();
        Parser<Chr, Expr> bin_expr = chr('(')
                .andR(expr)
                .and(binary_op())
                .and(expr)
                .andL(chr(')'))
                .map((l, op, r) -> new BinaryExpr(op, l, r));
        expr.set(choice(number_or_variable(), bin_expr));
        return expr;
    }

    public static Parser<Chr, AdJudgement> general_judgement()
    {
        return choice(forall_judgement(), exists_judgement());
    }

    /**
     * construct a parser which parses judgements
     * @return the parser
     */
    public static Parser<Chr, Judgement> judgement()
    {
        return left_expr()
                .and(judge_op())
                .and(expression())
                .map((l, op, r) -> new Judgement(op, l, r));
    }

    /**
     * construct a parser which parses forall judgements like "forall(x<=5)"
     * @return the parser
     */
    static Parser<Chr, AdJudgement> forall_judgement()
    {
        return string("forall(").andR(judgement())
                .andL(chr(')'))
                .map(j -> new AdJudgement(UnaryOp.FORALL, j));
    }

    /**
     * construct a parser which parses exists judgements like "exists(x>10)"
     * @return the parser
     */
    static Parser<Chr, AdJudgement> exists_judgement()
    {
        return string("exists(").andR(judgement())
                .andL(chr(')'))
                .map(j -> new AdJudgement(UnaryOp.EXISTS, j));
    }

    /**
     * construct a parser which parses assignments
     * @return the parser
     */
    public static Parser<Chr, Assignment> assignment()
    {
        return variable()
                .andL(string(":="))
                .and(expression())
                .map(Assignment::new);
    }

    /**
     * construct a parser which parses differential equations like "v'=5"
     * @return the parser
     */
    public static Parser<Chr, DeEquation> differential_equation()
    {
        return differential_expr()
                .andL(chr('='))
                .and(expression())
                .map(DeEquation::new);
    }

    /**
     * construct a parser which parses expressions consisting of variables
     * @return the parser
     */
    public static Parser<Chr, Expr> v_expr()
    {
        Ref<Chr, Expr> expr = Parser.ref();
        Parser<Chr, Expr> bin_expr = chr('(')
                .andR(expr)
                .and(binary_op())
                .and(expr)
                .andL(chr(')'))
                .map((l, op, r) -> new BinaryExpr(op, l, r));
        expr.set(choice(variable(), bin_expr));
        return expr;
    }

    /**
     * construct a parser which parses variables
     * in the form of "aa" or "aa12"
     * @return the parser
     */
    static Parser<Chr, Variable> variable()
    {
        return alpha.many1().and(digit.many())
                .map(IList::appendAll)
                .map(ls -> ls.foldLeft((acc, x) -> acc + x.toString(), ""))
                .map(Variable::new);
    }


    /**
     * construct a parser which parses left expressions
     * @return the parser
     */
    static Parser<Chr, Expr> left_expr()
    {
        return choice(
                v_expr(),
                abs_expr(),
                task_expr()
        );
    }

    /**
     * construct a parser which parses absolute expressions like "|x|" or "|(x-y)|"
     * @return the parser
     */
    static Parser<Chr, UnaryExpr> abs_expr()
    {
        return v_expr().between(chr('|'), chr('|'))
                .map(e -> new UnaryExpr(UnaryOp.ABS, e));
    }

    /**
     * construct a parser which parses task expressions like "^(x-y)"
     * @return the parser
     */
    static Parser<Chr, UnaryExpr> task_expr()
    {
        return chr('^').andR(v_expr())
                .map(e -> new UnaryExpr(UnaryOp.TASK_TIME, e));
    }

    /**
     * construct a parser which parses differential variable expression like "v'"
     * @return the parser
     */
    static Parser<Chr, UnaryExpr> differential_expr()
    {
        return variable().andL(chr('\''))
                .map(e -> new UnaryExpr(UnaryOp.DIFFERENTIAL, e));
    }

    /**
     * construct a parser which parses numbers in the form of "1" or "2.3"
     * @return the parser
     */
    static Parser<Chr, Expr> number()
    {
        return dble.map(Number::new);
    }

    /**
     * construct a parser which parses numbers or variables
     * @return the parser
     */
    static Parser<Chr, Expr> number_or_variable()
    {
        return number().or(variable());
    }

    /**
     * construct a parser which parses binary operations
     * @return the parser
     */
    static Parser<Chr, BinaryOp> binary_op()
    {
        return choice(
                chr('+').map(c -> BinaryOp.ADD),
                chr('-').map(c -> BinaryOp.SUB),
                chr('*').map(c -> BinaryOp.MUL),
                chr('/').map(c -> BinaryOp.DIV)
        );
    }

    /**
     * construct a parser which parses judgement operations
     * @return the parser
     */
    static Parser<Chr, JudgeOp> judge_op()
    {
        return choice(
                string("==").map(s -> JudgeOp.EQ),
                chr('<').andR(eq_or_nil())
                        .map(c -> c.isPresent() ? JudgeOp.LE : JudgeOp.LT),
                chr('>').andR(eq_or_nil())
                        .map(c -> c.isPresent() ? JudgeOp.GE : JudgeOp.GT)
        );
    }

    /**
     * construct a parser which parses the symbol '=' or nil
     * @return the parser
     */
    static Parser<Chr, Optional<Chr>> eq_or_nil()
    {
        return chr('=').optional();
    }

}
