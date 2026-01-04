# 在线电影选座系统

一个基于 Spring Boot + MyBatis + MySQL 的在线电影选座系统，实现了用户登录、场次查询、座位选择、订单管理等核心功能。

## 项目简介

本系统是数据库课程大作业项目，模拟真实的影院购票场景，包含以下核心功能：

- 👤 **用户认证**：基于 JWT 的无状态登录认证
- 🎬 **场次查询**：根据影院和日期查询电影场次
- 🪑 **座位选择**：实时显示座位状态，支持多座位选择
- 📝 **订单管理**：创建订单、支付、取消，支持订单超时自动释放
- 🔒 **并发控制**：座位锁定机制，防止超卖

## 技术栈

### 后端
- **Spring Boot 3.2.0** - 应用框架
- **MyBatis 3.0.3** - ORM 持久层框架
- **MySQL 8.0** - 关系型数据库
- **Druid 1.2.18** - 数据库连接池
- **JWT 4.4.0** - 身份认证
- **Lombok** - 简化代码
- **JDK 17** - Java 运行环境

### 前端
- **Vue 3** - 前端框架
- **TypeScript** - 类型安全
- **Element Plus** - UI 组件库
- **Vite** - 构建工具
- **Nginx** - 反向代理和静态资源服务

## 环境要求

在开始之前，请确保你的开发环境满足以下要求：

| 软件 | 版本要求 |
|------|----------|
| JDK | 17 或以上 |
| Maven | 3.6+ |
| MySQL | 8.0+ |
| Node.js | 16+ (如需运行前端) |
| Nginx | 1.20+ (如需部署前端) |

## 快速开始

### 1. 克隆项目

```bash
git clone <https://github.com/zhaoweida3214/cinema-system.git>
cd cinema-system
```

### 2. 数据库初始化

#### 2.1 创建数据库

登录 MySQL，创建数据库：

```bash
mysql -u root -p
```

```sql
CREATE DATABASE cinema_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cinema_system;
```

#### 2.2 执行建表语句

依次执行以下 SQL 语句创建表结构：

```sql
-- 用户表
CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `name` VARCHAR(50) NOT NULL COMMENT '真实姓名'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 影院表
CREATE TABLE `cinema` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '影院名称',
    `location` VARCHAR(200) NOT NULL COMMENT '影院地址'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='影院表';

-- 电影表
CREATE TABLE `movie` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(100) NOT NULL COMMENT '电影标题'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电影表';

-- 影厅表
CREATE TABLE `hall` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '影厅名称',
    `rows` INT NOT NULL COMMENT '座位行数',
    `cols` INT NOT NULL COMMENT '座位列数',
    `cinema_id` BIGINT NOT NULL COMMENT '所属影院ID',
    FOREIGN KEY (`cinema_id`) REFERENCES `cinema`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='影厅表';

-- 场次表
CREATE TABLE `schedule` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `movie_id` BIGINT NOT NULL COMMENT '电影ID',
    `hall_id` BIGINT NOT NULL COMMENT '影厅ID',
    `cinema_id` BIGINT NOT NULL COMMENT '影院ID',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    FOREIGN KEY (`movie_id`) REFERENCES `movie`(`id`),
    FOREIGN KEY (`hall_id`) REFERENCES `hall`(`id`),
    FOREIGN KEY (`cinema_id`) REFERENCES `cinema`(`id`),
    INDEX idx_start_time (`start_time`),
    INDEX idx_cinema_movie (`cinema_id`, `movie_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场次表';

-- 订单表
CREATE TABLE `orders` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `schedule_id` BIGINT NOT NULL COMMENT '场次ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING待支付/PAID已支付/CANCELLED已取消',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `expires_at` DATETIME NULL COMMENT '过期时间',
    `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`schedule_id`) REFERENCES `schedule`(`id`),
    INDEX idx_user_created (`user_id`, `created_at`),
    INDEX idx_status_expires (`status`, `expires_at`),
    INDEX idx_schedule_created (`schedule_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 座位状态表
