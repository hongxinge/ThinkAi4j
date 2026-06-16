---
name: "port-detector"
description: "端口检测、占用查询、端口扫描、服务可用性检查。当用户需要检查端口是否被占用、查找占用端口的进程、扫描端口、测试服务连通性时使用此技能。"
---

# Port Detector

端口检测与服务可用性检查专家技能。

## 核心功能

- 检查端口占用情况
- 查找占用端口的进程
- 端口扫描
- 服务连通性测试
- 端口释放与重新绑定

## 检查端口占用

### Linux/Mac

```bash
# 使用 netstat
netstat -tuln | grep :8080
netstat -an | grep 8080

# 使用 ss（推荐，更快）
ss -tuln | grep :8080
ss -lntp | grep :8080

# 使用 lsof
lsof -i :8080
lsof -i :8080 -P -n

# 检查特定端口
lsof -i :8080 -sTCP:LISTEN
```

### Windows

```powershell
# 使用 netstat
netstat -ano | findstr :8080

# 使用 PowerShell
Get-NetTCPConnection -LocalPort 8080

# 查看详细情况
netstat -ano | findstr "8080"

# 使用 Test-NetConnection
Test-NetConnection -ComputerName localhost -Port 8080
```

## 查找占用进程

### Linux/Mac

```bash
# 查找占用端口的进程ID
lsof -ti :8080

# 查看进程详细信息
lsof -i :8080 -P -n

# 使用 fuser
fuser 8080/tcp
fuser -v 8080/tcp

# 结合 ps 查看进程信息
lsof -i :8080 | awk 'NR>1 {print $2}' | xargs ps -fp

# 使用 netstat 和 ps
netstat -tulnp | grep :8080
```

### Windows

```powershell
# 查找进程ID
netstat -ano | findstr :8080

# 根据PID查看进程
tasklist | findstr <PID>

# 使用 PowerShell
Get-NetTCPConnection -LocalPort 8080 | Select-Object OwningProcess

# 查看进程详情
Get-Process -Id <PID>

# 一键查找占用端口的进程
$port = 8080
$connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
if ($connections) {
    $connections | ForEach-Object {
        Get-Process -Id $_.OwningProcess | Select-Object Id, ProcessName, Path
    }
} else {
    Write-Host "Port $port is not in use"
}
```

## 停止占用端口的进程

### Linux/Mac

```bash
# 优雅停止
kill $(lsof -ti :8080)

# 强制停止
kill -9 $(lsof -ti :8080)

# 使用 fuser
fuser -k 8080/tcp

# 确认进程已停止
lsof -i :8080
```

### Windows

```powershell
# 根据PID停止进程
taskkill /PID <PID>

# 强制停止
taskkill /PID <PID> /F

# 一键停止占用端口的所有进程
$port = 8080
$connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
if ($connections) {
    $connections | ForEach-Object {
        Stop-Process -Id $_.OwningProcess -Force
        Write-Host "Stopped process $($_.OwningProcess)"
    }
}
```

## 端口扫描

### nmap 扫描

```bash
# 安装 nmap
sudo apt-get install nmap  # Debian/Ubuntu
sudo yum install nmap      # CentOS/RHEL
brew install nmap          # Mac

# 扫描单个端口
nmap -p 8080 localhost

# 扫描端口范围
nmap -p 8000-9000 localhost

# 扫描常用端口
nmap --top-ports 100 localhost

# 扫描所有端口
nmap -p- localhost

# 服务版本检测
nmap -sV -p 8080 localhost

# 操作系统检测
nmap -O localhost

# 快速扫描
nmap -F localhost

# 详细扫描
nmap -A localhost
```

### 简单端口扫描脚本

```bash
#!/bin/bash
# port-scan.sh

HOST=${1:-localhost}
START_PORT=${2:-1}
END_PORT=${3:-1024}

echo "Scanning $HOST ports $START_PORT-$END_PORT..."

for port in $(seq $START_PORT $END_PORT); do
    (echo >/dev/tcp/$HOST/$port) 2>/dev/null && echo "Port $port is open"
done
```

### PowerShell 端口扫描

```powershell
# port-scan.ps1
param(
    [string]$Host = "localhost",
    [int]$StartPort = 1,
    [int]$EndPort = 1024
)

Write-Host "Scanning $Host ports $StartPort-$EndPort..."

for ($port = $StartPort; $port -le $EndPort; $port++) {
    $tcp = New-Object System.Net.Sockets.TcpClient
    try {
        $tcp.Connect($Host, $port)
        Write-Host "Port $port is open" -ForegroundColor Green
    } catch {
        # Port is closed
    } finally {
        $tcp.Close()
    }
}
```

## 服务连通性测试

### telnet 测试

```bash
# 测试端口连通性
telnet localhost 8080

# 退出 telnet
# 按 Ctrl+] 然后输入 quit
```

### nc (netcat) 测试

```bash
# 测试端口
nc -zv localhost 8080

# 测试多个端口
nc -zv localhost 8080 8081 8082

# 测试端口范围
nc -zv localhost 8000-8100

# 保持连接
nc localhost 8080
```

### curl 测试

```bash
# 测试 HTTP 服务
curl -I http://localhost:8080

# 测试 HTTPS
curl -kI https://localhost:8443

# 测试 API
curl http://localhost:8080/actuator/health

# 带超时的测试
curl --connect-timeout 5 -I http://localhost:8080
```

### PowerShell 测试

