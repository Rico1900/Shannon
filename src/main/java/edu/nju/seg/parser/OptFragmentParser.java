package edu.nju.seg.parser;

import edu.nju.seg.model.Instance;
import edu.nju.seg.model.OptFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OptFragmentParser {

    private static final Pattern OPT_PATTERN = Pattern.compile("^opt\\s*\\((.*)\\)");

    private final List<Instance> instances;

    private final String info;

    public OptFragmentParser(List<Instance> instances, String info)
    {
        this.instances = instances;
        this.info = info;
    }

    /**
     * construct opt fragment
     * @return opt fragment
     */
    public OptFragment parse_opt_fragment()
    {
        Matcher m = OPT_PATTERN.matcher(info);
        if (m.matches()) {
            return new OptFragment(new ArrayList<>(), instances, info, JudgementsParser.parse_judgement(m.group(1)));
        } else {
            return new OptFragment(new ArrayList<>(), instances, info);
        }
    }

}
