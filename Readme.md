# JIRA 工时自动填报 Agent

## 项目简介

这是一个基于 AI 的 JIRA 工时自动填报系统，能够根据用户的 Git 提交记录自动分析并填写 JIRA 工作日志。该系统通过集成 LangChain4j 框架，利用大语言模型的能力，智能地将 Git 提交内容与 JIRA 问题进行匹配，并自动生成符合要求的工作日志。

## 功能特性

- 自动获取用户当天的 Git 提交记录
- 查询用户在 JIRA 上未完成的任务
- 智能匹配 Git 提交与 JIRA 问题
- 自动计算所需填写的工时（周一/三/五：8小时，周二/四：10小时）
- 自动生成工作日志并提交至 JIRA 系统

## 技术架构

本项目采用以下技术栈：

- Java 21
- Spring Boot 3.2.3
- LangChain4j 0.27.1
- JIRA REST Java Client 7.0.1
- Eclipse JGit 6.8.0

## 项目结构

```
src/main/java/com/ovopark/jira/jira_workflor_agent/
├── JiraWorkflorAgentApplication.java  // 应用入口类
├── config/
│   ├── AgentConfig.java               // AI Agent 配置类
│   └── GitConfig.java                 // Git 配置类
├── controller/
│   └── WorklogController.java         // 工作日志控制器
├── model/dto/
│   ├── GitCommit.java                 // Git 提交数据传输对象
│   ├── JiraIssue.java                 // JIRA 问题数据传输对象
│   └── WorklogEntry.java              // 工作日志条目数据传输对象
├── service/
│   ├── WorklogAgent.java              // 工作日志 AI Agent 接口
│   └── WorklogService.java            // 工作日志服务类
└── tools/
    ├── DateTimeTools.java             // 日期时间工具类
    ├── GitTools.java                  // Git 操作工具类
    └── JiraTools.java                 // JIRA 操作工具类
```

## 核心代码展示

### 主应用类

类名：JiraWorkflorAgentApplication.java

```java
package com.ovopark.jira.jira_workflor_agent;

import com.ovopark.jira.jira_workflor_agent.service.WorklogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class JiraWorkflorAgentApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(JiraWorkflorAgentApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("工作日志Agent启动成功");
    }
}
```

### AI Agent 配置

类名：AgentConfig.java

```java
package com.ovopark.jira.jira_workflor_agent.config;

import com.ovopark.jira.jira_workflor_agent.service.WorklogAgent;
import com.ovopark.jira.jira_workflor_agent.tools.DateTimeTools;
import com.ovopark.jira.jira_workflor_agent.tools.GitTools;
import com.ovopark.jira.jira_workflor_agent.tools.JiraTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * @program: AgentConfig.java
 * @description:
 * @author: sunmouren
 * @create: 2025-12-15
 **/
@Configuration
@RequiredArgsConstructor
public class AgentConfig {

    @Value("${openai.api-key}")
    private String openaiApiKey;

    @Value("${openai.model}")
    private String openaiModel;

    @Value("${openai.base_url}")
    private String baseUrl;

    private final JiraTools jiraTools;
    private final GitTools gitTools;
    private final DateTimeTools dateTimeTools;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(openaiApiKey)
                .modelName(openaiModel)
                .baseUrl(baseUrl)
                .temperature(0.5)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public WorklogAgent worklogAgent(ChatLanguageModel chatLanguageModel) {
        return AiServices.builder(WorklogAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .tools(jiraTools, gitTools, dateTimeTools)
                .build();
    }
}
```

### 工作日志 AI Agent 接口

类名：WorklogAgent.java

```java
package com.ovopark.jira.jira_workflor_agent.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface WorklogAgent {

    @SystemMessage("""
        你是一个工作日志助手。你的任务是帮助用户自动记录JIRA工作时间。
        
        工作流程：
        1. 获取当前日期，判断今天是星期几
        2. 根据星期判断需要工作的时间（周一/三/五：8小时，周二/四：10小时）
        3. 获取用户今天已经记录的JIRA工作时间
        4. 如果工作时间不足，需要：
           - 获取用户今天的Git提交记录
           - 获取用户未完成的JIRA问题列表
           - 使用这些信息匹配并补充工作时间(git提交记录最匹配的jira问题，使用余弦相似度)
        5. 确认后记录工作时间
        
        注意：
        - 时间以0.5小时为最小单位
        - 最终的总工作时间必须等于要求的时间
        - 工作说明要基于Git提交的comment总结
        - 工作说明不需要明写是根据git总结的，只需要返回具体的总结信息即可
        - 如果不需要记录，只需要返回已经填满，不缺时间
        - 选择一个你觉得最好的方案进行填写jira，不必找我二次确认，直接填写
        """)
    String chat(@UserMessage String  message);
}
```

