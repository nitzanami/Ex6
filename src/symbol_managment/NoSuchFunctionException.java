package symbol_managment;

import exceptions.SyntaxException;

public class NoSuchFunctionException extends SyntaxException {
    public NoSuchFunctionException(String message) {
        super(message);
    }
}
