package com.thinkai4j.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中文兼容分词器 - 支持英文单词和中文词汇
 * 对于中文，使用基于 N-gram 的简单分词策略（bigram）
 * 适用于无需外部依赖的轻量级中文分词场景
 */
public class ChineseTokenizer implements Tokenizer {

    private static final Pattern ENGLISH_WORD = Pattern.compile("[a-zA-Z]+");
    private static final Pattern CHINESE_CHAR = Pattern.compile("[\\u4e00-\\u9fa5]");

    @Override
    public List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }

        StringBuilder english = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                english.append(Character.toLowerCase(c));
            } else {
                if (english.length() > 0) {
                    if (english.length() > 1) {
                        tokens.add(english.toString());
                    }
                    english.setLength(0);
                }
                if (CHINESE_CHAR.matcher(String.valueOf(c)).matches()) {
                    if (i + 1 < text.length() && CHINESE_CHAR.matcher(String.valueOf(text.charAt(i + 1))).matches()) {
                        tokens.add(String.valueOf(c) + text.charAt(i + 1));
                    }
                } else if (Character.isDigit(c) || c == '.' || c == '#') {
                    if (english.length() > 0) {
                        english.append(c);
                    }
                }
            }
        }
        if (english.length() > 1) {
            tokens.add(english.toString());
        }
        return tokens;
    }
}
