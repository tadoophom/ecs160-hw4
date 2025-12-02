package com.ecs160.hw.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SelectedRepoFile {
    private final Path filePath;

    public SelectedRepoFile(Path filePath) {
        this.filePath = filePath;
    }

    public void write(String repoId, List<String> filePaths) {
        if (repoId == null || repoId.isEmpty()) {
            throw new IllegalArgumentException("repoId required");
        }
        List<String> lines = new ArrayList<>();
        lines.add(repoId);
        if (filePaths != null) {
            lines.addAll(filePaths);
        }
        try {
            Files.write(filePath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write " + filePath, e);
        }
    }

    public Selection read() {
        if (!Files.exists(filePath)) {
            throw new IllegalStateException("Selection file missing: " + filePath);
        }
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                throw new IllegalStateException("Empty selection file: " + filePath);
            }
            String repoId = lines.get(0);
            List<String> files = new ArrayList<>();
            if (lines.size() > 1) {
                files.addAll(lines.subList(1, lines.size()));
            }
            return new Selection(repoId, files);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read " + filePath, e);
        }
    }

    public record Selection(String repoId, List<String> files) {}
}
