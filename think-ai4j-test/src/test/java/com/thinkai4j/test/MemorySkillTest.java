package com.thinkai4j.test;

import com.thinkai4j.skill.MemorySkill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemorySkillTest {

    private MemorySkill memorySkill;

    @BeforeEach
    void setUp() {
        memorySkill = new MemorySkill();
    }

    @Test
    void testRemember() {
        String result = memorySkill.remember("username", "张三");
        assertNotNull(result);
        assertTrue(result.contains("username"));
        assertTrue(result.contains("张三"));
    }

    @Test
    void testRecallSpecificKey() {
        memorySkill.remember("username", "张三");
        String result = memorySkill.recall("username");
        assertNotNull(result);
        assertEquals("username = 张三", result);
    }

    @Test
    void testRecallNonExistentKey() {
        String result = memorySkill.recall("nonexistent");
        assertNotNull(result);
        assertTrue(result.contains("没有记住键"));
    }

    @Test
    void testRecallAllMemories() {
        memorySkill.remember("name", "张三");
        memorySkill.remember("age", "25");
        String result = memorySkill.recall(null);
        assertNotNull(result);
        assertTrue(result.contains("name"));
        assertTrue(result.contains("age"));
    }

    @Test
    void testRecallEmptyMemory() {
        String result = memorySkill.recall(null);
        assertNotNull(result);
        assertEquals("当前没有记住任何信息", result);
    }

    @Test
    void testRecallWithEmptyString() {
        String result = memorySkill.recall("");
        assertNotNull(result);
        assertEquals("当前没有记住任何信息", result);
    }

    @Test
    void testForget() {
        memorySkill.remember("username", "张三");
        String result = memorySkill.forget("username");
        assertNotNull(result);
        assertTrue(result.contains("已忘记"));
        
        String recallResult = memorySkill.recall("username");
        assertTrue(recallResult.contains("没有记住键"));
    }

    @Test
    void testForgetNonExistentKey() {
        String result = memorySkill.forget("nonexistent");
        assertNotNull(result);
        assertTrue(result.contains("没有找到键"));
    }

    @Test
    void testClearMemory() {
        memorySkill.remember("key1", "value1");
        memorySkill.remember("key2", "value2");
        String result = memorySkill.clearMemory();
        assertNotNull(result);
        assertTrue(result.contains("2"));
        
        String recallResult = memorySkill.recall(null);
        assertEquals("当前没有记住任何信息", recallResult);
    }

    @Test
    void testClearEmptyMemory() {
        String result = memorySkill.clearMemory();
        assertNotNull(result);
        assertTrue(result.contains("0"));
    }

    @Test
    void testMultipleMemories() {
        memorySkill.remember("name", "李四");
        memorySkill.remember("age", "30");
        memorySkill.remember("city", "北京");

        assertEquals("name = 李四", memorySkill.recall("name"));
        assertEquals("age = 30", memorySkill.recall("age"));
        assertEquals("city = 北京", memorySkill.recall("city"));
    }
}
