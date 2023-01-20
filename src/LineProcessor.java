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
    
    // regex for pattern:
    // empty lines, and a function that find empty lines and returns true
    String emptyLineRegex = "^\\s*$";
    
    // regex for the !!!start!!! of if/while statmente.
    String StartOfWhileOrIfRegex = "^(while|if)\\s*\\(";
    
    // regex for the !!!start!!! of declaration statement.
    String startOfFunctionDeclarationRegex = "^void \\w(\\w|W)*\\(";
    
    // function call, a function that calls FunctionManager.isFunctionLegit(String FunName, List<VarType>)
    String startOfFunctionRegex = "";
    // function decleration,
    
    public LineProcessor(){
        memoryManager = new MemoryManager();
        functionManager = new FunctionManager();
    }
    
    public void procesLine(String line) {
        return isEmpty(line) ||
                (proccessIfOrWhile(line) && memoryManager.getScopeDepth()>1) ||
                proccessfunctionCall(line) ||
                (proccessfunctionDecleration(line) && memoryManager.getScopeDepth() == 1);
    }
    Predicate<String> isEmpty = (String line) -> ;
}
