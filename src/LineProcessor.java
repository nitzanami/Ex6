import symbol_managment.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LineProcessor main logic is to accept a s-java line and either throw an informative error of
 * returns false.
 * <p>
 * It's done by iterating twice on the file at first filling up
 * memoryManager - with global variable
 * functionManager - function names, and the parameters each func accepts
 * <p>
 * then iterating again and reading it line by line, using the knowledge from the prev iteration.
 */

class LineProcessor {
    // a predicate that handles empty lines
    private final static String VAR_REGEX_EXPRESSION = "(\\w[\\w\\d_]*|_[\\w\\d_]+)";
    private final static String DOUBLE_REGEX_EXPRESSION = "^[+-]?\\d+(\\.\\d*)?$";
    private final static String[] keywords = {"while", "if", "final", "void", "true", "false", "return"};
    
    private static final String ERR_FUNC_VERIFY_BOOLEAN = "in verifyBoolean, ";
    private static final String ERR_MGS_UNKNOWN_VARIABLE = "Unknown variable as boolean expression";
    private static final String ERR_MGS_NOT_INITIALISED = "attempt to use an uninitialized variable detected";
    
    private final String fileName;
    MemoryManager memoryManager;
    FunctionManager functionManager;
    private boolean lastLineWasReturn;
    private List<VariableAttributes> uninitializedGlobals;
    
    /**
     * constructor
     *
     * @param fileName the name of the file this LineProcessor reads.
     */
    public LineProcessor(String fileName) {
        this.fileName = fileName;
        memoryManager = new MemoryManager();
        functionManager = new FunctionManager();
        uninitializedGlobals = new ArrayList<>();
    }
    
    /**
     * @param line input
     * @return input contains only \s chars
     */
    private static boolean isEmptyLine(String line) {
        String l = line.replaceAll("\\s", "");
        return l.length() == 0;
    }
    
    /**
     * @param line input
     * @return input starts with //
     * @throws SyntaxException in case it contains // but not at the start of the input
     */
    private static boolean isCommentLine(String line) throws SyntaxException {
        Matcher m = Pattern.compile("//").matcher(line);
        if (!m.find()) return false;
        if (!line.split("//")[0].equals(""))
            throw new SyntaxException(
                    String.format("Comment must be a full line,Error in line:\"%s\"", line));
        return true;
    }
    
    /**
     * check if the input String is a number=: [0-9]+(.[0-9]*)
     *
     * @param str input
     * @return true/false
     */
    private static boolean literalStringIsNumber(String str) {
        return str.strip().matches(DOUBLE_REGEX_EXPRESSION);
    }
    
    /**
     * check if the input String is one of the Keyword
     *
     * @param varName input
     * @return true/false
     */
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
    
    /**
     * remove a semicolon from the end of a line.
     *
     * @param line The line to edit
     * @return the line, after removing the semicolon.
     * @throws MissingSemicolonException if the line doesn't end with a semicolon.
     */
    private static String removeSemicolon(String line) {
        //check if the line contains a semicolon
        Matcher m = Pattern.compile("[^;]*;\\s*$").matcher(line);
        if (!m.find())
            throw new MissingSemicolonException();
        return line.replaceFirst(";", "");
    }
    
    /**
     * is input a literal String Is Char=: made of 'x' format.
     *
     * @param input input line
     * @return true/false accordingly
     */
    private static boolean literalStringIsChar(String input) {
        String x = input.strip();
        return x.length() == 3 && x.charAt(0) == x.charAt(2) && x.charAt(2) == '\'';
    }
    
    /**
     * is input a string=: "..."
     *
     * @param input input
     * @return true/false accordingly
     */
    private static boolean literalStringIsString(String input) {
        return input.strip().matches("\"[^\"]*\"");
    }
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   MAIN TOOLS  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private boolean isBackwardsCurlyBraces(String line) throws SyntaxException {
        if (!line.contains("}")) return false;
        if (!(line.strip().equals("}"))) {
            throw new SyntaxException(String.format
                    ("Backwards Curl must be singular in a line,Error in line:\"%s\"", line));
        }
        // line is in format \s*}\s* :
        if (memoryManager.isOuterScope())
            throw new SyntaxException("Backwards Curl must not be at the outer scope");
        //in case of func, previous line must be return, so
        if (!lastLineWasReturn && memoryManager.isFunctionScope())
            throw new SyntaxException("in Method Backwards Curl must follow a \"return;\" line");
        // in case of exiting if/while or function (after return) closing braces:
        if (memoryManager.isFunctionScope())
            memoryManager.unInitializeGlobals(uninitializedGlobals);
        
        memoryManager.decreaseScopeDepth();
        return true;
    }
    
