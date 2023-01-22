import symbol_managment.FunctionManager;
import symbol_managment.MemoryManager;
import symbol_managment.VarType;

import java.util.ArrayList;
import java.util.Locale;
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
    
    private final Predicate<String> startOfFunctionDecleration =
            (String line) -> {
                String[] splited = line.split(" ");
                return splited[0].equals("void");
            };
    private final boolean emptyIsAllowed;
    MemoryManager memoryManager;
    FunctionManager functionManager;
    
    
    public LineProcessor() {
        memoryManager = new MemoryManager();
        functionManager = new FunctionManager();
        emptyIsAllowed = true;
    }
    
    // programing in stages:
    // first is ignoring empty lines:
    public boolean processLineFirstIteration(String line) throws SyntaxException {
        boolean emptyLineIsLegit = emptyIsAllowed && isEmptyLine.test(line);
        boolean functionDecleration = isFuncDecLegit(line);
        
        return emptyLineIsLegit && functionDecleration; // for successful completion
    }
    
    private boolean isFuncDecLegit(String line) throws SyntaxException {
        if (!startOfFunctionDecleration.test(line))
            return false;
        // read up to start of function '(' then capture the type, and continue to ignore the
        // following word. repeat at ',' stop at ')' check that
        String funcName = line.split("\\(")[0].replaceAll("void", " ").strip();
        if (functionManager.doesFunctionExist(funcName)) {
            throw new SyntaxException(String.format
                    ("s_java may not create 2 functions with the same name,%s", funcName));
        }
        
        // check that the variable types that the function use are legit:
        String[] params = line.split("\\(")[1].
                split(" (([a-zA-Z][a-zA-Z0-9_]*)|(_[a-zA-Z0-9_]+))( )*[,)]");
        ArrayList<VarType> functionVarTypes = new ArrayList<>();
        
        // make sure every type found at func declaration has its representation in VarType Enum.
        for (int i = 0; i < params.length - 1; i++) {
            boolean thisOneIsAnVarType = false;
            for (VarType v : VarType.values())
                if (params[i].toUpperCase(Locale.ROOT).strip().equalsIgnoreCase(v.name())) { // in case
                    // the param is a known type in VariableAttribute:
                    functionVarTypes.add(v);
                    thisOneIsAnVarType = true;
                    break;
                }
            if (!thisOneIsAnVarType) {
                throw new SyntaxException
                        (String.format("s_java does not support %s type", params[i]));
            }
        }
        
        // make sure that the declaration ends with '}'
        if (!params[params.length - 1].equals("{"))
            throw new SyntaxException("s_java function declaration must end with }");
        
        // add this function to the list of legit functions
        functionManager.addFunction(funcName, functionVarTypes);
        
        return true;
    }
}
    /*
    parameter _(w|d|_)* || w(w|d|_)*
    
    regex will be:([a-zA-Z][a-zA-Z0-9_]*)|(_[a-zA-Z0-9_]+)
    */
    

