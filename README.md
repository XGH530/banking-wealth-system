# 🏦 Bank Account System · 银行记账与理财系统

> 一个基于 **Spring Boot 3.2 + MyBatis-Plus + Redis + JWT** 构建的全栈银行核心业务模拟系统，包含账户管理、交易流水、理财产品申购/赎回等完整业务场景，内置响应式前端 SPA。

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-5.7+-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Latest-DC382D?logo=redis&logoColor=white)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5-229639)
![License](https://img.shields.io/badge/License-MIT-green)

---

## ✨ 功能特性

| 模块 | 功能 | 说明 |
|------|------|------|
| 🔐 鉴权 | 注册 / 登录 / 刷新 / 注销 | JWT AccessToken + RefreshToken 双 Token 机制 |
| 💳 账户 | 开户 / 查询 / 冻结 / 解冻 | 支持活期、定期、信用卡等多账户类型 |
| 💵 交易 | 存款 / 取款 / 转账 / 流水查询 | 事务保证 + 双向流水号 + 余额校验 |
| 📦 理财 | 产品列表 / 申购 / 赎回 | 风险等级、年化收益率、期限、额度管理 |
| 📈 持仓 | 持仓查询 / 收益计算 | 预期收益实时计算、到期自动赎回 |
| 🖥️ 前端 | SPA 仪表盘 / 账户卡片 / 交易表单 | Bootstrap 5 + 原生 JS，无前端构建依赖 |
| 📖 文档 | Knife4J / Swagger UI | 自动生成 API 文档，支持在线调试 |

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                     Browser (Chrome/Edge)                    │
│   ┌──────────────┐  ┌────────────────────────────────────┐  │
│   │  SPA 前端     │  │  Knife4j / Swagger API 文档         │  │
│   │  Vanilla JS   │  │  http://localhost:8080/doc.html     │  │
│   └──────┬───────┘  └────────────────────────────────────┘  │
│          │  fetch / Authorization: Bearer xxx                │
└──────────┼───────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────────┐
│                  Spring Boot 3.2.5  (port 8080)              │
│  ┌─────────────┐   ┌──────────────┐   ┌──────────────────┐  │
│  │  Controller │──▶│   Service    │──▶ │  Mapper (MyBatis)│  │
│  │  (RESTful)  │   │  (业务逻辑)  │   │   (SQL 映射)      │  │
│  └─────────────┘   └──────────────┘   └────────┬─────────┘  │
│         │                   │                     │            │
│         ▼                   ▼                     ▼            │
│  ┌──────────────┐   ┌─────────────┐   ┌──────────────────┐  │
│  │JwtInterceptor│   │  Redis 缓存  │   │  MySQL 5.7/8.0  │  │
│  │  (权限校验)   │   │  Lettuce    │   │  HikariCP 连接池  │  │
│  └──────────────┘   └─────────────┘   └──────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

---

## 🗄️ 数据库设计

```
sys_user ──┬── account (1:N, 用户拥有多个账户)
           │          │
           │          └── transaction_record (1:N, 账户产生多条流水)
           │
           └── product_holding (1:N, 用户持有多个理财产品)
                      │
financial_product ────┘ (1:N, 一个产品有多笔持仓)
```

**核心表：**

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `sys_user` | 用户表 | username, password(BCrypt), real_name |
| `account` | 账户表 | account_no, account_type, balance, status |
| `transaction_record` | 交易流水 | txn_no, txn_type, amount, balance_before/after |
| `financial_product` | 理财产品 | product_code, annual_rate, risk_level, term_days |
| `product_holding` | 理财持仓 | holding_no, amount, purchase_date, maturity_date |

建表 SQL 位于 [`src/main/resources/sql/schema.sql`](src/main/resources/sql/schema.sql)

---

## 📁 项目结构

```
bank-account-system/
├── src/main/java/com/bank/account/
│   ├── BankAccountApplication.java     # 启动类
│   ├── controller/                      # REST API 控制层
│   │   ├── AuthController.java
│   │   ├── AccountController.java
│   │   ├── TransactionController.java
│   │   ├── ProductController.java
│   │   └── HoldingController.java
│   ├── service/                         # 业务逻辑层
│   │   ├── (接口)
│   │   └── impl/                        # MyBatis-Plus ServiceImpl
│   ├── mapper/                          # MyBatis 接口
│   ├── entity/                          # 数据库实体
│   ├── dto/                             # 请求参数
│   ├── vo/                              # 响应对象
│   ├── config/                          # 配置类
│   │   ├── JwtInterceptor.java
│   │   ├── JwtUtils.java
│   │   ├── SwaggerConfig.java
│   │   ├── WebMvcConfig.java
│   │   └── MybatisPlusConfig.java
│   └── common/                          # 通用组件
│       ├── Result.java                  # 统一响应体
│       ├── GlobalExceptionHandler.java  # 全局异常处理
│       └── UserContext.java             # 线程安全用户上下文
├── src/main/resources/
│   ├── application.yml                  # 配置文件(支持环境变量)
│   ├── mapper/*.xml                     # MyBatis XML
│   ├── sql/schema.sql                   # 数据库初始化脚本
│   └── static/                          # 前端 SPA
│       ├── index.html                   # 主界面
│       ├── login.html                   # 登录页
│       └── assets/
│           ├── css/theme.css
│           └── js/{api,app,pages}.js
└── pom.xml
```

---

## 🛠️ 技术栈

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 17 | LTS，Records/Pattern Matching |
| 框架 | Spring Boot | 3.2.5 | Web / Validation / AOP |
| ORM | MyBatis-Plus | 3.5.5 | 单表 CRUD / 分页插件 / 乐观锁 |
| 数据库 | MySQL | 5.7+ / 8.0+ | HikariCP 连接池 |
| 缓存 | Redis | - | Lettuce 客户端 |
| 鉴权 | JWT (jjwt) | 0.12.5 | HS512 + Access/Refresh 双 Token |
| 加密 | Spring Crypto | - | BCryptPasswordEncoder |
| API 文档 | Knife4j | 4.5.0 | OpenAPI 3 / 中文界面 |
| JSON | Jackson | 2.x | Spring Boot 默认 |
| 前端 | Bootstrap 5 | 5.3 | CDN 引入，无需构建 |

---

## 🚀 快速开始

### 环境要求

- **JDK 17** 或更高
- **Maven 3.8+**
- **MySQL 5.7+ / 8.0+**（密码设为 `root` 或自行修改配置）
- **Redis**（默认端口 6379，无密码）

### 启动步骤

```bash
# 1. 克隆项目
git clone https://github.com/YOUR_USERNAME/bank-account-system.git
cd bank-account-system

# 2. 创建数据库并导入
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS bank_account DEFAULT CHARSET utf8mb4;"
mysql -u root -p bank_account < src/main/resources/sql/schema.sql

# 3. 启动 Redis (Windows)
redis-server

# 4. 启动后端
mvn spring-boot:run
```

> 如需自定义数据库密码，可通过环境变量覆盖：
> ```bash
> # Windows CMD
> set DB_PASSWORD=your_password
> mvn spring-boot:run
> 
> # Windows PowerShell
> $env:DB_PASSWORD = "your_password"
> mvn spring-boot:run
> 
> # Linux / macOS
> DB_PASSWORD=your_password mvn spring-boot:run
> ```

### 默认账号

| 用户名 | 密码 | 真实姓名 |
|--------|------|----------|
| zhangsan | 123456 | 张三 |
| lisi | 123456 | 李四 |
| wangwu | 123456 | 王五 |

### 访问地址

| 页面 | URL |
|------|-----|
| 🌐 前端主界面 | http://localhost:8080/index.html |
| 🔐 登录页 | http://localhost:8080/login.html |
| 📖 Knife4j 文档 | http://localhost:8080/doc.html |
| 📄 Swagger UI | http://localhost:8080/swagger-ui.html |

---

## 📡 API 接口清单

### 鉴权 `/api/auth`

| 方法 | 路径 | 说明 | 需鉴权 |
|------|------|------|--------|
| POST | `/api/auth/register` | 用户注册 | ❌ |
| POST | `/api/auth/login` | 用户登录（返回双 Token） | ❌ |
| POST | `/api/auth/refresh` | 刷新 Token | ❌ |
| POST | `/api/auth/logout` | 注销 | ✅ |

### 账户 `/api/accounts`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/accounts` | 当前用户账户列表（分页） |
| GET | `/api/accounts/{accountNo}` | 账户详情 |
| POST | `/api/accounts` | 开户 |
| PUT | `/api/accounts/{id}/status` | 冻结/解冻 |
| DELETE | `/api/accounts/{id}` | 销户 |

### 交易 `/api/transactions`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/transactions` | 交易流水查询（分页） |
| POST | `/api/transactions/deposit` | 存款 |
| POST | `/api/transactions/withdraw` | 取款 |
| POST | `/api/transactions/transfer` | 转账（支持跨账户） |

### 理财 `/api/products` + `/api/holdings`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/products` | 产品列表 |
| GET | `/api/products/{id}` | 产品详情 |
| POST | `/api/holdings/purchase` | 申购 |
| POST | `/api/holdings/redeem` | 赎回 |
| GET | `/api/holdings` | 我的持仓 |

---

## 🧱 核心设计要点

### 1. 统一响应体

```java
public class Result<T> {
    private int code;         // 200=成功, 401=未登录, 403=无权限, 500=服务异常
    private String message;
    private T data;
}
```

前端所有 fetch 都封装在 `API.request()` 里，自动处理 401 跳转登录页。

### 2. 全局异常处理

`GlobalExceptionHandler` 统一捕获：
- `BusinessException`（业务异常，返回自定义 code）
- `MethodArgumentNotValidException`（参数校验失败）
- 其他 `Exception`（兜底 500）

### 3. 线程安全的用户上下文

`UserContext` 用 `ThreadLocal<LoginUser>` 存储当前用户信息，`JwtInterceptor` 在 preHandle 时解析 Token 写入，afterCompletion 时清理，完美适配 Servlet 线程模型。

### 4. 交易事务

存款/取款/转账方法加 `@Transactional`，确保"扣钱→记账→加钱"要么全部成功，要么全部回滚。转账操作生成双向流水号便于对账。

### 5. 前端 SPA 无构建

纯 HTML + Bootstrap 5 + 原生 JS，通过 Spring Boot 静态资源直接托管。好处是零构建门槛，JAR 包打完就能跑。

---

## 📄 License

MIT License © 2026
