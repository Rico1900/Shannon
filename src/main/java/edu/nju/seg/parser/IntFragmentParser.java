package edu.nju.seg.parser;

import edu.nju.seg.exception.ParseException;
import edu.nju.seg.model.Instance;
import edu.nju.seg.model.IntFragment;
import edu.nju.seg.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntFragmentParser {

    private static final Pattern FULL_INT_PATTERN = Pattern.compile("^int\\s*\\(p=(.*)\\)\\s*\\((.*),(.*)\\)\\s*\\[(.*)\\]\\s*$");

    private static final Pattern LIMIT_INT_PATTERN = Pattern.compile("^int\\s*\\(p=(.*)\\)\\s*\\((.*),(.*)\\)\\s*$");

    private static final Pattern INSTRUCTION_INT_PATTERN = Pattern.compile("^int\\s*\\(p=(.*)\\)\\s*\\[(.*)\\]\\s*$");

    private static final Pattern SIMPLE_INT_PATTERN = Pattern.compile("^int\\s*\\(p=(.*)\\)\\s*$");

    private final List<Instance> instances;

    private final String info;

    public IntFragmentParser(List<Instance> instances, String info)
    {
        this.instances = instances;
        this.info = info;
    }

    /**
     * construct interrupt fragment
     * @return interrupt fragment
     */
    public IntFragment parse_int_fragment()
    {
        Pair<Integer, Integer> p = count_brackets();
        int bracket = p.get_left();
        int parenthesis = p.get_right();
        if (bracket == 1 && parenthesis == 2) {
            Matcher m = FULL_INT_PATTERN.matcher(info);
            checkIntFragMat(m);
            return new IntFragment(new ArrayList<>(),
                    instances,
                    info,
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)),
                    AssignmentsParser.parse_assignment(m.group(4)));
        } else if (bracket == 0 && parenthesis == 2) {
            Matcher m = LIMIT_INT_PATTERN.matcher(info);
            checkIntFragMat(m);
            return new IntFragment(new ArrayList<>(),
                    instances,
                    info,
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)));
        } else if (bracket == 1 && parenthesis == 1) {
            Matcher m = INSTRUCTION_INT_PATTERN.matcher(info);
            checkIntFragMat(m);
            return new IntFragment(new ArrayList<>(),
                    instances,
                    info,
                    Integer.parseInt(m.group(1)),
                    1,
                    1,
                    AssignmentsParser.parse_assignment(m.group(4)));
        } else if (bracket == 0 && parenthesis == 1) {
            Matcher m = SIMPLE_INT_PATTERN.matcher(info);
            checkIntFragMat(m);
            return new IntFragment(new ArrayList<>(), instances, info, Integer.parseInt(m.group(1)));
        } else {
            throw new ParseException("wrong int fragment modeling language: " + info);
        }
    }

    private Pair<Integer, Integer> count_brackets()
    {
        int bracket_count = 0;
        int parenthesis_count = 0;
        for (char c: info.toCharArray()) {
            if (c == '[') {
                bracket_count++;
            }
            if (c == '(') {
                parenthesis_count++;
            }
        }
        return new Pair<>(bracket_count, parenthesis_count);
    }

    /**
     * check if the interrupt fragment is right
     * @param m match status
     */
    private void checkIntFragMat(Matcher m)
    {
        if (!m.find()) {
            throw new ParseException("wrong int fragment modeling language");
        }
    }

}
