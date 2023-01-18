import symbol_managment.MemoryManager;

import java.util.function.Predicate;
import java.util.function.Predicate;
import java.util.regex.*;

public class LineProcessor {
    MemoryManager memoryManager;
    
    
    public LineProcessor(){
        memoryManager = new MemoryManager();
        
    }
    
    public void procesLine(String line) {
        return isEmpty(line) || isDecleration(line) || isIfOrWhile(line) || functionCall(line) || functionDecleration(line);
    }
    Predicate<String> isEmpty = (String line) -> rene
}
