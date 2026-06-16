---
name: "maven-build"
description: "Maven 项目构建、编译、打包、依赖管理。当用户需要执行 mvn clean、mvn install、mvn package、依赖更新、多模块构建、Maven 插件配置时使用此技能。"
---

# Maven Build Management

Maven 项目构建与依赖管理专家技能。

## 核心功能

- 编译项目代码
- 执行单元测试和集成测试
- 打包 JAR/WAR 文件
- 管理依赖版本和冲突
- 多模块项目构建
- Maven 插件配置与优化

## 常用命令

### 基础构建命令

```bash
# 清理并编译
mvn clean compile

# 运行测试
mvn test

# 打包（跳过测试）
mvn package -DskipTests

# 安装到本地仓库
mvn clean install

# 部署到远程仓库
mvn deploy
```

### 多模块项目

```bash
# 从根目录构建所有模块
mvn clean install

# 只构建特定模块
mvn clean install -pl think-ai4j-core

# 构建模块及其依赖
mvn clean install -pl think-ai4j-core -am

# 跳过测试快速构建
mvn clean install -DskipTests -T 4C
```

### 依赖管理

```bash
# 查看依赖树
mvn dependency:tree

# 分析依赖冲突
mvn dependency:analyze

# 更新依赖版本
mvn versions:display-dependency-updates

# 更新插件版本
mvn versions:display-plugin-updates
```

### 性能优化

```bash
# 并行构建（4个线程）
mvn clean install -T 4

# 每个 CPU 核心一个线程
mvn clean install -T 1C

# 离线模式（使用本地缓存）
mvn clean install -o
```

## pom.xml 配置最佳实践

### 版本管理

```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

### 依赖管理

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 构建插件

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## 常见问题解决

### 依赖冲突

1. 使用 `mvn dependency:tree` 查看冲突
2. 使用 `<exclusions>` 排除冲突依赖
3. 使用 `<dependencyManagement>` 统一管理版本

### 内存不足

```bash
# 增加 Maven 内存
set MAVEN_OPTS=-Xmx2048m -XX:MaxPermSize=512m
mvn clean install
```

### 构建速度慢

1. 使用并行构建 `-T 1C`
2. 跳过测试 `-DskipTests`
3. 使用离线模式 `-o`
4. 配置本地仓库镜像
