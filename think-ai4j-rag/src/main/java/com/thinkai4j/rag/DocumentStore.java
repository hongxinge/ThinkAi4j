package com.thinkai4j.rag;

import java.util.List;

public interface DocumentStore {

    void addDocuments(List<Document> documents);

    List<Document> search(String query, int topK);

    void clear();
}
