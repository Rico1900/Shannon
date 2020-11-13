package edu.nju.seg.metric;

public class SimpleTimer {

    private final long start;

    public SimpleTimer()
    {
        start = System.currentTimeMillis();
    }

    /**
     * count the duration past since the start of the timer in seconds
     * @return the past time in seconds
     */
    public double past_seconds()
    {
        double now = System.currentTimeMillis();
        return (now - start) / 1000;
    }

}
