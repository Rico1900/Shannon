package edu.nju.seg.solver;

import com.microsoft.z3.Context;

public class Example {

    public static void main(String[] args) {
        new Example().simpleExample();
    }

    public void simpleExample()
    {
        System.out.println("SimpleExample");
        {
            Context ctx = new Context();
            /* do something with the context */

            /* be kind to dispose manually and not wait for the GC. */
            ctx.close();
        }
    }

}
