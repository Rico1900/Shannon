package edu.nju.seg.parser;

import edu.nju.seg.exception.ParseException;
import edu.nju.seg.model.Instance;
import edu.nju.seg.model.LoopFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoopFragmentParser {

    private static final Pattern LOOP_PATTERN = Pattern.compile("^loop\\s*\\(\\s*(.*)\\s*,\\s*(.*)\\s*\\)$");

    private static final Pattern TIMER_LOOP = Pattern.compile("^loop\\((.*),(.*)\\)\\[(.*),(.*)\\]$");

    private final List<Instance> instances;

    private final String info;

    public LoopFragmentParser(List<Instance> instances, String info)
    {
        this.instances = instances;
        this.info = info;
    }

    /**
     * construct loop fragment
     * @return loop fragment
     */
    public LoopFragment parse_loop_fragment()
    {
        Matcher m = LOOP_PATTERN.matcher(info);
        Matcher tm = TIMER_LOOP.matcher(info);
        if (tm.matches()) {
            return new LoopFragment(new ArrayList<>(),
                    instances,
                    info,
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    AssignmentsParser.parse_assignment(m.group(3)),
                    AssignmentsParser.parse_assignment(m.group(4)));
        } else if (m.matches()) {
            return new LoopFragment(new ArrayList<>(),
                    instances,
                    info,
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)));
        } else {
            throw new ParseException("error loop fragment: " + info);
        }
    }


}
