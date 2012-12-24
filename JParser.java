/**
 * Use parseEquation for Strings such as 
 * "(sinr(pi/2) - -1 + (3 + 3)*4)/(-0.25^0.5)"
 * to find what value it equates to.
 * 
 * Use parseBooleanEquation for Strings such as 
 * "sqrt(1) < 2 && 2 != 2"
 * to find if the statement is true or false.
 */
public class JParser {
    public static void test() {
        String[] tests = {
            //parseBooleanEquation tests
            "1 < 2",
            "sqrt(1) < 2 && 2 != 2",
            "1! <= 9*2/18 || 1 != 1^2",
            
            //parseEquation tests
            "(sinr(pi/2) - -1 + (3 + 3)*4)/(-0.25^0.5)",
            "cos(2*x/5)*6*(7 - 10/2 - 9.5)",
            "foo(3)!"
        };
        Result[] expectedResults = {
            new Result(true),
            new Result(false),
            new Result(true),
            new Result(-52),
            new Result(45),
            new Result(24)
        };
        Variable[][] vars = {
            null,
            null,
            null,
            null,
            new Variable[]{new Variable("x", 450)},
            null
        };
        Function[][] funcs = {
            null,
            null,
            null,
            null,
            null,
            new Function[]{new Function("foo") {public double applyFunction(double val) {
                return val + 1;
            }}}
        };
        java.util.List<Integer> fails = new java.util.ArrayList<Integer>();
        
        for(int i = 0; i < tests.length; i++) {
            if(expectedResults[i].equals(
                expectedResults[i].isBool
                ? parseBooleanEquation(tests[i], vars[i], funcs[i])
                : parseEquation(tests[i], vars[i], funcs[i]))) {
                fails.add(i);
            }
        }
        
        if(fails.isEmpty()) {
            System.out.println("All tests passed!");
        } else {
            System.out.println("These tests failed:");
            for(Integer i : fails) {
                System.out.println("    [" + i + "] " + tests[i]);
            }
        }
    }
    
    private static class Result {
        private double dVal;
        private boolean bVal;
        public boolean isBool;
        
        public Result(double dVal) {
            isBool = false;
            this.dVal = dVal;
        }
        
        public Result(boolean bVal) {
            isBool = true;
            this.bVal = bVal;
        }
        
        public boolean equals(double dVal) {
            return !isBool && this.dVal == dVal;
        }
        
        public boolean equals(boolean bVal) {
            return isBool && this.bVal == bVal;
        }
    }
    
    public static boolean parseBooleanEquation(String equation) {
        return parseBooleanEquation(equation, null, null);
    }
    
    public static boolean parseBooleanEquation(String equation, Variable[] vars) {
        return parseBooleanEquation(equation, vars, null);
    }
    
    public static boolean parseBooleanEquation(String equation, Function[] funcs) {
        return parseBooleanEquation(equation, null, funcs);
    }

