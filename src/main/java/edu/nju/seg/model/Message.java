package edu.nju.seg.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message extends SDComponent {

    private String name;

    private Event from;

    private Event to;

    public Event getHead()
    {
        return from;
    }

    public Event getTail()
    {
        return to;
    }

}
