package edu.nju.seg.parser;

import edu.nju.seg.model.AltFragment;
import edu.nju.seg.model.Instance;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AltFragmentParser {

    private static final Pattern ALT_PATTERN = Pattern.compile("^alt\\s*\\(\\s*(.*)\\s*,\\s*(.*)\\s*\\)$");

    private final List<Instance> instances;

    private final String info;

    public AltFragmentParser(List<Instance> instances, String info)
    {
        this.instances = instances;
        this.info = info;
    }

    /**
     * construct alt fragment
     * @return alt fragment
     */
    public AltFragment parse_alt_fragment()
    {
        Matcher m = ALT_PATTERN.matcher(info);
        if (m.matches()) {
            return new AltFragment(new ArrayList<>(),
                    instances,
                    info,
                    JudgementsParser.parse_judgement(m.group(1)),
                    JudgementsParser.parse_judgement(m.group(2)),
                    new ArrayList<>());
        } else {
            return new AltFragment(new ArrayList<>(),
                    instances,
                    info,
                    new ArrayList<>());
        }
    }

}
