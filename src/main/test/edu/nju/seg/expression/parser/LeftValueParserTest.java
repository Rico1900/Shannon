package edu.nju.seg.expression.parser;

import edu.nju.seg.expression.BinaryExpr;
import edu.nju.seg.expression.BinaryOp;
import edu.nju.seg.expression.Expr;
import edu.nju.seg.expression.Variable;
import org.junit.jupiter.api.Test;
import org.typemeta.funcj.data.Chr;
import org.typemeta.funcj.parser.Input;
import org.typemeta.funcj.parser.Parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LeftValueParserTest {

    Parser<Chr, Expr> p = ParserGenerator.left_expr();

    @Test
    void test_single_variable()
    {
        assertEquals(new Variable("x"),
                p.parse(Input.of("x")).getOrThrow());
    }

    @Test
    void test_two_variables()
    {
        assertEquals(new BinaryExpr(BinaryOp.SUB,
                        new Variable("x"),
                        new Variable("y")),
                p.parse(Input.of("x-y")).getOrThrow());
    }



}
