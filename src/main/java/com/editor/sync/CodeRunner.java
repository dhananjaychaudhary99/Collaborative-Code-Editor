package com.editor.sync;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public class CodeRunner {
    public static String execute(String code, String language) {
        if (code == null || code.trim().isEmpty()) {
            return "Output: [Empty Code]";
        }
        try {
            Path tempDir = Files.createTempDirectory("collab_run_");
            String lang = language.toLowerCase();

            if (lang.contains("python")) {
                Path pyFile = tempDir.resolve("script.py");
                Files.writeString(pyFile, code, StandardCharsets.UTF_8);
                return runProcess(new ProcessBuilder("python", pyFile.toString()), tempDir);
            } else if (lang.contains("java")) {
                Path javaFile = tempDir.resolve("Main.java");
                Files.writeString(javaFile, code, StandardCharsets.UTF_8);
                Process compile = new ProcessBuilder("javac", "Main.java").directory(tempDir.toFile()).start();
                if (!compile.waitFor(8, TimeUnit.SECONDS) || compile.exitValue() != 0) {
                    return "Compilation Error:\n" + new String(compile.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                }
                return runProcess(new ProcessBuilder("java", "Main"), tempDir);
            } else {
                return "Runner currently configured for: Java, Python.";
            }
        } catch (Exception e) {
            return "Execution failed: " + e.getMessage();
        }
    }

    private static String runProcess(ProcessBuilder pb, Path dir) throws Exception {
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        boolean finished = p.waitFor(6, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            return "Execution timed out (6s limit).";
        }
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return output.isEmpty() ? "Executed successfully with no output." : output;
    }
}