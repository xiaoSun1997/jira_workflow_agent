package com.xsun.jira.jira_workflor_agent;

import com.xsun.jira.jira_workflor_agent.service.WorklogService;
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
