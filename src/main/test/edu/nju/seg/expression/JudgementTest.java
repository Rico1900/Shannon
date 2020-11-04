package edu.nju.seg.expression;

import edu.nju.seg.expression.parser.ParserGenerator;
import org.junit.jupiter.api.Test;
import org.typemeta.funcj.data.Chr;
import org.typemeta.funcj.parser.Input;
import org.typemeta.funcj.parser.Parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JudgementTest {

    private final Parser<Chr, Judgement> j_parser = ParserGenerator.judgement();

    @Test
    void test_simple_judge()
    {
        assertEquals(new Judgement(JudgeOp.LT,
                        new Variable("x"),
                        new Variable("y")),
                j_parser.parse(Input.of("x<y")).getOrThrow());
    }

    @Test
    void test_sub_judge()
    {
        assertEquals(new Judgement(JudgeOp.LE,
                        new BinaryExpr(BinaryOp.SUB,
                                new Variable("x"),
                                new Variable("y")),
                        new Number(2.0)),
                j_parser.parse(Input.of("(x-y)<=2")).getOrThrow());
    }

    @Test
    void test_simple_abs_judge()
    {
        assertEquals(new Judgement(JudgeOp.LT,
                        new UnaryExpr(UnaryOp.ABS,
                                new Variable("x")),
                        new Number(2.0)),
                j_parser.parse(Input.of("|x|<2")).getOrThrow());
    }

    @Test
    void test_sub_abs_judge()
    {
        assertEquals(new Judgement(JudgeOp.LE,
                        new UnaryExpr(UnaryOp.ABS,
                                new BinaryExpr(BinaryOp.SUB,
                                        new Variable("x"),
                                        new Variable("y"))),
                        new Number(20D)),
                j_parser.parse(Input.of("|(x-y)|<=20")).getOrThrow());
    }

    @Test
    void test_task_time_judge()
    {
        assertEquals(new Judgement(JudgeOp.LE,
                        new UnaryExpr(UnaryOp.TASK_TIME,
                                new BinaryExpr(BinaryOp.SUB,
                                        new Variable("x"),
                                        new Variable("y"))),
                        new Number(20D)),
                j_parser.parse(Input.of("^(x-y)<=20")).getOrThrow());
    }

    @Test
    void test_complex()
    {
        assertEquals(new Judgement(JudgeOp.GE,
                        new UnaryExpr(UnaryOp.ABS,
                                new BinaryExpr(BinaryOp.SUB,
                                        new Variable("x"),
                                        new Variable("y"))),
                        new BinaryExpr(BinaryOp.ADD,
                                new Variable("a1"),
                                new BinaryExpr(BinaryOp.SUB,
                                        new BinaryExpr(BinaryOp.DIV,
                                                new Variable("b1"),
                                                new Variable("c1")),
                                        new Number(2D)))),
                j_parser.parse(Input.of("|(x-y)|>=(a1+((b1/c1)-2))")).getOrThrow());
    }

}
