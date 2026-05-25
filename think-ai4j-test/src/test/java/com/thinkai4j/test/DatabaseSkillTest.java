package com.thinkai4j.test;

import com.thinkai4j.skill.DatabaseSkill;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseSkillTest {

    private static Connection sharedConnection;
    private static DatabaseSkill staticDatabaseSkill;
    private DatabaseSkill databaseSkill;

    @BeforeAll
    static void setUpClass() throws SQLException {
        sharedConnection = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        
        try (Statement stmt = sharedConnection.createStatement()) {
            stmt.execute("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100), age INT, city VARCHAR(100))");
            stmt.execute("INSERT INTO users VALUES (1, '张三', 25, '北京')");
            stmt.execute("INSERT INTO users VALUES (2, '李四', 30, '上海')");
            stmt.execute("INSERT INTO users VALUES (3, '王五', 28, '广州')");
        }
        
        staticDatabaseSkill = new DatabaseSkill(sharedConnection);
    }

    @BeforeEach
    void setUp() {
        databaseSkill = staticDatabaseSkill;
    }

    @AfterAll
    static void tearDownClass() throws SQLException {
        if (staticDatabaseSkill != null) {
            staticDatabaseSkill.close();
        }
    }

    @Test
    void testExecuteQuery() {
        String result = databaseSkill.executeQuery("SELECT * FROM users");
        assertNotNull(result);
        assertTrue(result.contains("查询成功"));
        assertTrue(result.contains("3 条记录"));
        assertTrue(result.contains("张三"));
        assertTrue(result.contains("李四"));
        assertTrue(result.contains("王五"));
    }

    @Test
    void testExecuteQueryWithWhereClause() {
        String result = databaseSkill.executeQuery("SELECT * FROM users WHERE city = '北京'");
        assertNotNull(result);
        assertTrue(result.contains("1 条记录"));
        assertTrue(result.contains("张三"));
    }

    @Test
    void testExecuteQueryNoResults() {
        String result = databaseSkill.executeQuery("SELECT * FROM users WHERE age > 100");
        assertNotNull(result);
        assertEquals("查询成功，返回0条记录", result);
    }

    @Test
    void testExecuteQueryInvalidSQL() {
        String result = databaseSkill.executeQuery("SELECT * FROM nonexistent_table");
        assertNotNull(result);
        assertTrue(result.contains("SQL执行失败"));
    }

    @Test
    void testListTables() {
        String result = databaseSkill.listTables();
        assertNotNull(result);
        assertTrue(result.toUpperCase().contains("USERS"));
    }

    @Test
    void testDescribeTable() {
        String result = databaseSkill.describeTable("USERS");
        assertNotNull(result);
        assertTrue(result.contains("结构"), "Expected structure info but got: " + result);
        assertTrue(result.toUpperCase().contains("ID") || result.toLowerCase().contains("id"), "Expected column names but got: " + result);
    }

    @Test
    void testDescribeNonExistentTable() {
        String result = databaseSkill.describeTable("nonexistent_table");
        assertNotNull(result);
        assertFalse(result.contains("NOT NULL"));
    }

    @Test
    void testConstructorWithConnection() {
        assertNotNull(databaseSkill);
    }

    @Test
    void testMultipleQueries() {
        String result1 = databaseSkill.executeQuery("SELECT name FROM users WHERE id = 1");
        assertTrue(result1.contains("张三"));

        String result2 = databaseSkill.executeQuery("SELECT city FROM users WHERE id = 2");
        assertTrue(result2.contains("上海"));
    }

    @Test
    void testQueryWithOrderBy() {
        String result = databaseSkill.executeQuery("SELECT * FROM users ORDER BY age DESC");
        assertNotNull(result);
        assertTrue(result.contains("3 条记录"));
    }

    @Test
    void testQueryWithCount() {
        String result = databaseSkill.executeQuery("SELECT COUNT(*) FROM users");
        assertNotNull(result);
        assertTrue(result.contains("1 条记录"));
    }

    @Test
    void testSqlInjectionDropTable() {
        String result = databaseSkill.executeQuery("DROP TABLE users");
        assertNotNull(result);
        assertTrue(result.contains("安全限制"), "Should block DROP TABLE, got: " + result);
    }

    @Test
    void testSqlInjectionDelete() {
        String result = databaseSkill.executeQuery("DELETE FROM users WHERE 1=1");
        assertNotNull(result);
        assertTrue(result.contains("安全限制"), "Should block DELETE, got: " + result);
    }

    @Test
    void testSqlInjectionUpdate() {
        String result = databaseSkill.executeQuery("UPDATE users SET name='hacked'");
        assertNotNull(result);
        assertTrue(result.contains("安全限制"), "Should block UPDATE, got: " + result);
    }

    @Test
    void testSqlInjectionInsert() {
        String result = databaseSkill.executeQuery("INSERT INTO users VALUES (99, 'hacker', 0, 'nowhere')");
        assertNotNull(result);
        assertTrue(result.contains("安全限制"), "Should block INSERT, got: " + result);
    }

    @Test
    void testSqlInjectionSemicolon() {
        String result = databaseSkill.executeQuery("SELECT 1; DROP TABLE users");
        assertNotNull(result);
        assertTrue(result.contains("安全限制"), "Should block semicolon injection, got: " + result);
    }

    @Test
    void testSqlInjectionComment() {
        String result = databaseSkill.executeQuery("SELECT * FROM users -- drop table");
        assertNotNull(result);
        assertTrue(result.contains("安全限制"), "Should block SQL comment injection, got: " + result);
    }

    @Test
    void testSqlInjectionUnion() {
        String result = databaseSkill.executeQuery("SELECT * FROM users UNION SELECT * FROM secrets");
        assertNotNull(result);
        assertTrue(result.contains("安全限制"), "Should block UNION injection, got: " + result);
    }

    @Test
    void testEmptySql() {
        String result = databaseSkill.executeQuery("");
        assertNotNull(result);
        assertTrue(result.contains("不能为空"));
    }

    @Test
    void testNullSql() {
        String result = databaseSkill.executeQuery(null);
        assertNotNull(result);
        assertTrue(result.contains("不能为空"));
    }

    @Test
    void testInvalidTableName() {
        String result = databaseSkill.describeTable("users; DROP TABLE users");
        assertNotNull(result);
        assertTrue(result.contains("无效") || result.contains("安全"));
    }
}
