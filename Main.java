import javax.tools.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static final Map<Integer, Character> numberToCharMap = new HashMap<>();

    public static void main(String[] args) {
        File javaFile = new File("DynamicCode.java");
        File classFile = new File("DynamicCode.class");
        try {
            // Load the dictionary from dict.txt
            String dictFileName = "dict.txt";
            loadDictionary(dictFileName);

            // Read the command file
            String commandFileName = "command.txt";
            String code = readCodeFromFile(commandFileName);

            // Convert numbers to Java code
            String javaCode = convertNumbersToJavaCode(code);

            // Save the Java code to a file named DynamicCode.java
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(javaFile))) {
                writer.write(javaCode);
            }

            // Make the file hidden on Windows
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Process process = new ProcessBuilder("cmd", "/c", "attrib", "+h", javaFile.getAbsolutePath()).start();
                process.waitFor();
            }

            // Compile the Java code
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            int result = compiler.run(null, null, null, javaFile.getPath());
            if (result != 0) {
                System.err.println("Compilation failed");
                return;
            }

            // Load and execute the compiled class
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File(".").toURI().toURL()});
            Class<?> cls = Class.forName("DynamicCode", true, classLoader);
            cls.getMethod("main", String[].class).invoke(null, (Object) new String[]{});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Delete generated files
            if (javaFile.exists() && !javaFile.delete()) {
                System.err.println("Failed to delete " + javaFile.getAbsolutePath());
            }
            if (classFile.exists() && !classFile.delete()) {
                System.err.println("Failed to delete " + classFile.getAbsolutePath());
            }
        }
    }

    private static void loadDictionary(String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    int number = Integer.parseInt(parts[0].trim());
                    char character = parts[1].charAt(0);
                    numberToCharMap.put(number, character);
                }
            }
        }
    }

    private static String readCodeFromFile(String fileName) throws IOException {
        return Files.readString(Path.of(fileName)).trim();
    }

    private static String convertNumbersToJavaCode(String code) {
        StringBuilder sb = new StringBuilder();
        String[] tokens = code.split("\\D+");
        for (String token : tokens) {
            if (!token.isEmpty()) {
                try {
                    int number = Integer.parseInt(token.trim());
                    Character c = numberToCharMap.get(number);
                    if (c != null) {
                        sb.append(c);
                    }
                } catch (NumberFormatException e) {
                    // Handle number format errors silently
                }
            }
        }
        // Ensure correct syntax in generated code with class name DynamicCode
        return "public class DynamicCode {\n" +
               "    public static void main(String[] args) {\n" +
               "        System.out.println(\"" + sb.toString().replace("\"", "\\\"") + "\");\n" +
               "    }\n" +
               "}";
    }
}
