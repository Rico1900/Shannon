package edu.nju.seg.metric;

public class ExperimentalData {

    private final int bound;

    private final int clause_num;

    private double running_time;

    public ExperimentalData(int bound, int clause_num)
    {
        this.bound = bound;
        this.clause_num = clause_num;
    }

    public double get_running_time()
    {
        return running_time;
    }

    public void set_running_time(double running_time)
    {
        this.running_time = running_time;
    }

    public int get_bound()
    {
        return bound;
    }

    public int get_clause_num()
    {
        return clause_num;
    }

    @Override
    public String toString() {
        return "ExperimentalData{" +
                "bound=" + bound +
                ", clause_num=" + clause_num +
                ", running_time=" + running_time +
                '}';
    }
}
