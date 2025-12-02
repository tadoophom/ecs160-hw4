package com.ecs160.hw.util;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileReader {
    
    public static String readFile(Path path) throws IOException {
        return Files.readString(path);
    }
    
    public static List<FileContent> readFiles(Path base, List<String> files) {
        List<FileContent> results = new ArrayList<>();
        
        for (String f : files) {
            try {
                Path p = base.resolve(f);
                if (Files.exists(p)) {
                    results.add(new FileContent(f, readFile(p)));
                } else {
                    System.out.println("Missing: " + p);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return results;
    }
    
    public static class FileContent {
        public final String fileName;
        public final String content;
        
        public FileContent(String fileName, String content) {
            this.fileName = fileName;
            this.content = content;
        }
    }
}