package com.thinkai4j.test;

import com.thinkai4j.rag.Document;
import com.thinkai4j.rag.InMemoryDocumentStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryDocumentStoreTest {

    @Test
    void testAddAndSearch() {
        InMemoryDocumentStore store = new InMemoryDocumentStore();

        store.addDocuments(List.of(
                new Document("doc1", "Java is a programming language", "file1"),
                new Document("doc2", "Python is great for data science", "file2")
        ));

        List<Document> results = store.search("programming Java", 1);
        assertFalse(results.isEmpty());
        assertEquals("doc1", results.get(0).getId());
    }

    @Test
    void testCosineSimilarity() {
        InMemoryDocumentStore store = new InMemoryDocumentStore();

        store.addDocuments(List.of(
                new Document("a", "machine learning artificial intelligence", "file"),
                new Document("b", "cooking recipe food", "file")
        ));

        List<Document> results = store.search("AI machine learning", 1);
        assertFalse(results.isEmpty());
        assertEquals("a", results.get(0).getId());
    }

    @Test
    void testClear() {
        InMemoryDocumentStore store = new InMemoryDocumentStore();
        store.addDocuments(List.of(new Document("doc", "test content", "file")));
        store.clear();

        assertTrue(store.search("test", 1).isEmpty());
    }

    @Test
    void testTopKLimit() {
        InMemoryDocumentStore store = new InMemoryDocumentStore();
        store.addDocuments(List.of(
                new Document("d1", "hello world", "file"),
                new Document("d2", "hello foo", "file"),
                new Document("d3", "hello bar", "file")
        ));

        List<Document> results = store.search("hello", 2);
        assertEquals(2, results.size());
    }
}
