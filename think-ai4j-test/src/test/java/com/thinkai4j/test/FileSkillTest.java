package com.thinkai4j.test;

import com.thinkai4j.skill.FileSkill;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileSkillTest {

    @TempDir
    Path tempDir;

    private FileSkill fileSkill;

    @BeforeEach
    void setUp() {
        fileSkill = new FileSkill(tempDir.toString());
    }

    @Test
    void testWriteAndReadFile() throws IOException {
        String content = "Hello, ThinkAi4j!";
        String writeResult = fileSkill.writeFile("test.txt", content);
        assertTrue(writeResult.contains("写入成功"));

        assertTrue(Files.exists(tempDir.resolve("test.txt")));

        String readResult = fileSkill.readFile("test.txt");
        assertEquals(content, readResult);
    }

    @Test
    void testReadNonExistentFile() {
        String result = fileSkill.readFile("nonexistent.txt");
        assertTrue(result.contains("文件不存在"));
    }

    @Test
    void testWriteToSubDirectory() throws IOException {
        String content = "Sub directory content";
        String result = fileSkill.writeFile("subdir/nested/file.txt", content);
        assertTrue(result.contains("写入成功"));

        assertTrue(Files.exists(tempDir.resolve("subdir/nested/file.txt")));

        String readResult = fileSkill.readFile("subdir/nested/file.txt");
        assertEquals(content, readResult);
    }

    @Test
    void testListDirectory() throws IOException {
        Files.createDirectory(tempDir.resolve("folder1"));
        Files.writeString(tempDir.resolve("file1.txt"), "content1");
        Files.writeString(tempDir.resolve("file2.txt"), "content2");

        String result = fileSkill.listDirectory(tempDir.toString());
        assertNotNull(result);
        assertTrue(result.contains("[DIR] folder1") || result.contains("[DIR]folder1"), "Expected DIR folder1 but got: " + result);
        assertTrue(result.contains("[FILE]file1.txt") || result.contains("[FILE] file1.txt"), "Expected FILE file1.txt but got: " + result);
        assertTrue(result.contains("[FILE]file2.txt") || result.contains("[FILE] file2.txt"), "Expected FILE file2.txt but got: " + result);
    }

    @Test
    void testListNonExistentDirectory() {
        String result = fileSkill.listDirectory("nonexistent_dir");
        assertTrue(result.contains("目录不存在"));
    }

    @Test
    void testWriteFileWithAbsolutePath() throws IOException {
        Path absolutePath = tempDir.resolve("absolute.txt");
        String content = "Absolute path content";
        
        String result = fileSkill.writeFile(absolutePath.toString(), content);
        assertTrue(result.contains("写入成功"));
        assertTrue(Files.exists(absolutePath));

        String readResult = fileSkill.readFile(absolutePath.toString());
        assertEquals(content, readResult);
    }

    @Test
    void testWriteOverwriteFile() throws IOException {
        fileSkill.writeFile("overwrite.txt", "original content");
        fileSkill.writeFile("overwrite.txt", "new content");

        String result = fileSkill.readFile("overwrite.txt");
        assertEquals("new content", result);
    }

    @Test
    void testReadMultilineFile() throws IOException {
        String multilineContent = "Line 1\nLine 2\nLine 3";
        fileSkill.writeFile("multiline.txt", multilineContent);

        String result = fileSkill.readFile("multiline.txt");
        assertEquals(multilineContent, result);
    }

    @Test
    void testListEmptyDirectory() throws IOException {
        Files.createDirectory(tempDir.resolve("empty_folder"));
        String result = fileSkill.listDirectory("empty_folder");
        assertNotNull(result);
        assertEquals("", result.trim());
    }
}
