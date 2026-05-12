package com.thinkai4j.rag;

import java.util.List;

/**
 * 分词器接口 - 用于 RAG 文档向量化时的文本分词
 * 默认实现支持英文和中文基础分词
 */
public interface Tokenizer {
    List<String> tokenize(String text);
}
