package edu.nju.seg.metric;

public class SimpleTimer {

    private long start;

    public SimpleTimer()
    {
        start = System.currentTimeMillis();
    }

    /**
     * count the duration past since the start of the timer in seconds
     * @return seconds
     */
    public double pastSeconds()
    {
        double now = System.currentTimeMillis();
        return (now - start) / 1000;
    }

}
