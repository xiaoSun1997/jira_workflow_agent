package com.xsun.jira.jira_workflor_agent.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @program: DateTimeTools.java
 * @description:
 * @author: sunmouren
 * @create: 2025-12-14
 **/
@Slf4j
@Component
public class DateTimeTools {

    @Tool("获取当前日期和星期信息")
    public String getCurrentDate() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        String result = String.format(
                "今天是 %s，星期%s",
                today.format(DateTimeFormatter.ISO_LOCAL_DATE),
                getDayOfWeekChinese(dayOfWeek)
        );

        log.info(result);
        return result;
    }

    @Tool("判断今天应该工作多少小时")
    public double getRequiredWorkHours() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        double hours;

        switch (dayOfWeek) {
            case MONDAY:
            case WEDNESDAY:
            case FRIDAY:
                hours = 8.0;
                break;
            case TUESDAY:
            case THURSDAY:
                hours = 10.0;
                break;
            default:
                hours = 0.0;
        }

        log.info("今天应该工作 {} 小时", hours);
        return hours;
    }

    private String getDayOfWeekChinese(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "一";
            case TUESDAY -> "二";
            case WEDNESDAY -> "三";
            case THURSDAY -> "四";
            case FRIDAY -> "五";
            case SATURDAY -> "六";
            case SUNDAY -> "日";
        };
    }
}