CREATE TABLE `seat_status` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `schedule_id` BIGINT NOT NULL COMMENT '场次ID',
    `row` INT NOT NULL COMMENT '座位行号',
    `col` INT NOT NULL COMMENT '座位列号',
    `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '座位状态：AVAILABLE可选/LOCKED已锁定/SOLD已售',
    `type` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '座位类型：NORMAL普通座/VIP贵宾座',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '座位价格',
    `order_id` BIGINT NULL COMMENT '关联订单ID',
    `locked_until` DATETIME NULL COMMENT '锁定截止时间',
    FOREIGN KEY (`schedule_id`) REFERENCES `schedule`(`id`),
    FOREIGN KEY (`order_id`) REFERENCES `orders`(`id`),
    UNIQUE KEY `uk_schedule_row_col` (`schedule_id`, `row`, `col`),
    INDEX idx_schedule_status (`schedule_id`, `status`),
    INDEX idx_order_id (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位状态表';
```

#### 2.3 插入测试数据

```sql
-- 插入测试用户
INSERT INTO `user` (`username`, `password`, `name`) VALUES
('zhangsan', '123456', '张三'),
('lisi', '123456', '李四');

-- 插入影院数据
INSERT INTO `cinema` (`name`, `location`) VALUES
('万达影城', '北京市朝阳区建国路93号'),
('CGV影城', '上海市黄浦区南京东路422号'),
('博纳国际影城', '广州市天河区天河路208号'),
('大地影院', '深圳市南山区南海大道2068号'),
('金逸影城', '杭州市西湖区文二路28号'),
('UME影城', '成都市锦江区总府路2号'),
('百老汇影城', '重庆市渝中区解放碑步行街88号'),
('星美国际影城', '西安市雁塔区长安中路38号'),
('中影国际影城', '南京市鼓楼区中山路18号'),
('太平洋影城', '天津市和平区和平路263号');

-- 插入电影数据
INSERT INTO `movie` (`title`) VALUES
('阿凡达：水之道'),
('流浪地球2'),
('满江红'),
('无名'),
('深海'),
('熊出没·伴我成长'),
('中国乒乓之绝地反击'),
('交换人生'),
('毒舌律师'),
('风再起时'),
('明日战记'),
('独行月球'),
('这个杀手不太冷静'),
('奇迹·笨小孩'),
('长津湖之水门桥'),
('我和我的父辈'),
('你好，李焕英'),
('唐人街探案3'),
('姜子牙'),
('哪吒之魔童降世');

-- 插入影厅数据
INSERT INTO `hall` (`name`, `rows`, `cols`, `cinema_id`) VALUES
('1号厅', 10, 15, 1), ('2号厅', 8, 12, 1), ('3号厅', 12, 18, 1),
('1号厅', 10, 15, 2), ('2号厅', 8, 12, 2), ('3号厅', 12, 18, 2),
('1号厅', 10, 15, 3), ('2号厅', 8, 12, 3), ('3号厅', 12, 18, 3),
('1号厅', 10, 15, 4), ('2号厅', 8, 12, 4), ('3号厅', 12, 18, 4),
('1号厅', 10, 15, 5), ('2号厅', 8, 12, 5), ('3号厅', 12, 18, 5),
('1号厅', 10, 15, 6), ('2号厅', 8, 12, 6), ('3号厅', 12, 18, 6),
('1号厅', 10, 15, 7), ('2号厅', 8, 12, 7), ('3号厅', 12, 18, 7),
('1号厅', 10, 15, 8), ('2号厅', 8, 12, 8), ('3号厅', 12, 18, 8),
('1号厅', 10, 15, 9), ('2号厅', 8, 12, 9), ('3号厅', 12, 18, 9),
('1号厅', 10, 15, 10), ('2号厅', 8, 12, 10), ('3号厅', 12, 18, 10);

-- 插入场次数据
INSERT INTO `schedule` (`movie_id`, `hall_id`, `cinema_id`, `start_time`, `end_time`) VALUES
(1, 1, 1, '2026-01-15 10:00:00', '2026-01-15 12:30:00'),
(2, 2, 1, '2026-01-15 14:00:00', '2026-01-15 16:30:00'),
(3, 3, 1, '2026-01-15 18:00:00', '2026-01-15 20:30:00'),
(4, 4, 2, '2026-01-15 10:30:00', '2026-01-15 12:45:00'),
(5, 5, 2, '2026-01-15 15:00:00', '2026-01-15 17:15:00'),
(6, 6, 2, '2026-01-15 19:00:00', '2026-01-15 21:00:00'),
(7, 7, 3, '2026-01-15 11:00:00', '2026-01-15 13:20:00'),
(8, 8, 3, '2026-01-15 14:30:00', '2026-01-15 16:50:00'),
(9, 9, 3, '2026-01-15 18:30:00', '2026-01-15 20:45:00'),
(10, 10, 4, '2026-01-15 10:15:00', '2026-01-15 12:25:00'),
(11, 11, 4, '2026-01-15 13:45:00', '2026-01-15 16:00:00'),
(12, 12, 4, '2026-01-15 17:30:00', '2026-01-15 19:45:00'),
(13, 13, 5, '2026-01-15 11:30:00', '2026-01-15 13:50:00'),
(14, 14, 5, '2026-01-15 15:15:00', '2026-01-15 17:30:00'),
(15, 15, 5, '2026-01-15 19:15:00', '2026-01-15 21:30:00'),
(16, 16, 6, '2026-01-15 10:45:00', '2026-01-15 13:00:00'),
(17, 17, 6, '2026-01-15 14:45:00', '2026-01-15 17:00:00'),
(18, 18, 6, '2026-01-15 18:45:00', '2026-01-15 21:00:00'),
(19, 19, 7, '2026-01-15 11:15:00', '2026-01-15 13:30:00'),
(20, 20, 7, '2026-01-15 15:30:00', '2026-01-15 17:45:00'),
(1, 21, 7, '2026-01-15 19:30:00', '2026-01-15 22:00:00'),
(2, 22, 8, '2026-01-15 10:00:00', '2026-01-15 12:30:00'),
(3, 23, 8, '2026-01-15 14:00:00', '2026-01-15 16:30:00'),
(4, 24, 8, '2026-01-15 18:00:00', '2026-01-15 20:30:00'),
(5, 25, 9, '2026-01-15 10:30:00', '2026-01-15 12:45:00'),
(6, 26, 9, '2026-01-15 15:00:00', '2026-01-15 17:15:00'),
(7, 27, 9, '2026-01-15 19:00:00', '2026-01-15 21:00:00'),
(8, 28, 10, '2026-01-15 11:00:00', '2026-01-15 13:20:00'),
(9, 29, 10, '2026-01-15 14:30:00', '2026-01-15 16:50:00'),
(10, 30, 10, '2026-01-15 18:30:00', '2026-01-15 20:45:00');

-- 中厅：10行 x 15列 (schedule_id: 1,4,7,...,28)
INSERT INTO `seat_status` (`schedule_id`, `row`, `col`, `status`, `type`, `price`, `order_id`, `locked_until`)
SELECT
    s.schedule_id,
    r.row_num,
    c.col_num,
    'AVAILABLE',
    CASE WHEN r.row_num BETWEEN 4 AND 7 AND c.col_num BETWEEN 5 AND 11 THEN 'VIP' ELSE 'NORMAL' END,
    CASE WHEN r.row_num BETWEEN 4 AND 7 AND c.col_num BETWEEN 5 AND 11 THEN 80.00 ELSE 50.00 END,
    NULL, NULL
FROM
    (SELECT 1 AS schedule_id UNION SELECT 4 UNION SELECT 7 UNION SELECT 10 UNION SELECT 13
     UNION SELECT 16 UNION SELECT 19 UNION SELECT 22 UNION SELECT 25 UNION SELECT 28) s
CROSS JOIN
    (SELECT 1 AS row_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) r
CROSS JOIN
    (SELECT 1 AS col_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
     UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15) c;

-- 小厅：8行 x 12列 (schedule_id: 2,5,8,...,29)
INSERT INTO `seat_status` (`schedule_id`, `row`, `col`, `status`, `type`, `price`, `order_id`, `locked_until`)
SELECT
    s.schedule_id,
    r.row_num,
    c.col_num,
    'AVAILABLE',
    CASE WHEN r.row_num BETWEEN 3 AND 6 AND c.col_num BETWEEN 4 AND 9 THEN 'VIP' ELSE 'NORMAL' END,
    CASE WHEN r.row_num BETWEEN 3 AND 6 AND c.col_num BETWEEN 4 AND 9 THEN 75.00 ELSE 45.00 END,
    NULL, NULL
FROM
    (SELECT 2 AS schedule_id UNION SELECT 5 UNION SELECT 8 UNION SELECT 11 UNION SELECT 14
     UNION SELECT 17 UNION SELECT 20 UNION SELECT 23 UNION SELECT 26 UNION SELECT 29) s
CROSS JOIN
    (SELECT 1 AS row_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) r
CROSS JOIN
    (SELECT 1 AS col_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
     UNION SELECT 11 UNION SELECT 12) c;

-- 大厅：12行 x 18列 (schedule_id: 3,6,9,...,30)
INSERT INTO `seat_status` (`schedule_id`, `row`, `col`, `status`, `type`, `price`, `order_id`, `locked_until`)
SELECT
    s.schedule_id,
    r.row_num,
    c.col_num,
    'AVAILABLE',
    CASE WHEN r.row_num BETWEEN 5 AND 8 AND c.col_num BETWEEN 6 AND 13 THEN 'VIP' ELSE 'NORMAL' END,
    CASE WHEN r.row_num BETWEEN 5 AND 8 AND c.col_num BETWEEN 6 AND 13 THEN 85.00 ELSE 55.00 END,
    NULL, NULL
FROM
    (SELECT 3 AS schedule_id UNION SELECT 6 UNION SELECT 9 UNION SELECT 12 UNION SELECT 15
     UNION SELECT 18 UNION SELECT 21 UNION SELECT 24 UNION SELECT 27 UNION SELECT 30) s
CROSS JOIN
    (SELECT 1 AS row_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
     UNION SELECT 11 UNION SELECT 12) r
CROSS JOIN
    (SELECT 1 AS col_num UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
     UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
     UNION SELECT 16 UNION SELECT 17 UNION SELECT 18) c;
```

