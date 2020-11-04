package edu.nju.seg.parser;

import edu.nju.seg.expression.Assignment;
import edu.nju.seg.model.Instance;
import edu.nju.seg.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageParser {

    private static final Pattern MESSAGE_DETAIL = Pattern.compile("^(.*?):(.*);(.*);?$");

    private final Map<String, Instance> instance_map;

    private final String from_var;

    private final String to_var;

    private final String info;

    public MessageParser(Map<String, Instance> instance_map,
                         String from,
                         String to,
                         String info)
    {
        this.instance_map = instance_map;
        this.from_var = from;
        this.to_var = to;
        this.info = info;
    }

    /**
     * parse a message on the sequence diagram
     * @return the message
     */
    public Message parse_message()
    {
        Instance from = instance_map.get(from_var);
        Instance to = instance_map.get(to_var);
        String name;
        List<Assignment> assignments;
        Assignment mask_instruction;
        Matcher m = MESSAGE_DETAIL.matcher(info);
        if (m.matches()) {
            name = m.group(1);
            assignments = AssignmentsParser.parse_assignments(m.group(2));
            mask_instruction = AssignmentsParser.parse_assignment(m.group(3));
        } else {
            name = info.replace(";", "").trim();
            assignments = new ArrayList<>(0);
            mask_instruction = null;
        }
        return new Message(name, assignments, mask_instruction, from, to);
    }


}
