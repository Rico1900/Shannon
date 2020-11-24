package edu.nju.seg.metric;

public class ExperimentalData {

    private final String case_name;

    private final int bound;

    private final int clause_num;

    private double encoding_time;

    private double running_time;

    public ExperimentalData(String file_path,
                            int bound,
                            int clause_num)
    {
        String[] splits = file_path.split("/");
        this.case_name = splits[splits.length - 1];
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

    public double get_encoding_time() {
        return encoding_time;
    }

    public void set_encoding_time(double encoding_time) {
        this.encoding_time = encoding_time;
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
                "case_name='" + case_name + '\'' +
                ", bound=" + bound +
                ", clause_num=" + clause_num +
                ", encoding_time=" + encoding_time +
                ", running_time=" + running_time +
                '}';
    }

}
