package com.ecs160.hw.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class GitService {

    public boolean cloneRepo(String url, Path path) {
        try {
            File dir = path.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            ProcessBuilder pb = new ProcessBuilder("git", "clone", "--depth", "1", url, path.toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            return p.waitFor() == 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getRepoUrl(String id) {
        String[] parts = id.split(":");
        if (parts.length >= 3) {
            return "https://github.com/" + parts[1] + "/" + parts[2] + ".git";
        }
        throw new IllegalArgumentException("Invalid id: " + id);
    }
}