---
name: "log-viewer"
description: "日志查看、分析、搜索、过滤、日志轮转。当用户需要查看应用日志、搜索日志内容、分析错误信息、日志过滤、日志轮转配置时使用此技能。"
---

# Log Viewer

日志查看与分析专家技能。

## 核心功能

- 实时查看日志
- 日志搜索与过滤
- 错误分析
- 日志轮转配置
- 日志聚合分析

## 基础日志查看

### Linux/Mac 命令

```bash
# 查看文件末尾（默认10行）
tail app.log

# 查看末尾100行
tail -n 100 app.log

# 实时跟踪日志
tail -f app.log

# 跟踪多个文件
tail -f app.log error.log

# 查看文件开头
head app.log

# 查看开头100行
head -n 100 app.log

# 查看整个文件
cat app.log

# 分页查看
less app.log
more app.log
```

### Windows PowerShell

```powershell
# 查看末尾内容
Get-Content app.log -Tail 100

# 实时跟踪
Get-Content app.log -Wait -Tail 100

# 查看整个文件
Get-Content app.log

# 分页查看
Get-Content app.log | more
```

## 日志搜索

### grep 搜索

```bash
# 搜索关键字
grep "ERROR" app.log

# 忽略大小写
grep -i "error" app.log

# 显示行号
grep -n "ERROR" app.log

# 递归搜索目录
grep -r "ERROR" /var/log/app/

# 搜索多个关键字
grep -E "ERROR|WARN|Exception" app.log

# 显示上下文（前后各5行）
grep -C 5 "ERROR" app.log

# 显示前5行
grep -B 5 "ERROR" app.log

# 显示后5行
grep -A 5 "ERROR" app.log

# 统计匹配行数
grep -c "ERROR" app.log

# 反向匹配（不包含）
grep -v "DEBUG" app.log
```

### PowerShell 搜索

```powershell
# 搜索关键字
Select-String -Path app.log -Pattern "ERROR"

# 忽略大小写
Select-String -Path app.log -Pattern "error" -CaseSensitive:$false

# 显示上下文
Select-String -Path app.log -Pattern "ERROR" -Context 5,5

# 递归搜索
Get-ChildItem -Path . -Recurse -Include *.log | Select-String -Pattern "ERROR"
```

## 日志分析

### 统计错误

```bash
# 统计 ERROR 数量
grep -c "ERROR" app.log

# 统计各种级别日志数量
grep -c "DEBUG" app.log
grep -c "INFO" app.log
grep -c "WARN" app.log
grep -c "ERROR" app.log

# 按小时统计错误
grep "ERROR" app.log | awk '{print $2}' | cut -d: -f1 | sort | uniq -c

# 统计最常见的错误
grep "ERROR" app.log | sort | uniq -c | sort -rn | head -20
```

### 提取特定信息

```bash
# 提取所有异常堆栈
grep -A 20 "Exception" app.log

# 提取特定时间段的日志
sed -n '/2024-01-01 10:00/,/2024-01-01 11:00/p' app.log

# 提取特定类的日志
grep "com.thinkai4j" app.log

# 提取请求日志
grep "HTTP" app.log | grep "200 OK"
```

### 高级分析

```bash
# 查看错误趋势（按分钟统计）
grep "ERROR" app.log | \
  awk '{print $1, $2}' | \
  cut -d: -f1,2 | \
  sort | \
  uniq -c | \
  sort -k2,2 -k3,3

# 查找耗时最长的请求
grep "Request completed" app.log | \
  awk '{print $NF, $0}' | \
  sort -rn | \
  head -10

# 统计响应时间分布
grep "Request completed" app.log | \
  awk '{print $NF}' | \
  awk '{
    if ($1 < 100) bucket="<100ms";
    else if ($1 < 500) bucket="100-500ms";
    else if ($1 < 1000) bucket="500-1000ms";
    else bucket=">1000ms";
    count[bucket]++;
  }
  END {
    for (b in count) print b, count[b]
  }'
```

## 实时日志监控

### tail 组合命令

```bash
# 实时监控错误
tail -f app.log | grep --line-buffered "ERROR"

# 实时监控特定类
tail -f app.log | grep --line-buffered "com.thinkai4j"

# 实时监控并高亮
tail -f app.log | grep --color=always -E "ERROR|WARN|$"
```

### multitail（多文件监控）

