package edu.nju.seg;

import com.microsoft.z3.*;
import edu.nju.seg.exception.Z3Exception;
import edu.nju.seg.util.Z3Wrapper;

public class Z3Playground {

    public static void main(String[] args) throws Z3Exception {
        Context c = new Context();
        Optimize o = c.mkOptimize();
        Z3Wrapper w = new Z3Wrapper(c);
//        IntExpr x = w.mkIntVar("x");
//        IntExpr y = w.mkIntVar("y");
        RealExpr x = w.mkRealVar("x");
        RealExpr y = w.mkRealVar("y");
        o.Add(c.mkLe(x, c.mkInt(2)));
        o.Add(c.mkLe(c.mkSub(y , x), c.mkInt(1)));
        Optimize.Handle h = o.MkMaximize(c.mkAdd(x, y));
        System.out.println(o.Check());
        System.out.println(h.getValue());
    }

}
