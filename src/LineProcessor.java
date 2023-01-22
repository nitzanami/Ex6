import symbol_managment.FunctionManager;
import symbol_managment.MemoryManager;

import java.util.function.Predicate;

/**
 * java line i one of those:
 * empty line - only spaces. \s regex.
 */

class LineProcessor {
    // a predicate that handles empty lines
    private final Predicate<String> isEmptyLine = (String line) ->
    {
        String l = line.replaceAll("\\s", "");
        return l.length() == 0;
    };
    // a regex that identifies a declaration of global variable
    private final Predicate<String> startOfVariable = (String line) -> {
        // todo make pattern for begining of variable decleration
        // todo make matcher for that pattern
        // todo stop after final or vartype, remomber each of them and add to memoryManager
        return false;
    };
    
    MemoryManager memoryManager;
    FunctionManager functionManager;
    
    
    public LineProcessor() {
        memoryManager = new MemoryManager();
        functionManager = new FunctionManager();
    }
    
    // programing in stages:
    // first is ignoring empty lines:
    public boolean processLineFirstIteration(String line) {
        return isEmptyLine.test(line); // for successful completion
    }
    
}
    

