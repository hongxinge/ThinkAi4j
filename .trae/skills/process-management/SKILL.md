---
name: "process-management"
description: "Java 应用进程启停管理、服务部署、后台运行。当用户需要启动 Java 应用、停止进程、查看运行状态、后台部署、服务管理时使用此技能。"
---

# Process Management

Java 应用进程启停与服务管理专家技能。

## 核心功能

- Java 应用启动与停止
- 后台进程管理
- 进程状态监控
- 服务部署与重启
- 端口占用处理

## Java 应用启动

### 基础启动

```bash
# 直接运行 JAR
java -jar target/app.jar

# 指定配置文件
java -jar target/app.jar --spring.config.location=classpath:/application-prod.yml

# 指定 JVM 参数
java -Xms512m -Xmx2048m -jar target/app.jar

# 指定端口
java -jar target/app.jar --server.port=8080
```

### 后台运行

```bash
# Linux/Mac 后台运行
nohup java -jar target/app.jar > app.log 2>&1 &

# 查看后台进程
jobs -l

# 将前台进程放到后台
# 按 Ctrl+Z 暂停，然后
bg %1

# 将后台进程放到前台
fg %1
```

### Windows 后台运行

```powershell
# 使用 start 命令
start /b java -jar target/app.jar

# 创建 Windows 服务（使用 NSSM）
nssm install MyApp "C:\Program Files\Java\jdk-17\bin\java.exe" "-jar C:\app\target\app.jar"
nssm start MyApp

# 使用 PowerShell 后台运行
Start-Process -NoNewWindow java -ArgumentList "-jar","target/app.jar"
```

## 进程管理

### 查看进程

```bash
# Linux/Mac 查看 Java 进程
ps -ef | grep java
jps -l

# Windows 查看 Java 进程
tasklist | findstr java
jps -l

# 查看特定应用
ps -ef | grep app.jar
```

### 停止进程

```bash
# 优雅停止（推荐）
kill -15 <PID>
# 或
kill <PID>

# 强制停止
kill -9 <PID>

# Windows 停止进程
taskkill /PID <PID>
taskkill /PID <PID> /F

# 通过端口查找并停止
# Linux/Mac
lsof -ti:8080 | xargs kill -9

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### 重启应用

```bash
# 停止并重启
./stop.sh && ./start.sh

# 一键重启脚本示例
#!/bin/bash
APP_NAME="app.jar"
PID=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')

if [ ! -z "$PID" ]; then
    echo "Stopping application with PID: $PID"
    kill $PID
    sleep 5
fi

echo "Starting application..."
nohup java -jar target/$APP_NAME > app.log 2>&1 &
echo "Application started with PID: $!"
```

## 启动脚本模板

### Linux 启动脚本

```bash
#!/bin/bash

APP_NAME="think-ai4j-example"
JAR_FILE="target/think-ai4j-example.jar"
LOG_FILE="app.log"
PID_FILE="app.pid"

# JVM 参数
JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC"
SPRING_OPTS="--spring.profiles.active=prod"

start() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat $PID_FILE)
        if ps -p $PID > /dev/null; then
            echo "Application is already running with PID: $PID"
            return 1
        fi
    fi

    echo "Starting $APP_NAME..."
    nohup java $JAVA_OPTS -jar $JAR_FILE $SPRING_OPTS > $LOG_FILE 2>&1 &
    echo $! > $PID_FILE
    echo "Application started with PID: $!"
}

stop() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat $PID_FILE)
        if ps -p $PID > /dev/null; then
            echo "Stopping application with PID: $PID..."
            kill $PID
            sleep 5
            if ps -p $PID > /dev/null; then
                echo "Force stopping..."
                kill -9 $PID
            fi
            rm -f $PID_FILE
            echo "Application stopped"
        else
            echo "Application is not running"
            rm -f $PID_FILE
        fi
    else
        echo "PID file not found"
    fi
}

status() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat $PID_FILE)
        if ps -p $PID > /dev/null; then
            echo "Application is running with PID: $PID"
        else
            echo "Application is not running (stale PID file)"
        fi
    else
        echo "Application is not running"
    fi
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        stop
        start
        ;;
    status)
        status
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
```

### Windows 启动脚本

```powershell
# start.ps1
$AppName = "think-ai4j-example"
$JarFile = "target\think-ai4j-example.jar"
$LogFile = "app.log"
$PidFile = "app.pid"

