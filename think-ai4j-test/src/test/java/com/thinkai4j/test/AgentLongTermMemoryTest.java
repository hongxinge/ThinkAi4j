package com.thinkai4j.agent;

import com.thinkai4j.core.memory.ChatMemory;
import com.thinkai4j.memory.InMemoryChatMemory;
import com.thinkai4j.core.model.AiMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentLongTermMemoryTest {

    private AgentLongTermMemory memory;
    private ChatMemory shortTermMemory;
    private static final String AGENT_ID = "test-agent";

    @BeforeEach
    void setUp() {
        shortTermMemory = new InMemoryChatMemory();
        memory = new AgentLongTermMemory(AGENT_ID, shortTermMemory);
    }

    @Test
    void testAddShortTermMessage() {
        AiMessage message = AiMessage.user("Hello");
        memory.addShortTermMessage(message);

        List<AiMessage> history = memory.getShortTermHistory();
        assertEquals(1, history.size());
        assertEquals("Hello", history.get(0).getContent());
    }

    @Test
    void testGetShortTermHistoryEmpty() {
        List<AiMessage> history = memory.getShortTermHistory();
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    void testRememberFact() {
        memory.rememberFact("用户名是张三");
        List<String> facts = memory.getLongTermFacts();
        assertEquals(1, facts.size());
        assertEquals("用户名是张三", facts.get(0));
    }

    @Test
    void testRememberMultipleFacts() {
        memory.rememberFact("用户名是张三");
        memory.rememberFact("年龄是25岁");
        memory.rememberFact("城市是北京");

        List<String> facts = memory.getLongTermFacts();
        assertEquals(3, facts.size());
        assertTrue(facts.contains("用户名是张三"));
        assertTrue(facts.contains("年龄是25岁"));
        assertTrue(facts.contains("城市是北京"));
    }

    @Test
    void testRememberNullFact() {
        memory.rememberFact(null);
        assertTrue(memory.getLongTermFacts().isEmpty());
    }

    @Test
    void testRememberEmptyFact() {
        memory.rememberFact("");
        assertTrue(memory.getLongTermFacts().isEmpty());
    }

    @Test
    void testSearchFactsWithKeyword() {
        memory.rememberFact("用户名是张三");
        memory.rememberFact("年龄是25岁");
        memory.rememberFact("用户名是李四");

        List<String> results = memory.searchFacts("用户名");
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(f -> f.contains("用户名")));
    }

    @Test
    void testSearchFactsNoMatch() {
        memory.rememberFact("用户名是张三");
        List<String> results = memory.searchFacts("不存在的关键词");
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearchFactsWithNullKeyword() {
        memory.rememberFact("用户名是张三");
        memory.rememberFact("年龄是25岁");
        List<String> results = memory.searchFacts(null);
        assertEquals(2, results.size());
    }

    @Test
    void testSearchFactsWithEmptyKeyword() {
        memory.rememberFact("用户名是张三");
        List<String> results = memory.searchFacts("");
        assertEquals(1, results.size());
    }

    @Test
    void testClearLongTermMemory() {
        memory.rememberFact("事实1");
        memory.rememberFact("事实2");
        assertEquals(2, memory.getLongTermFacts().size());

        memory.clearLongTermMemory();
        assertTrue(memory.getLongTermFacts().isEmpty());
    }

    @Test
    void testClearShortTermMemory() {
        memory.addShortTermMessage(AiMessage.user("Hello"));
        memory.addShortTermMessage(AiMessage.assistant("Hi"));
        assertEquals(2, memory.getShortTermHistory().size());

        memory.clearShortTermMemory();
        assertTrue(memory.getShortTermHistory().isEmpty());
    }

    @Test
    void testGetSystemContextWithFacts() {
        memory.rememberFact("用户名是张三");
        memory.rememberFact("年龄是25岁");
        
        String context = memory.getSystemContext();
        assertNotNull(context);
        assertTrue(context.contains("长期记忆中的关键信息"));
        assertTrue(context.contains("用户名是张三"));
        assertTrue(context.contains("年龄是25岁"));
    }

    @Test
    void testGetSystemContextEmpty() {
        String context = memory.getSystemContext();
        assertEquals("", context);
    }

    @Test
    void testWithNullShortTermMemory() {
        AgentLongTermMemory memoryWithoutShortTerm = new AgentLongTermMemory("agent-2", null);
        
        memoryWithoutShortTerm.rememberFact("事实1");
        assertEquals(1, memoryWithoutShortTerm.getLongTermFacts().size());
        
        List<AiMessage> history = memoryWithoutShortTerm.getShortTermHistory();
        assertTrue(history.isEmpty());
        
        memoryWithoutShortTerm.clearShortTermMemory();
        memoryWithoutShortTerm.clearLongTermMemory();
        assertTrue(memoryWithoutShortTerm.getLongTermFacts().isEmpty());
    }
}
