package edu.nju.seg;

import com.microsoft.z3.*;
import edu.nju.seg.exception.Z3Exception;

public class Z3Playground {

    public static void main(String[] args) throws Z3Exception {
        Context c = new Context();
        BitVecSort bvs = c.mkBitVecSort(32);
        BitVecExpr x = (BitVecExpr) c.mkConst("x", bvs);
        BitVecExpr a = (BitVecExpr) c.mkConst("a", bvs);
        Solver s = c.mkSolver();
        s.add(c.mkNot(c.mkEq(c.mkBVSMod(x, a), c.mkBVSMod(c.mkBVAdd(x, a), a))));
        System.out.println(s.check());
        System.out.println(s.getModel());
    }

}
