package com.thinkai4j.test;

import com.thinkai4j.rag.ChineseTokenizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChineseTokenizerTest {

    @Test
    void testEnglishTokenization() {
        ChineseTokenizer tokenizer = new ChineseTokenizer();
        List<String> tokens = tokenizer.tokenize("hello world");

        assertTrue(tokens.contains("hello"));
        assertTrue(tokens.contains("world"));
    }

    @Test
    void testChineseTokenization() {
        ChineseTokenizer tokenizer = new ChineseTokenizer();
        List<String> tokens = tokenizer.tokenize("北京天气");

        assertTrue(tokens.contains("北京"));
        assertTrue(tokens.contains("天气"));
    }

    @Test
    void testMixedContent() {
        ChineseTokenizer tokenizer = new ChineseTokenizer();
        List<String> tokens = tokenizer.tokenize("Beijing天气很好");

        assertTrue(tokens.contains("beijing"));
        assertTrue(tokens.contains("天气"));
    }

    @Test
    void testEmptyString() {
        ChineseTokenizer tokenizer = new ChineseTokenizer();
        List<String> tokens = tokenizer.tokenize("");

        assertTrue(tokens.isEmpty());
    }

    @Test
    void testNullString() {
        ChineseTokenizer tokenizer = new ChineseTokenizer();
        List<String> tokens = tokenizer.tokenize(null);

        assertTrue(tokens.isEmpty());
    }
}