    /**
     * find all the global variables and function signatures
     *
     * @return a value that represents the validity of the input
     * @throws IOException     if file io fails
     * @throws SyntaxException if illegal syntax is found
     */
    public Status runFirstIteration() throws IOException, SyntaxException {
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        while ((line = reader.readLine()) != null) {
            if (!processLineFirstIteration(line))
                return Status.SYNTAX; // for illegal code
        }
        uninitializedGlobals = memoryManager.getUninitializedGlobals();
        return Status.VALID;
    }
    
    /**
     * for the first iteration: include empty ines, global variable, and function declarations
     *
     * @param line input
     * @return true if the line is valid. else false
     * @throws SyntaxException throe in case of syntax err
     */
    private boolean processLineFirstIteration(String line) throws SyntaxException {
        // if it's not the outer scope
        
        boolean isValid = isEmptyLine(line) ||
                isFuncDecValid(line, true) ||
                isWhileOrIf(line, true) ||
                isBackwardsCurlyBraces(line) ||
                (!memoryManager.isOuterScope() || (isVarDecLineValid(line) ||
                        isVarAssignmentLineLegit(line)));
        lastLineWasReturn = isReturnLine(line);
        return isValid || lastLineWasReturn;
    }
    
    /**
     * finish the syntax checking and return the status
     *
     * @return a value that represents the validity of the input
     * @throws IOException     if file io fails
     * @throws SyntaxException if illegal syntax is found
     */
    public Status runSecondIteration() throws IOException, SyntaxException {
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        while ((line = reader.readLine()) != null) {
            if (!processLineSecondIteration(line))
                return Status.SYNTAX; // for illegal code
        }
        return Status.VALID;
    }
    
    /**
     * for sec iteration include if/while. empty lines. var declaration, var =, } , return,
     * AND function calls
     *
     * @param line input
     * @return true if ok. else false
     * @throws SyntaxException throe in case of syntax err
     */
    private boolean processLineSecondIteration(String line) throws SyntaxException {
        if (memoryManager.isOuterScope() && line.contains(";"))
            return true; //was checked 1st iteration
        boolean isValid = isWhileOrIf(line, false) ||
                isFunctionCallLegit(line) ||
                isFuncDecValid(line, false) ||
                isBackwardsCurlyBraces(line) ||
                (!memoryManager.isOuterScope() &&
                        (isVarAssignmentLineLegit(line) || isVarDecLineValid(line))) ||
                (isCommentLine(line) || isEmptyLine(line));
        
        lastLineWasReturn = isReturnLine(line); // !must not happen before isBackwardsCurlyBraces()!
        return isValid || lastLineWasReturn;
    }
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   MAIN TOOLS END    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /**
     * get a string and verify that the value it hold is boolean
     *
     * @param s the input string
     * @return true if boolean expression. else false.
     */
    private boolean verifyBoolean(String s) throws SyntaxException {
        String boolExpression = "^\\s*((true|false)|([+-]*\\d+\\.\\d*)|(\\w[\\w\\d_]*)|" +
                "(_[\\w\\d_]+))(\\s*(&&|\\|\\|)\\s*.*)?$";
        Matcher m = Pattern.compile(boolExpression).matcher(s);
        if (!m.find() || m.group(1) == null) return false;
        
        // find if the number of &&/|| is OK
        if (countRemovedChars(s, ("(&&)")) / 2 + countRemovedChars(s, ("(\\|\\|)")) / 2 >=
                s.split("(&&|\\|\\|)").length)
            throw new SyntaxException("in verifyBoolean: Too Many && or || where found in boolean" +
                    " expression");
        
        //ex:true false or 54.3
        boolean firstPartIsBooleanLiteral = (m.group(2) != null || m.group(3) != null);
        if (!firstPartIsBooleanLiteral) {
            var v = m.group(4) != null ?
                    memoryManager.getVarAttributes(m.group(4)) :
                    memoryManager.getVarAttributes(m.group(5));
            if (v == null)
                throw new SyntaxException(ERR_FUNC_VERIFY_BOOLEAN + ERR_MGS_UNKNOWN_VARIABLE);
            var t = v.getVariableType();
            if (!v.isInitialized())
                throw new SyntaxException(ERR_FUNC_VERIFY_BOOLEAN + ERR_MGS_NOT_INITIALISED);
            if (!(t.equals(VarType.BOOLEAN) || t.equals(VarType.INT) || t.equals(VarType.DOUBLE)))
                throw new SyntaxException(ERR_FUNC_VERIFY_BOOLEAN + ERR_MGS_UNKNOWN_VARIABLE);
        }
        
        if (m.group(6) == null) return true;
        String[] expressions = m.group(6).strip().split("(&&|\\|\\|)");
        if (expressions.length == 0)
            throw new SyntaxException(ERR_FUNC_VERIFY_BOOLEAN + "extra && or || in boolean expression");
        
        for (int i = 1; i < expressions.length; i++)
            if (!verifyBoolean(expressions[i].strip()))// bound to end as it's smaller than s in size
                return false;
            
        return true;
        }
        
