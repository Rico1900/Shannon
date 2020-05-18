package edu.nju.seg.util;

public class SimpleLog {

    /**
     * print error information to the error stream
     * @param info error information
     */
    public static void error(String info)
    {
        System.err.println("*****************************");
        System.err.println("* " + info);
        System.err.println("*****************************");
    }
}
