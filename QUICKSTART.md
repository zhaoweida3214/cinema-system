# 快速开始指南

## 环境准备

### 1. 安装 Redis

**Windows:**
```bash
# 下载 Redis for Windows
https://github.com/microsoftarchive/redis/releases

# 启动 Redis 服务器
redis-server.exe redis.windows.conf
```

**Linux:**
```bash
# 安装 Redis
sudo apt-get install redis-server  # Ubuntu/Debian
sudo yum install redis             # CentOS/RHEL

# 启动 Redis
sudo systemctl start redis

# 设置开机自启
sudo systemctl enable redis
```

### 2. 配置 Redis 连接

修改 `src/main/resources/application.yml`:

```yaml
redis:
  host: localhost  # 修改为你的 Redis 服务器地址
  port: 6379       # Redis 端口
  password:        # 如果有密码请设置
  database: 0
```

## 启动项目

### 方式一：使用 Maven

```bash
# 在项目根目录执行
mvn spring-boot:run
```

### 方式二：使用 IDE

直接运行 `CinemaSeatApplication.java` 的 main 方法

## 验证功能

### 1. 测试多级缓存性能

```bash
# 发送 HTTP 请求
curl http://localhost:8080/test/cache/performance?iterations=1000

# 预期响应
{
  "code": 1,
  "msg": "success",
  "data": "缓存性能测试完成：执行 1000 次操作，耗时 XXXms"
}
```

### 2. 测试分布式锁并发

```bash
# 10 个线程，每个线程执行 100 次
curl http://localhost:8080/test/lock/concurrency?threads=10&iterations=100

# 预期响应（成功）
{
  "code": 1,
  "msg": "success",
  "data": "分布式锁并发测试成功：10 个线程，每个线程执行 100 次，总计 1000 次操作全部完成，耗时 XXXms"
}
```

### 3. 测试缓存命中率

```bash
# 预热 100 次，测试 1000 次
curl http://localhost:8080/test/cache/hit-rate?warmup=100&testIterations=1000

# 预期响应
{
  "code": 1,
  "msg": "success",
  "data": "缓存命中率测试完成：预热 100 次，测试 1000 次，命中 XXX 次，命中率 XX.XX%"
}
```

## 业务接口测试

### 1. 查询排期列表（带缓存）

```bash
# 查询影院 ID 为 1，日期为今天的排期
curl "http://localhost:8080/schedules?cinemaId=1&date=2026-03-11"

# 第一次请求会查询数据库并写入缓存
# 第二次及以后的请求会直接从缓存读取
```

### 2. 查询座位布局（带缓存）

```bash
# 查询排期 ID 为 1 的座位布局
curl http://localhost:8080/seats/1/layout

# 同样会使用多级缓存
```

### 3. 创建订单（分布式锁保护）

```bash
# 创建订单（需要 JWT Token）
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "scheduleId": 1,
    "seatIds": [1, 2, 3]
  }'

# 分布式锁会确保同一场次的订单串行处理
```

## 监控与日志

### 查看缓存命中率

启动应用后，观察控制台日志：

```
【本地缓存命中】key=schedule_1
【Redis 缓存命中】key=schedule_1
【缓存未命中，查询数据库】key=schedule_1
【缓存已更新】key=schedule_1
```

### 查看分布式锁状态

```
【获取锁成功】lockKey=seat_inventory_lock:1
【释放锁成功】lockKey=seat_inventory_lock:1
【获取锁失败】lockKey=seat_inventory_lock:1, 请求过于频繁
```

## 常见问题

### Q1: Redis 连接失败

**错误信息:**
```
io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
```

**解决方案:**
1. 确认 Redis 服务已启动：`redis-cli ping` 应返回 `PONG`
2. 检查防火墙是否开放 6379 端口
3. 确认 application.yml 中的 Redis 配置正确

### Q2: 依赖下载失败

**错误信息:**
```
Could not resolve dependencies for project
```

**解决方案:**
```bash
# 清理 Maven 缓存
mvn clean

# 重新下载依赖
mvn dependency:resolve

# 使用国内镜像（阿里云）
# 在 pom.xml 中添加:
<mirrors>
  <mirror>
    <id>aliyunmaven</id>
    <mirrorOf>*</mirrorOf>
    <name>阿里云公共仓库</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### Q3: 端口被占用

**错误信息:**
```
Port 8080 is already in use
```

**解决方案:**
```bash
# 方案 1: 修改服务器端口
# 在 application.yml 中修改:
server:
  port: 8081  # 改为其他端口

# 方案 2: 关闭占用端口的进程
# Windows:
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux:
lsof -i :8080
kill -9 <PID>
```

## 性能调优建议

### 1. Caffeine 缓存大小

根据内存情况调整：

```yaml
cache:
  caffeine:
    max-size: 10000  # 增加或减少这个值
```

### 2. Redis 连接池

根据并发量调整：

```yaml
redis:
  lettuce:
    pool:
      max-active: 100   # 最大连接数
      max-idle: 50      # 最大空闲连接
      min-idle: 10      # 最小空闲连接
```

### 3. 分布式锁超时时间

根据业务处理时长调整：

```yaml
distributed-lock:
  wait-time: 5      # 等待锁的时间（秒）
  lease-time: 30    # 锁持有时间（秒）
```

## 下一步

1. **阅读详细文档**: 查看 `CACHE_AND_LOCK_IMPLEMENTATION.md` 了解完整实现细节

2. **压力测试**: 使用 JMeter 或其他工具进行高并发测试

3. **生产部署**: 
   - 配置 Redis 集群提高可用性
   - 启用 Redis 持久化防止数据丢失
   - 配置监控告警系统

## 技术支持

如有问题，请查看：
- 项目日志文件
- Redis 日志：`/var/log/redis/redis-server.log` (Linux)
- 应用控制台输出
