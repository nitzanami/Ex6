import symbol_managment.FunctionManager;
import symbol_managment.MemoryManager;

import java.util.function.Predicate;
import java.util.function.Predicate;
import java.util.regex.*;

/**
 * java line i one of those:
 *      empty line - only spaces. \s regex.
 *
 *
 */

public class LineProcessor {
    MemoryManager memoryManager;
    FunctionManager functionManager;
    
    // in outer scope only global declarations and methods are allowed.
    private boolean outerScope;
    
    public LineProcessor(){
        memoryManager = new MemoryManager();
        functionManager = new FunctionManager();
    }
    
    public void procesLine(String line) {
        return isEmpty(line) ||
                isDecleration(line) ||
                memoryManager.isIfOrWhile(line) ||
                functionCall(line) ||
                functionDecleration(line);
    }
    Predicate<String> isEmpty = (String line) -> (if(String.splite(line)));
}
