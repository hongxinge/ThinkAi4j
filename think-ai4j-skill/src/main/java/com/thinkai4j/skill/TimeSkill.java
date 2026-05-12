package com.thinkai4j.skill;

import com.thinkai4j.tool.annotation.AiTool;
import com.thinkai4j.tool.annotation.ToolParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * 时间日期 Skill - 提供时间相关操作
 */
public class TimeSkill {

    @AiTool("获取当前日期和时间")
    public String getCurrentDateTime(
            @ToolParam(description = "时区（如 Asia/Shanghai），默认系统时区") String timezone) {
        LocalDateTime now = LocalDateTime.now();
        if (timezone != null && !timezone.isEmpty()) {
            TimeZone tz = TimeZone.getTimeZone(timezone);
            now = LocalDateTime.now(java.time.ZoneId.of(tz.getID()));
        }
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @AiTool("获取当前日期")
    public String getCurrentDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @AiTool("获取当前时间")
    public String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @AiTool("格式化时间戳")
    public String formatTimestamp(
            @ToolParam(description = "Unix时间戳（秒）") long timestamp,
            @ToolParam(description = "输出格式（如 yyyy-MM-dd HH:mm:ss）") String pattern) {
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestamp, 0, java.time.ZoneOffset.UTC);
        String fmt = pattern != null && !pattern.isEmpty() ? pattern : "yyyy-MM-dd HH:mm:ss";
        return dateTime.format(DateTimeFormatter.ofPattern(fmt));
    }
}
