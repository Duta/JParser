public class JParserTests {
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
        JParser.Variable[][] vars = {
            null,
            null,
            null,
            null,
            new JParser.Variable[]{new JParser.Variable("x", 450)},
            null
        };
        JParser.Function[][] funcs = {
            null,
            null,
            null,
            null,
            null,
            new JParser.Function[]{new JParser.Function("foo") {public double applyFunction(double val) {
                return val + 1;
            }}}
        };
        java.util.List<Integer> fails = new java.util.ArrayList<Integer>();
        
        for(int i = 0; i < tests.length; i++) {
            if(expectedResults[i].equals(
                expectedResults[i].isBool
                ? JParser.parseBooleanEquation(tests[i], vars[i], funcs[i])
                : JParser.parseEquation(tests[i], vars[i], funcs[i]))) {
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
}