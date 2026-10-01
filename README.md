# 校园拼车平台（oh-my-taxi）

> 基于 **Java + Spring Boot 3 + MySQL** 开发的简易校园拼车系统，配套原生 HTML/CSS/JavaScript 前端。

一个面向校园场景的拼车平台，支持学生、司机、管理员三种角色，覆盖从注册登录、行程发布与搜索、拼车申请与审批、在线支付到行程评价的完整业务闭环。

[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

---

## ✨ 功能特性

- 🔐 **认证与权限**：基于 JWT 的无状态认证，BCrypt 密码加密，角色分级（学生 / 司机 / 管理员）
- 🚗 **车辆管理**：司机可添加、查看、修改、删除自己的车辆
- 🗺️ **行程管理**：司机发布行程，所有人可按出发地 / 目的地 / 日期搜索行程
- 🤝 **拼车申请**：学生申请拼车，司机审批（接受 / 拒绝），支持多座位预订
- 💰 **在线支付**：模拟支付流程，实时统计司机收入
- ⭐ **双向评价**：行程完成后参与者可互相评分与评论
- 🛠️ **管理后台**：管理员仪表盘统计、用户 / 车辆 / 行程的增删改查
- 📚 **接口文档**：集成 SpringDoc OpenAPI（Swagger UI）

---

## 🧱 技术栈

| 分类 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot 3.2.5 |
| 语言 | Java 17 |
| 持久层 | Spring Data JPA + Hibernate |
| 数据库 | MySQL 8.0 |
| 数据库迁移 | Flyway |
| 安全认证 | Spring Security + JWT（jjwt 0.12.5） |
| 接口文档 | SpringDoc OpenAPI 2.5.0 |
| 对象映射 | MapStruct 1.5.5 |
| 代码简化 | Lombok 1.18.30 |
| 前端 | HTML + CSS + JavaScript（Bootstrap 5 + Axios + FontAwesome） |
| 构建工具 | Maven |

---

## 🗂️ 项目结构

```
carpool-boot
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/carpool
│   │   │   ├── CarpoolApplication.java     # 启动类
│   │   │   ├── config/                     # 配置（Security / CORS / OpenAPI）
│   │   │   ├── controller/                 # REST 控制器
│   │   │   ├── dto/                        # 请求 / 响应 DTO
│   │   │   ├── entity/                     # JPA 实体
│   │   │   ├── enums/                      # 枚举类型
│   │   │   ├── exception/                  # 异常与全局处理
│   │   │   ├── repository/                 # 数据访问层
│   │   │   ├── security/                   # JWT 过滤器与工具
│   │   │   └── service/                    # 业务逻辑层
│   │   └── resources
│   │       ├── application.yml             # 应用配置
│   │       ├── db/migration/               # Flyway 建表脚本
│   │       └── static/                     # 前端静态页面
│   └── test                                # 测试代码
└── .gitignore
```

---

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+

### 1. 准备数据库

项目使用 Flyway 自动建表，只需创建数据库连接账号即可。默认配置如下：

| 配置项 | 默认值 |
| --- | --- |
| 数据库地址 | `jdbc:mysql://localhost:3306/carpool_db` |
| 用户名 | `carpool_user` |
| 密码 | `123123` |

可通过环境变量 `DB_USERNAME`、`DB_PASSWORD` 覆盖：

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
```

> 💡 Flyway 会在启动时自动创建 `carpool_db` 数据库及所有表，并插入默认管理员账号。

### 2. 启动应用

```bash
mvn spring-boot:run
```

启动成功后访问：

- 前端页面：<http://localhost:8080/>
- Swagger 接口文档：<http://localhost:8080/swagger-ui.html>
- 健康检查：<http://localhost:8080/health>

### 3. 默认账号

| 角色 | 手机号 | 密码 |
| --- | --- | --- |
| 管理员 | `13800000000` | `admin123` |

> 学生与司机账号可在前端页面自行注册。

---

## 🔑 环境变量配置

| 变量名 | 默认值 | 说明 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` | 激活的 Spring Profile |
| `SERVER_PORT` | `8080` | 服务端口 |
| `DB_USERNAME` | `carpool_user` | 数据库用户名 |
| `DB_PASSWORD` | `123123` | 数据库密码 |
| `JWT_SECRET` | 内置默认值 | JWT 签名密钥（生产环境务必修改） |

---

## 📚 接口概览

### 认证接口 `/api/users`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/users/register` | 用户注册 |
| POST | `/api/users/login` | 用户登录 |
| GET | `/api/users/me` | 查看个人信息 |
| GET | `/api/users/{id}` | 查看指定用户 |
| PUT | `/api/users/{id}` | 修改个人信息 |

### 行程接口 `/api/trips`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/trips` | 搜索行程（公开） |
| GET | `/api/trips/{id}` | 行程详情 |
| POST | `/api/trips` | 发布行程（司机） |
| PUT | `/api/trips/{id}` | 修改行程状态（司机） |
| DELETE | `/api/trips/{id}` | 删除行程（司机） |
| GET | `/api/trips/driver` | 查看我的行程 |

### 车辆接口 `/api/vehicles`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/vehicles` | 添加车辆（司机） |
| GET | `/api/vehicles/driver` | 查看我的车辆 |
| GET | `/api/vehicles/{id}` | 车辆详情 |
| PUT | `/api/vehicles/{id}` | 修改车辆 |
| DELETE | `/api/vehicles/{id}` | 删除车辆 |

### 拼车接口 `/api/participants`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/participants/trip/{tripId}` | 申请加入行程 |
| GET | `/api/participants/trips/{tripId}/applications` | 申请列表（司机） |
| GET | `/api/participants/trips/{tripId}/passengers` | 乘客与收入（司机） |
| PUT | `/api/participants/{id}/status` | 审批申请（司机） |
| GET | `/api/participants/student` | 查看我的申请 |
| DELETE | `/api/participants/{id}` | 取消申请 |
| POST | `/api/participants/{id}/pay` | 支付 |

### 评分接口 `/api/ratings`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/ratings/trip` | 创建评价 |
| GET | `/api/ratings/trip/{tripId}` | 查看行程评价 |
| GET | `/api/ratings/user/{userId}` | 查看用户评价 |

### 管理后台 `/api/admin`（仅管理员）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/stats` | 仪表盘统计 |
| GET/POST | `/api/admin/users` | 用户列表 / 创建用户 |
| PUT/DELETE | `/api/admin/users/{id}` | 修改 / 删除用户 |
| GET/POST | `/api/admin/vehicles` | 车辆列表 / 添加车辆 |
| PUT/DELETE | `/api/admin/vehicles/{id}` | 修改 / 删除车辆 |
| GET | `/api/admin/trips` | 行程列表 |
| PUT/DELETE | `/api/admin/trips/{id}` | 修改 / 删除行程 |

---

## 🗃️ 数据库设计

| 表名 | 说明 | 关键字段 |
| --- | --- | --- |
| `users` | 用户 | 学号、姓名、手机号、密码、角色、评分 |
| `vehicles` | 车辆 | 车牌号、品牌、型号、颜色、座位数 |
| `trips` | 行程 | 出发/到达地、时间、座位、价格、状态 |
| `participants` | 拼车申请 | 行程、用户、座位数、审批状态、支付状态 |
| `ratings` | 评分 | 评价人、被评人、行程、分数、评论 |

---

## 📦 构建与打包

```bash
# 编译并运行测试
mvn clean test

# 打包为可执行 JAR
mvn clean package

# 运行打包产物
java -jar target/carpool-boot-1.0.0.jar
```

---

## 📄 License

本项目仅用于学习交流，采用 [MIT License](LICENSE) 开源。
