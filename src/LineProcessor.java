import symbol_managment.FunctionManager;
import symbol_managment.MemoryManager;
import symbol_managment.VarType;
import symbol_managment.VariableAttribute;

import java.util.ArrayList;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * java line i one of those:
 * empty line - only spaces. \s regex.
 */

class LineProcessor {
    // a predicate that handles empty lines
    private static final Predicate<String> isEmptyLine = (String line) ->
    {
        String l = line.replaceAll("\\s", "");
        return l.length() == 0;
    };
    private final String VAR_REGEX_EXPRESSION = "([a-zA-Z][a-zA-Z0-9_]*|_[a-zA-Z0-9_]+)";
    // a regex that identifies a declaration of global variable

    MemoryManager memoryManager;
    FunctionManager functionManager;
    private boolean nextLineMustNotBeEmpty;
    public LineProcessor() {
        memoryManager = new MemoryManager();
        functionManager = new FunctionManager();
        nextLineMustNotBeEmpty = true;
    }
    
    public static void main(String[] args) {
        LineProcessor l = new LineProcessor();
        System.out.println(l.isVarDec("final    int a = 5, gba, asf;", (x, y) -> {
        }));
        /*System.out.println(l.isVarDec("final boolean asgagadg;"));
        System.out.println(l.isVarDec("final boolean asga,gad;"));
        System.out.println(l.isVarDec("final t a;"));*/
    }
    
    private boolean isVarDec(String line, BiConsumer<String, VariableAttribute> onVariableDeclare) {
        String types = VarType.geVarTypesRegex();
        String identifier = VAR_REGEX_EXPRESSION;
        //String typeAndIdentifier = types + "\\s+" + identifier + "\\s*";
        //String varDecRegex = "\\s*(final\\s+)?"+ typeAndIdentifier + "(,\\s*" + typeAndIdentifier + ")*;";
        Matcher m = Pattern.compile("^\\s*(final\\s+)?(\\b\\S*\\b)(.*?);\\s*$").matcher(line);
        if (!m.find())
            return false;
        boolean isFinal = m.group(1) != null;
        String type = m.group(2);
        String varString = m.group(3);
        String[] vars = varString.split(",");
        for (String var : vars) {
            var = var.strip();
            m = Pattern.compile("^" + identifier + "\\s*((=)(.*))?$").matcher(var);
            if (!m.find())
                throw new IllegalVarDecException("Invalid variable identifier or initialization: " + var);
            
            String varName = m.group(1).strip();
            String value = m.group(4);
            VariableAttribute variableAttribute;
            VarType varType = VarType.getVarType(type);
            variableAttribute = new VariableAttribute(varName, isFinal, varType,/*isLegalValueForType(value, varType)*/false);
            //todo make a function that checks this
            onVariableDeclare.accept(varName, variableAttribute);
        }
        return true;
    }
    
    // programing in stages:
    // first is ignoring empty lines:
    public boolean processLineFirstIteration(String line) throws SyntaxException {
        boolean emptyLineIsLegit = nextLineMustNotBeEmpty && isEmptyLine.test(line);
        boolean functionDecleration = isFuncDecLegit(line);
        boolean isVariableDecleration = isVarDec(line, this::addGlobalVariable);
        return emptyLineIsLegit
                || functionDecleration
                || isVariableDecleration;
    }
    
    /**
     * for the second iteration.
     *
     * @param line
     * @return
     * @throws SyntaxException
     */
    public boolean processLineSecondIteration(String line) throws SyntaxException {
        boolean isWhileOrIfChunck = !memoryManager.isOuterScope() && isWhileOrIf(line);
        return isWhileOrIfChunck;
    }
    
    private void addGlobalVariable(String name, VariableAttribute variableAttribute) {
        //todo make this function add a var if the scope is global
        // NOTE I MAKE ANOTHER FIELD TO FILL IN CASE THE GLOABAL WAS
    }
    
    
    private boolean isFuncDecLegit(String line) throws SyntaxException {
        
        Predicate<String> startOfFunctionDecleration =
                (String str) -> {
                    String[] splited = str.split(" ");
                    return splited[0].equals("void");
                };
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
                split(" (([a-zA-Z][a-zA-Z0-9_]*)|(_[a-zA-Z0-9_]+))(\\r)*[,)]");
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
        if (!params[params.length - 1].strip().equals("{"))
            throw new SyntaxException("s_java function declaration must end with }");
        
        // add this function to the list of legit functions
        functionManager.addFunction(funcName, functionVarTypes);
        
        return true;
    }
    
    
    private boolean isWhileOrIf(String line) throws SyntaxException {
        // regex for "if"\"while" then "(" then something that should be boolean expression, then ){
        String WhileIfRegex = "^\\s*(while|if)\\s*\\((.*)\\)\\s*\\{\\s*$";
        Matcher matcher = Pattern.compile(WhileIfRegex).matcher(line);
        if (!matcher.find() || matcher.group(1) == null) return false;
//        varifyBoolean(matcher.group(2)); // TODO func that verifies that the expression is boolean
        
        memoryManager.increaseScopeDepth(); // upon entering a new scope.
        nextLineMustNotBeEmpty = true;  // after { the next line must not be empty
        return true;
    }
}
    

