package edu.nju.seg.config;

import java.util.List;

public class ExCase {

    private final boolean runable;

    private final List<String> targets;

    private final String input_folder;

    private final String result_folder;

    public ExCase(boolean runable,
                  List<String> targets,
                  String input_folder,
                  String result_folder)
    {
        this.runable = runable;
        this.targets = targets;
        this.input_folder = input_folder;
        this.result_folder = result_folder;
    }

    public boolean is_runable() {
        return runable;
    }

    public List<String> get_targets()
    {
        return targets;
    }

    public String get_input_folder()
    {
        return input_folder;
    }

    public String get_result_folder()
    {
        return result_folder;
    }

}