### 3. 配置后端项目

#### 3.1 修改数据库配置

编辑 `src/main/resources/application-dev.yml` 文件，修改数据库连接信息：

```yaml
sky:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    host: localhost          # 数据库主机地址
    port: 3306              # 数据库端口
    database: cinema_system # 数据库名称
    username: root          # 数据库用户名
    password: 1234          # 数据库密码（修改为你的密码）
```

#### 3.2 安装依赖

```bash
mvn clean install
```

### 4. 启动后端服务

#### 方式一：使用 Maven 命令

```bash
mvn spring-boot:run
```

#### 方式二：使用 IDE (IDEA/Eclipse)

1. 导入项目为 Maven 项目
2. 找到主类 `CinemaSeatApplication.java`
3. 右键运行 `Run 'CinemaSeatApplication'`

#### 方式三：打包后运行

```bash
# 打包
mvn clean package -DskipTests

# 运行
java -jar target/Cinema-Seat-Selection-1.0-SNAPSHOT.jar
```

启动成功后，后端服务运行在 `http://localhost:8080`

### 5. 验证服务

使用 Postman 或 curl 测试登录接口：

```bash
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"123456"}'
```

成功返回示例：

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "zhangsan",
    "name": "张三",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "msg": "success"
}
```

## 默认测试账号

系统已预置以下测试账号，可直接使用：

| 用户名 | 密码 | 姓名 |
|--------|------|------|
| zhangsan | 123456 | 张三 |
| lisi | 123456 | 李四 |

## API 接口说明

### 基础 URL
```
http://localhost:8080
```

### 接口列表

| 接口 | 方法 | 说明 | 是否需要Token |
|------|------|------|---------------|
| `/login` | POST | 用户登录 | ❌ |
| `/cinemas` | GET | 查询影院列表 | ✅ |
| `/schedules` | GET | 查询场次列表 | ✅ |
| `/schedules/{scheduleId}/seats` | GET | 查询座位布局 | ✅ |
| `/orders` | POST | 创建订单 | ✅ |
| `/orders/{orderId}/pay` | PUT | 支付订单 | ✅ |
| `/orders/{orderId}/cancel` | PUT | 取消订单 | ✅ |
| `/orders` | GET | 查询用户订单 | ✅ |

### 请求头设置

需要认证的接口请在请求头中携带 Token：

```
Authorization: Bearer <your_token>
```

### 接口使用示例

#### 1. 用户登录

```http
POST /login
Content-Type: application/json

