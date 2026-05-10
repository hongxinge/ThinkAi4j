package com.thinkai4j.rag;

import com.thinkai4j.core.api.AiChat;

import java.util.List;
import java.util.stream.Collectors;

public class RagPipeline {

    private final AiChat chat;
    private final DocumentStore documentStore;

    public RagPipeline(AiChat chat, DocumentStore documentStore) {
        this.chat = chat;
        this.documentStore = documentStore;
    }

    public void ingest(List<Document> documents) {
        documentStore.addDocuments(documents);
    }

    public String query(String question) {
        List<Document> relevantDocs = documentStore.search(question, 3);

        String context = relevantDocs.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));

        String ragPrompt = "根据以下参考资料回答问题：\n\n参考资料：\n" + context + "\n\n问题：" + question;

        return chat.system("你是一个基于参考资料回答问题的助手。如果资料中没有相关信息，请明确说明。")
                   .ask(ragPrompt);
    }

    public DocumentStore getDocumentStore() {
        return documentStore;
    }
}
