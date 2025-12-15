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