{
  "username": "zhangsan",
  "password": "123456"
}
```

#### 2. 查询场次

```http
GET /schedules?cinemaId=1&date=2026-01-03
Authorization: Bearer <token>
```

#### 3. 查询座位

```http
GET /schedules/1/seats
Authorization: Bearer <token>
```

#### 4. 创建订单

```http
POST /orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "scheduleId": 1,
  "seatIds": [1, 2, 3]
}
```

#### 5. 支付订单

```http
PUT /orders/1/pay
Authorization: Bearer <token>
```

## 项目结构

```
cinema-system/
├── src/main/
│   ├── java/org.example.cinemaseat/
│   │   ├── common/              # 公共模块
│   │   │   ├── Jwt/            # JWT认证相关
│   │   │   ├── BaseContext.java
│   │   │   ├── BusinessException.java
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── Result.java
│   │   ├── controller/         # 控制层（REST API）
│   │   │   ├── UserController.java
│   │   │   ├── CinemaController.java
│   │   │   ├── ScheduleController.java
│   │   │   ├── SeatController.java
│   │   │   └── OrderController.java
│   │   ├── service/           # 业务层
│   │   │   ├── Impl/         # 业务实现
│   │   │   └── *Service.java # 业务接口
│   │   ├── mapper/           # 数据访问层
│   │   └── pojo/             # 实体对象
│   │       ├── entity/       # 数据库实体
│   │       ├── DTO/         # 数据传输对象
│   │       └── VO/          # 视图对象
│   └── resources/
│       ├── mapper/          # MyBatis XML映射文件
│       ├── application.yml  # 主配置文件
│       └── application-dev.yml # 开发环境配置
└── pom.xml                 # Maven依赖配置
```

## 核心功能说明

### 1. JWT 认证机制

- 用户登录成功后返回 JWT Token
- 后续请求需在 Header 中携带 Token
- Token 有效期：7天
- 拦截器自动验证 Token 有效性

### 2. 座位锁定机制

- 创建订单时，选中的座位状态变为 `LOCKED`
- 锁定时间：**15分钟**
- 超时未支付，订单自动取消，座位释放
- 使用悲观锁（`SELECT FOR UPDATE`）防止并发冲突

### 3. 订单状态流转

```
PENDING (待支付)
   ├─> PAID (已支付)
   └─> CANCELLED (已取消)