        /**
         * check if the given line is in the format for a variable declaration, and if it is, declare the variables
         *
         * @param line The line to parse.
         * @return true if the line is a variable declaration line, else false.
         */
        private boolean isVarDecLineValid (String line){
            //check if the line contains a variable declaration.
            Matcher m = Pattern.compile("^\\s*(final\\s+)?" + VarType.getVarTypesRegex() + ".*$").matcher(line);
            if (!m.find())
                return false;
            line = removeSemicolon(line);
            return isVarDecLegit(line, memoryManager::declareVariable);
        }
        
        /**
         * for line formatted as word(...){
         * where word!= if and word!=while, that
         * 'word' is a known function name and
         * '...' hold the matching parameters for function 'word'
         *
         * @param line input
         * @return false if not formatted as word(...), true if its a legit function call
         * @throws SyntaxException in case of unknown function name, or '...' not aligns with the
         *                         expected function parameter values
         */
        private boolean isFunctionCallLegit (String line) throws SyntaxException {
            String format = "^\\s*(\\w[\\w\\d_]*)\\s*\\((.*)\\)\\s*;\\s*$";
            Matcher matcher = Pattern.compile(format).matcher(line);
            if (!matcher.find()) return false;
            String funcName = matcher.group(1);
            if (!functionManager.doesFunctionExist(funcName))
                throw new SyntaxException(String.format("unknown function at: %s", line));
            return parametersMatchRequiredVariableTypes(matcher.group(2), funcName);
        }
        
        /**
         * @param group    is the '...' in a line such as: "func(...){"
         * @param funcName the name of the function
         * @return true if '...' matches the parameters needed, false otherwise
         * @throws SyntaxException the errors that might rise from miss-matching parameters
         */
        private boolean parametersMatchRequiredVariableTypes (String group, String funcName) throws
        SyntaxException {
            ArrayList<VarType> paramType;
            try {
                paramType = functionManager.getParameterTypes(funcName);
            } catch (Exception NoSuchMethodException) {
                throw new SyntaxException(String.format("\"%s\":Unknown function error.", funcName));
            }
            var varNames = group.strip().split(",");
            if (varNames.length == 1 && varNames[0].equals("") && paramType.size() == 0)
                return true; //empty
            if (varNames.length != paramType.size())
                throw new InvalidParameterException(String.format("in " +
                                "parametersMatchRequiredVariableTypes,\n Wrong " +
                                "number Of variables used in function call \"%s(%s)\"", funcName,
                        group));
            for (int i = 0; i < paramType.size(); i++) {
                var v = paramType.get(i);
                var input = varNames[i].strip();
                // does literal string fill the demand of paramType[i] param type?
                if (v == VarType.BOOLEAN && literalStringIsBoolean(input)) continue;
                if ((v == VarType.INT || v == VarType.DOUBLE) && literalStringIsNumber(input))
                    continue;
                if (v == VarType.STRING && literalStringIsString(input)) continue;
                if (v == VarType.CHAR && literalStringIsChar(input)) continue;
                // if not, if it's a known variable (and initialized):
                try {
                    if (!DownCaster.firstAcceptsSecond
                            (paramType.get(i), memoryManager.getVarAttributes(input).getVariableType())
                            && memoryManager.getVarAttributes(input).isInitialized()) {
                        throw new InvalidParameterException(String.format("Wrong " +
                                "type Of variables in function call \"%s\" using\"%s\"", funcName, group));
                    }
                } catch (NullPointerException exception) {
                    throw new SyntaxException(String.format("At parametersMatchRequiredVariableTypes, " +
                            "mismatching parameters while calling function: \"%s\"", funcName));
                }
            }
            return true;
        }
        
