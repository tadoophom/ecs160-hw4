package com.ecs160.hw;

import com.ecs160.hw.model.Issue;
import com.ecs160.hw.model.Repo;
import com.ecs160.hw.service.GitService;
import com.ecs160.hw.service.RedisRepoService;
import com.ecs160.hw.util.FileReader;
import com.ecs160.hw.util.JsonHandler;
import com.ecs160.hw.util.SelectedRepoFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class App {
    
    public static void main(String[] args) {
        RedisRepoService redisRepoService = new RedisRepoService();
        String repoId = "repo:Genymobile:scrcpy";
        Repo selected = redisRepoService.loadRepo(repoId);

        if (selected == null) {
            System.out.println("Repo not found: " + repoId);
            return;
        }

        List<String> filesToAnalyze = new ArrayList<>();
        filesToAnalyze.add("app/src/main.c");
        filesToAnalyze.add("app/src/scrcpy.c");
        filesToAnalyze.add("app/src/adb/adb.c");
        filesToAnalyze.add("app/src/server.c");

        SelectedRepoFile selectedRepoFile = new SelectedRepoFile(Path.of("selected_repo.dat"));
        selectedRepoFile.write(selected.repoId, filesToAnalyze);

        System.out.println("Selected repo: " + selected.repoId);
        System.out.println("Files:");
        filesToAnalyze.forEach(System.out::println);

        try {
            GitService gitService = new GitService();
            String repoUrl = gitService.getRepoUrl(selected.repoId);
            Path cloneDir = Paths.get("cloned_repos", selected.repoId.replace(":", "_"));

            if (!cloneDir.toFile().exists()) {
                gitService.cloneRepo(repoUrl, cloneDir);
            }

            String baseUrl = "http://localhost:30000";

            List<String> issueList1 = new ArrayList<>();
            if (selected.issues != null) {
                for (int i = 0; i < 10; i++) {
                    Issue issue = selected.issues.get(i);
                    String json = JsonHandler.issueToJson(issue);
                    
                    String encoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
                    URL url = new URL(baseUrl + "/summarize_issue?issue=" + encoded);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String resp = "";
                    String line;
                    while ((line = in.readLine()) != null) {
                        resp += line;
                    }
                    in.close();
                    
                    issueList1.add(resp);
                    System.out.println("summarized issue " + i);
                }
            }

            List<String> issueList2 = new ArrayList<>();
            List<FileReader.FileContent> files = FileReader.readFiles(cloneDir, filesToAnalyze);
            for (FileReader.FileContent file : files) {
                String encoded = URLEncoder.encode(file.content, StandardCharsets.UTF_8);
                URL url = new URL(baseUrl + "/find_bugs?code=" + encoded);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String resp = "";
                String line;
                while ((line = in.readLine()) != null) {
                    resp += line;
                }
                in.close();
                
                if (resp != null && !resp.equals("[]")) {
                    issueList2.add(resp);
                }
            }

            System.out.println("comparing issues...");
            List<String> matches = new ArrayList<>();

            for (int i = 0; i < issueList1.size(); i++) {
                for (int j = 0; j < issueList2.size(); j++) {
                    String enc1 = URLEncoder.encode(issueList1.get(i), StandardCharsets.UTF_8);
                    String enc2 = URLEncoder.encode(issueList2.get(j), StandardCharsets.UTF_8);
                    
                    URL url = new URL(baseUrl + "/compare_issues?issue1=" + enc1 + "&issue2=" + enc2);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String resp = "";
                    String line;
                    while ((line = in.readLine()) != null) {
                        resp += line;
                    }
                    in.close();
                    
                    if (resp.contains("\"is_same_issue\":true")) {
                        matches.add(resp);
                    }
                }
            }

            System.out.println("matches found: " + matches.size());
            for (String m : matches) {
                System.out.println(m);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}