```bash
# 安装 multitail
sudo apt-get install multitail  # Debian/Ubuntu
sudo yum install multitail      # CentOS/RHEL

# 同时监控多个文件
multitail app.log error.log access.log

# 监控并过滤
multitail -e "ERROR" app.log
```

## 日志轮转

### logrotate 配置

```bash
# /etc/logrotate.d/think-ai4j
/var/log/think-ai4j/*.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    missingok
    create 0644 appuser appuser
    postrotate
        systemctl kill -s HUP think-ai4j
    endscript
}
```

### 手动轮转

```bash
# 压缩旧日志
gzip app.log.1

# 移动日志文件
mv app.log app.log.$(date +%Y%m%d)

# 创建新日志文件
touch app.log

# 通知应用重新打开日志文件
kill -USR1 $(cat app.pid)
```

## Spring Boot 日志配置

### application.yml

```yaml
logging:
  level:
    root: INFO
    com.thinkai4j: DEBUG
    org.springframework: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/application.log
    max-size: 100MB
    max-history: 30
    total-size-cap: 3GB
```

### logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_PATH" value="logs"/>
    <property name="LOG_FILE" value="application"/>

    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${LOG_FILE}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${LOG_FILE}-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Error File Appender -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/error-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="ERROR_FILE"/>
    </root>

    <logger name="com.thinkai4j" level="DEBUG"/>
</configuration>
```

## 常用日志分析命令

```bash
# 查看最近的错误
tail -n 1000 app.log | grep "ERROR" | tail -20

# 查看今天的错误
grep "$(date +%Y-%m-%d)" app.log | grep "ERROR"

# 查看特定异常的完整堆栈
awk '/NullPointerException/,/^[0-9]{4}-/' app.log

# 统计每小时错误数量
grep "ERROR" app.log | \
  awk '{print substr($2,1,2)}' | \
  sort | uniq -c

# 查找最频繁的异常
grep "Exception" app.log | \
  awk -F'Exception: ' '{print $2}' | \
  sort | uniq -c | sort -rn | head -10

# 查看请求响应时间分布
grep "Request completed in" app.log | \
  awk '{print $NF}' | \
  sort -n | \
  awk 'BEGIN {print "Count\tMin\tAvg\tMax"} \
  {sum+=$1; count++; if(count==1) min=$1; max=$1} \
  END {print count "\t" min "\t" sum/count "\t" max}'
```

## 日志聚合工具

### ELK Stack

```bash
# Filebeat 配置
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /var/log/think-ai4j/*.log
  fields:
    app: think-ai4j
  fields_under_root: true

output.elasticsearch:
  hosts: ["localhost:9200"]
```

### 简单日志聚合脚本

```bash
#!/bin/bash
# aggregate-logs.sh

LOG_DIR="/var/log/think-ai4j"
OUTPUT_DIR="/tmp/log-analysis"
DATE=$(date +%Y-%m-%d)

mkdir -p $OUTPUT_DIR

# 合并今天的日志
find $LOG_DIR -name "*.log" -exec grep "^$DATE" {} \; > $OUTPUT_DIR/combined-$DATE.log

# 统计错误
echo "Error Summary for $DATE" > $OUTPUT_DIR/summary-$DATE.txt
echo "========================" >> $OUTPUT_DIR/summary-$DATE.txt
echo "Total Errors: $(grep -c 'ERROR' $OUTPUT_DIR/combined-$DATE.log)" >> $OUTPUT_DIR/summary-$DATE.txt
echo "Total Warnings: $(grep -c 'WARN' $OUTPUT_DIR/combined-$DATE.log)" >> $OUTPUT_DIR/summary-$DATE.txt
echo "" >> $OUTPUT_DIR/summary-$DATE.txt
echo "Top 10 Errors:" >> $OUTPUT_DIR/summary-$DATE.txt
grep 'ERROR' $OUTPUT_DIR/combined-$DATE.log | sort | uniq -c | sort -rn | head -10 >> $OUTPUT_DIR/summary-$DATE.txt
```

## 最佳实践

1. **结构化日志**：使用 JSON 格式便于解析
2. **日志级别**：合理使用 DEBUG/INFO/WARN/ERROR
3. **日志轮转**：配置自动轮转避免磁盘满
4. **集中管理**：使用 ELK/EFK 等日志平台
5. **监控告警**：配置错误日志告警
6. **保留策略**：根据合规要求设置保留时间
7. **敏感信息**：避免记录密码、token 等敏感信息
