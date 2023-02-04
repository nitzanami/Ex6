package oop.ex6.main;

public enum Status {
    VALID, SYNTAX, IOERROR;

    @Override
    public String toString() {
        return Integer.toString(ordinal());
    }
}
