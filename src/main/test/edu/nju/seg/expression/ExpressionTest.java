package edu.nju.seg.expression;

import edu.nju.seg.expression.parser.ParserGenerator;
import org.junit.jupiter.api.Test;
import org.typemeta.funcj.data.Chr;
import org.typemeta.funcj.parser.Input;
import org.typemeta.funcj.parser.Parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExpressionTest {

    private final Parser<Chr, Expr> expr_parser = ParserGenerator.expression();

    @Test
    void test_empty()
    {
        assertThrows(RuntimeException.class, expr_parser.parse(Input.of(""))::getOrThrow);
    }

    @Test
    void test_variable()
    {
        assertEquals(new Variable("a"), expr_parser.parse(Input.of("a")).getOrThrow());
    }

    @Test
    void test_variable_1()
    {
        assertEquals(new Variable("a12"), expr_parser.parse(Input.of("a12")).getOrThrow());
    }

    @Test
    void test_variable_2()
    {
        assertEquals(new Variable("abc120"), expr_parser.parse(Input.of("abc120")).getOrThrow());
    }

    @Test
    void test_number()
    {
        assertEquals(new Number(10.0), expr_parser.parse(Input.of("10")).getOrThrow());
    }

    @Test
    void test_number_1()
    {
        assertEquals(new Number(12.1), expr_parser.parse(Input.of("12.1")).getOrThrow());
    }

    @Test
    void test_number_2()
    {
        assertEquals(new Number(0.0), expr_parser.parse(Input.of("0")).getOrThrow());
    }

    @Test
    void test_paren()
    {
        assertEquals(new BinaryExpr(BinaryOp.ADD,
                        new Variable("x"),
                        new Variable("y")),
                expr_parser.parse(Input.of("(x+y)")).getOrThrow());
    }

    @Test
    void test_paren_1()
    {
        assertEquals(new BinaryExpr(BinaryOp.ADD,
                        new BinaryExpr(BinaryOp.ADD,
                                new Variable("x"),
                                new Variable("y")),
                        new Number(8.0)),
                expr_parser.parse(Input.of("((x+y)+8)")).getOrThrow());
    }

    @Test
    void test_paren_2()
    {
        assertEquals(new BinaryExpr(BinaryOp.MUL,
                        new Variable("k"),
                        new BinaryExpr(BinaryOp.ADD,
                                new BinaryExpr(BinaryOp.ADD,
                                        new Variable("x"),
                                        new Variable("y")),
                                new Number(8.0))),
                expr_parser.parse(Input.of("(k*((x+y)+8))")).getOrThrow());
    }

}
