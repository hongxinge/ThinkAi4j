package com.thinkai4j.skill;

import com.thinkai4j.tool.annotation.AiTool;
import com.thinkai4j.tool.annotation.ToolParam;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileSkill {

    private final Path basePath;
    private final boolean sandboxEnabled;

    public FileSkill() {
        this(System.getProperty("user.dir"), true);
    }

    public FileSkill(String basePath) {
        this(basePath, true);
    }

    public FileSkill(String basePath, boolean sandboxEnabled) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
        this.sandboxEnabled = sandboxEnabled;
    }

    @AiTool("读取文件内容")
    public String readFile(
            @ToolParam(description = "文件路径（相对于basePath或绝对路径）") String path) {
        try {
            Path filePath = resolveAndValidate(path);
            if (!Files.exists(filePath)) {
                return "文件不存在: " + path;
            }
            if (Files.isDirectory(filePath)) {
                return "路径是目录，不是文件: " + path;
            }
            return Files.lines(filePath, StandardCharsets.UTF_8)
                    .collect(Collectors.joining("\n"));
        } catch (SecurityException e) {
            return "安全限制: " + e.getMessage();
        } catch (IOException e) {
            return "读取文件失败: " + e.getMessage();
        }
    }

    @AiTool("写入文件内容")
    public String writeFile(
            @ToolParam(description = "文件路径") String path,
            @ToolParam(description = "文件内容") String content) {
        try {
            Path filePath = resolveAndValidate(path);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            return "文件写入成功: " + path;
        } catch (SecurityException e) {
            return "安全限制: " + e.getMessage();
        } catch (IOException e) {
            return "写入文件失败: " + e.getMessage();
        }
    }

    @AiTool("列出目录内容")
    public String listDirectory(
            @ToolParam(description = "目录路径") String path) {
        try {
            Path dirPath = resolveAndValidate(path);
            if (!Files.isDirectory(dirPath)) {
                return "目录不存在: " + path;
            }
            try (Stream<Path> stream = Files.list(dirPath)) {
                return stream.map(p -> {
                    String type = Files.isDirectory(p) ? "[DIR] " : "[FILE] ";
                    return type + p.getFileName();
                }).collect(Collectors.joining("\n"));
            }
        } catch (SecurityException e) {
            return "安全限制: " + e.getMessage();
        } catch (IOException e) {
            return "列出目录失败: " + e.getMessage();
        }
    }

    private Path resolveAndValidate(String path) throws SecurityException {
        Path p = Paths.get(path);
        Path resolved;
        if (p.isAbsolute()) {
            resolved = p.normalize();
        } else {
            resolved = basePath.resolve(p).normalize();
        }

        if (sandboxEnabled) {
            // 词法校验：快速拦截 ../ 穿越与沙箱外的绝对路径
            if (!resolved.startsWith(basePath)) {
                throw new SecurityException(
                        "路径超出允许范围: " + path + "（仅允许访问 " + basePath + " 下的文件）");
            }

            // 真实路径校验：normalize 仅做词法规范化，不解析符号链接/junction，
            // 沙箱内指向外部的链接会绕过上面的 startsWith 判断，需解析真实落点再比较
            Path realBase;
            try {
                realBase = basePath.toRealPath();
            } catch (IOException e) {
                // 沙箱根目录尚不存在时回退到词法路径
                realBase = basePath;
            }
            Path realPath;
            try {
                realPath = toRealPathAllowingNonExisting(resolved);
            } catch (IOException e) {
                // 无法确认真实落点时按安全优先拒绝访问
                throw new SecurityException("无法解析路径真实位置，拒绝访问: " + path);
            }
            if (!realPath.startsWith(realBase)) {
                throw new SecurityException(
                        "路径超出允许范围: " + path + "（仅允许访问 " + basePath + " 下的文件）");
            }
        }

        return resolved;
    }

    /**
     * 解析路径的真实位置（含符号链接/junction）。
     * 待写入的新文件、乃至其父目录都可能尚不存在，此时向上找到最近真实存在的祖先，
     * 取其真实路径后拼回剩余片段，从而在路径不存在的前提下仍能校验真实落点。
     */
    private Path toRealPathAllowingNonExisting(Path path) throws IOException {
        Path existing = path;
        while (existing != null && !Files.exists(existing)) {
            existing = existing.getParent();
        }
        if (existing == null) {
            return path.normalize();
        }
        Path realExisting = existing.toRealPath();
        return realExisting.resolve(existing.relativize(path)).normalize();
    }
}
