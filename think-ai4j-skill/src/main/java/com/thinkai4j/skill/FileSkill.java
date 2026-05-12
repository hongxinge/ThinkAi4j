package com.thinkai4j.skill;

import com.thinkai4j.tool.annotation.AiTool;
import com.thinkai4j.tool.annotation.ToolParam;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件操作 Skill - 提供文件读写能力
 */
public class FileSkill {

    private final String basePath;

    public FileSkill() {
        this(System.getProperty("user.dir"));
    }

    public FileSkill(String basePath) {
        this.basePath = basePath;
    }

    @AiTool("读取文件内容")
    public String readFile(
            @ToolParam(description = "文件路径（相对于basePath或绝对路径）") String path) {
        try {
            Path filePath = resolvePath(path);
            if (!Files.exists(filePath)) {
                return "文件不存在: " + path;
            }
            return Files.lines(filePath, StandardCharsets.UTF_8)
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "读取文件失败: " + e.getMessage();
        }
    }

    @AiTool("写入文件内容")
    public String writeFile(
            @ToolParam(description = "文件路径") String path,
            @ToolParam(description = "文件内容") String content) {
        try {
            Path filePath = resolvePath(path);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            return "文件写入成功: " + path;
        } catch (IOException e) {
            return "写入文件失败: " + e.getMessage();
        }
    }

    @AiTool("列出目录内容")
    public String listDirectory(
            @ToolParam(description = "目录路径") String path) {
        try {
            Path dirPath = resolvePath(path);
            if (!Files.isDirectory(dirPath)) {
                return "目录不存在: " + path;
            }
            try (Stream<Path> stream = Files.list(dirPath)) {
                return stream.map(p -> {
                    String type = Files.isDirectory(p) ? "[DIR] " : "[FILE]";
                    return type + p.getFileName();
                }).collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            return "列出目录失败: " + e.getMessage();
        }
    }

    private Path resolvePath(String path) {
        Path p = Paths.get(path);
        if (p.isAbsolute()) {
            return p;
        }
        return Paths.get(basePath).resolve(p);
    }
}
