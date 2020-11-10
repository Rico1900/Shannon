package edu.nju.seg.parser;

import edu.nju.seg.exception.ParseException;
import edu.nju.seg.expression.Judgement;
import edu.nju.seg.model.Element;
import edu.nju.seg.model.NoteType;
import edu.nju.seg.util.$;
import edu.nju.seg.util.Pair;

import java.util.List;

public class UmlNoteParser {

    /**
     * parse a single note on the graph
     * @param e the element
     * @return the structural result
     */
    public static Pair<NoteType, List<Judgement>> parse_note(Element e)
    {
        String content = e.getContent();
        String[] lines = content.split("\n");
        NoteType t = parse_note_type(lines[0]);
        List<Judgement> js = JudgementsParser.parse_judgements($.get_list_after_one(lines));
        return new Pair<>(t, js);
    }

    /**
     * parse the note type
     * @param first the first line of the note
     * @return the note type
     */
    private static NoteType parse_note_type(String first)
    {
        if (first.equals(NoteType.CONSTRAINTS.getV())) {
            return NoteType.CONSTRAINTS;
        } else if (first.equals(NoteType.PROPERTIES.getV())) {
            return NoteType.PROPERTIES;
        } else if (first.equals(NoteType.GOAL.getV())) {
            return NoteType.GOAL;
        } else {
            throw new ParseException("Wrong note type");
        }
    }


}
