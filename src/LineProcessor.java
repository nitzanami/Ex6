import symbol_managment.FunctionManager;
import symbol_managment.MemoryManager;
import symbol_managment.VarType;
import symbol_managment.VariableAttribute;

import java.util.ArrayList;
import java.util.HashSet;
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
    private static final String VAR_REGEX_EXPRESSION = "(\\s*[a-zA-Z][a-zA-Z0-9_]*|_[a-zA-Z0-9_]+\\s*)";
    private static final String DOUBLE_REGEX_EXPRESSION = "(\\s*(\\+|\\-)?[0-9]+(\\.(0-9)*)?\\s*)";
    private static final String INT_REGEX_EXPRESSION = "(\\s*(\\+|\\-)?[0-9]+)\\s*";
    private static final String BOOLEAN_REGEX_EXPRESSION = "(\\s*(true|false)?\\s*)";
    private static final String RESEREVED_WORD =
            ("(if|while|true|false|final|return|void|int|char|boolean|double|String)");
    
    MemoryManager memoryManager;
    FunctionManager functionManager;
    private boolean nextLineMustNotBeEmpty;
    
    public LineProcessor() {
        memoryManager = new MemoryManager();
        functionManager = new FunctionManager();
        nextLineMustNotBeEmpty = true;
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
//        boolean isWhileOrIfChunck = !memoryManager.isOuterScope() && isWhileOrIf(line);
        boolean isWhileOrIfChunck = isWhileOrIf(line);
        return isWhileOrIfChunck;
    }
    
    private void addGlobalVariable(String name, VariableAttribute variableAttribute) {
        //todo make this function add a var if the scope is global
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
    

