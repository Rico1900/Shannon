package edu.nju.seg.model;

public class EncodeException extends RuntimeException {

    private String message;

    public EncodeException(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }

}
