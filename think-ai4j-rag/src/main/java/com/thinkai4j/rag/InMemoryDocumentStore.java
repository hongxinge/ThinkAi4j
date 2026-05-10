package com.thinkai4j.rag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryDocumentStore implements DocumentStore {

    private final List<Document> documents = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, double[]> vectorCache = new ConcurrentHashMap<>();
    private int vectorSize = 0;
    private final Set<String> vocabulary = ConcurrentHashMap.newKeySet();

    @Override
    public void addDocuments(List<Document> documents) {
        for (Document doc : documents) {
            if (doc.getId() == null) {
                doc.setId("doc_" + System.currentTimeMillis() + "_" + this.documents.size());
            }
            this.documents.add(doc);
        }
        rebuildVectors();
    }

    @Override
    public List<Document> search(String query, int topK) {
        if (documents.isEmpty()) {
            return Collections.emptyList();
        }

        double[] queryVector = buildVector(query);
        List<DocumentScore> scores = new ArrayList<>();

        for (Document doc : documents) {
            double[] docVector = vectorCache.get(doc.getId());
            if (docVector != null) {
                double similarity = cosineSimilarity(queryVector, docVector);
                scores.add(new DocumentScore(doc, similarity));
            }
        }

        return scores.stream()
                .sorted(Comparator.comparingDouble(DocumentScore::getScore).reversed())
                .limit(topK)
                .map(DocumentScore::getDocument)
                .collect(Collectors.toList());
    }

    @Override
    public void clear() {
        documents.clear();
        vectorCache.clear();
        vocabulary.clear();
        vectorSize = 0;
    }

    private void rebuildVectors() {
        Set<String> newVocab = new HashSet<>();
        for (Document doc : documents) {
            tokenize(doc.getContent()).forEach(newVocab::add);
        }

        if (!newVocab.equals(vocabulary)) {
            vocabulary.clear();
            vocabulary.addAll(newVocab);
            List<String> sortedVocab = new ArrayList<>(vocabulary);
            sortedVocab.sort(String::compareTo);
            vectorSize = sortedVocab.size();

            vectorCache.clear();
            Map<String, Integer> wordIndex = new HashMap<>();
            for (int i = 0; i < sortedVocab.size(); i++) {
                wordIndex.put(sortedVocab.get(i), i);
            }

            for (Document doc : documents) {
                vectorCache.put(doc.getId(), buildVector(doc.getContent(), wordIndex));
            }
        }
    }

    private double[] buildVector(String text) {
        if (vocabulary.isEmpty()) {
            return new double[0];
        }

        List<String> sortedVocab = new ArrayList<>(vocabulary);
        sortedVocab.sort(String::compareTo);
        Map<String, Integer> wordIndex = new HashMap<>();
        for (int i = 0; i < sortedVocab.size(); i++) {
            wordIndex.put(sortedVocab.get(i), i);
        }
        return buildVector(text, wordIndex);
    }

    private double[] buildVector(String text, Map<String, Integer> wordIndex) {
        double[] vector = new double[vectorSize];
        Map<String, Integer> termFreq = new HashMap<>();

        for (String word : tokenize(text)) {
            termFreq.merge(word, 1, Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            Integer index = wordIndex.get(entry.getKey());
            if (index != null) {
                vector[index] = entry.getValue();
            }
        }

        return normalize(vector);
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        String[] words = text.toLowerCase().split("\\s+|[^\\w]+");
        for (String word : words) {
            if (word.length() > 1) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    private double[] normalize(double[] vector) {
        double norm = 0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) return vector;

        double[] normalized = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        return normalized;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) return 0;
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    private static class DocumentScore {
        private final Document document;
        private final double score;

        DocumentScore(Document document, double score) {
            this.document = document;
            this.score = score;
        }

        public Document getDocument() {
            return document;
        }

        public double getScore() {
            return score;
        }
    }
}
