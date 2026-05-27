package com.saurav.agentic.compiler;

import com.saurav.agentic.constants.FrameworkConstants;
import com.saurav.agentic.models.CompileResult;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * JavaCompilerUtil - Compiles generated Java files using javax.tools
 * Returns structured CompileResult with errors for Agent 4 to fix
 */
public class JavaCompilerUtil {

    // Classpath entries needed to compile generated files
    private static final String[] CLASSPATH_JARS = {
        "target/classes",
        "target/test-classes"
    };

    private JavaCompilerUtil() {}

    /**
     * Compile a single Java file and return structured result
     *
     * @param filePath - absolute or relative path to .java file
     * @return CompileResult with success/failure and error details
     */
    public static CompileResult compile(String filePath) {
        File file = new File(filePath);
        String className = file.getName().replace(".java", "");
        CompileResult result = new CompileResult(filePath, className);

        // Read source code for Agent 4 to use
        try {
            result.setSourceCode(Files.readString(file.toPath()));
        } catch (IOException e) {
            result.addError("Could not read source file: " + e.getMessage());
            return result;
        }

        // Get Java compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            result.addError("Java compiler not available — ensure JDK is installed, not just JRE");
            return result;
        }

        // Set up diagnostic collector to capture errors
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnostics, null, null)) {

            // Build classpath from Maven dependencies
            String classpath = buildClasspath();

            // Compile options
            List<String> options = Arrays.asList(
                "-classpath", classpath,
                "-sourcepath", "src/test/java",
                "-d", "target/test-classes"
            );

            // Get compilation units
            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjects(file);

            // Run compilation
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, diagnostics, options, null, compilationUnits
            );

            boolean compiled = task.call();
            result.setSuccess(compiled);

            // Collect errors if compilation failed
            if (!compiled) {
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                        String error = String.format(
                            "Line %d: %s",
                            diagnostic.getLineNumber(),
                            diagnostic.getMessage(null)
                        );
                        result.addError(error);
                    }
                }
            }

        } catch (IOException e) {
            result.addError("Compiler IO error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Compile all .java files in a directory
     */
    public static List<CompileResult> compileDirectory(String dirPath) {
        File dir = new File(dirPath);
        List<CompileResult> results = new java.util.ArrayList<>();

        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " Directory not found: " + dirPath);
            return results;
        }

        File[] javaFiles = dir.listFiles(
                (d, name) -> name.endsWith(".java")
        );

        if (javaFiles == null || javaFiles.length == 0) {
            System.out.println(FrameworkConstants.LOG_WARNING +
                    " No .java files found in: " + dirPath);
            return results;
        }

        for (File javaFile : javaFiles) {
            System.out.println(FrameworkConstants.LOG_INFO +
                    "   Compiling: " + javaFile.getName());
            CompileResult result = compile(javaFile.getPath());
            results.add(result);
            System.out.println(result.isSuccess()
                    ? FrameworkConstants.LOG_SUCCESS + "   " + result
                    : FrameworkConstants.LOG_ERROR + "   " + result);
        }

        return results;
    }

    /**
     * Builds classpath from Maven local repository and target folders
     */
    private static String buildClasspath() {
        StringBuilder cp = new StringBuilder();

        // Add target directories
        for (String entry : CLASSPATH_JARS) {
            cp.append(entry).append(File.pathSeparator);
        }

        // Add all JARs from Maven local repository
        String m2 = System.getProperty("user.home") + "/.m2/repository";
        addJarsFromDirectory(new File(m2), cp);

        return cp.toString();
    }

    /**
     * Recursively adds all .jar files from a directory to classpath
     */
    private static void addJarsFromDirectory(File dir, StringBuilder cp) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                addJarsFromDirectory(file, cp);
            } else if (file.getName().endsWith(".jar")) {
                cp.append(file.getAbsolutePath()).append(File.pathSeparator);
            }
        }
    }
}