package com.xsun.jira.jira_workflor_agent.tools;

import ai.djl.util.JsonUtils;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.input.WorklogInput;
import com.atlassian.jira.rest.client.api.domain.input.WorklogInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.google.gson.JsonNull;
import com.xsun.jira.jira_workflor_agent.model.dto.JiraIssue;
import com.xsun.jira.jira_workflor_agent.model.dto.WorklogEntry;
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
 * @create: 2025-12-14
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
