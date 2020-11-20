package edu.nju.seg.config;


import java.util.List;

public class ExperimentConfig {

    private final boolean debug;

    private final ExperimentalType type;

    private final int bound;

    private final List<String> targets;

    private final String input_folder;

    private final String result_folder;

    public ExperimentConfig(boolean debug,
                            ExperimentalType type,
                            int bound,
                            List<String> targets,
                            String input_folder,
                            String result_folder)
    {
        this.debug = debug;
        this.type = type;
        this.bound = bound;
        this.targets = targets;
        this.input_folder = input_folder;
        this.result_folder = result_folder;
    }

    public ExperimentalType get_type() {
        return type;
    }

    public int get_bound() {
        return bound;
    }

    public List<String> get_targets() {
        return targets;
    }

    public String get_input_folder() {
        return input_folder;
    }

    public String get_result_folder() {
        return result_folder;
    }

    public boolean is_debug() {
        return debug;
    }

}