    public static boolean parseBooleanEquation(String equation, Variable[] vars, Function[] funcs) {
        String original = equation;
        
        // Remove any spaces.
        for(int i = 0; i < equation.length(); i++) {
            if(equation.charAt(i) == ' ') {
                String leftSide = equation.substring(0, i);
                String rightSide = equation.substring(i + 1, equation.length());
                equation = leftSide + rightSide;
                i--;
            }
        }
        
        // Convert any "--"s to "+"s
        for(int i = 1; i < equation.length(); i++) {
            if(equation.charAt(i-1) == '-'
            && equation.charAt(i) == '-') {
                String leftSide = equation.substring(0, i-1);
                String rightSide = equation.substring(i + 1, equation.length());
                equation = leftSide + '+' + rightSide;
                i--;
            }
        }
    
        // Parse the equation.
        if(equation.length() == 0) {
            return true;
        }
        try {
            return parseOr(equation, vars, funcs);
        } catch(Exception e) {
            System.err.println("Exception in Parser.parseBooleanEquation(" + original + "). Check the equation for errors.");
            System.err.println("Exception message: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean parseOr(String equation, Variable[] vars, Function[] funcs) throws Exception {
        // Try to pattern match it to:
        // and || or
        // If that fails, match it to:
        // and
        int orIndex = getIndex(equation, "||");
        if(orIndex != -1) {
            String leftSide = equation.substring(0, orIndex);
            String rightSide = equation.substring(orIndex + 2, equation.length());
            return parseAnd(leftSide, vars, funcs) || parseOr(rightSide, vars, funcs);
        } else {
            return parseAnd(equation, vars, funcs);
        }
    }
    
    private static boolean parseAnd(String equation, Variable[] vars, Function[] funcs) throws Exception {
        // Try to pattern match it to:
        // boolparentheses && and
        // If that fails, match it to:
        // boolparentheses
        int andIndex = getIndex(equation, "&&");
        if(andIndex != -1) {
            String leftSide = equation.substring(0, andIndex);
            String rightSide = equation.substring(andIndex + 2, equation.length());
            return parseBoolParentheses(leftSide, vars, funcs) && parseAnd(rightSide, vars, funcs);
        } else {
            return parseBoolParentheses(equation, vars, funcs);
        }
    }
    
    private static boolean parseBoolParentheses(String equation, Variable[] vars, Function[] funcs) throws Exception {
        // Try to pattern match it to:
        // (or)
        // If that fails, match it to:
        // comparison
        if(equation.length() > 2 && equation.charAt(0) == '(' && equation.charAt(equation.length() - 1) == ')') {
            String middle = equation.substring(1, equation.length() - 1);
            return parseOr(middle, vars, funcs);
        } else {
            return parseComparison(equation, vars, funcs);
        }
    }
    
    private static boolean parseComparison(String equation, Variable[] vars, Function[] funcs) throws Exception {
        // Try to pattern match it to:
        // add < add
        // If that fails, match it to:
        // add > add
        // If that fails, match it to:
        // add <= add
        // If that fails, match it to:
        // add >= add
        // If that fails, match it to:
        // add == add
        // If that fails, match it to:
        // add != add
        if(equation.indexOf("<=") != -1) {
            int index = equation.indexOf("<=");
            String leftSide = equation.substring(0, index);
            String rightSide = equation.substring(index + 2, equation.length());
            return parseAdd(leftSide, vars, funcs) <= parseAdd(rightSide, vars, funcs);
        } else if(equation.indexOf(">=") != -1) {
            int index = equation.indexOf(">=");
            String leftSide = equation.substring(0, index);
            String rightSide = equation.substring(index + 2, equation.length());
            return parseAdd(leftSide, vars, funcs) >= parseAdd(rightSide, vars, funcs);
        } else if(equation.indexOf('<') != -1) {
            int index = equation.indexOf('<');
            String leftSide = equation.substring(0, index);
            String rightSide = equation.substring(index + 1, equation.length());
            return parseAdd(leftSide, vars, funcs) < parseAdd(rightSide, vars, funcs);
        } else if(equation.indexOf('>') != -1) {
            int index = equation.indexOf('<');
            String leftSide = equation.substring(0, index);
            String rightSide = equation.substring(index + 1, equation.length());
            return parseAdd(leftSide, vars, funcs) > parseAdd(rightSide, vars, funcs);
        } else if(equation.indexOf("==") != -1) {
            int index = equation.indexOf("==");
            String leftSide = equation.substring(0, index);
            String rightSide = equation.substring(index + 2, equation.length());
            return parseAdd(leftSide, vars, funcs) == parseAdd(rightSide, vars, funcs);
        } else if(equation.indexOf("!=") != -1) {
            int index = equation.indexOf("!=");
            String leftSide = equation.substring(0, index);
            String rightSide = equation.substring(index + 2, equation.length());
            return parseAdd(leftSide, vars, funcs) != parseAdd(rightSide, vars, funcs);
        } else {
            throw new Exception("Check here: \"" + equation + "\"");
        }
    }

    /**
     * Example Usage:
     * 
     * String equationOne = "(sinr(pi/2) - -1 + (3 + 3)*4)/(-0.25^0.5)";
     * double resultOne = JParser.parseEquation(equationOne);
     * //resultOne == -52
     * 
     * String equationTwo = "cos(2*x/5)*6*(7 - 10/2 - 9.5)";
     * JParser.Variable[] varsTwo = {new JParser.Variable("x", 450)};
     * double resultTwo = JParser.parseEquation(equationTwo, varsTwo);
     * //resultTwo == 45
     * 
     * String equationThree = "foo(3)!";
     * JParser.Function[] funcsThree = {new JParser.Function("foo") {
     *         public double applyFunction(double val) {
     *             return val + 1;
     *         }
     *     }};
     * double resultThree = JParser.parseEquation(equationThree, funcsThree);
     * //resultThree == 4! == 24
     */
    public static double parseEquation(String equation) {
        return parseEquation(equation, null, null);
    }
    
    public static double parseEquation(String equation, Variable[] vars) {
        return parseEquation(equation, vars, null);
    }
    
    public static double parseEquation(String equation, Function[] funcs) {
        return parseEquation(equation, null, funcs);
    }

    public static double parseEquation(String equation, Variable[] vars, Function[] funcs) {
        String original = equation;
        
        // Remove any spaces.
        for(int i = 0; i < equation.length(); i++) {
            if(equation.charAt(i) == ' ') {
                String leftSide = equation.substring(0, i);
                String rightSide = equation.substring(i + 1, equation.length());
                equation = leftSide + rightSide;
                i--;
            }
        }
        
        // Convert any "--"s to "+"s
        for(int i = 1; i < equation.length(); i++) {
            if(equation.charAt(i-1) == '-'
            && equation.charAt(i) == '-') {
                String leftSide = equation.substring(0, i-1);
                String rightSide = equation.substring(i + 1, equation.length());
                equation = leftSide + '+' + rightSide;
                i--;
            }
        }
    
        // Parse the equation.
        if(equation.length() == 0) {
            return 0;
        }
        try {
            return parseAdd(equation, vars, funcs);
        } catch(Exception e) {
            System.err.println("Exception in Parser.parseEquation(" + original + "). Check the equation for errors.");
            System.err.println("Exception message: " + e.getMessage());
            return 0;
        }
    }
    
    private static double parseAdd(String equation, Variable[] vars, Function[] funcs) throws Exception {
        // Try to pattern match it to:
        // subtract + add
        // If that fails, match it to:
        // subtract
        int plusIndex = getIndex(equation, '+');
        if(plusIndex != -1) {
            String leftSide = equation.substring(0, plusIndex);
            String rightSide = equation.substring(plusIndex + 1, equation.length());
            return parseSubtract(leftSide, vars, funcs) + parseAdd(rightSide, vars, funcs);
        } else {
            return parseSubtract(equation, vars, funcs);
        }
    }
    
    private static double parseSubtract(String equation, Variable[] vars, Function[] funcs) throws Exception {
        // Try to pattern match it to:
        // add - timesdivide
        // If that fails, match it to:
        // timesdivide
        int minusIndex = getReverseIndex(equation, '-');
        if(minusIndex != -1) {
            String leftSide = equation.substring(0, minusIndex);
            String rightSide = equation.substring(minusIndex + 1, equation.length());
            leftSide = leftSide.equals("") ? "0" : leftSide;
            return parseAdd(leftSide, vars, funcs) - parseTimes(rightSide, vars, funcs);
        } else {
            return parseTimes(equation, vars, funcs);
        }
    }
    
    private static double parseTimes(String equation, Variable[] vars, Function[] funcs) throws Exception {
        // Try to pattern match it to:
        // divide * times
        // If that fails, match it to:
        // divide
        int timesIndex = getIndex(equation, '*');
        if(timesIndex != -1) {
            String leftSide = equation.substring(0, timesIndex);
            String rightSide = equation.substring(timesIndex + 1, equation.length());
            return parseDivide(leftSide, vars, funcs) * parseTimes(rightSide, vars, funcs);
        } else {
            return parseDivide(equation, vars, funcs);
        }
    }
    
    private static double parseDivide(String equation, Variable[] vars, Function[] funcs) throws Exception {
        // Try to pattern match it to:
        // divide / power
        // If that fails, match it to:
        // power
        int divIndex = getReverseIndex(equation, '/');
        if(divIndex != -1) {
            String leftSide = equation.substring(0, divIndex);
            String rightSide = equation.substring(divIndex + 1, equation.length());
            double num = parseDivide(leftSide, vars, funcs);
            double denom = parsePower(rightSide, vars, funcs);
            if(denom == 0) throw new Exception("Division by zero error: \"" + num + "/0\"");
            return num / denom;
        } else {
            return parsePower(equation, vars, funcs);
        }
    }
    
    private static double parsePower(String equation, Variable[] vars, Function[] funcs) throws Exception {
        // Try to pattern match it to:
        // parentheses ^ parentheses
        // If that fails, match it to:
        // parentheses
        int powerIndex = getIndex(equation, '^');
        if(powerIndex != -1) {
            String leftSide = equation.substring(0, powerIndex);
            String rightSide = equation.substring(powerIndex + 1, equation.length());
            return Math.pow(parseParentheses(leftSide, vars, funcs), parseParentheses(rightSide, vars, funcs));
        } else {
            return parseParentheses(equation, vars, funcs);
        }
    }
    
    private static double parseParentheses(String equation, Variable[] vars, Function[] funcs) throws Exception {
        // Try to pattern match it to:
        // (add)
        // If that fails, match it to:
        // func
        if(equation.length() > 2 && equation.charAt(0) == '(' && equation.charAt(equation.length() - 1) == ')') {
            String middle = equation.substring(1, equation.length() - 1);
            return parseAdd(middle, vars, funcs);
        } else {
            return parseFunc(equation, vars, funcs);
        }
    }
    
    private static double parseFunc(String equation, Variable[] vars, Function[] funcs) throws Exception {
        // Try to pattern match it to:
        // sin(add)
        // If that fails, match it to:
        // cos(add)
        // If that fails, match it to:
        // tan(add)
        // If that fails, match it to:
        // sinr(add)
        // If that fails, match it to:
        // cosr(add)
        // If that fails, match it to:
        // tanr(add)
        // If that fails, match it to:
        // sqrt(add)
        // If that fails, match it to:
        // cbrt(add)
        // If that fails, match it to:
        // add!
        // If that fails, match it to:
        // <the custom functions>
        // If that fails, match it to:
        // num
        if(equation.length() > 5 && equation.substring(0, 4).equalsIgnoreCase("sin(") && equation.charAt(equation.length() - 1) == ')') {
            String middle = equation.substring(4, equation.length() - 1);
            return Math.sin(Math.toRadians(parseAdd(middle, vars, funcs)));
        } else if(equation.length() > 5 && equation.substring(0, 4).equalsIgnoreCase("cos(") && equation.charAt(equation.length() - 1) == ')') {
            String middle = equation.substring(4, equation.length() - 1);
            return Math.cos(Math.toRadians(parseAdd(middle, vars, funcs)));
        } else if(equation.length() > 5 && equation.substring(0, 4).equalsIgnoreCase("tan(") && equation.charAt(equation.length() - 1) == ')') {
            String middle = equation.substring(4, equation.length() - 1);
            return Math.tan(Math.toRadians(parseAdd(middle, vars, funcs)));
        } else if(equation.length() > 6 && equation.substring(0, 5).equalsIgnoreCase("sinr(") && equation.charAt(equation.length() - 1) == ')') {
            String middle = equation.substring(5, equation.length() - 1);
            return Math.sin(parseAdd(middle, vars, funcs));
        } else if(equation.length() > 6 && equation.substring(0, 5).equalsIgnoreCase("cosr(") && equation.charAt(equation.length() - 1) == ')') {
            String middle = equation.substring(5, equation.length() - 1);
            return Math.cos(parseAdd(middle, vars, funcs));
        } else if(equation.length() > 6 && equation.substring(0, 5).equalsIgnoreCase("tanr(") && equation.charAt(equation.length() - 1) == ')') {
            String middle = equation.substring(5, equation.length() - 1);
            return Math.tan(parseAdd(middle, vars, funcs));
        } else if(equation.length() > 6 && equation.substring(0, 5).equalsIgnoreCase("sqrt(") && equation.charAt(equation.length() - 1) == ')') {
            String middle = equation.substring(5, equation.length() - 1);
            return Math.sqrt(parseAdd(middle, vars, funcs));
        } else if(equation.length() > 6 && equation.substring(0, 5).equalsIgnoreCase("cbrt(") && equation.charAt(equation.length() - 1) == ')') {
            String middle = equation.substring(5, equation.length() - 1);
            return Math.pow(parseAdd(middle, vars, funcs), 1/3.);
        } else if(equation.length() > 1 && equation.charAt(equation.length() - 1) == '!') {
            // This is a simple implementation, and should only be used with integers.
            // Here the floor is used - i.e. 2.5! == 2! (which isn't true).
            // If a factorial of a negative number is attempted, an exception is thrown.
            int sum = 1;
            int lim = (int)parseAdd(equation.substring(0, equation.length() - 1), vars, funcs);
            if(lim < 0) {
                throw new Exception("Factorial of a negative number error: \"" + lim + "!\"");
            }
            for(int i = 2; i <= lim; i++) {
                sum *= i;
            }
            return sum;
        } else {
            if(funcs != null) {
                for(Function func : funcs) {
                    String funcName = func.getName();
                    if(funcName == null || funcName.length() == 0) continue;
                    if(equation.length() > 2 + funcName.length()
                    && equation.substring(0, funcName.length() + 1).equalsIgnoreCase(funcName + "(")
                    && equation.charAt(equation.length() - 1) == ')') {
                        String middle = equation.substring(funcName.length() + 1, equation.length() - 1);
                        return func.applyFunction(parseAdd(middle, vars, funcs));
                    }
                }
            }
            return parseNum(equation, vars, funcs);
        }
    }
    
    private static double parseNum(String equation, Variable[] vars, Function[] funcs) throws Exception {
        if(vars != null) {
            for(Variable var : vars) {
                if(equation.equalsIgnoreCase(var.getName())) return var.getValue();
            }
        }
        if(equation.equalsIgnoreCase("pi")) return Math.PI;
        if(equation.equalsIgnoreCase("e")) return Math.E;
        try {
            return Double.parseDouble(equation);
        } catch(Exception e) {
            String error = e.getMessage();
            error = error.substring("For input string: ".length(), error.length());
            throw new Exception("Check here: " + error);
        }
    }
    
    private static int getIndex(String equation, String str) {
        int strIndex;
        int fromPoint = 0;
        while(true) {
            strIndex = equation.indexOf(str, fromPoint);
            if(strIndex == -1) break;
            int leftOpenCount = 0;
            int leftCloseCount = 0;
            int rightOpenCount = 0;
            int rightCloseCount = 0;
            for(int i2 = 0; i2 < strIndex; i2++) {
                char ch = equation.charAt(i2);
                if(ch == '(') leftOpenCount++;
                else if(ch == ')') leftCloseCount++;
            }
            for(int i2 = strIndex + str.length(); i2 < equation.length(); i2++) {
                char ch = equation.charAt(i2);
                if(ch == '(') rightOpenCount++;
                else if(ch == ')') rightCloseCount++;
            }
            if(leftOpenCount == leftCloseCount
            && rightOpenCount == rightCloseCount) {
                break;
            } else {
                fromPoint = strIndex + 1;
            }
        }
        return strIndex;
    }
    
    private static int getIndex(String equation, char chr) {
        int chIndex;
        int fromPoint = 0;
        while(true) {
            chIndex = equation.indexOf(chr, fromPoint);
            if(chIndex == -1) break;
            int leftOpenCount = 0;
            int leftCloseCount = 0;
            int rightOpenCount = 0;
            int rightCloseCount = 0;
            for(int i2 = 0; i2 < chIndex; i2++) {
                char ch = equation.charAt(i2);
                if(ch == '(') leftOpenCount++;
                else if(ch == ')') leftCloseCount++;
            }
            for(int i2 = chIndex + 1; i2 < equation.length(); i2++) {
                char ch = equation.charAt(i2);
                if(ch == '(') rightOpenCount++;
                else if(ch == ')') rightCloseCount++;
            }
            if(leftOpenCount == leftCloseCount
            && rightOpenCount == rightCloseCount) {
                break;
            } else {
                fromPoint = chIndex + 1;
            }
        }
        return chIndex;
    }
    
    private static int getReverseIndex(String equation, char chr) {
        if(equation.length() == 0) return -1;
        for(int fromPoint = equation.length() - 1; fromPoint >=0; fromPoint--) {
            int chIndex = equation.indexOf(chr, fromPoint);
            if(chIndex == -1) continue;
            int leftOpenCount = 0;
            int leftCloseCount = 0;
            int rightOpenCount = 0;
            int rightCloseCount = 0;
            for(int i2 = 0; i2 < chIndex; i2++) {
                char ch = equation.charAt(i2);
                if(ch == '(') leftOpenCount++;
                else if(ch == ')') leftCloseCount++;
            }
            for(int i2 = chIndex + 1; i2 < equation.length(); i2++) {
                char ch = equation.charAt(i2);
                if(ch == '(') rightOpenCount++;
                else if(ch == ')') rightCloseCount++;
            }
            if(leftOpenCount == leftCloseCount
            && rightOpenCount == rightCloseCount) {
                return chIndex;
            }
        }
        return -1;
    }
    
    public static class Variable {
        private String name;
        private double value;
        
        public Variable(String name, double value) {
            this.name = name;
            this.value = value;
        }
        
        public String getName() {
            return name;
        }
        
        public double getValue() {
            return value;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public void setValue(double value) {
            this.value = value;
        }
    }
    
    public static abstract class Function {
        private String name;
        
        public Function(String name) {
            this.name = name;
        }
        
        public abstract double applyFunction(double functionInput);
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
}