package edu.nju.seg.model;

public enum NoteType {
    CONSTRAINTS("Constraints"),
    PROPERTIES("Properties"),
    GOAL("Goal");

    private final String v;

    NoteType(String v) {
        this.v = v;
    }

    public String getV() {
        return v;
    }

}
