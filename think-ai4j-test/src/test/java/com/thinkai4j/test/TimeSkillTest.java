package com.thinkai4j.test;

import com.thinkai4j.skill.TimeSkill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class TimeSkillTest {

    private TimeSkill timeSkill;

    @BeforeEach
    void setUp() {
        timeSkill = new TimeSkill();
    }

    @Test
    void testGetCurrentDateTime() {
        String result = timeSkill.getCurrentDateTime(null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void testGetCurrentDateTimeWithTimezone() {
        String result = timeSkill.getCurrentDateTime("Asia/Shanghai");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void testGetCurrentDateTimeWithUTCTimezone() {
        String result = timeSkill.getCurrentDateTime("UTC");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void testGetCurrentDate() {
        String result = timeSkill.getCurrentDate();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), result);
    }

    @Test
    void testGetCurrentTime() {
        String result = timeSkill.getCurrentTime();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.matches("\\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void testFormatTimestamp() {
        long timestamp = 1700000000L;
        String result = timeSkill.formatTimestamp(timestamp, null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testFormatTimestampWithCustomPattern() {
        long timestamp = 1700000000L;
        String result = timeSkill.formatTimestamp(timestamp, "yyyy/MM/dd");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("/"));
    }

    @Test
    void testFormatTimestampWithPattern() {
        long timestamp = 0L;
        String result = timeSkill.formatTimestamp(timestamp, "yyyy-MM-dd HH:mm:ss");
        assertEquals("1970-01-01 00:00:00", result);
    }
}
