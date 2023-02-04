package oop.ex6.parsing;

import oop.ex6.SyntaxException;
import oop.ex6.main.*;
import oop.ex6.symbol_managment.*;

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
 * oop.ex6.parsing.LineProcessor main logic is to accept an s-java line and either throw an informative error of
 * returns false.
 * <p>
 * It's done by iterating twice on the file at first filling up
 * memoryManager - with global variable
 * functionManager - function names, and the parameters each func accepts
 * <p>
 * then iterating again and reading it line by line, using the knowledge from the prev iteration.
 */

public class LineProcessor {
    private final static String VAR_REGEX_EXPRESSION = "(\\w[\\w\\d_]*|_[\\w\\d_]+)";
    private final static String[] keywords = {"while", "if", "final", "void", "true", "false", "return"};
    private static final String FUNC_CALL_PATTERN = "^\\s*(\\w[\\w\\d_]*)\\s*\\((.*)\\)\\s*;\\s*$";

    private final String fileName;
    MemoryManager memoryManager;
    FunctionManager functionManager;
    private boolean lastLineWasReturn;
    private List<VariableAttributes> uninitializedGlobals;

    /**
     * constructor
     *
     * @param fileName the name of the file this oop.ex6.parsing.LineProcessor reads.
     */
    public LineProcessor(String fileName) {
        this.fileName = fileName;
        memoryManager = new MemoryManager();
        functionManager = new FunctionManager();
        uninitializedGlobals = new ArrayList<>();
    }

    /**
     * @param line input
     * @return input contains only whitespace chars
     */
    private static boolean isEmptyLine(String line) {
        String l = line.replaceAll("\\s", "");
        return l.length() == 0;
    }

