package com.thinkai4j.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 中文兼容分词器 - 支持英文单词、数字/编号和中文词汇
 * 对于中文，使用基于 N-gram 的简单分词策略（bigram）
 * 适用于无需外部依赖的轻量级中文分词场景
 */
public class ChineseTokenizer implements Tokenizer {

    private static final Pattern CHINESE_CHAR = Pattern.compile("[\\u4e00-\\u9fa5]");

    @Override
    public List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }

        // 缓存连续的英文/数字/编号字符（含 '.' 与 '#'）作为一个 token
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isWordChar(c)) {
                // 英文字母统一转小写，数字、'.'、'#' 原样保留
                word.append(Character.toLowerCase(c));
                continue;
            }

            // 遇到非单词字符：先输出已缓存内容，单字符也作为合法 token 保留
            flushWord(tokens, word);

            if (CHINESE_CHAR.matcher(String.valueOf(c)).matches()) {
                // 中文 bigram：当前汉字与后一个汉字组成一个 token，逐字滑动
                if (i + 1 < text.length()
                        && CHINESE_CHAR.matcher(String.valueOf(text.charAt(i + 1))).matches()) {
                    tokens.add(String.valueOf(c) + text.charAt(i + 1));
                }
            }
            // 其余字符（空格、其它标点等）忽略
        }
        // 处理末尾可能残留的单词/数字/编号
        flushWord(tokens, word);
        return tokens;
    }

    /**
     * 组成"英文单词/数字/编号"的字符：英文字母、数字、'.'、'#'。
     */
    private boolean isWordChar(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || Character.isDigit(c)
                || c == '.'
                || c == '#';
    }

    /**
     * 输出缓存的 token 并清空；单字符（含单字母英文词）也保留。
     */
    private void flushWord(List<String> tokens, StringBuilder word) {
        if (word.length() > 0) {
            tokens.add(word.toString());
            word.setLength(0);
        }
    }
}