$JavaOpts = "-Xms512m -Xmx2048m -XX:+UseG1GC"
$SpringOpts = "--spring.profiles.active=prod"

function Start-App {
    if (Test-Path $PidFile) {
        $PID = Get-Content $PidFile
        if (Get-Process -Id $PID -ErrorAction SilentlyContinue) {
            Write-Host "Application is already running with PID: $PID"
            return
        }
    }

    Write-Host "Starting $AppName..."
    $Process = Start-Process -FilePath "java" `
        -ArgumentList "$JavaOpts -jar $JarFile $SpringOpts" `
        -RedirectStandardOutput $LogFile `
        -RedirectStandardError "error.log" `
        -PassThru `
        -WindowStyle Hidden
    
    $Process.Id | Out-File $PidFile
    Write-Host "Application started with PID: $($Process.Id)"
}

function Stop-App {
    if (Test-Path $PidFile) {
        $PID = Get-Content $PidFile
        if (Get-Process -Id $PID -ErrorAction SilentlyContinue) {
            Write-Host "Stopping application with PID: $PID..."
            Stop-Process -Id $PID -Force
            Remove-Item $PidFile
            Write-Host "Application stopped"
        } else {
            Write-Host "Application is not running"
            Remove-Item $PidFile
        }
    } else {
        Write-Host "PID file not found"
    }
}

param(
    [ValidateSet("start", "stop", "restart")]
    [string]$Action = "start"
)

switch ($Action) {
    "start" { Start-App }
    "stop" { Stop-App }
    "restart" { Stop-App; Start-App }
}
```

## Spring Boot 服务管理

### Systemd 服务（Linux）

```ini
# /etc/systemd/system/think-ai4j.service
[Unit]
Description=ThinkAi4j Application
After=syslog.target network.target

[Service]
Type=simple
User=appuser
Group=appuser
WorkingDirectory=/opt/think-ai4j
ExecStart=/usr/bin/java -Xms512m -Xmx2048m -jar target/think-ai4j-example.jar
ExecStop=/bin/kill -15 $MAINPID
Restart=on-failure
RestartSec=10

StandardOutput=append:/var/log/think-ai4j/app.log
StandardError=append:/var/log/think-ai4j/error.log

Environment=SPRING_PROFILES_ACTIVE=prod

[Install]
WantedBy=multi-user.target
```

```bash
# 启用服务
sudo systemctl daemon-reload
sudo systemctl enable think-ai4j

# 启动/停止/重启
sudo systemctl start think-ai4j
sudo systemctl stop think-ai4j
sudo systemctl restart think-ai4j

# 查看状态
sudo systemctl status think-ai4j

# 查看日志
sudo journalctl -u think-ai4j -f
```

## Docker 容器管理

```bash
# 构建镜像
docker build -t think-ai4j:latest .

# 运行容器
docker run -d \
  --name think-ai4j \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v /var/log/think-ai4j:/app/logs \
  think-ai4j:latest

# 停止容器
docker stop think-ai4j

# 启动容器
docker start think-ai4j

# 重启容器
docker restart think-ai4j

# 查看容器状态
docker ps -a | grep think-ai4j

# 查看容器日志
docker logs -f think-ai4j

# 进入容器
docker exec -it think-ai4j /bin/sh
```

## 进程监控

### 实时监控

```bash
# 查看进程资源使用
top -p <PID>

# 查看 Java 进程详细信息
jstat -gc <PID> 1000

# 查看线程
jstack <PID>

# 查看 JVM 信息
jinfo <PID>
```

### 健康检查

```bash
# Spring Boot Actuator 健康检查
curl http://localhost:8080/actuator/health

# 检查端口
curl -I http://localhost:8080

# 检查进程
ps -p <PID> -o pid,cmd,%cpu,%mem
```

## 最佳实践

1. **优雅停机**：使用 `kill -15` 而不是 `kill -9`
2. **日志管理**：使用日志轮转，避免日志文件过大
3. **监控告警**：配置进程监控和自动重启
4. **配置管理**：使用配置文件管理 JVM 参数
5. **版本管理**：使用版本号命名 JAR 文件
6. **备份恢复**：定期备份配置和数据
