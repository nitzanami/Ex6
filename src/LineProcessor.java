import symbol_managment.*;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * java line i one of those:
 * empty line - only spaces. \s regex.
 */

class LineProcessor {
    // a predicate that handles empty lines
    private final static String VAR_REGEX_EXPRESSION = "([a-zA-Z][a-zA-Z0-9_]*|_[a-zA-Z0-9_]+)";
    private final static String DOUBLE_REGEX_EXPRESSION = "^[+-]?\\d+(\\.\\d*)?$"; // todo fix it
    // to not accept 54f.3
    private final static String INT_REGEX_EXPRESSION = "([+-]?[0-9]+)";
    private final static String BOOLEAN_REGEX_EXPRESSION = "(true|false)";
    private final static String[] keywords = {"while", "if", "final", "void", "true", "false"};

    private static final String FUNCTION_REGEX_START = "^\\s*(void)\\s+(\\w[a-zA-Z0-9_]*)\\s*\\((.*)\\)";
    private static final Pattern FUNCTION_CALL = Pattern.compile(FUNCTION_REGEX_START + "\\s*;\\s*$");
    private static final Pattern METHOD_DEC_REGEX = Pattern.compile(FUNCTION_REGEX_START + "\\s*\\{\\s*$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile(DOUBLE_REGEX_EXPRESSION);
    MemoryManager memoryManager;
    FunctionManager functionManager;
    private int scopeDepth = 1;
    private boolean nextLineMustNotBeEmpty;
    private boolean lastLineWasReturn; // this must be ^\\s*}\\s*$
    private final DepthHandlerFirstIteration depthManager;
    
    public LineProcessor() {
        memoryManager = new MemoryManager();
        functionManager = new FunctionManager();
        nextLineMustNotBeEmpty = false;
        depthManager = new DepthHandlerFirstIteration();
    }
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   MAIN TOOLS  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private static boolean isEmptyLine(String line) {
        String l = line.replaceAll("\\s", "");
        return l.length() == 0;
    }
    
    private static boolean isCommentLine(String line) throws SyntaxException {
        Matcher m = Pattern.compile("//").matcher(line);
        if (!m.find()) return false;
        if (!line.split("//")[0].equals(""))
            throw new SyntaxException(
                    String.format("Commant must be a full line,Error in line:\"%s\"", line));
        return true;
    }
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   MAIN TOOLS END    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public static void main(String[] args) {
        System.out.println(literalStringIsString("\"a\""));
        System.out.println(literalStringIsString("\""));
        System.out.println(literalStringIsString("ad2\""));
        System.out.println(literalStringIsString("\"\"234\""));
        
        LineProcessor l = new LineProcessor();
        try {
            System.out.println(l.isVarDecLineLegit("final    double true  , gba, asf;", (x) -> {
            }));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~  HELPERS  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private static boolean literalStringIsNumber(String str) {
        return str.strip().matches(DOUBLE_REGEX_EXPRESSION);
    }
    
    private static boolean isKeyword(String varName) {
        try {
            VarType.getVarType(varName);
            return true;
        } catch (Exception ignored) {
        }
        
        for (String keyword : keywords) {
            if (varName.equals(keyword))
                return true;
        }
        return false;
    }
    
    private static String removeSemicolon(String line) {
        //check if the line contains a semicolon
        Matcher m = Pattern.compile("(.*);\\s*$").matcher(line);
        if (!m.find())
            throw new MissingSemicolonException();
        return line.replaceFirst(";", "");
    }
    
    private static boolean literalStringIsChar(String varName) {
        String x = varName.strip();
        return x.length() == 3 && x.charAt(0) == x.charAt(2) && x.charAt(2) == '\'';
    }
    
    private static boolean literalStringIsString(String varName) {
        return varName.strip().matches("\".*\"");
    }
    
    private boolean isBackwardsCurlyBraces(String line) throws SyntaxException {
        Matcher m = Pattern.compile("}").matcher(line);
        if (!m.find()) return false;
        if (!line.strip().equals("}")) // more then \\s*}\\s*
            throw new SyntaxException(String.format
                    ("Backwards Curl must be singular in a line,Error in line:\"%s\"", line));
        if (memoryManager.isOuterScope())
            throw new SyntaxException("Backwards Curl must not be at the outer scope");
        if (!lastLineWasReturn)
            throw new SyntaxException("Backwards Curl must follow a \"return;\" line");
        // in case of exiting if/while/func:
        memoryManager.decreaseScopeDepth();
        return true;
    }
    
    /**
     * get a string and varify that the value it hold is boolean
     *
     * @param s the input string
     * @return true if boolean expression- otherwise false
     */
    private boolean varifyBoolean(String s) {
        String boolExpression = "^\\s*(true|false|[+-]*[0-9]+\\.[0-9]*|\\w[\\w\\d_]*|_[\\w\\d_]+)" +
                "(\\s*(&&|\\|\\|)\\s*.*)$";
        // with groups:0-all, 1-one of *_REGEX_EXPRESSION, 2-&& or || following by REGEX_EXPRESSION unlimited times
        Matcher m = Pattern.compile(boolExpression).matcher(s);
        if (!m.find()) return false;
        literalStringIsBoolean(m.group(1));
        // sure that if its var, its of bool/int/double and is
        if (m.group(2) == null) return true;
        String[] expressions = m.group(2).strip().split("(&&|\\|\\|)");
        for (int i = 1; i < expressions.length; i++)
            if (!literalStringIsBoolean(expressions[i].strip())) return false;
        return true;
    }
    
    /**
     * check if the given line is in the format for a variable declaration, and if it is, declare the variables
     *
     * @param line              The line to parse.
     * @param onVariableDeclare a function that decides what to do with a valid variable declaration.
     * @return true if the line is a variable declaration line, else false.
     */
    private boolean isVarDecLineLegit(String line, Consumer<VariableAttribute> onVariableDeclare) {
        //check if the line contains a variable declaration.
        Matcher m = Pattern.compile("^\\s*(final\\s+)?" + VarType.getVarTypesRegex() + ".*$").matcher(line);
        if (!m.find())
            return false;
        line = removeSemicolon(line);
        return isVarDecLegit(line, onVariableDeclare);
    }
    
    private boolean isFunctionCallLegit(String line) throws SyntaxException {
        String format = "^\\s*(\\w[\\w\\d_]*)\\s*\\((.*)\\)\\s*;\\s*$";
        Matcher matcher = Pattern.compile(format).matcher(line);
        if (!matcher.find()) return false;
        String funcname = matcher.group(1);
        if (functionManager.doesFunctionExist(funcname)) throw new SyntaxException(
                String.format("unknown function at: %s", line));
        return parametersMatchRequiredVariableTypes(matcher.group(2), funcname);
    }
    
    private boolean parametersMatchRequiredVariableTypes(String group, String funcname) throws SyntaxException {
        ArrayList<VarType> paramType;
        try{
             paramType = functionManager.getParameterTypes(funcname);
        } catch (Exception NoSuchMethodException){
            throw new SyntaxException(String.format("\"%s\":Unknown function error.",funcname));
        }
        var varNames = group.strip().split(",");
        if (varNames.length != paramType.size())
            throw new InvalidParameterException(String.format("Wrong " +
                    "number Of variables in function call \"%s\" using\"%s\"", funcname, group));
        for (int i = 0; i < paramType.size(); i++) {
            var v = paramType.get(i);
            var input = varNames[i].strip();
            // does literal string fill the demand of paramType[i] param type?
            if (v == VarType.BOOLEAN && literalStringIsBoolean(input)) continue;
            if ((v == VarType.INT || v == VarType.DOUBLE) && literalStringIsNumber(input)) continue;
            if (v == VarType.STRING && literalStringIsString(input)) continue;
            if (v == VarType.CHAR && literalStringIsChar(input)) continue;
            // if not, if it's a known variable:
            if (!DownCaster.firstAcceptsSecond
                    (paramType.get(i), memoryManager.getVarAttributes(input).getVariableType())) {
                throw new InvalidParameterException(String.format("Wrong " +
                        "type Of variables in function call \"%s\" using\"%s\"", funcname, group));
            }
        }
        return true;
    }
    
    /**
     * for sec iteration include if/while. empty lines. var declaration, var =, } , return,
     * AND function calls
     *
     * @param line input
     * @return true if ok false if not ok
     * @throws SyntaxException throe in case of syntax err
     */
    public boolean processLineSecondIteration(String line) throws SyntaxException{
        if (memoryManager.isOuterScope()) return true;
        boolean isWhileOrIfChunck = isWhileOrIf(line);
        return isWhileOrIfChunck
                || isCommentLine(line)
                || isBackwardsCurlyBraces(line)
                || isFunctionCallLegit(line)
                || isFunctionDeclarationSecondIteration(line);
        // todo add variable declaration,
    }
    
    /**
     * if a function was found, enters a function after increasing scope depth and saving the
     * arguments there
     *
     * @param line the line
     * @return true if it's a function declaration
     * @throws SyntaxException if something went wrong
     */
    private boolean isFunctionDeclarationSecondIteration(String line) throws SyntaxException {
        Matcher m = METHOD_DEC_REGEX.matcher(line);
        // if match not found
        if (!m.find() || m.group(1) == null) return false;
        if (!memoryManager.isOuterScope()) throw new SyntaxException(String.
                format("May not use declare func in inner scope. line %s", line));
        
        memoryManager.increaseScopeDepth();
        
        if (m.group(3).equals("")) return true;
        // check legit of function inputs(in case there is input)
        String[] type_Variable = m.group(3).split(",");
        
        var patNameCompile = Pattern.compile(VAR_REGEX_EXPRESSION);
        for (String s : type_Variable) {
            // declare final stuff?
            String[] splitInto = s.strip().split(" ");
            if (splitInto.length > 2) throw new SyntaxException(
                    String.format("%s is not a valid pair of (type ,var_name)", s));
            
            s = s.strip();
            String sType = s.split(" ")[0].strip(), sVarName = s.split(" ")[1].strip();
            
            VariableAttribute variableAttribute = new
                    VariableAttribute(sVarName, false, VarType.getVarType(sType), true);
        }
        return true;
    }
    
    /**
     * get a string and varify that the value it hold is boolean
     *
     * @param expression the input string
     * @return true if boolean expression- otherwise false
     */
    private boolean verifyBooleanExpression(String expression) {
        String boolExpression = "^\\s*(true|false|[+-]*[0-9]+\\.[0-9]*|\\w[\\w\\d_]*|_[\\w\\d_]+)" +
                "(\\s*(&&|\\|\\|)\\s*.*)$";
        // with groups:0-all, 1-one of *_REGEX_EXPRESSION, 2-&& or || following by REGEX_EXPRESSION unlimited times
        Matcher m = Pattern.compile(boolExpression).matcher(expression);
        if (!m.find()) return false;
        literalStringIsBoolean(m.group(1));
        // sure that if its var, its of bool/int/double and is
        if (m.group(2) == null) return true;
        String[] expressions = m.group(2).strip().split("(&&|\\|\\|)");
        for (int i = 1; i < expressions.length; i++)
            if (!literalStringIsBoolean(expressions[i].strip())) return false;
        return true;
    }
    
    /**
     * given an expression, answer if its a boolean or not
     *
     * @param str input
     * @return true/false
     */
    private boolean literalStringIsBoolean(String str) {
        str = str.strip();
        // if its either true/false strings or a double/int number literal - accept
        if (str.equals("true") || str.equals("false") || literalStringIsNumber(str))
            return true;
        // if it's an initiated variable with boolean meaning variable - accept
        var variable = memoryManager.getVarAttributes(str);
        if (variable == null || !variable.getInitiated()) return false;
        return variable.getVariableType() == VarType.BOOLEAN || variable.getVariableType() == VarType.INT ||
                variable.getVariableType() == VarType.DOUBLE;
    }
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~  VARIABLES HELPERS  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void addGlobalVariable(String name, VariableAttribute variableAttribute) {
        if (memoryManager.isOuterScope()) {
            if (memoryManager.declareable(name)) {
                memoryManager.declareVariable(variableAttribute);
            }
        }
        // NOTE I MAKE ANOTHER FIELD TO FILL IN CASE THE GLOABAL WAS
    }
    
    public boolean isVarAssignmentLineLegit(String line) {
        Matcher m = Pattern.compile("\\s*" + VAR_REGEX_EXPRESSION + ".*").matcher(line);
        if (!m.find())
            return false;
        if (memoryManager.getVarAttributes(m.group(1)) == null)
            return false; // todo shouldn't this and the one from above be in the same line?
        line = removeSemicolon(line);
        String[] varAssignments = line.split(",");
        for (String varAssignment : varAssignments) {
            varAssignment = varAssignment.strip();
            if (varAssignment.equals(""))
                throw new IllegalVarDecException("Missing variable assignment between commas");
            m = Pattern.compile("^" + VAR_REGEX_EXPRESSION + "\\s*=\\s* (\\S*|\"[^\"]*\")\\s*$").matcher(varAssignment);
            if (!m.find())
                throw new IllegalVarDecException("Illegal format for variable assignment: " + line);
            String name = m.group(1);
            VariableAttribute attr = memoryManager.getVarAttributes(name);
            if (attr == null)
                throw new IllegalVarDecException("Variable " + name + " is not declared");
            VarType type = attr.getVariableType();
            if (attr.isFinal())
                throw new IllegalVarDecException("The variable " + name + " is final and cant be assigned to");
            String value = m.group(2);
            if (isLegalValueForType(value, type)) {
                attr.setInitiated(true);
            }
        }
        return true;
    }
    
    /**
     * check if the given declaration is a valid declaration of a variable list, and if it is, declare the variables.
     * doesnt excpect a semicolon at the end of the input.
     *
     * @param declaration       the variable declaration to parse.
     * @param onVariableDeclare a function that decides what to do with a valid variable declaration.
     * @return true if the decleration is valid, else false.
     */
    private boolean isVarDecLegit(String declaration, Consumer<VariableAttribute> onVariableDeclare) {
        //check if the line start with final
        Matcher m = Pattern.compile("^\\s*(final\\s+)?(\\b\\S*\\b)(.*?)$").matcher(declaration);
        if (!m.find())
            return false;
        boolean isFinal = m.group(1) != null;
        //get the type of the variables in the declaration.
        String type = m.group(2);
        VarType varType = VarType.getVarType(type);
        
        //separate the declaration into individual variables
        String[] vars = m.group(3).split(",");
        for (String varDec : vars) {
            varDec = varDec.strip();
            String varName = getVariableName(varDec);
            if (isKeyword(varName))
                throw new IllegalVarDecException("The name " + varName +
                        " is a keyword and cant be used for a variable name");
            //check if the variable should start as initialized
            String initialize = varDec.replaceFirst(varName, "");
            boolean isInitialized = isVarInitialized(varType, initialize);
            //handle the variable declaration according to the OnVariableDeclare function.
            VariableAttribute variableAttribute = new VariableAttribute(varName, isFinal, varType, isInitialized);
            onVariableDeclare.accept(variableAttribute);
        }
        return true;
    }
    
    /**
     * gets a variable initialization in the format (= <value>)? and sets the variable to initialized
     *
     * @param initString a string in the format (= <value>)?
     * @param type       the type that we exprect the initialization value to have
     * @return true if the variable is initialized in the string, else false
     */
    private boolean isVarInitialized(VarType type, String initString) {
        //check if the initialization is in the required format
        Matcher m = Pattern.compile("^\\s*((=)(.*))?$").matcher(initString);
        if (!m.find())
            throw new IllegalVarDecException("Invalid variable initialization format: " + initString);
        //if there is no initialization, return false
        if (initString.strip().equals(""))
            return false;
        //if there is an initialization, return true if it is valid, else it will throw an exception
        String value = m.group(3).strip();
        if (value.equals(""))
            throw new IllegalTypeException("Missing value in variable assignment");
        if (!isLegalValueForType(value, type))
            throw new IllegalTypeException("The value " + value +
                    " is not a valid value for type " + type.toString().toLowerCase());
        return true;
    }
    
    /**
     * checks if the given value can be assigned into a variable of the given type
     *
     * @param value the string representing the value
     * @param type  the type we want to assign into
     * @return true if the value is legal, else throw an exception.
     */
    private boolean isLegalValueForType(String value, VarType type) {
        VarType potentialType;
        potentialType = VarType.getTypeOfValue(value);
        if (potentialType != null)
            return DownCaster.firstAcceptsSecond(type, potentialType);
        VariableAttribute var = memoryManager.getVarAttributes(value);
        if (var != null) {
            return DownCaster.firstAcceptsSecond(type, var.getVariableType());
        }
        return false;
    }
    
    /**
     * gets a variable initialization in the format <varname> (= <value>)? and sets the variable to initialized
     *
     * @param varDec a string in the format <varname> (= <value>)?
     * @return the name of the variable
     */
    private String getVariableName(String varDec) {
        Matcher m = Pattern.compile("^" + VAR_REGEX_EXPRESSION + "\\s*((=)(.*))?$").matcher(varDec);
        if (!m.find())
            throw new IllegalVarDecException("Invalid variable identifier or initialization: " + varDec);
        return m.group(1).strip();
    }
    
    /**
     * for the first iteration: include empty ines, global variable, and function declarations
     *
     * @param line input
     * @return true if ok false if not ok
     * @throws SyntaxException throe in case of syntax err
     */
    public boolean processLineFirstIteration(String line) throws SyntaxException {
        // if it's not the outer scope
        if (!depthManager.isOuterScope()) {
            depthManager.depthAdapter(line);
            return true;
        }
        boolean emptyLineIsLegit = !nextLineMustNotBeEmpty && isEmptyLine(line);
        boolean functionDeclaration = isFuncDecLegit(line);
        boolean isVariableDeclaration = memoryManager.isOuterScope() &&
                isVarDecLineLegit(line, this::addGlobalVariable);
        boolean isVarAssignmentLegit = memoryManager.isOuterScope() &&
                isVarAssignmentLineLegit(line);
        
        depthManager.depthAdapter(line);
        return emptyLineIsLegit
                || isCommentLine(line)
                || functionDeclaration
                || isVariableDeclaration
                || isVarAssignmentLegit;
    }
    
    private void addGlobalVariable(VariableAttribute variableAttribute) {
        if (memoryManager.isOuterScope()) {
            memoryManager.declareVariable(variableAttribute);
        }
        // NOTE I MAKE ANOTHER FIELD TO FILL IN CASE THE GLOABAL WAS
    }
    
    private void addLocalVariable(VariableAttribute variableAttribute) {
        if (!memoryManager.isOuterScope()) {
            memoryManager.declareVariable(variableAttribute);
        }
    }
    
    
    /**
     * read a function decleration lin: make sure that
     * it starts with void,
     * continue with legit name,
     * (
     * then pairs of Type ,Name where Name doesn't repeat itself in th declaration
     * ){
     *
     * @param line given input
     * @return true/false accordingly
     * @throws SyntaxException if it was found.
     */
    private boolean isFuncDecLegit(String line) throws SyntaxException {
        String funcDecRegex = "^\\s*(void)\\s+(\\w[a-zA-Z0-9_]*)\\s*\\((.*)\\)\\{\\s*$";
        Matcher m = Pattern.compile(funcDecRegex).matcher(line);
        // if match not found
        if (!line.contains("void ")) return false;
        if (!m.find() || m.group(1) == null) throw new SyntaxException(String.format(
                "\"%s\": Not legit line declaration", line));
        String funcName = m.group(2);
        if (functionManager.doesFunctionExist(funcName)) throw new SyntaxException(String.
                format("May not use the same name for different function. name %s", funcName));
    
        ArrayList<VarType> funcVariable = new ArrayList<>();
        HashSet<String> variableNames = new HashSet<>();
        
        if (m.group(3).equals("")) {
            functionManager.addFunction(funcName, funcVariable);
            return true;
        }
        // check legit of function inputs(in case there is input)
        String[] type_Variable = m.group(3).split(",");
        
        
        var patNameCompile = Pattern.compile(VAR_REGEX_EXPRESSION);
        for (String s : type_Variable) {
            String[] splitInto = s.strip().split(" ");
            if (splitInto.length > 2) throw new SyntaxException(
                    String.format("%s is not a valid pair of (type ,var_name)", s));
            
            s = s.strip();
            String sType = s.split(" ")[0], sVarName = s.split(" ")[1];
            
            m = patNameCompile.matcher(sVarName);
            if (!m.find()) throw new SyntaxException(
                    String.format("%s is not a valid var_name", sVarName));
            if (variableNames.contains(sVarName)) throw new SyntaxException(String.format("%s is " +
                    "used twice in the same function as variable name", sVarName));
            
            variableNames.add(sVarName);
            funcVariable.add(VarType.getVarType(sType)); // add to list of func vairable
        }
        functionManager.addFunction(funcName, funcVariable);
        return true;
    }
    
    private boolean isWhileOrIf(String line) throws SyntaxException {
        // regex for "if"\"while" then "(" then something that should be boolean expression, then ){
        String WhileIfRegex = "^\\s*(while|if)\\s*\\((.*)\\)\\s*\\{\\s*$";
        Matcher matcher = Pattern.compile(WhileIfRegex).matcher(line);
        if (!matcher.find() || matcher.group(1) == null) return false;
        varifyBoolean(matcher.group(2).strip());
        memoryManager.increaseScopeDepth(); // upon entering a new scope.
        nextLineMustNotBeEmpty = true;  // after { the next line must not be empty
        return true;
    }
    
    /**
     * count how many curly brackets will be open after this line.
     * in case of 1 do not enter outer scope but to increase\decrease scope depth.
     */
    private static class DepthHandlerFirstIteration {
        static Stack<Character> depth;
        
        public DepthHandlerFirstIteration() {
            depth = new Stack<>();
        }
        
        boolean isOuterScope() {
            return depth.size() == 0;
        }
        
        void depthAdapter(String s) throws SyntaxException {
            try {
                if (s.contains("{")) depth.add('{');
                else if (s.contains("}")) depth.pop();
            } catch (Exception EmptyStackException) {
                throw new SyntaxException("miss matching curly braces");
            }
        }
    }
}
    

