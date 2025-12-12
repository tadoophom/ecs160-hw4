package com.ecs160.hw;

import com.ecs160.hw.model.Issue;
import com.ecs160.hw.model.Repo;
import com.ecs160.hw.service.GitService;
import com.ecs160.hw.service.MicroserviceCaller;
import com.ecs160.hw.service.RedisRepoService;
import com.ecs160.hw.util.FileReader;
import com.ecs160.hw.util.JsonHandler;
import com.ecs160.hw.util.SelectedRepoFile;

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

            MicroserviceCaller client = new MicroserviceCaller("http://localhost:30000");
            List<String> issueList1 = new ArrayList<>();

            if (selected.issues != null) {
                for (int i = 0; i < 10; i++) {
                    Issue issue = selected.issues.get(i);
                    String json = JsonHandler.issueToJson(issue);
                    String summary = client.post("/summarize_issue", json);
                    issueList1.add(summary);
                }
            }

            List<String> issueList2 = new ArrayList<>();
            List<FileReader.FileContent> files = FileReader.readFiles(cloneDir, filesToAnalyze);

            for (FileReader.FileContent file : files) {
                String bugs = client.post("/find_bugs", file.content);
                if (bugs != null && !bugs.isEmpty()) {
                    issueList2.add(bugs);
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{\"list1\":[");
            for (int i = 0; i < issueList1.size(); i++) {
                sb.append(issueList1.get(i));
                if (i < issueList1.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("],\"list2\":[");
            for (int i = 0; i < issueList2.size(); i++) {
                String b = issueList2.get(i).trim();
                if (b.startsWith("[")) {
                    b = b.substring(1);
                }
                if (b.endsWith("]")) {
                    b = b.substring(0, b.length() - 1);
                }
                sb.append(b);
                if (i < issueList2.size() - 1 && !b.isEmpty()) {
                    sb.append(",");
                }
            }
            sb.append("]}");

            String result = client.post("/check_equivalence", sb.toString());
            System.out.println(result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