```powershell
# 使用 Test-NetConnection
Test-NetConnection -ComputerName localhost -Port 8080

# 使用 TcpClient
$tcp = New-Object System.Net.Sockets.TcpClient
try {
    $tcp.Connect("localhost", 8080)
    Write-Host "Port 8080 is open" -ForegroundColor Green
} catch {
    Write-Host "Port 8080 is closed" -ForegroundColor Red
} finally {
    $tcp.Close()
}

# 批量测试端口
$ports = @(8080, 8081, 8082, 3306, 5432, 6379)
foreach ($port in $ports) {
    $result = Test-NetConnection -ComputerName localhost -Port $port -WarningAction SilentlyContinue
    if ($result.TcpTestSucceeded) {
        Write-Host "Port $port is open" -ForegroundColor Green
    } else {
        Write-Host "Port $port is closed" -ForegroundColor Red
    }
}
```

## 常用端口列表

### Web 服务
- 80: HTTP
- 443: HTTPS
- 8080: HTTP 备用
- 8443: HTTPS 备用
- 3000: Node.js/React
- 4200: Angular
- 5173: Vite
- 8000: Python/Django

### 数据库
- 3306: MySQL
- 5432: PostgreSQL
- 1521: Oracle
- 1433: SQL Server
- 27017: MongoDB
- 6379: Redis
- 9200: Elasticsearch
- 9300: Elasticsearch (transport)

### 消息队列
- 5672: RabbitMQ
- 15672: RabbitMQ Management
- 9092: Kafka
- 2181: Zookeeper
- 61616: ActiveMQ

### 其他服务
- 22: SSH
- 21: FTP
- 25: SMTP
- 110: POP3
- 143: IMAP
- 389: LDAP
- 8888: Jupyter Notebook

## 端口检测脚本

### 综合检测脚本

```bash
#!/bin/bash
# check-port.sh

PORT=$1

if [ -z "$PORT" ]; then
    echo "Usage: $0 <port>"
    exit 1
fi

echo "Checking port $PORT..."

# 检查端口是否被占用
if lsof -i :$PORT > /dev/null 2>&1; then
    echo "Port $PORT is in use"
    echo ""
    echo "Process details:"
    lsof -i :$PORT -P -n
else
    echo "Port $PORT is available"
fi

echo ""
echo "Testing connectivity..."
if nc -zv localhost $PORT 2>/dev/null; then
    echo "Port $PORT is accessible"
else
    echo "Port $PORT is not accessible"
fi
```

### Windows 综合检测脚本

```powershell
# check-port.ps1
param(
    [Parameter(Mandatory=$true)]
    [int]$Port
)

Write-Host "Checking port $Port..." -ForegroundColor Cyan

# 检查端口占用
$connections = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue

if ($connections) {
    Write-Host "Port $Port is in use" -ForegroundColor Yellow
    Write-Host "`nProcess details:" -ForegroundColor Cyan
    $connections | ForEach-Object {
        $process = Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue
        if ($process) {
            Write-Host "PID: $($_.OwningProcess), Name: $($process.ProcessName), Path: $($process.Path)"
        }
    }
} else {
    Write-Host "Port $Port is available" -ForegroundColor Green
}

# 测试连通性
Write-Host "`nTesting connectivity..." -ForegroundColor Cyan
$result = Test-NetConnection -ComputerName localhost -Port $Port -WarningAction SilentlyContinue
if ($result.TcpTestSucceeded) {
    Write-Host "Port $Port is accessible" -ForegroundColor Green
} else {
    Write-Host "Port $Port is not accessible" -ForegroundColor Red
}
```

## Spring Boot 端口配置

### application.yml

```yaml
server:
  port: 8080
  # 如果端口被占用，自动尝试下一个可用端口
  #（注意：Spring Boot 不直接支持此功能，需要自定义）
  
# 管理端口
management:
  server:
    port: 8081
```

### 检查应用端口

```bash
# 检查 Spring Boot 应用是否启动
curl -s http://localhost:8080/actuator/health | jq .status

# 检查应用启动时间
curl -s http://localhost:8080/actuator/info

# 查看所有端点
curl -s http://localhost:8080/actuator | jq '._links | keys'
```

## 常见问题解决

### 端口被占用

```bash
# 1. 查找占用进程
lsof -i :8080

# 2. 停止占用进程
kill -9 $(lsof -ti :8080)

# 3. 或者使用其他端口
java -jar app.jar --server.port=8081
```

### 无法绑定端口

```bash
# 检查端口是否被占用
netstat -tuln | grep :8080

# 检查权限（Linux 下 1024 以下端口需要 root）
sudo java -jar app.jar --server.port=80

# 或者使用非特权端口
java -jar app.jar --server.port=8080
```

### 防火墙阻止端口

```bash
# Linux (iptables)
sudo iptables -A INPUT -p tcp --dport 8080 -j ACCEPT
sudo iptables-save | sudo tee /etc/iptables/rules.v4

# Linux (firewalld)
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload

# Windows (PowerShell)
New-NetFirewallRule -DisplayName "Allow 8080" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
```

## 最佳实践

1. **使用非特权端口**：应用使用 1024 以上的端口
2. **端口规划**：为不同服务分配固定的端口范围
3. **文档记录**：记录所有服务使用的端口
4. **自动化检测**：在部署脚本中自动检测端口可用性
5. **优雅处理**：应用启动时检测端口占用并给出明确提示
6. **安全考虑**：不要开放不必要的端口到公网
