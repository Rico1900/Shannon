package edu.nju.seg.model;

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

}
