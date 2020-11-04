package edu.nju.seg.model;

import java.util.*;

public class Seq {

    private String label = "";

    private Map<Instance, List<Message>> seq = new HashMap<>();

    public Seq(Map<Instance, List<Message>> seq)
    {
        this.seq = seq;
    }

    public Seq(List<Instance> covered)
    {
        for (Instance i: covered) {
            seq.put(i, new ArrayList<>());
        }
    }

    public Seq(String label, Map<Instance, List<Message>> seq)
    {
        this.label = label;
        this.seq = seq;
    }

    public void set_label(String label) {
        this.label = label;
    }

    public String get_label()
    {
        return label;
    }

    public Map<Instance, List<Message>> get_seq()
    {
        return seq;
    }

    public static Seq clone(Seq s)
    {
        return new Seq(s.get_label(), s.get_seq());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Seq seq1 = (Seq) o;
        return label.equals(seq1.label) &&
                seq.equals(seq1.seq);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seq);
    }

}
