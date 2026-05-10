package com.thinkai4j.store.pgvector;

import com.thinkai4j.rag.Document;
import com.thinkai4j.rag.DocumentStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PgVectorStore implements DocumentStore {

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final int dimensions;

    public PgVectorStore(JdbcTemplate jdbcTemplate, String tableName, int dimensions) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        this.dimensions = dimensions;
        initializeTable();
    }

    private void initializeTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "content TEXT NOT NULL, " +
                "source VARCHAR(500), " +
                "metadata TEXT, " +
                "embedding vector(" + dimensions + ")" +
                ")");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS " + tableName + "_embedding_idx ON " +
                tableName + " USING ivfflat (embedding vector_cosine_ops)");
    }

    @Override
    public void addDocuments(List<Document> documents) {
        String sql = "INSERT INTO " + tableName + " (id, content, source, metadata, embedding) VALUES (?, ?, ?, ?, ?)";
        for (Document doc : documents) {
            if (doc.getId() == null) {
                doc.setId(UUID.randomUUID().toString());
            }
            jdbcTemplate.update(sql, doc.getId(), doc.getContent(), doc.getSource(), doc.getMetadata(), (Object) null);
        }
    }

    @Override
    public List<Document> search(String query, int topK) {
        return jdbcTemplate.query(
                "SELECT id, content, source, metadata FROM " + tableName +
                        " ORDER BY embedding <=> (SELECT embedding FROM " + tableName + " WHERE content = ? LIMIT 1) LIMIT ?",
                documentRowMapper(), query, topK);
    }

    @Override
    public void clear() {
        jdbcTemplate.execute("TRUNCATE TABLE " + tableName);
    }

    private RowMapper<Document> documentRowMapper() {
        return (rs, rowNum) -> {
            Document doc = new Document();
            doc.setId(rs.getString("id"));
            doc.setContent(rs.getString("content"));
            doc.setSource(rs.getString("source"));
            doc.setMetadata(rs.getString("metadata"));
            return doc;
        };
    }
}
