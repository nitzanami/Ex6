package exceptions;

import javax.lang.model.UnknownEntityException;

public class IllegalTypeException extends UnknownEntityException {
    public IllegalTypeException(String s) {
        super(s);
    }
}