```

### 4. 定时任务

- 每分钟扫描一次过期订单
- 自动取消超时未支付的订单
- 自动释放锁定的座位

## 常见问题

### 1. 启动时提示数据库连接失败

**解决方法**：
- 检查 MySQL 服务是否启动
- 确认数据库配置信息是否正确
- 检查数据库用户权限

### 2. 提示 "Table doesn't exist"

**解决方法**：
- 确认已创建数据库 `cinema_system`
- 确认已执行所有建表 SQL 语句

### 3. JWT Token 验证失败

**解决方法**：
- 检查请求头是否携带 Token
- 确认 Token 格式：`Authorization: Bearer <token>`
- Token 可能已过期，重新登录获取新 Token

### 4. 座位已被占用

**解决方法**：
- 该座位可能已被其他用户锁定或购买
- 等待 15 分钟后座位会自动释放（如果未支付）
- 选择其他可用座位

## 数据库设计亮点

- ✅ **外键约束**：保证数据引用完整性
- ✅ **索引优化**：针对高频查询场景建立索引
- ✅ **唯一约束**：防止同一场次座位重复（schedule_id + row + col）
- ✅ **事务支持**：订单创建和座位锁定保证原子性
- ✅ **并发控制**：行级锁防止超卖

## 技术亮点

- ✅ **三层架构**：Controller - Service - Mapper 职责清晰
- ✅ **RESTful API**：符合 REST 规范的接口设计
- ✅ **统一响应**：全局异常处理和统一响应格式
- ✅ **无状态认证**：JWT Token 支持水平扩展
- ✅ **定时任务**：自动处理订单超时
- ✅ **悲观锁**：防止高并发场景下的数据冲突

## 开发团队

- **组长**：赵唯达 (学号: 71123219)
- **组员**：邢玉杰 (学号: 71123108)

## 许可证

本项目仅供学习交流使用。

---

如有问题，请联系项目开发团队。
