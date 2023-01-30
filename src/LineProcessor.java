import symbol_managment.*;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * java line i one of those:
 * empty line - only spaces. \s regex.
 */

class LineProcessor {
    // a predicate that handles empty lines
    private final static String VAR_REGEX_EXPRESSION = "([a-zA-Z]\\w*|_\\w+)";
    private final static String DOUBLE_REGEX_EXPRESSION = "^[+-]?\\d+(\\.\\d*)?$"; // todo fix it
    // to not accept 54f.3
    private final static String INT_REGEX_EXPRESSION = "([+-]?[0-9]+)";
    private final static String BOOLEAN_REGEX_EXPRESSION = "(true|false)";
    private final static String[] keywords = {"while", "if", "final", "void", "true", "false", "return"};

    private static final String FUNCTION_REGEX_START = "^\\s*(void)\\s+(\\w+)\\s*\\((.*)\\)";
    private static final Pattern FUNCTION_CALL = Pattern.compile(FUNCTION_REGEX_START + "\\s*;\\s*$");
    private static final Pattern METHOD_DEC_REGEX = Pattern.compile(FUNCTION_REGEX_START + "\\s*\\{\\s*$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile(DOUBLE_REGEX_EXPRESSION);
    MemoryManager memoryManager;
    FunctionManager functionManager;
    //    private boolean nextLineMustNotBeEmpty;
    private boolean lastLineWasReturn; // this must be ^\\s*}\\s*$
    private List<VariableAttributes> uninitializedGlobals;

    public LineProcessor() {
        memoryManager = new MemoryManager();
        functionManager = new FunctionManager();
        uninitializedGlobals = new ArrayList<>();
//        nextLineMustNotBeEmpty = false;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   MAIN TOOLS  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * for the first iteration: include empty ines, global variable, and function declarations
     *
     * @param line input
     * @return true if the line is valid. else false
     * @throws SyntaxException throe in case of syntax err
     */
    public boolean processLineFirstIteration(String line) throws SyntaxException {
        // if it's not the outer scope

        boolean isValid = isEmptyLine(line) ||
                isFuncDecValid(line, true) ||
                isWhileOrIf(line) ||
                isBackwardsCurlyBraces(line) ||
                (!memoryManager.isOuterScope() || (isVarDecLineValid(line) ||
                        isVarAssignmentLineLegit(line))) ;
        lastLineWasReturn = isReturnLine(line);
        return isValid || lastLineWasReturn;
    }

    /**
     * for sec iteration include if/while. empty lines. var declaration, var =, } , return,
     * AND function calls
     *
     * @param line input
     * @return true if ok. else false
     * @throws SyntaxException throe in case of syntax err
     */
    public boolean processLineSecondIteration(String line) throws SyntaxException {
        if (memoryManager.isOuterScope() && line.contains(";")) return true; //was checked 1st iteration
        boolean isValid = isWhileOrIf(line) ||
                isFunctionCallLegit(line) ||
                isFuncDecValid(line, false) ||
                isBackwardsCurlyBraces(line) ||
                (!memoryManager.isOuterScope() &&
                        (isVarAssignmentLineLegit(line) || isVarDecLineValid(line))) ||
                (isCommentLine(line) ||
                        isEmptyLine(line));


        lastLineWasReturn = isReturnLine(line); // !must not happen before isBackwardsCurlyBraces()!
        return isValid || lastLineWasReturn;
    }

    private static boolean isEmptyLine(String line) {
        String l = line.replaceAll("\\s", "");
        return l.length() == 0;
    }

    private static boolean isCommentLine(String line) throws SyntaxException {
        Matcher m = Pattern.compile("//").matcher(line);
        if (!m.find()) return false;
        if (!line.split("//")[0].equals(""))
            throw new SyntaxException(
                    String.format("Comment must be a full line,Error in line:\"%s\"", line));
        return true;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   MAIN TOOLS END    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


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

    private static boolean literalStringIsChar(String varName) {
        String x = varName.strip();
        return x.length() == 3 && x.charAt(0) == x.charAt(2) && x.charAt(2) == '\'';
    }

    private static boolean literalStringIsString(String varName) {
        return varName.strip().matches("\"[^\"]*\"");
    }

    public void prepareForIteration2() {
        uninitializedGlobals = memoryManager.getUninitializedGlobals();
    }

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
        if(memoryManager.isFunctionScope())
            memoryManager.unInitializeGlobals(uninitializedGlobals);

        memoryManager.decreaseScopeDepth();
        return true;
    }

    /**
     * get a string and verify that the value it hold is boolean
     *
     * @param s the input string
     * @return true if boolean expression. else false.
     */
    private boolean verifyBoolean(String s) {
        String boolExpression = "^\\s*(true|false|[+-]*\\d+\\.\\d*|\\w[\\w\\d_]*|_[\\w\\d_]+)" +
                "(\\s*(&&|\\|\\|)\\s*.*)$";
        // with groups:0-all, 1-one of *_REGEX_EXPRESSION, 2-&& or || following by REGEX_EXPRESSION unlimited times
        Matcher m = Pattern.compile(boolExpression).matcher(s);
        if (!m.find()) return false;
        literalStringIsBoolean(m.group(1));
        // sure that if its var, it's of bool/int/double and is
        if (m.group(2) == null) return true;
        String[] expressions = m.group(2).strip().split("(&&|\\|\\|)");
        for (int i = 1; i < expressions.length; i++)
            if (!literalStringIsBoolean(expressions[i].strip())) return false;
        return true;
    }

    /**
     * check if the given line is in the format for a variable declaration, and if it is, declare the variables
     *
     * @param line The line to parse.
     * @return true if the line is a variable declaration line, else false.
     */
    private boolean isVarDecLineValid(String line) {
        //check if the line contains a variable declaration.
        Matcher m = Pattern.compile("^\\s*(final\\s+)?" + VarType.getVarTypesRegex() + ".*$").matcher(line);
        if (!m.find())
            return false;
        line = removeSemicolon(line);
        return isVarDecLegit(line, memoryManager::declareVariable);
    }

    private boolean isFunctionCallLegit(String line) throws SyntaxException {
        String format = "^\\s*(\\w[\\w\\d_]*)\\s*\\((.*)\\)\\s*;\\s*$";
        Matcher matcher = Pattern.compile(format).matcher(line);
        if (!matcher.find()) return false;
        String funcName = matcher.group(1);
        if (!functionManager.doesFunctionExist(funcName))
            throw new SyntaxException(String.format("unknown function at: %s", line));
        return parametersMatchRequiredVariableTypes(matcher.group(2), funcName);
    }

    private boolean parametersMatchRequiredVariableTypes(String group, String funcName) throws SyntaxException {
        ArrayList<VarType> paramType;
        try {
            paramType = functionManager.getParameterTypes(funcName);
        } catch (Exception NoSuchMethodException) {
            throw new SyntaxException(String.format("\"%s\":Unknown function error.", funcName));
        }
        var varNames = group.strip().split(",");
        if (varNames.length == 1 && varNames[0].equals("") && paramType.size() == 0) return true; //empty
        if (varNames.length != paramType.size())
            throw new InvalidParameterException(String.format("Wrong " +
                    "number Of variables in function call \"%s\" using\"%s\"", funcName, group));
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
                        "type Of variables in function call \"%s\" using\"%s\"", funcName, group));
            }
        }
        return true;
    }


    private boolean isReturnLine(String line) {
        Matcher matcher = Pattern.compile("^\\s*return\\s*;\\s*$").matcher(line);
        return matcher.find();
    }

