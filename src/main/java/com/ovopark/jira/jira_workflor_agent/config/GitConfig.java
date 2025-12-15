package com.ovopark.jira.jira_workflor_agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: GitConfig.java
 * @description:
 * @author: sunmouren
 * @create: 2025-12-14
 **/
@Component
@ConfigurationProperties(prefix = "git")
@Data
public class GitConfig {
    private List<Repository> repositories = new ArrayList<>();

    @Data
    public static class Repository {
        private String url;
        private String token;
        private String branch;
    }
}
