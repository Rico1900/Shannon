package edu.nju.seg.model;

public enum FragmentType {
    INT("int"),
    LOOP("loop"),
    ALT("alt"),
    OPT("opt");

    private final String v;

    FragmentType(String v) {
        this.v = v;
    }

    public String getV() {
        return v;
    }

}
