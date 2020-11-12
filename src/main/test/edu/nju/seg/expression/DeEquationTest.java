package edu.nju.seg.expression;

import edu.nju.seg.expression.parser.ParserGenerator;
import org.junit.jupiter.api.Test;
import org.typemeta.funcj.data.Chr;
import org.typemeta.funcj.parser.Input;
import org.typemeta.funcj.parser.Parser;

public class DeEquationTest {

    Parser<Chr, DeEquation> p = ParserGenerator.differential_equation();

    @Test
    void test_1()
    {
        p.parse(Input.of("'q=0")).getOrThrow();
    }

    @Test
    void test_2()
    {
        p.parse(Input.of("'q>9")).getOrThrow();
    }

    @Test
    void test_3()
    {
        p.parse(Input.of("'q=('inq-'outq)")).getOrThrow();
    }

}
