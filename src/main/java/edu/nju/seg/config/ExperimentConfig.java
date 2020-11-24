package edu.nju.seg.config;


import java.util.List;

public class ExperimentConfig {

    private final boolean debug;

    private final ExperimentalType type;

    private final int bound;

    private final String base_path;

    private final List<ExCase> cases;

    public ExperimentConfig(boolean debug,
                            ExperimentalType type,
                            int bound,
                            String base_path,
                            List<ExCase> cases)
    {
        this.debug = debug;
        this.type = type;
        this.bound = bound;
        this.base_path = base_path;
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

    public String get_base_path() {
        return base_path;
    }

    public List<ExCase> get_cases()
    {
        return cases;
    }
}