### Git 操作工具

类名：GitTools.java

```java
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
 * @create: 2025-12-15
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
```

### JIRA 操作工具

类名：JiraTools.java

```java
package com.ovopark.jira.jira_workflor_agent.tools;

import ai.djl.util.JsonUtils;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.input.WorklogInput;
import com.atlassian.jira.rest.client.api.domain.input.WorklogInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.google.gson.JsonNull;
import com.ovopark.jira.jira_workflor_agent.model.dto.JiraIssue;
import com.ovopark.jira.jira_workflor_agent.model.dto.WorklogEntry;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * @program: JiraTools.java
 * @description:
 * @author: sunmouren
 * @create: 2025-12-15
 **/
@Slf4j
@Component
public class JiraTools {

    @Value("${jira.url}")
    private String jiraUrl;

    @Value("${jira.username}")
    private String username;

    @Value("${jira.api-token}")
    private String apiToken;

    private JiraRestClient jiraClient;

    private JiraRestClient getClient() {
        if (jiraClient == null) {
            JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
            URI jiraServerUri = URI.create(jiraUrl);
            jiraClient = factory.createWithBasicHttpAuthentication(
                    jiraServerUri, username, apiToken
            );
        }
        return jiraClient;
    }

    @Tool("从JIRA获取我未完成的问题列表")
    public List<JiraIssue> getMyUnfinishedIssues() {
        log.info("正在获取未完成的JIRA问题...");
        List<JiraIssue> issues = new ArrayList<>();

        try {
            String jql = String.format(
                    "assignee = currentUser() AND resolution = Unresolved "
            );

            var searchResult = getClient()
                    .getSearchClient()
                    .searchJql(jql)
                    .claim();

            searchResult.getIssues().forEach(issue -> {
                JiraIssue jiraIssue = new JiraIssue();
                jiraIssue.setKey(issue.getKey());
                jiraIssue.setSummary(issue.getSummary());
                jiraIssue.setDescription(
                        issue.getDescription() != null ? issue.getDescription() : ""
                );
                jiraIssue.setStatus(issue.getStatus().getName());
                jiraIssue.setCreated(
                        LocalDateTime.ofInstant(
                                issue.getCreationDate().toDate().toInstant(),
                                ZoneId.systemDefault()
                        )
                );
                jiraIssue.setUpdated(
                        LocalDateTime.ofInstant(
                                issue.getUpdateDate().toDate().toInstant(),
                                ZoneId.systemDefault()
                        )
                );
                issues.add(jiraIssue);
            });

            log.info("找到 {} 个未完成的问题", issues.size());
        } catch (Exception e) {
            log.error("获取JIRA问题失败", e);
        }

        return issues;
    }

    @Tool("从JIRA获取我今天的工作记录")
    public List<WorklogEntry> getMyTodayWorklogs() {
        log.info("正在获取今天的工作记录...");
        List<WorklogEntry> worklogs = new ArrayList<>();

        try {
            LocalDate today = LocalDate.now();
            String jql = String.format(
                    "worklogAuthor = currentUser() AND worklogDate = '%s'",
                    today.toString()
            );

            SearchResult searchResult = getClient()
                    .getSearchClient()
                    .searchJql(jql)
                    .claim();
            log.info("找到 {} 个问题", searchResult.getTotal());
            searchResult.getIssues().forEach(issue -> {
                var issueWorklogs = getClient()
                        .getIssueClient()
                        .getIssue(issue.getKey())
                        .claim()
                        .getWorklogs();

                StreamSupport.stream(issueWorklogs.spliterator(), false)
                        .filter(wl -> {
                            LocalDate worklogDate = LocalDateTime.ofInstant(
                                    wl.getUpdateDate().toDate().toInstant(),
                                    ZoneId.systemDefault()
                            ).toLocalDate();
                            return worklogDate.equals(today);
                        })
                        .forEach(wl -> {
                            WorklogEntry entry = new WorklogEntry();
                            entry.setIssueKey(issue.getKey());
                            entry.setTimeSpentHours(wl.getMinutesSpent() / 60.0);
                            entry.setComment(wl.getComment());
                            entry.setStarted(
                                    LocalDateTime.ofInstant(
                                            wl.getStartDate().toDate().toInstant(),
                                            ZoneId.systemDefault()
                                    )
                            );
                            worklogs.add(entry);
                        });
            });

            log.info("找到 {} 条今天的工作记录", worklogs.size());
        } catch (Exception e) {
            log.error("获取工作记录失败", e);
        }

        return worklogs;
    }

    @Tool("记录JIRA工作时间，参数：issueKey-问题编号, timeSpentHours-工作时长(小时), comment-工作说明")
    public boolean logWork(String issueKey, double timeSpentHours, String comment) {
        log.info("正在记录工作时间: {} - {}小时 - {}",
                issueKey, timeSpentHours, comment);

        try {
            int minutes = (int) (timeSpentHours * 60);
            WorklogInputBuilder worklogInputBuilder = new WorklogInputBuilder(URI.create(jiraUrl + "/rest/api/2/issue/" + issueKey + "/worklog"));
            WorklogInput build = worklogInputBuilder.setMinutesSpent(minutes).setComment(comment).build();
            getClient()
                    .getIssueClient()
                    .addWorklog(
                            URI.create(jiraUrl + "/rest/api/2/issue/" + issueKey + "/worklog"),
                            build
                    )
                    .claim();

            log.info("工作时间记录成功");
            return true;
        } catch (Exception e) {
            log.error("记录工作时间失败", e);
            return false;
        }
    }
}
```

