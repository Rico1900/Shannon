package edu.nju.seg.model;

public class ParseException extends RuntimeException {

    private String message;

    public ParseException(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }

}
