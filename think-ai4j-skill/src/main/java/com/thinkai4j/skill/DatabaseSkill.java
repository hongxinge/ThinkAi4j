package com.thinkai4j.skill;

import com.thinkai4j.tool.annotation.AiTool;
import com.thinkai4j.tool.annotation.ToolParam;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class DatabaseSkill {

    private final Connection connection;
    private final boolean readOnly;
    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
            "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "CREATE",
            "TRUNCATE", "REPLACE", "GRANT", "REVOKE", "EXEC", "EXECUTE",
            "MERGE", "CALL", "RENAME"
    );
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(;\\s*(DROP|DELETE|UPDATE|INSERT|ALTER|CREATE|TRUNCATE|REPLACE|GRANT|REVOKE|EXEC|MERGE))" +
            "|(--.*)|(/\\*.*/)|((?:UNION)\\s+(?:SELECT))"
    );

    public DatabaseSkill(String jdbcUrl, String username, String password) throws SQLException {
        this.connection = DriverManager.getConnection(jdbcUrl, username, password);
        this.readOnly = true;
        try {
            connection.setReadOnly(true);
        } catch (SQLException ignored) {
        }
    }

    public DatabaseSkill(Connection connection) {
        this.connection = connection;
        this.readOnly = true;
        try {
            connection.setReadOnly(true);
        } catch (SQLException ignored) {
        }
    }

    public DatabaseSkill(Connection connection, boolean readOnly) {
        this.connection = connection;
        this.readOnly = readOnly;
        if (readOnly) {
            try {
                connection.setReadOnly(true);
            } catch (SQLException ignored) {
            }
        }
    }

    @AiTool("执行SQL查询（只读）")
    public String executeQuery(
            @ToolParam(description = "SQL查询语句") String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "SQL语句不能为空";
        }

        String validationError = validateSql(sql);
        if (validationError != null) {
            return validationError;
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            stmt.setMaxRows(100);

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            List<Map<String, Object>> results = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnLabel(i), rs.getObject(i));
                }
                results.add(row);
            }

            if (results.isEmpty()) {
                return "查询成功，返回0条记录";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("查询成功，返回 ").append(results.size()).append(" 条记录:\n");
            sb.append(results.subList(0, Math.min(10, results.size())));
            if (results.size() > 10) {
                sb.append("\n... 还有 ").append(results.size() - 10).append(" 条记录未显示");
            }
            return sb.toString();
        } catch (SQLException e) {
            return "SQL执行失败: " + e.getMessage();
        }
    }

    @AiTool("获取数据库表列表")
    public String listTables() {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});

            List<String> tableNames = new ArrayList<>();
            while (tables.next()) {
                tableNames.add(tables.getString("TABLE_NAME"));
            }
            return "数据库表: " + String.join(", ", tableNames);
        } catch (SQLException e) {
            return "获取表列表失败: " + e.getMessage();
        }
    }

    @AiTool("获取表结构信息")
    public String describeTable(
            @ToolParam(description = "表名") String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return "表名不能为空";
        }
        if (!isValidIdentifier(tableName)) {
            return "无效的表名: " + tableName;
        }
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, tableName, null);

            StringBuilder sb = new StringBuilder();
            sb.append("表 ").append(tableName).append(" 的结构:\n");
            while (columns.next()) {
                sb.append("- ").append(columns.getString("COLUMN_NAME"))
                        .append(" (").append(columns.getString("TYPE_NAME")).append(")");
                if (columns.getString("IS_NULLABLE").equals("NO")) {
                    sb.append(" NOT NULL");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (SQLException e) {
            return "获取表结构失败: " + e.getMessage();
        }
    }

    private String validateSql(String sql) {
        String upperSql = sql.toUpperCase().trim();

        if (!upperSql.startsWith("SELECT") && !upperSql.startsWith("SHOW") && !upperSql.startsWith("DESCRIBE") && !upperSql.startsWith("EXPLAIN")) {
            return "安全限制：仅允许SELECT/SHOW/DESCRIBE/EXPLAIN查询，不允许修改数据";
        }

        if (SQL_INJECTION_PATTERN.matcher(sql).find()) {
            return "安全限制：检测到潜在的SQL注入风险，查询被拒绝";
        }

        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                return "安全限制：SQL中包含不允许的关键词: " + keyword;
            }
        }

        return null;
    }

    private boolean isValidIdentifier(String identifier) {
        return identifier.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