### 数据传输对象

类名：GitCommit.java

```java
package com.ovopark.jira.jira_workflor_agent.model.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * @program: GitCommit.java
 * @description:
 * @author: sunmouren
 * @create: 2025-12-15
 **/
@Getter
@Setter
@Accessors(chain=true)
public class GitCommit {
    private String commitId;
    private String author;
    private String message;
    private LocalDateTime commitTime;
    private String repository;
    private int filesChanged;
    private int additions;
    private int deletions;
}
```

类名：JiraIssue.java

```java
package com.ovopark.jira.jira_workflor_agent.model.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * @program: JiraIssue.java
 * @description:
 * @author: sunmouren
 * @create: 2025-12-15
 **/
@Getter
@Setter
@Accessors(chain=true)
public class JiraIssue {
    private String key;
    private String summary;
    private String description;
    private String status;
    private LocalDateTime created;
    private LocalDateTime updated;
}
```

类名：WorklogEntry.java

```java
package com.ovopark.jira.jira_workflor_agent.model.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * @program: WorklogEntry.java
 * @description:
 * @author: sunmouren
 * @create: 2025-12-15
 **/
@Getter
@Setter
@Accessors(chain=true)
public class WorklogEntry {
    private String issueKey;
    private double timeSpentHours;
    private String comment;
    private LocalDateTime started;
    /**
     * 相似度分数
     */
    private double similarityScore;
}
```

## 配置文件

配置文件路径：application.yml

```yaml
jira:
  url: http://jira***
  #  用户名
  username: xxx
  #  密码
  api-token: xxx

git:
  repositories:
    # 仓库地址
    - url: https://gitlab.ovopark.com/***
      #      密码
      token: xxx
      #      统计分支
      branch: xxx
  #      账号
  username: xxx

# 相关ai的key等
openai:
  api-key: sk-YOUR-API-KEY
  model: qwen-plus
  base_url: https://dashscope.aliyuncs.com/compatible-mode/v1

# 每天需要填写时长
work-hours:
  monday: 8.0
  tuesday: 10.0
  wednesday: 8.0
  thursday: 10.0
  friday: 8.0
  minimum-unit: 0.5  # 最小时间单位（小时）
```

## 使用方法

1. 配置 JIRA 和 Git 相关信息到 application.yml 文件中
2. 启动应用程序
3. 访问 `/api/worklog/chat?message=请帮我填写今日工时` 接口触发工时填报流程

## 工作原理

1. 当用户请求填写工时时，系统首先判断今天是星期几，确定应填写的工时数（周一/三/五为8小时，周二/四为10小时）
2. 检查用户今天已填写的工时总数
3. 如果还需要补填工时，则：
   - 获取用户今天的所有 Git 提交记录
   - 获取用户所有的未完成 JIRA 任务
   - 利用 AI 模型对 Git 提交内容和 JIRA 任务描述进行语义匹配
   - 自动分配剩余工时到最相关的 JIRA 任务上
   - 自动生成合适的工作说明并提交到 JIRA

## 依赖管理

类名：pom.xml（部分）

```xml
<!-- LangChain4j Core -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>${langchain4j.version}</version>
</dependency>

<!-- LangChain4j OpenAI -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>${langchain4j.version}</version>
</dependency>

<!-- JIRA REST Client -->
<dependency>
    <groupId>com.atlassian.jira</groupId>
    <artifactId>jira-rest-java-client-core</artifactId>
    <version>7.0.1</version>
</dependency>

<!-- JGit for Git operations -->
<dependency>
    <groupId>org.eclipse.jgit</groupId>
    <artifactId>org.eclipse.jgit</artifactId>
    <version>6.8.0.202311291450-r</version>
</dependency>
```
