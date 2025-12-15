package com.ovopark.jira.jira_workflor_agent.tools;

import com.ovopark.jira.jira_workflor_agent.config.GitConfig;
import com.ovopark.jira.jira_workflor_agent.model.dto.GitCommit;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: GitTools.java
 * @description:
 * @author: sunmouren
 * @create: 2025-12-14
 **/
@Slf4j
@Component
public class GitTools {

    @Value("${git.username}")
    private String gitUsername;

    @Autowired
    private GitConfig gitConfig;



    @Tool("从Git仓库获取指定用户今天的提交记录")
    public List<GitCommit> getTodayCommitsByUser() {
        log.info("正在获取用户 {} 今天的Git提交记录...", gitUsername);
        List<GitCommit> commits = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (GitConfig.Repository repo : gitConfig.getRepositories()) {
            try {
                File tempDir = File.createTempFile("git-", "-repo");
                tempDir.delete();
                tempDir.mkdirs();

                log.info("克隆仓库: {}", repo.getUrl());

                Git git = Git.cloneRepository()
                        .setURI(repo.getUrl())
                        .setDirectory(tempDir)
                        .setCredentialsProvider(
                                new UsernamePasswordCredentialsProvider(gitUsername, repo.getToken())
                        )
                        .setBranch(repo.getBranch())
                        .call();

                Iterable<RevCommit> logs = git.log().call();

                for (RevCommit commit : logs) {
                    PersonIdent author = commit.getAuthorIdent();
                    LocalDateTime commitTime = LocalDateTime.ofInstant(
                            author.getWhen().toInstant(),
                            ZoneId.systemDefault()
                    );

                    // 只获取今天的提交
                    if (commitTime.toLocalDate().equals(today) &&
                            author.getName().equals(gitUsername)) {

                        GitCommit gitCommit = new GitCommit();
                        gitCommit.setCommitId(commit.getName());
                        gitCommit.setAuthor(author.getName());
                        gitCommit.setMessage(commit.getFullMessage());
                        gitCommit.setCommitTime(commitTime);
                        gitCommit.setRepository(repo.getUrl());

                        commits.add(gitCommit);
                    }
                }

                git.close();
                deleteDirectory(tempDir);

            } catch (Exception e) {
                log.error("获取仓库 {} 的提交记录失败", repo.getUrl(), e);
            }
        }

        log.info("共找到 {} 条今天的提交记录", commits.size());
        return commits;
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
