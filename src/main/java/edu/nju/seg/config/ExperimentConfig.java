package edu.nju.seg.config;


import java.util.List;

public class ExperimentConfig {

    private final boolean debug;

    private final ExperimentalType type;

    private final int bound;

    private final List<ExCase> cases;

    public ExperimentConfig(boolean debug,
                            ExperimentalType type,
                            int bound,
                            List<ExCase> cases)
    {
        this.debug = debug;
        this.type = type;
        this.bound = bound;
        this.cases = cases;
    }

    public ExperimentalType get_type() {
        return type;
    }

    public int get_bound() {
        return bound;
    }

    public boolean is_debug() {
        return debug;
    }

    public List<ExCase> get_cases()
    {
        return cases;
    }
}
