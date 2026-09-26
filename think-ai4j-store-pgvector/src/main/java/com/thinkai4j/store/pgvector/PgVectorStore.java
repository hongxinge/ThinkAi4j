package com.thinkai4j.store.pgvector;

import com.thinkai4j.core.api.EmbeddingProvider;
import com.thinkai4j.core.exception.AiException;
import com.thinkai4j.core.model.EmbeddingRequest;
import com.thinkai4j.core.model.EmbeddingResponse;
import com.thinkai4j.rag.Document;
import com.thinkai4j.rag.DocumentStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 基于 PostgreSQL pgvector 扩展的文档向量存储。
 *
 * <p>写入时若文档未携带向量，会通过 {@link EmbeddingProvider} 自动生成；
 * 语义检索时同样通过 {@link EmbeddingProvider} 将查询文本向量化，
 * 再使用 pgvector 的余弦距离操作符（&lt;=&gt;）完成相似度排序。</p>
 */
public class PgVectorStore implements DocumentStore {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final int dimensions;
    private final EmbeddingProvider embeddingProvider;

    public PgVectorStore(JdbcTemplate jdbcTemplate, String tableName, int dimensions) {
        this(jdbcTemplate, tableName, dimensions, null);
    }

    public PgVectorStore(JdbcTemplate jdbcTemplate, String tableName, int dimensions,
                         EmbeddingProvider embeddingProvider) {
        if (!IDENTIFIER_PATTERN.matcher(tableName).matches()) {
            throw new AiException("pgvector", "INVALID_TABLE_NAME",
                    "Invalid table name: " + tableName + " (only letters, digits and underscores allowed)");
        }
        if (dimensions <= 0) {
            throw new AiException("pgvector", "INVALID_DIMENSIONS",
                    "Dimensions must be positive: " + dimensions);
        }
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        this.dimensions = dimensions;
        this.embeddingProvider = embeddingProvider;
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
        if (documents == null || documents.isEmpty()) {
            return;
        }
        fillMissingEmbeddings(documents);

        String sql = "INSERT INTO " + tableName +
                " (id, content, source, metadata, embedding) VALUES (?, ?, ?, ?, ?::vector)";
        for (Document doc : documents) {
            jdbcTemplate.update(sql, doc.getId(), doc.getContent(), doc.getSource(),
                    doc.getMetadata(), toVectorLiteral(doc.getEmbedding()));
        }
    }

    @Override
    public List<Document> search(String query, int topK) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>();
        }
        if (topK <= 0) {
            return new ArrayList<>();
        }
        if (embeddingProvider == null) {
            throw new AiException("pgvector", "EMBEDDING_REQUIRED",
                    "Semantic search requires an EmbeddingProvider to vectorize the query text");
        }

        List<Double> queryVector = embedInputs(List.of(query)).get(0);
        String queryVectorLiteral = toVectorLiteral(queryVector);

        return jdbcTemplate.query(
                "SELECT id, content, source, metadata FROM " + tableName +
                        " ORDER BY embedding <=> ?::vector LIMIT ?",
                documentRowMapper(), queryVectorLiteral, topK);
    }

    @Override
    public void clear() {
        jdbcTemplate.execute("TRUNCATE TABLE " + tableName);
    }

    /**
     * 为未携带向量的文档批量生成 embedding，并校验向量维度与表定义一致。
     */
    private void fillMissingEmbeddings(List<Document> documents) {
        List<Document> pending = new ArrayList<>();
        for (Document doc : documents) {
            if (doc.getId() == null) {
                doc.setId(UUID.randomUUID().toString());
            }
            if (doc.getEmbedding() == null || doc.getEmbedding().isEmpty()) {
                pending.add(doc);
            } else {
                validateDimensions(doc.getEmbedding());
            }
        }

        if (pending.isEmpty()) {
            return;
        }
        if (embeddingProvider == null) {
            throw new AiException("pgvector", "EMBEDDING_REQUIRED",
                    "Documents without embedding require an EmbeddingProvider to generate vectors");
        }

        List<String> inputs = pending.stream().map(Document::getContent).toList();
        List<List<Double>> embeddings = embedInputs(inputs);
        for (int i = 0; i < pending.size(); i++) {
            pending.get(i).setEmbedding(embeddings.get(i));
        }
    }

    private List<List<Double>> embedInputs(List<String> inputs) {
        EmbeddingResponse response;
        try {
            response = embeddingProvider.embed(new EmbeddingRequest(inputs));
        } catch (Exception e) {
            throw new AiException("pgvector", "EMBEDDING_FAILED",
                    "EmbeddingProvider failed to embed " + inputs.size() + " input(s)", e);
        }
        if (response == null || response.getData() == null
                || response.getData().size() != inputs.size()) {
            throw new AiException("pgvector", "EMBEDDING_FAILED",
                    "EmbeddingProvider returned invalid response for " + inputs.size() + " input(s)");
        }

        List<List<Double>> result = new ArrayList<>(inputs.size());
        for (EmbeddingResponse.EmbeddingData data : response.getData()) {
            if (data == null || data.getEmbedding() == null || data.getEmbedding().isEmpty()) {
                throw new AiException("pgvector", "EMBEDDING_FAILED",
                        "EmbeddingProvider returned empty embedding vector");
            }
            validateDimensions(data.getEmbedding());
            result.add(data.getEmbedding());
        }
        return result;
    }

    private void validateDimensions(List<Double> embedding) {
        if (embedding.size() != dimensions) {
            throw new AiException("pgvector", "DIMENSION_MISMATCH",
                    "Embedding dimension mismatch: expected " + dimensions + " but got " + embedding.size());
        }
    }

    private String toVectorLiteral(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            throw new AiException("pgvector", "EMPTY_EMBEDDING",
                    "Cannot insert document with empty embedding");
        }
        StringBuilder sb = new StringBuilder(embedding.size() * 10);
        sb.append('[');
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding.get(i));
        }
        sb.append(']');
        return sb.toString();
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
