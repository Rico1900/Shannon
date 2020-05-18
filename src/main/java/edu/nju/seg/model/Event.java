package edu.nju.seg.model;

import edu.nju.seg.util.Pair;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    private String name;

    /** the interruption mask update command **/
    private String instruction;

    /** which the event belongs to **/
    private Instance belongTo;

    private boolean virtual = false;

    public Pair<String, Integer> parseInstruction()
    {
        if (instruction != null) {
            String[] splits = instruction.split("=");
            return new Pair<>(splits[0].trim(), Integer.parseInt(splits[1].trim()));
        } else {
            return new Pair<>();
        }
    }

}
