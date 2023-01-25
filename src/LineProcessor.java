import symbol_managment.*;

import javax.lang.model.type.UnknownTypeException;
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

    private static boolean isEmptyLine(String line) {
        String l = line.replaceAll("\\s", "");
        return l.length() == 0;
    };
    private final static String VAR_REGEX_EXPRESSION = "([a-zA-Z][a-zA-Z0-9_]*|_[a-zA-Z0-9_]+)";
    private final static String DOUBLE_REGEX_EXPRESSION = "(\\s*(\\+|\\-)?[0-9]+(\\.(0-9)*)?\\s*)";
    private final static String INT_REGEX_EXPRESSION = "(\\s*(\\+|\\-)?[0-9]+)\\s*";
    private final static String BOOLEAN_REGEX_EXPRESSION = "(\\s*(true|false)?\\s*)";
    private final static String RESEREVED_WORD =
            ("(if|while|true|false|final|return|void|int|char|boolean|double|String)");
    private final static String[] keywords = {"while", "if", "final", "void","true","false"};
    
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
        try {
            System.out.println(l.isVarDecLineLegit("final    double true  , gba, asf;", (x, y) -> {
            }));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * get a string and varify that the value it hold is boolean
     *
     * @param s the input string
     * @return true if boolean expression- otherwise false
     */
    private static boolean varifyBoolean(String s) {
        //splits into
        String boolLegitValueExpression =
                ("\\s*(" +
                        BOOLEAN_REGEX_EXPRESSION + "|" +
                        DOUBLE_REGEX_EXPRESSION + "|" +
                        VAR_REGEX_EXPRESSION + ")" +
                        "(\\s+&&|\\|\\|\\s*(" +
                        BOOLEAN_REGEX_EXPRESSION + "|" +
                        DOUBLE_REGEX_EXPRESSION + "|" +
                        VAR_REGEX_EXPRESSION + "))*");
        // with groups:0-all, 1-one of *_REGEX_EXPRESSION, 2-&& or || following by REGEX_EXPRESSION unlimited times
        Matcher m = Pattern.compile(boolLegitValueExpression).matcher(s);
        if (!m.find()) return false;
        String first = m.group(1).strip(); // todo need to make sure its not reserved_word, make
        varifyExpressionIsBoolean(first);
        // sure that if its var, its of bool/int/double and is
        if (m.group(5) == null) return true;
        String[] expressions = m.group(5).strip().split("&&|(\\|\\|)");
        for (int i = 0; i < expressions.length; i++)
            varifyExpressionIsBoolean(expressions[i]);
        
        return true;
    }
    
    /**
     * given an expression, answer if its a boolean or not
     *
     * @param expression input
     * @return true/false
     */
    private static boolean varifyExpressionIsBoolean(String expression) {
        expression = expression.strip();
        if (expression.equals("true") || expression.equals("false"))
            return true;
        return true;
    }

    /**
     * check if the given line is in the format for a variable declaration, and if it is, declare the variables
     *
     * @param line              The line to parse.
     * @param onVariableDeclare a function that decides what to do with a valid variable declaration.
     * @return true if the line is a variable declaration line, else false.
     */
    private boolean isVarDecLineLegit(String line, BiConsumer<String, VariableAttribute> onVariableDeclare) {
        Matcher m = Pattern.compile("(.*);\\s*$").matcher(line);
        if (!m.find())
            throw new MissingSemicolonException();
        //pattern for checking
        return isVarDecLegit(line.replaceFirst(";", ""), onVariableDeclare);
    }

    /**
     * check if the given declaration is a valid declaration of a variable list, and if it is, declare the variables.
     * doesnt excpect a semicolon at the end of the input.
     *
     * @param declaration       the variable declaration to parse.
     * @param onVariableDeclare a function that decides what to do with a valid variable declaration.
     * @return true if the decleration is valid, else false.
     */
    private boolean isVarDecLegit(String declaration, BiConsumer<String, VariableAttribute> onVariableDeclare) {
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
            onVariableDeclare.accept(varName, variableAttribute);
        }
        return true;
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
            return DownCaster.cast(type, potentialType);
        VariableAttribute var = memoryManager.getVarAttributes(value);
        if (var != null) {
            return DownCaster.cast(type, var.getVariableType());
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

    // programing in stages:
    // first is ignoring empty lines:
    public boolean processLineFirstIteration(String line) throws SyntaxException {
        boolean emptyLineIsLegit = nextLineMustNotBeEmpty && isEmptyLine(line);
        boolean functionDecleration = isFuncDecLegit(line);
        boolean isVariableDecleration = isVarDecLineLegit(line, this::addGlobalVariable);
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
//        boolean isWhileOrIfChunck = !memoryManager.isOuterScope() && isWhileOrIf(line);
        boolean isWhileOrIfChunck = isWhileOrIf(line);
        return isWhileOrIfChunck;
    }

    private void addGlobalVariable(String name, VariableAttribute variableAttribute) {
        if (memoryManager.isOuterScope()) {
            if (memoryManager.declareable(name)) {
                memoryManager.declareVariable(variableAttribute);
            }
        }
        // NOTE I MAKE ANOTHER FIELD TO FILL IN CASE THE GLOABAL WAS
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
        if (!m.find() || m.group(1) == null) return false;
        String funcName = m.group(2);
        if (functionManager.doesFunctionExist(funcName)) throw new SyntaxException(String.
                format("May not use the same name for different function. name %s", funcName));
        
        if (m.group(3).equals("")) return true;
        // check legit of function inputs(in case there is input)
        String[] type_Variable = m.group(3).split(",");
        
        ArrayList<VarType> funcVariable = new ArrayList<>();
        HashSet<String> variableNames = new HashSet<>();
        
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
        varifyBoolean(matcher.group(2).strip()); // TODO func that verifies that the expression is boolean
        
        memoryManager.increaseScopeDepth(); // upon entering a new scope.
        nextLineMustNotBeEmpty = true;  // after { the next line must not be empty
        return true;
    }
}
    

