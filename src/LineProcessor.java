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
    }

    private final static String[] keywords = {"while", "if", "final", "void","true","false"};
    private final static String VAR_REGEX_EXPRESSION = "([a-zA-Z][a-zA-Z0-9_]*|_[a-zA-Z0-9_]+)";
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
        try {
            System.out.println(l.isVarDecLineLegit("final    double true  , gba, asf;", (x, y) -> {
            }));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
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
        boolean isWhileOrIfChunck = !memoryManager.isOuterScope() && isWhileOrIf(line);
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
    