        /**
         * is it a legit return line
         *
         * @param line input
         * @return line == legit return line
         * @throws SyntaxException in case of return but not legit line. like "return f;"
         */
        private boolean isReturnLine (String line) throws SyntaxException {
            if (!line.contains(" return")) return false;
            Matcher matcher = Pattern.compile("^\\s*return\\s*;\\s*$").matcher(line);
            if (matcher.find())
                return true;
            throw new SyntaxException(String.format("in Method isReturnLine:\"%s\"", line));
        }
        
        /**
         * given an expression, answer if it's a boolean or not
         *
         * @param str input
         * @return true/false
         */
        private boolean literalStringIsBoolean (String str){
            str = str.strip();
            // if it is either true/false strings or a double/int number literal - accept
            if (str.equals("true") || str.equals("false") || literalStringIsNumber(str))
                return true;
            // if it's an initiated variable with boolean meaning variable - accept
            var variable = memoryManager.getVarAttributes(str);
            if (variable == null || !variable.isInitialized()) return false;
            return variable.getVariableType() == VarType.BOOLEAN || variable.getVariableType() == VarType.INT ||
                    variable.getVariableType() == VarType.DOUBLE;
        }
        
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~  VARIABLES HELPERS  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        /**
         * @param line s-java line
         * @return is it a legit declaration variable line
         */
        public boolean isVarAssignmentLineLegit (String line){
            Matcher m = Pattern.compile("\\s*" + VAR_REGEX_EXPRESSION + ".*").matcher(line);
            if (!m.find())
                return false;
            if (memoryManager.getVarAttributes(m.group(1)) == null)
                return false;
            line = removeSemicolon(line);
            //this regex splits by commas that have an even number of " after them-commas not inside strings
            String[] varAssignments = splitByCommas(line);
            for (String varAssignment : varAssignments) {
                varAssignment = varAssignment.strip();
                if (varAssignment.equals(""))
                    throw new IllegalVarDecException("Missing variable assignment between commas");
                m = Pattern.compile("^" + VAR_REGEX_EXPRESSION +
                        "\\s*=\\s*(\\S*|\"[^\"]*\")\\s*$").matcher(varAssignment);
                if (!m.find())
                    throw new IllegalVarDecException("Illegal format for variable assignment: " + line);
                String name = m.group(1);
                VariableAttributes attr = memoryManager.getVarAttributes(name);
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
         * split a line by commas that are not inside of strings
         *
         * @param line the input line
         * @return the original string, split by commas that are not in string.
         */
        private String[] splitByCommas (String line){
            return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        }
        
        /**
         * check if the given declaration is a valid declaration of a variable list, and if it is, declare the variables.
         * doesn't expect a semicolon at the end of the input.
         *
         * @param declaration       the variable declaration to parse.
         * @param onVariableDeclare a function that decides what to do with a valid variable declaration.
         * @return true if the declaration is valid, else false.
         */
        private boolean isVarDecLegit (String
        declaration, Consumer < VariableAttributes > onVariableDeclare){
            //check if the line start with final
            Matcher m = Pattern.compile("^\\s*(final\\s+)?(\\b\\S*\\b)(.*?)$").matcher(declaration);
            if (!m.find())
                return false;
            boolean isFinal = m.group(1) != null;
            //get the type of the variables in the declaration.
            String type = m.group(2);
            VarType varType = VarType.getVarType(type);
            
            //separate the declaration into individual variables
            String[] vars = splitByCommas(m.group(3));
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
                VariableAttributes variableAttributes = new VariableAttributes(varName, isFinal, varType, isInitialized);
                onVariableDeclare.accept(variableAttributes);
            }
            return true;
        }
        
        /**
         * gets a variable initialization in the format (= <value>)? and sets the variable to initialized
         *
         * @param initString a string in the format (= <value>)?
         * @param type       the type that we expect the initialization value to have
         * @return true if the variable is initialized in the string, else false
         */
        private boolean isVarInitialized (VarType type, String initString){
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
        private boolean isLegalValueForType (String value, VarType type){
            VarType potentialType;
            potentialType = VarType.getTypeOfValue(value);
            if (potentialType != null)
                return DownCaster.firstAcceptsSecond(type, potentialType);
            VariableAttributes var = memoryManager.getVarAttributes(value);
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
        private String getVariableName (String varDec){
            Matcher m = Pattern.compile("^" + VAR_REGEX_EXPRESSION + "\\s*((=)(.*))?$").matcher(varDec);
            if (!m.find())
                throw new IllegalVarDecException("Invalid variable identifier or initialization: " + varDec);
            return m.group(1).strip();
        }
        
        /**
         * inserts a VariableAttributes type parameter to the function scope
         *
         * @param attributes the value
         */
        private void addFunctionArgument (VariableAttributes attributes){
            //function arguments are always initialized
            memoryManager.declareVariable(new VariableAttributes(
                    attributes.getName(),
                    attributes.isFinal(),
                    attributes.getVariableType(),
                    true));
        }
        
        /**
         * read a function declaration line: make sure that
         * it starts with void,
         * continue with valid name,
         * (
         * then pairs of Type ,Name where Name doesn't repeat itself in th declaration
         * ){
         *
         * @param line given input
         * @return true/false accordingly
         * @throws SyntaxException if it was found.
         */
        private boolean isFuncDecValid (String line,boolean isFirstIteration) throws SyntaxException
        {
            //check if the line is a function decleration line:
            if (!line.matches("^\\s*void\\s+.*$")) return false;
            
            String funcDecRegex = "^\\s*(void)\\s+(\\w\\w*)\\s*\\((.*)\\)\\{\\s*$";
            Matcher m = Pattern.compile(funcDecRegex).matcher(line);
            // if match not found
            if (!m.find() || m.group(1) == null) throw new SyntaxException(String.format(
                    "\"%s\": Illegal function declaration", line));
            String funcName = m.group(2);
            if (isFirstIteration && functionManager.doesFunctionExist(funcName))
                throw new SyntaxException("May not use the same name for different function. name: " + funcName);
            if (!memoryManager.isOuterScope())
                throw new SyntaxException("May declare a function only in outer scope. name: " + funcName);
            
            ArrayList<VarType> funcVariables = new ArrayList<>();
            
            memoryManager.increaseScopeDepth();
            // check validity of function inputs(in case there is input)
            String arguments = m.group(3);
            if (arguments.strip().equals("")) {
                if (isFirstIteration) {
                    functionManager.addFunction(funcName, funcVariables);
                }
                return true;
            }
            String[] argumentDeclarations = m.group(3).split(",");
            if(argumentDeclarations.length <= countRemovedChars(m.group(3),","))
                throw new SyntaxException("Wrong number of ',' in function declaration");
            for (String s : argumentDeclarations) {
                isVarDecLegit(s, this::addFunctionArgument);
                String varName = getVariableName(s.replaceFirst("(?:final\\s+)?\\s*" +
                        VarType.getVarTypesRegex(), "").strip());
                funcVariables.add(memoryManager.getVarAttributes(varName).getVariableType());// add to list of func variable
            }
            if (isFirstIteration)
                functionManager.addFunction(funcName, funcVariables);
            return true;
        }
        
        /**
         * @param line a line of s-java
         * @return does line starts with if or while, the (...){, with '...' a boolean expression
         * @throws SyntaxException in case of starting with if/while(...){ BUT not ... is not boolean ex
         */
        private boolean isWhileOrIf (String line, boolean isFirstIteration) throws SyntaxException {
            // regex for "if"\"while" then "(" then something that should be boolean expression, then ){
            String WhileIfRegex = "^\\s*(while|if)\\s*\\((.*)\\)\\s*\\{\\s*$";
            Matcher matcher = Pattern.compile(WhileIfRegex).matcher(line);
            if (!matcher.find() || matcher.group(1) == null) return false;
            if (isFirstIteration) {
                memoryManager.increaseScopeDepth();
                return true; // as parameters aren't updated yet
            }
            if (!verifyBoolean(matcher.group(2).strip())) {
                throw new SyntaxException("in isWhileOrIf, expression is not a boolean expression");
            }
            memoryManager.increaseScopeDepth(); // upon entering a new scope.
            return true;
        }
        
        /**
         *
         * @param src source string
         * @param sub regex remove
         * @return the number of letters removed from src
         */
        private int countRemovedChars (String src, String sub){
            return src.length() - src.replaceAll(sub, "").length();
        }
    }
    

