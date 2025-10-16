package com.piyush.mockarena.service;

import com.piyush.mockarena.entity.TestCase;
import com.piyush.mockarena.entity.Problem;
import com.piyush.mockarena.repository.ProblemRepository;
import com.piyush.mockarena.repository.LanguageRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CodeTemplateService {

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private LanguageRepository languageRepository;

    /**
     * 🎯 UNIVERSAL: Data type detection for any input/output combination
     */
    private enum DataType {
        BOOLEAN,         // true, false
        INTEGER,         // 123, -456
        LONG,           // 9876543210
        DOUBLE,         // 3.14, -2.5
        STRING,         // "hello", text without quotes
        ARRAY_INT,      // [1,2,3], [1, 2, 3]
        ARRAY_STRING,   // ["a","b"], ["hello", "world"]
        ARRAY_DOUBLE,   // [1.5, 2.7]
        MATRIX_INT,     // [[1,2],[3,4]]
        MATRIX_STRING,  // [["a","b"],["c","d"]]
        OBJECT,         // {key: value}
        NULL,           // null, empty
        UNKNOWN         // fallback
    }

    /**
     * 🎯 FUNCTION SIGNATURE: All possible function signatures
     */
    private enum FunctionSignature {
        SINGLE_PARAM,        // func(x)
        DOUBLE_PARAM,        // func(x, y)
        TRIPLE_PARAM,        // func(x, y, z)
        ARRAY_PARAM,         // func(arr)
        ARRAY_TARGET_PARAM,  // func(arr, target)
        STRING_PARAM,        // func(s)
        MATRIX_PARAM,        // func(matrix)
        MULTI_PARAM,         // func(a, b, c, d, ...)
        VARIABLE_PARAM       // dynamic detection
    }

    /**
     * 🚀 UNIVERSAL: Generate executable code for ANY problem type
     */
    public String generateExecutableCode(String userCode, Long problemId, Integer languageId, List<TestCase> testCases) {
        try {
            Problem problem = problemRepository.findById(problemId).orElse(null);
            String language = getLanguageFromId(languageId);

            if (problem == null) {
                throw new RuntimeException("Problem not found: " + problemId);
            }

            log.info("🔧 Generating executable code for problem: {} ({}) in {}", problem.getTitle(), problemId, language);

            switch (language.toLowerCase()) {
                case "java":
                    return generateJavaExecutable(userCode, problem, testCases);
                case "python":
                    return generatePythonExecutable(userCode, problem, testCases);
                case "cpp":
                    return generateCppExecutable(userCode, problem, testCases);
                case "javascript":
                    return generateJavaScriptExecutable(userCode, problem, testCases);
                default:
                    throw new RuntimeException("Language not supported: " + language);
            }
        } catch (Exception e) {
            log.error("❌ Error generating executable code: {}", e.getMessage());
            throw new RuntimeException("Failed to generate executable code: " + e.getMessage());
        }
    }

    /**
     * 🚀 COMPLETE: Universal Java executable generator
     */
    private String generateJavaExecutable(String userCode, Problem problem, List<TestCase> testCases) {
        StringBuilder sb = new StringBuilder();

        // Add comprehensive imports
        sb.append("import java.util.*;\n");
        sb.append("import java.util.stream.*;\n");
        sb.append("import java.io.*;\n");
        sb.append("import java.math.*;\n");
        sb.append("import java.time.*;\n\n");

        // Add user's Solution class
        sb.append(userCode).append("\n\n");

        // Add Main class with universal test runner
        sb.append("public class Main {\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        Solution solution = new Solution();\n");
        sb.append("        int passed = 0;\n");
        sb.append("        int total = ").append(testCases.size()).append(";\n\n");

        // Generate test cases using individual analysis
        for (int i = 0; i < testCases.size(); i++) {
            TestCase testCase = testCases.get(i);
            String input = testCase.getInput().trim();
            String expectedOutput = testCase.getExpectedOutput().trim();

            sb.append("        // Test Case ").append(i + 1).append("\n");
            sb.append("        try {\n");

            // Generate universal test case
            sb.append(generateUniversalTestCase(i, input, expectedOutput, problem));

            sb.append("        } catch (Exception e) {\n");
            sb.append("            System.out.println(\"Test Case ").append(i + 1).append(": ERROR - \" + e.getMessage());\n");
            sb.append("            e.printStackTrace();\n");
            sb.append("        }\n\n");
        }

        // Add summary
        sb.append("        System.out.println(\"\\n=== FINAL RESULTS ===\");\n");
        sb.append("        System.out.println(\"Passed: \" + passed + \"/\" + total + \" test cases\");\n");
        sb.append("        System.out.println(\"Success Rate: \" + (passed * 100.0 / total) + \"%\");\n");
        sb.append("        if (passed == total) {\n");
        sb.append("            System.out.println(\"🎉 SUCCESS: All test cases passed!\");\n");
        sb.append("        } else {\n");
        sb.append("            System.out.println(\"❌ FAILED: \" + (total - passed) + \" test cases failed\");\n");
        sb.append("        }\n");
        sb.append("    }\n");

        // Add comprehensive helper methods
        sb.append(generateComprehensiveHelpers());
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * 🎯 UNIVERSAL: Generate test case for any input/output combination
     */
    private String generateUniversalTestCase(int index, String input, String expectedOutput, Problem problem) {
        StringBuilder sb = new StringBuilder();

        // Detect input and output data types
        DataType inputType = detectDataType(input);
        DataType outputType = detectDataType(expectedOutput);
        FunctionSignature signature = detectFunctionSignature(input, problem);

        log.info("🔍 Test case {}: Input type: {}, Output type: {}, Signature: {}",
                index + 1, inputType, outputType, signature);

        // Generate input parsing
        sb.append(generateInputParsing(index, input, inputType, signature));

        // Generate function call
        sb.append(generateFunctionCall(index, problem, signature));

        // Generate output parsing and comparison
        sb.append(generateOutputComparison(index, expectedOutput, outputType));

        return sb.toString();
    }

    /**
     * 🎯 INPUT PARSING: Universal input parsing for any data type
     */
    private String generateInputParsing(int index, String input, DataType inputType, FunctionSignature signature) {
        StringBuilder sb = new StringBuilder();

        switch (signature) {
            case ARRAY_TARGET_PARAM:
                // Handle array + target (like two sum)
                sb.append("            int[] arr").append(index).append(" = parseIntArray(\"").append(escapeString(input)).append("\");\n");
                sb.append("            int target").append(index).append(" = parseTarget(\"").append(escapeString(input)).append("\");\n");
                break;

            case ARRAY_PARAM:
                // Single array parameter
                if (inputType == DataType.ARRAY_INT) {
                    sb.append("            int[] input").append(index).append(" = parseIntArray(\"").append(escapeString(input)).append("\");\n");
                } else if (inputType == DataType.ARRAY_STRING) {
                    sb.append("            String[] input").append(index).append(" = parseStringArray(\"").append(escapeString(input)).append("\");\n");
                } else if (inputType == DataType.ARRAY_DOUBLE) {
                    sb.append("            double[] input").append(index).append(" = parseDoubleArray(\"").append(escapeString(input)).append("\");\n");
                }
                break;

            case MATRIX_PARAM:
                // Matrix parameter
                sb.append("            int[][] input").append(index).append(" = parseMatrix(\"").append(escapeString(input)).append("\");\n");
                break;

            case DOUBLE_PARAM:
                // Two parameters
                String[] parts = splitInput(input);
                if (parts.length >= 2) {
                    sb.append("            ").append(getJavaType(detectDataType(parts[0]))).append(" param1_").append(index)
                            .append(" = ").append(parseValue(parts[0], detectDataType(parts[0]))).append(";\n");
                    sb.append("            ").append(getJavaType(detectDataType(parts[1]))).append(" param2_").append(index)
                            .append(" = ").append(parseValue(parts[1], detectDataType(parts[1]))).append(";\n");
                }
                break;

            case STRING_PARAM:
                sb.append("            String input").append(index).append(" = \"").append(escapeString(input)).append("\";\n");
                break;

            default:
                // Single parameter - auto-detect type
                if (inputType == DataType.INTEGER) {
                    sb.append("            int input").append(index).append(" = ").append(input).append(";\n");
                } else if (inputType == DataType.LONG) {
                    sb.append("            long input").append(index).append(" = ").append(input).append("L;\n");
                } else if (inputType == DataType.DOUBLE) {
                    sb.append("            double input").append(index).append(" = ").append(input).append(";\n");
                } else if (inputType == DataType.BOOLEAN) {
                    sb.append("            boolean input").append(index).append(" = ").append(input.toLowerCase()).append(";\n");
                } else {
                    sb.append("            String input").append(index).append(" = \"").append(escapeString(input)).append("\";\n");
                }
                break;
        }

        return sb.toString();
    }

    /**
     * 🎯 FUNCTION CALL: Universal function call generation
     */
    private String generateFunctionCall(int index, Problem problem, FunctionSignature signature) {
        StringBuilder sb = new StringBuilder();
        String functionName = problem.getFunctionName() != null ? problem.getFunctionName() : "solve";
        String returnType = problem.getReturnType() != null ? problem.getReturnType() : "Object";

        // Determine Java return type
        String javaReturnType = mapToJavaType(returnType);

        switch (signature) {
            case ARRAY_TARGET_PARAM:
                sb.append("            ").append(javaReturnType).append(" result").append(index)
                        .append(" = solution.").append(functionName).append("(arr").append(index)
                        .append(", target").append(index).append(");\n");
                break;

            case ARRAY_PARAM:
                sb.append("            ").append(javaReturnType).append(" result").append(index)
                        .append(" = solution.").append(functionName).append("(input").append(index).append(");\n");
                break;

            case MATRIX_PARAM:
                sb.append("            ").append(javaReturnType).append(" result").append(index)
                        .append(" = solution.").append(functionName).append("(input").append(index).append(");\n");
                break;

            case DOUBLE_PARAM:
                sb.append("            ").append(javaReturnType).append(" result").append(index)
                        .append(" = solution.").append(functionName).append("(param1_").append(index)
                        .append(", param2_").append(index).append(");\n");
                break;

            default:
                sb.append("            ").append(javaReturnType).append(" result").append(index)
                        .append(" = solution.").append(functionName).append("(input").append(index).append(");\n");
                break;
        }

        return sb.toString();
    }

    /**
     * 🎯 OUTPUT COMPARISON: Universal output comparison
     */
    private String generateOutputComparison(int index, String expectedOutput, DataType outputType) {
        StringBuilder sb = new StringBuilder();

        switch (outputType) {
            case BOOLEAN:
                sb.append("            boolean expected").append(index).append(" = ").append(expectedOutput.toLowerCase()).append(";\n");
                sb.append("            if (Objects.equals(result").append(index).append(", expected").append(index).append(")) {\n");
                break;

            case INTEGER:
                sb.append("            int expected").append(index).append(" = ").append(expectedOutput).append(";\n");
                sb.append("            if (Objects.equals(result").append(index).append(", expected").append(index).append(")) {\n");
                break;

            case LONG:
                sb.append("            long expected").append(index).append(" = ").append(expectedOutput).append("L;\n");
                sb.append("            if (Objects.equals(result").append(index).append(", expected").append(index).append(")) {\n");
                break;

            case DOUBLE:
                sb.append("            double expected").append(index).append(" = ").append(expectedOutput).append(";\n");
                sb.append("            if (Math.abs((Double)result").append(index).append(" - expected").append(index).append(") < 1e-9) {\n");
                break;

            case STRING:
                sb.append("            String expected").append(index).append(" = \"").append(escapeString(expectedOutput)).append("\";\n");
                sb.append("            if (Objects.equals(result").append(index).append(", expected").append(index).append(")) {\n");
                break;

            case ARRAY_INT:
                sb.append("            int[] expected").append(index).append(" = parseIntArray(\"").append(escapeString(expectedOutput)).append("\");\n");
                sb.append("            if (Arrays.equals((int[])result").append(index).append(", expected").append(index).append(")) {\n");
                break;

            case ARRAY_STRING:
                sb.append("            String[] expected").append(index).append(" = parseStringArray(\"").append(escapeString(expectedOutput)).append("\");\n");
                sb.append("            if (Arrays.equals((String[])result").append(index).append(", expected").append(index).append(")) {\n");
                break;

            case MATRIX_INT:
                sb.append("            int[][] expected").append(index).append(" = parseMatrix(\"").append(escapeString(expectedOutput)).append("\");\n");
                sb.append("            if (Arrays.deepEquals((int[][])result").append(index).append(", expected").append(index).append(")) {\n");
                break;

            default:
                sb.append("            String expected").append(index).append(" = \"").append(escapeString(expectedOutput)).append("\";\n");
                sb.append("            if (Objects.equals(String.valueOf(result").append(index).append("), expected").append(index).append(")) {\n");
                break;
        }

        // Add success/failure logic
        sb.append("                passed++;\n");
        sb.append("                System.out.println(\"✅ Test Case ").append(index + 1).append(": PASS\");\n");
        sb.append("            } else {\n");
        sb.append("                System.out.println(\"❌ Test Case ").append(index + 1).append(": FAIL\");\n");
        sb.append("                System.out.println(\"   Expected: \" + formatOutput(expected").append(index).append("));\n");
        sb.append("                System.out.println(\"   Got:      \" + formatOutput(result").append(index).append("));\n");
        sb.append("            }\n");

        return sb.toString();
    }

    /**
     * 🎯 DATA TYPE DETECTION: Universal data type detection
     */
    private DataType detectDataType(String value) {
        if (value == null || value.trim().isEmpty()) return DataType.NULL;

        value = value.trim();

        // Boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return DataType.BOOLEAN;
        }

        // Arrays
        if (value.startsWith("[") && value.endsWith("]")) {
            String content = value.substring(1, value.length() - 1).trim();
            if (content.isEmpty()) return DataType.ARRAY_INT;

            // Check for nested arrays (matrix)
            if (content.startsWith("[")) return DataType.MATRIX_INT;

            // Check if it contains quotes (string array)
            if (content.contains("\"")) return DataType.ARRAY_STRING;

            // Check if it contains decimals (double array)
            if (content.contains(".")) return DataType.ARRAY_DOUBLE;

            // Default to int array
            return DataType.ARRAY_INT;
        }

        // Objects
        if (value.startsWith("{") && value.endsWith("}")) {
            return DataType.OBJECT;
        }

        // Numbers
        try {
            if (value.contains(".")) {
                Double.parseDouble(value);
                return DataType.DOUBLE;
            } else {
                long num = Long.parseLong(value);
                if (num >= Integer.MIN_VALUE && num <= Integer.MAX_VALUE) {
                    return DataType.INTEGER;
                } else {
                    return DataType.LONG;
                }
            }
        } catch (NumberFormatException e) {
            // Not a number
        }

        // Default to string
        return DataType.STRING;
    }

    /**
     * 🎯 FUNCTION SIGNATURE DETECTION: Detect function signature from input
     */
    private FunctionSignature detectFunctionSignature(String input, Problem problem) {
        // Check for array + target pattern
        if (containsArrayAndTarget(input)) {
            return FunctionSignature.ARRAY_TARGET_PARAM;
        }

        // Check for matrix
        if (input.contains("[[")) {
            return FunctionSignature.MATRIX_PARAM;
        }

        // Check for array
        if (input.startsWith("[") && input.endsWith("]")) {
            return FunctionSignature.ARRAY_PARAM;
        }

        // Check for multiple parameters (comma-separated but not in array)
        if (input.contains(",") && !input.startsWith("[")) {
            String[] parts = input.split(",");
            if (parts.length == 2) return FunctionSignature.DOUBLE_PARAM;
            if (parts.length == 3) return FunctionSignature.TRIPLE_PARAM;
            return FunctionSignature.MULTI_PARAM;
        }

        // Check if it's a string
        if (detectDataType(input) == DataType.STRING) {
            return FunctionSignature.STRING_PARAM;
        }

        // Default to single parameter
        return FunctionSignature.SINGLE_PARAM;
    }

    /**
     * 🚀 COMPREHENSIVE HELPERS: All-in-one parsing methods
     */
    private String generateComprehensiveHelpers() {
        return """
        
        // ===================== UNIVERSAL PARSING HELPERS =====================
        
        private static int[] parseIntArray(String input) {
            if (input == null || input.trim().isEmpty()) return new int[0];
            
            // Handle array + target format
            String cleaned = input;
            if (cleaned.contains("target")) {
                cleaned = cleaned.substring(0, cleaned.indexOf("target")).trim();
            }
            
            // Extract array content
            int start = cleaned.indexOf('[');
            int end = cleaned.lastIndexOf(']');
            if (start == -1 || end == -1) return new int[0];
            
            String content = cleaned.substring(start + 1, end);
            if (content.trim().isEmpty()) return new int[0];
            
            return Arrays.stream(content.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .mapToInt(Integer::parseInt)
                .toArray();
        }
        
        private static String[] parseStringArray(String input) {
            if (input == null || input.trim().isEmpty()) return new String[0];
            
            int start = input.indexOf('[');
            int end = input.lastIndexOf(']');
            if (start == -1 || end == -1) return new String[0];
            
            String content = input.substring(start + 1, end);
            if (content.trim().isEmpty()) return new String[0];
            
            return Arrays.stream(content.split(","))
                .map(String::trim)
                .map(s -> s.replaceAll("^\\"|\\"$", ""))
                .toArray(String[]::new);
        }
        
        private static double[] parseDoubleArray(String input) {
            if (input == null || input.trim().isEmpty()) return new double[0];
            
            int start = input.indexOf('[');
            int end = input.lastIndexOf(']');
            if (start == -1 || end == -1) return new double[0];
            
            String content = input.substring(start + 1, end);
            if (content.trim().isEmpty()) return new double[0];
            
            return Arrays.stream(content.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .mapToDouble(Double::parseDouble)
                .toArray();
        }
        
        private static int[][] parseMatrix(String input) {
            if (input == null || input.trim().isEmpty()) return new int[0][0];
            
            // Remove outer brackets
            String content = input.trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                content = content.substring(1, content.length() - 1);
            }
            
            List<int[]> rows = new ArrayList<>();
            int level = 0;
            StringBuilder currentRow = new StringBuilder();
            
            for (char c : content.toCharArray()) {
                if (c == '[') {
                    level++;
                    currentRow.append(c);
                } else if (c == ']') {
                    level--;
                    currentRow.append(c);
                    if (level == 0) {
                        rows.add(parseIntArray(currentRow.toString()));
                        currentRow = new StringBuilder();
                    }
                } else if (c == ',' && level == 0) {
                    // Skip comma between rows
                } else {
                    currentRow.append(c);
                }
            }
            
            return rows.toArray(new int[0][]);
        }
        
        private static int parseTarget(String input) {
            if (input == null || !input.contains("target")) return 0;
            
            try {
                // Look for target = X pattern
                int targetPos = input.indexOf("target");
                String afterTarget = input.substring(targetPos + 6);
                
                StringBuilder number = new StringBuilder();
                boolean foundEquals = false;
                
                for (char c : afterTarget.toCharArray()) {
                    if (c == '=') {
                        foundEquals = true;
                    } else if (Character.isDigit(c) || c == '-') {
                        if (foundEquals || !foundEquals) number.append(c);
                    } else if (number.length() > 0 && foundEquals) {
                        break;
                    }
                }
                
                return number.length() > 0 ? Integer.parseInt(number.toString()) : 0;
            } catch (Exception e) {
                return 0;
            }
        }
        
        private static String formatOutput(Object obj) {
            if (obj == null) return "null";
            if (obj.getClass().isArray()) {
                if (obj instanceof int[]) return Arrays.toString((int[]) obj);
                if (obj instanceof String[]) return Arrays.toString((String[]) obj);
                if (obj instanceof double[]) return Arrays.toString((double[]) obj);
                if (obj instanceof int[][]) return Arrays.deepToString((int[][]) obj);
                return Arrays.toString((Object[]) obj);
            }
            return obj.toString();
        }
        """;
    }

    /**
     * 🎯 TEMPLATE GENERATION: Universal smart templates
     */
    public String getTemplate(Long problemId, Integer languageId) {
        try {
            Problem problem = problemRepository.findById(problemId).orElse(null);
            String language = getLanguageFromId(languageId);

            if (problem != null) {
                return generateSmartTemplate(problem, language);
            }
        } catch (Exception e) {
            log.error("Error getting template for problem {}: {}", problemId, e.getMessage());
        }
        return getDefaultTemplate(languageId);
    }

    /**
     * 🎯 SMART TEMPLATE: Generate template based on problem metadata
     */
    private String generateSmartTemplate(Problem problem, String language) {
        String functionName = problem.getFunctionName() != null ? problem.getFunctionName() : "solve";
        String returnType = problem.getReturnType() != null ? problem.getReturnType() : "int";

        switch (language.toLowerCase()) {
            case "java":
                return generateJavaSmartTemplate(functionName, returnType, problem);
            case "python":
                return generatePythonSmartTemplate(functionName, returnType, problem);
            case "cpp":
                return generateCppSmartTemplate(functionName, returnType, problem);
            case "javascript":
                return generateJavaScriptSmartTemplate(functionName, problem);
            default:
                return getDefaultTemplate(62);
        }
    }

    private String generateJavaSmartTemplate(String functionName, String returnType, Problem problem) {
        String javaReturnType = mapToJavaType(returnType);
        String paramType = "int";
        String paramName = "x";
        String defaultReturn = getDefaultReturn(javaReturnType);

        // Smart parameter detection based on function name
        if (functionName.toLowerCase().contains("twosum") || functionName.toLowerCase().contains("two_sum")) {
            paramType = "int[], int";
            paramName = "nums, target";
        } else if (javaReturnType.equals("int[]") || javaReturnType.equals("Integer[]")) {
            if (functionName.toLowerCase().contains("sort") || functionName.toLowerCase().contains("merge")) {
                paramType = "int[]";
                paramName = "nums";
            }
        } else if (javaReturnType.equals("String")) {
            paramType = "String";
            paramName = "s";
        }

        return String.format("""
            class Solution {
                public %s %s(%s %s) {
                    // Write your code here
                    return %s;
                }
            }
            """, javaReturnType, functionName, paramType, paramName, defaultReturn);
    }

    private String generatePythonSmartTemplate(String functionName, String returnType, Problem problem) {
        String pythonReturnType = mapToPythonType(returnType);
        String paramType = "int";
        String paramName = "x";
        String defaultReturn = getPythonDefaultReturn(pythonReturnType);

        return String.format("""
            class Solution:
                def %s(self, %s: %s) -> %s:
                    # Write your code here
                    return %s
            """, functionName, paramName, paramType, pythonReturnType, defaultReturn);
    }

    private String generateCppSmartTemplate(String functionName, String returnType, Problem problem) {
        return """
            class Solution {
            public:
                // Write your code here
            };
            """;
    }

    private String generateJavaScriptSmartTemplate(String functionName, Problem problem) {
        return String.format("""
            /**
             * @param {any} x
             * @return {any}
             */
            var %s = function(x) {
                // Write your code here
            };
            """, functionName);
    }

    // =============== UTILITY METHODS ===============

    private String[] splitInput(String input) {
        if (input.contains(",") && !input.startsWith("[")) {
            return input.split(",");
        }
        return new String[]{input};
    }

    private String parseValue(String value, DataType type) {
        value = value.trim();
        switch (type) {
            case INTEGER:
                return value;
            case LONG:
                return value + "L";
            case DOUBLE:
                return value;
            case BOOLEAN:
                return value.toLowerCase();
            case STRING:
                return "\"" + escapeString(value) + "\"";
            default:
                return "\"" + escapeString(value) + "\"";
        }
    }

    private String getJavaType(DataType type) {
        switch (type) {
            case INTEGER: return "int";
            case LONG: return "long";
            case DOUBLE: return "double";
            case BOOLEAN: return "boolean";
            case STRING: return "String";
            case ARRAY_INT: return "int[]";
            case ARRAY_STRING: return "String[]";
            case ARRAY_DOUBLE: return "double[]";
            default: return "String";
        }
    }

    private String mapToJavaType(String returnType) {
        if (returnType == null) return "int";

        switch (returnType.toLowerCase()) {
            case "boolean": case "bool": return "boolean";
            case "int": case "integer": return "int";
            case "long": return "long";
            case "double": case "float": return "double";
            case "string": return "String";
            case "int[]": case "array<int>": case "list<int>": return "int[]";
            case "string[]": case "array<string>": case "list<string>": return "String[]";
            case "double[]": case "array<double>": return "double[]";
            case "int[][]": case "matrix<int>": return "int[][]";
            default: return returnType;
        }
    }

    private String mapToPythonType(String returnType) {
        if (returnType == null) return "int";

        switch (returnType.toLowerCase()) {
            case "boolean": case "bool": return "bool";
            case "int": case "integer": return "int";
            case "long": return "int";
            case "double": case "float": return "float";
            case "string": return "str";
            case "int[]": case "array<int>": case "list<int>": return "List[int]";
            case "string[]": case "array<string>": case "list<string>": return "List[str]";
            case "double[]": case "array<double>": return "List[float]";
            case "int[][]": case "matrix<int>": return "List[List[int]]";
            default: return "Any";
        }
    }

    private String getDefaultReturn(String javaType) {
        switch (javaType) {
            case "boolean": return "false";
            case "int": case "long": return "0";
            case "double": case "float": return "0.0";
            case "String": return "\"\"";
            case "int[]": return "new int[]{}";
            case "String[]": return "new String[]{}";
            case "double[]": return "new double[]{}";
            case "int[][]": return "new int[][]{}";
            default: return "null";
        }
    }

    private String getPythonDefaultReturn(String pythonType) {
        switch (pythonType) {
            case "bool": return "False";
            case "int": case "float": return "0";
            case "str": return "\"\"";
            case "List[int]": case "List[str]": case "List[float]": return "[]";
            case "List[List[int]]": return "[[]]";
            default: return "None";
        }
    }

    private boolean containsArrayAndTarget(String input) {
        return input.contains("[") && input.contains("]") &&
                (input.toLowerCase().contains("target") || input.matches(".*\\]\\s*,?\\s*\\d+.*"));
    }

    // =============== EXISTING UTILITY METHODS ===============

    private String generatePythonExecutable(String userCode, Problem problem, List<TestCase> testCases) {
        // TODO: Implement Python executable generation
        return userCode;
    }

    private String generateCppExecutable(String userCode, Problem problem, List<TestCase> testCases) {
        // TODO: Implement C++ executable generation
        return userCode;
    }

    private String generateJavaScriptExecutable(String userCode, Problem problem, List<TestCase> testCases) {
        // TODO: Implement JavaScript executable generation
        return userCode;
    }

    public Map<String, String> generateAllTemplates() {
        Map<String, String> templates = new HashMap<>();
        templates.put("java", "class Solution {\n    // Write your code here\n}");
        templates.put("python", "class Solution:\n    # Write your code here\n    pass");
        templates.put("cpp", "class Solution {\npublic:\n    // Write your code here\n};");
        templates.put("javascript", "// Write your code here");
        return templates;
    }

    public String getDefaultTemplate(Integer languageId) {
        String language = getLanguageFromId(languageId);
        switch (language.toLowerCase()) {
            case "java": return "class Solution {\n    // Write your code here\n}";
            case "python": return "class Solution:\n    # Write your code here\n    pass";
            case "cpp": return "class Solution {\npublic:\n    // Write your code here\n};";
            case "javascript": return "// Write your code here";
            default: return "// Write your code here";
        }
    }

    public String getImports(String language) {
        switch (language.toLowerCase()) {
            case "java": return "import java.util.*;";
            case "python": return "from typing import *";
            case "cpp": return "#include <vector>\n#include <iostream>\nusing namespace std;";
            case "javascript": return "";
            default: return "";
        }
    }

    private String getLanguageFromId(Integer languageId) {
        switch (languageId) {
            case 62: case 71: return "java";
            case 70: case 72: return "python";
            case 54: return "cpp";
            case 63: return "javascript";
            case 50: return "c";
            default: return "java";
        }
    }

    private String escapeString(String input) {
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t").replace("\\", "\\\\");
    }

    public String getCodeTemplate(Long problemId, String language) {
        Integer languageId = switch (language.toLowerCase()) {
            case "java" -> 62;
            case "python" -> 70;
            case "cpp" -> 54;
            case "javascript" -> 63;
            case "c" -> 50;
            default -> 62;
        };
        return getTemplate(problemId, languageId);
    }
}