    /**
     * @param line input
     * @return input starts with //
     * @throws InvalidCommentException in case it contains // but not at the start of the input
     */
    private static boolean isCommentLine(String line) throws InvalidCommentException {
        Matcher m = Pattern.compile("//").matcher(line);
        if (!m.find()) return false;
        if (!line.split("//")[0].equals(""))
            throw new InvalidCommentException();
        return true;
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

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   MAIN TOOLS  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private boolean isBackwardsCurlyBraces(String line) throws SyntaxException {
        if (!line.contains("}")) return false;
        if (!(line.strip().equals("}"))) {
            throw new BackwardsCurlyException("Backwards Curl must be singular in a line");
        }
        // line is in format \s*}\s* :
        if (memoryManager.isOuterScope())
            throw new BackwardsCurlyException("Backwards Curl must not be at the outer scope");
        //in case of func, previous line must be return, so
        if (!lastLineWasReturn && memoryManager.isFunctionScope())
            throw new BackwardsCurlyException("in Method Backwards Curl must follow a \"return;\" line");
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
        String line = "";
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        try {
            while ((line = reader.readLine()) != null) {
                if (!processLineFirstIteration(line))
                    return Status.SYNTAX; // for illegal code;
            }
        } catch (SyntaxException e) {
            throw new SyntaxException(e.getMessage() + "\nLine: " + line);
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
                isCommentLine(line) ||
                (!memoryManager.isOuterScope() || (isVarDecLineValid(line) ||
                        isVarAssignmentLineValid(line)));
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
        String line = "";
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        try {
            while ((line = reader.readLine()) != null) {
                if (!processLineSecondIteration(line))
                    return Status.SYNTAX; // for illegal code
            }
        } catch (SyntaxException e) {
            throw new SyntaxException(e.getMessage() + "\nline: " + line);
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
        if (memoryManager.isOuterScope() && line.contains(";")) {
            if (line.matches(FUNC_CALL_PATTERN))
                throw new IllegalFuncCallException("Calling functions in global scope is illegal");
            return true; //was checked 1st iteration
        }
        //only need to make sure there are no function calls
        boolean isValid = isWhileOrIf(line, false) ||
                isFuncDecValid(line, false) ||
                isBackwardsCurlyBraces(line) ||
                isFunctionCallValid(line) ||
                (!memoryManager.isOuterScope() &&
                        (isVarAssignmentLineValid(line) || isVarDecLineValid(line))) ||
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
            throw new InvalidBooleanException("Too Many && or || were found in boolean expression");

        //ex:true false or 54.3
        boolean firstPartIsBooleanLiteral = (m.group(2) != null || m.group(3) != null);
        if (!firstPartIsBooleanLiteral) {
            var v = m.group(4) != null ?
                    memoryManager.getVarAttributes(m.group(4)) :
                    memoryManager.getVarAttributes(m.group(5));
            if (v == null)
                throw new InvalidBooleanException("Undeclared variable in boolean expression");
            VarType t = v.getVariableType();
            if (v.isUninitialized())
                throw new InvalidBooleanException("Attempt to use an uninitialized variable detected");
            if (!VarType.firstAcceptsSecond(VarType.BOOLEAN, t))
                throw new InvalidBooleanException("Invalid type in boolean expression");
        }

        if (m.group(6) == null) return true;
        String[] expressions = m.group(6).strip().split("(&&|\\|\\|)");
        if (expressions.length == 0)
            throw new InvalidBooleanException("Extra && or || in boolean expression");

        for (int i = 1; i < expressions.length; i++)
            if (!isLegalValueForType(expressions[i].strip(), VarType.BOOLEAN))
                throw new InvalidBooleanException("Invalid type or variable in boolean expression: " +
                        expressions[i].strip());
        return true;
    }

    /**
     * check if the given line is in the format for a variable declaration, and if it is, declare the variables
     *
     * @param line The line to parse.
     * @return true if the line is a variable declaration line, else false.
     */
    private boolean isVarDecLineValid(String line) throws IllegalVarDecException {
        //check if the line contains a variable declaration.
        Matcher m = Pattern.compile("^\\s*(final\\s+)?" + VarType.getVarTypesRegex() + ".*$").matcher(line);
        if (!m.find())
            return false;
        line = removeSemicolon(line);
        return isVarDecValid(line, this::createVariable);
    }

    private void createVariable(VariableAttributes attributes) throws IllegalVarDecException {
        if (attributes.isFinal() && attributes.isUninitialized())
            throw new IllegalVarDecException("A final variable was declared and not initialized");
        memoryManager.declareVariable(attributes);
    }

    /**
     * for line formatted as word(...){
     * where word!= if and word!=while, that
     * 'word' is a known function name and
     * '...' hold the matching parameters for function 'word'
     *
     * @param line input
     * @return false if not formatted as word(...), true if its a valid function call
     * @throws SyntaxException in case of unknown function name, or '...' not aligns with the
     *                         expected function parameter values
     */
    private boolean isFunctionCallValid(String line) throws SyntaxException {
        Matcher matcher = Pattern.compile(FUNC_CALL_PATTERN).matcher(line);
        if (!matcher.find()) return false;

        String funcName = matcher.group(1);
        if (!functionManager.doesFunctionExist(funcName))
            throw new UnknownIdentifierException("The function " + funcName + " is not defined");
        return parametersMatchRequiredVariableTypes(matcher.group(2), funcName);
    }

    /**
     * @param group    is the '...' in a line such as: "func(...){"
     * @param funcName the name of the function
     * @return true if '...' matches the parameters needed, false otherwise
     * @throws SyntaxException the errors that might rise from miss-matching parameters
     */
    private boolean parametersMatchRequiredVariableTypes(String group, String funcName)
            throws SyntaxException {
        ArrayList<VarType> paramTypes;

        paramTypes = functionManager.getParameterTypes(funcName);

        var varNames = group.split(",");
        if (varNames.length == 1 && varNames[0].equals("") && paramTypes.size() == 0)
            return true; //empty
        if (varNames.length != paramTypes.size())
            throw new IllegalFuncCallException(String.format("Mismatching parameter count for function %s. " +
                    "Expected %d, found %d.", funcName, paramTypes.size(), varNames.length));

        for (int i = 0; i < paramTypes.size(); i++) {
            VarType type = paramTypes.get(i);
            String input = varNames[i].strip();
            if (input.equals(""))
                throw new IllegalFuncCallException("Missing argument in function call");

            if (VarType.firstAcceptsSecond(type, VarType.getTypeOfValue(input))) continue;
            // if not, if it's a known variable (and initialized):

            VariableAttributes variable = memoryManager.getVarAttributes(input);
            if (variable == null)
                throw new UnknownIdentifierException("The variable " + input + " is not declared in this scope");
            if (!VarType.firstAcceptsSecond(type, variable.getVariableType()))
                throw new InvalidParameterException("Wrong type of argument in function call");
            if (variable.isUninitialized())
                throw new IllegalFuncCallException("The variable " + input + " is not initialized in this scope");

        }
        return true;
    }


    /**
     * is it a valid return line
     *
     * @param line input
     * @return true if the line is a valid return line
     */
    private boolean isReturnLine(String line) {
        Matcher matcher = Pattern.compile("^\\s*return\\s*;\\s*$").matcher(line);
        return matcher.find();
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~  VARIABLES HELPERS  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * @param line s-java line
     * @return is it a valid declaration variable line
     */
    public boolean isVarAssignmentLineValid(String line) throws IllegalVarDecException {
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
                throw new IllegalVarDecException("Illegal format for variable assignment: " + varAssignment);
            String name = m.group(1);
            VariableAttributes attr = memoryManager.getVarAttributes(name);
            if (attr == null)
                throw new IllegalVarDecException("Variable " + name + " is not declared");
            VarType type = attr.getVariableType();
            if (attr.isFinal())
                throw new IllegalVarDecException("The variable " + name + " is final and cant be reassigned");
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
    private String[] splitByCommas(String line) {
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
    private boolean isVarDecValid(String declaration, Consumer<VariableAttributes> onVariableDeclare)
            throws IllegalVarDecException {
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
    private boolean isVarInitialized(VarType type, String initString) throws IllegalVarDecException {
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
            throw new IllegalVarDecException("Missing value in variable assignment");
        if (!isLegalValueForType(value, type))
            throw new IllegalVarDecException("The value " + value +
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
            return VarType.firstAcceptsSecond(type, potentialType);
        VariableAttributes var = memoryManager.getVarAttributes(value);
        if (var != null) {
            if (var.isUninitialized())
                throw new UninitializedVarException("Attempting to use an uninitialized variable.");
            return VarType.firstAcceptsSecond(type, var.getVariableType());
        }
        return false;
    }

    /**
     * gets a variable initialization in the format <var-name> (= <value>)? and sets the variable to initialized
     *
     * @param varDec a string in the format <var-name> (= <value>)?
     * @return the name of the variable
     */
    private String getVariableName(String varDec) throws IllegalVarDecException {
        if (varDec.equals(""))
            throw new IllegalVarDecException("Missing variable declaration");
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
    private void addFunctionArgument(VariableAttributes attributes) {
        //function arguments are always initialized
        attributes.setInitiated(true);
        memoryManager.declareVariable(attributes);

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
    private boolean isFuncDecValid(String line, boolean isFirstIteration) throws SyntaxException {
        //check if the line is a function declaration line:
        if (!line.matches("^\\s*void\\s+.*$")) return false;

        String funcDecRegex = "^\\s*(void)\\s+(\\w+)\\s*\\((.*)\\)\\s*\\{\\s*$";
        Matcher m = Pattern.compile(funcDecRegex).matcher(line);
        // if match not found
        if (!m.find() || m.group(1) == null) throw new IllegalFuncDecException("Illegal function declaration");
        String funcName = m.group(2);
        if (isFirstIteration && functionManager.doesFunctionExist(funcName))
            throw new IllegalFuncDecException("May not use the same name for different function: " + funcName);
        if (!memoryManager.isOuterScope())
            throw new IllegalFuncDecException("Function declaration inside functions are not allowed");

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
        if (argumentDeclarations.length <= countRemovedChars(m.group(3), ","))
            throw new IllegalFuncDecException("Wrong number of ',' in function declaration");
        for (String s : argumentDeclarations) {
            isVarDecValid(s, this::addFunctionArgument);
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
     * @return true if the line is in the format (if|while) "("...")" {
     * @throws SyntaxException in case of starting with if/while(exp){ but exp is not a valid boolean expression
     */
    private boolean isWhileOrIf(String line, boolean isFirstIteration) throws SyntaxException {
        // regex for "if"\"while" then "(" then something that should be boolean expression, then ){
        String WhileIfRegex = "^\\s*(while|if)\\s*\\((.*)\\)\\s*\\{\\s*$";
        Matcher matcher = Pattern.compile(WhileIfRegex).matcher(line);
        if (!matcher.find() || matcher.group(1) == null) return false;
        if (isFirstIteration) {
            memoryManager.increaseScopeDepth();
            return true; // as parameters aren't updated yet
        }
        String expression = matcher.group(2).strip();
        if ("".equals(expression))
            throw new InvalidBooleanException("Missing boolean expression in if or while block");
        if (!verifyBoolean(matcher.group(2).strip())) {
            throw new InvalidBooleanException("The expression: \"" + matcher.group(2) + "\" is not a valid boolean expression");
        }
        memoryManager.increaseScopeDepth(); // upon entering a new scope.
        return true;
    }

    /**
     * @param src source string
     * @param sub regex remove
     * @return the number of letters removed from src
     */
    private int countRemovedChars(String src, String sub) {
        return src.length() - src.replaceAll(sub, "").length();
    }
}
    

