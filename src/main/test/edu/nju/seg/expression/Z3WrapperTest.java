package edu.nju.seg.expression;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import edu.nju.seg.exception.Z3Exception;
import edu.nju.seg.util.Z3Wrapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Z3WrapperTest {

    @Test
    public void test_mk_neg() throws Z3Exception
    {
        Context c = new Context();
        Z3Wrapper w = new Z3Wrapper(c);
        ArithExpr a = w.mk_real("1");
        assertEquals("(- 0.0 1.0)", w.mk_neg(a).toString());
    }

}
