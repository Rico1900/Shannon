package edu.nju.seg.expression;

import edu.nju.seg.expression.parser.ParserGenerator;
import org.junit.jupiter.api.Test;
import org.typemeta.funcj.data.Chr;
import org.typemeta.funcj.parser.Input;
import org.typemeta.funcj.parser.Parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssignmentTest {

    private final Parser<Chr, Expr> assign_parser = ParserGenerator.assignment();

    @Test
    void test_assign_number()
    {
        assertEquals(new Assignment(new Variable("x"), new Number(10.0)),
                assign_parser.parse(Input.of("x=10")).getOrThrow());
    }

    @Test
    void test_assign_var()
    {
        assertEquals(new Assignment(new Variable("x"), new Variable("a12")),
                assign_parser.parse(Input.of("x=a12")).getOrThrow());
    }

    @Test
    void test_assign_simple_paren()
    {
        assertEquals(new Assignment(
                new Variable("x"),
                new BinaryExpr(BinaryOp.ADD,
                new Variable("a1"), new Number(10.0))),
                assign_parser.parse(Input.of("x=(a1+10.0)")).getOrThrow());
    }

    @Test
    void test_assign_complex_paren()
    {
        assertEquals(new Assignment(new Variable("x12"),
                new BinaryExpr(BinaryOp.DIV,
                        new BinaryExpr(BinaryOp.ADD,
                                new Variable("x"),
                                new Number(12.1)),
                        new Number(2.0))),
                assign_parser.parse(Input.of("x12=((x+12.1)/2)")).getOrThrow());
    }

}
