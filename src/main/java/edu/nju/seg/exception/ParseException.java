package edu.nju.seg.exception;

public class ParseException extends RuntimeException {

    private String message;

    public ParseException(String message)
    {
        this.message = message;
    }

    @Override
    public String toString()
    {
        return message;
    }

}
