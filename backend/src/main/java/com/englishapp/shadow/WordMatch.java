package com.englishapp.shadow;

public record WordMatch(String expected, String actual, Status status) {

    public enum Status {
        MATCH, MISSING, EXTRA, MISPRONOUNCED
    }
}
