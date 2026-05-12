package com.thinkai4j.skill;

import com.thinkai4j.tool.annotation.AiTool;
import com.thinkai4j.tool.annotation.ToolParam;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库操作 Skill - 提供 SQL 查询能力
 * 注意：生产环境建议限制为只读查询，避免安全风险
 */
public class DatabaseSkill {

    private final Connection connection;

    public DatabaseSkill(String jdbcUrl, String username, String password) throws SQLException {
        this.connection = DriverManager.getConnection(jdbcUrl, username, password);
    }

    public DatabaseSkill(Connection connection) {
        this.connection = connection;
    }

    @AiTool("执行SQL查询（只读）")
    public String executeQuery(
            @ToolParam(description = "SQL查询语句") String sql) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

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

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