//    /**
//     * if a function was found, enters a function after increasing scope depth and saving the
//     * arguments there
//     *
//     * @param line the line
//     * @return true if it's a function declaration
//     * @throws SyntaxException if something went wrong
//     */
//    private boolean LegitFunctionDeclaration2rdIteration(String line) throws SyntaxException {
//        Matcher m = METHOD_DEC_REGEX.matcher(line);
//        // if match not found
//        if (!m.find() || m.group(1) == null) return false;
//        if (!memoryManager.isOuterScope()) throw new SyntaxException(String.
//                format("Method Declaration In Inner Scope Error: \"%s\"", line));
//        if (!functionManager.doesFunctionExist(m.group(2))) throw new SyntaxException(String.
//                format("Unknown Method Error: \"%s\"", line));
//        memoryManager.increaseScopeDepth();
//
//        if (m.group(3).equals("")) {
//            return true;
//        }
//        // check legit of function inputs(in case there is input)
//        String[] type_Variable = m.group(3).split(",");
//
//        Pattern patNameCompile = Pattern.compile(VAR_REGEX_EXPRESSION);
//        for (String s : type_Variable) {
//            // declare final stuff?
//            String[] splitInto = s.strip().split(" ");
//            if (splitInto.length > 2) throw new SyntaxException(
//                    String.format("%s is not a valid pair of (type ,var_name)", s));
//
//            s = s.strip();
//            String sType = s.split(" ")[0].strip(), sVarName = s.split(" ")[1].strip();
//
//            memoryManager.declareVariable(new VariableAttributes
//                    (sVarName, false, VarType.getVarType(sType), true));
//        }
//
//        return true;
//    }

    // todo how come its not used (asking  myself)

    /**
     * get an expression and verify that the value it hold is boolean
     *
     * @param expression the input string
     * @return true if boolean expression. Else false
     */
    private boolean verifyBooleanExpression(String expression) {
        String boolExpression = "^\\s*(true|false|[+-]*\\d+\\.\\d*|\\w[\\w\\d_]*|_[\\w\\d_]+)" +
                "(\\s*(&&|\\|\\|)\\s*.*)$";
        // with groups:0-all, 1-one of *_REGEX_EXPRESSION, 2-&& or || following by REGEX_EXPRESSION unlimited times
        Matcher m = Pattern.compile(boolExpression).matcher(expression);
        if (!m.find()) return false;
        literalStringIsBoolean(m.group(1));
        // sure that if its var, it's of bool/int/double and is
        if (m.group(2) == null) return true;
        String[] expressions = m.group(2).strip().split("(&&|\\|\\|)");
        for (int i = 1; i < expressions.length; i++)
            if (!literalStringIsBoolean(expressions[i].strip())) return false;
        return true;
    }

    /**
     * given an expression, answer if it's a boolean or not
     *
     * @param str input
     * @return true/false
     */
    private boolean literalStringIsBoolean(String str) {
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

    public boolean isVarAssignmentLineLegit(String line) {
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
                    "\\s*=\\s* (\\S*|\"[^\"]*\")\\s*$").matcher(varAssignment);
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
    private boolean isVarDecLegit(String declaration, Consumer<VariableAttributes> onVariableDeclare) {
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
    private String getVariableName(String varDec) {
        Matcher m = Pattern.compile("^" + VAR_REGEX_EXPRESSION + "\\s*((=)(.*))?$").matcher(varDec);
        if (!m.find())
            throw new IllegalVarDecException("Invalid variable identifier or initialization: " + varDec);
        return m.group(1).strip();
    }


    private void addGlobalVariable(VariableAttributes attributes) {
        if (!memoryManager.isOuterScope())
            return;
        if (attributes.isFinal() && !attributes.isInitialized())
            throw new RuntimeException("final variable is not initialized");//todo change exception type
        memoryManager.declareVariable(attributes);
    }

    private void addFunctionArgument(VariableAttributes attributes) {
        //function arguments are always initialized
        memoryManager.declareVariable(new VariableAttributes(
                attributes.getName(),
                attributes.isFinal(),
                attributes.getVariableType(),
                true));
    }

    private void addLocalVariable(VariableAttributes variableAttributes) {
        if (!memoryManager.isOuterScope()) {
            memoryManager.declareVariable(variableAttributes);
        }
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

    private boolean isWhileOrIf(String line) {
        // regex for "if"\"while" then "(" then something that should be boolean expression, then ){
        String WhileIfRegex = "^\\s*(while|if)\\s*\\((.*)\\)\\s*\\{\\s*$";
        Matcher matcher = Pattern.compile(WhileIfRegex).matcher(line);
        if (!matcher.find() || matcher.group(1) == null) return false;
        verifyBoolean(matcher.group(2).strip());
        memoryManager.increaseScopeDepth(); // upon entering a new scope.
        return true;
    }
}
    

