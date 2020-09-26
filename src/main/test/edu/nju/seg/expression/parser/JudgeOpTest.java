package edu.nju.seg.expression.parser;

import edu.nju.seg.expression.JudgeOp;
import org.junit.jupiter.api.Test;
import org.typemeta.funcj.data.Chr;
import org.typemeta.funcj.parser.Input;
import org.typemeta.funcj.parser.Parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JudgeOpTest {

    Parser<Chr, JudgeOp> p = ParserGenerator.judge_op();

    @Test
    void test_1()
    {
        assertEquals(JudgeOp.EQ, p.parse(Input.of("==")).getOrThrow());
    }

    @Test
    void test_2()
    {
        assertEquals(JudgeOp.LT, p.parse(Input.of("<")).getOrThrow());
    }

    @Test
    void test_3()
    {
        assertEquals(JudgeOp.LE, p.parse(Input.of("<=")).getOrThrow());
    }

    @Test
    void test_4()
    {
        assertEquals(JudgeOp.GT, p.parse(Input.of(">")).getOrThrow());
    }

    @Test
    void test_5()
    {
        assertEquals(JudgeOp.GE, p.parse(Input.of(">=")).getOrThrow());
    }

}
