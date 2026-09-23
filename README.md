# 图书管理系统（library-system）

基于 **Spring Boot + MyBatis-Plus + MySQL + Knife4j** 的图书借阅后端服务，包含用户、图书、借阅记录三张核心表，实现了注册登录、图书管理、借还书、借阅记录查询与**基于角色的接口级权限控制**。

启动后可通过 `http://localhost:8080/doc.html` 在线调试全部接口。

## 技术栈

| 技术 | 版本 | 说明 |
| --- | --- | --- |
| Spring Boot | 2.7.18 | Web 框架 |
| MyBatis-Plus | 3.5.3.1 | 持久层 + 分页插件 |
| MySQL | 8.x | 数据库 |
| JWT (jjwt) | 0.11.5 | 无状态认证（HS256） |
| Knife4j | 3.0.3 | 接口文档 |
| Lombok | 1.18.x | 简化样板代码 |
| PBKDF2 (JDK 原生) | - | 密码加盐哈希 |

## 项目结构

```
library-system
├── sql/library.sql                        # 建库建表 + 索引 + 初始化数据
├── src/main/java/com/example/library
│   ├── LibraryApplication.java            # 启动类
│   ├── annotation/RequireRole.java        # 接口级角色要求注解
│   ├── common/                            # 统一响应、异常、用户上下文
│   │   ├── Result.java / ResultCode.java
│   │   ├── BusinessException.java
│   │   ├── GlobalExceptionHandler.java
│   │   └── UserContext.java               # ThreadLocal 用户上下文
│   ├── config/                             # MyBatis-Plus / Knife4j / Web / 密码加密器
│   ├── interceptor/JwtInterceptor.java     # JWT 认证 + 角色授权
│   ├── util/JwtUtil.java / PasswordUtil.java
│   ├── entity/ mapper/ dto/ vo/ service/ controller/
└── src/main/resources/application.yml
```

## 快速开始

1. **初始化数据库**：在 MySQL 中执行 `sql/library.sql`（自动创建 `library` 库、三张表、索引与测试数据）。

2. **配置数据库连接**：所有敏感配置都通过环境变量注入，`application.yml` 中不含真实凭据。三选一：
   - 设置环境变量：`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`
   - 或新建 `src/main/resources/application-local.yml` 覆盖（**该文件已加入 `.gitignore`，不会被提交**）：
     ```yaml
     spring:
       datasource:
         username: your_username
         password: your_password
     jwt:
       secret: your-local-random-secret-at-least-32-bytes
     ```
   - 或直接修改 `application.yml` 中的默认占位值（**请勿提交真实密码**）

   > `jwt.secret` 若短于 32 字节，`Keys.hmacShaKeyFor` 会抛 `WeakKeyException`；
   > 本项目在 `JwtUtil` 中做了 SHA-256 派生的兜底处理，但仍建议配置足够长度的随机密钥。

3. **启动**：IDEA 中运行 `LibraryApplication`，或命令行 `mvn spring-boot:run`。

4. **接口文档**：浏览器打开 `http://localhost:8080/doc.html`。

## 测试账号

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| admin | 123456 | ADMIN |
| zhangsan | 123456 | USER |

> 库中存储的是历史 MD5 摘要。**首次登录成功后会自动升级为 PBKDF2 加盐哈希**，无需手动处理。

## 认证与授权设计

### 一、整体流程

```
请求 → JwtInterceptor.preHandle
        ├─ 1. 认证：解析 Authorization 头中的 token（支持 "Bearer " 前缀）
        │      → 校验签名与有效期 → 把 userId / username / role 写入 UserContext(ThreadLocal)
        └─ 2. 授权：读取目标方法（或所在类）上的 @RequireRole 注解
               → 角色命中则放行，否则返回 403 业务码
请求处理完成 → JwtInterceptor.afterCompletion → UserContext.clear()
```

### 二、接口权限一览

| 接口 | 权限要求 |
| --- | --- |
| `POST /api/auth/login`、`POST /api/auth/register` | 无需登录（拦截器放行） |
| `GET /api/book/list`、`GET /api/book/{id}` | 登录即可 |
| `POST/PUT/DELETE /api/book/**` | **ADMIN** |
| `POST /api/borrow/borrow`、`POST /api/borrow/return` | 登录即可（普通用户只能操作自己的记录） |
| `GET /api/borrow/list` | **ADMIN**（查询全部借阅记录） |
| `GET /api/borrow/my` | 登录即可（查询自己的借阅记录） |
| `GET /api/user/info` | 登录即可 |

拦截器注册路径：`/api/**`，放行 `POST /api/auth/login`、`POST /api/auth/register`。

### 三、关键设计说明（面试可讲）

**1）为什么用"条件更新"扣减库存，而不是"先查再改"或悲观锁**

借书扣减的 SQL 是：

```sql
UPDATE book SET available_count = available_count - 1
WHERE id = ? AND available_count > 0
```

- 「先 `SELECT` 判断库存 > 0，再 `UPDATE` 减 1」在并发下两个线程可能都读到 1，导致超借（check-then-act 竞态）。
- 把判断条件写进 `UPDATE` 的 `WHERE`，由 InnoDB 行锁保证互斥，**受影响行数为 0 即代表库存已被抢完**，一条 SQL 同时完成"校验 + 扣减"。
- 相比 `SELECT ... FOR UPDATE` 悲观锁，这种方式锁持有时间更短、无需显式加锁，且在"读多写少"场景下不阻塞普通查询。
- 代价：`available_count` 是热点行，极高并发下会成为瓶颈，届时可改用 Redis 预扣减或分段库存（本项目未实现）。

**2）`ThreadLocal` 的坑与清理**

`UserContext` 用 `ThreadLocal` 承载当前用户，必须在请求结束时 `remove()`：

- Tomcat 使用线程池复用线程，若只 `set` 不 `remove`，**下一个请求复用同一线程时会读到上一个用户的身份**（用户数据串号，属于越权级别的 bug）。
- 本项目在 `afterCompletion` 中统一清理。该方法在**正常返回和抛异常两种路径下都会执行**，因此比在 Controller 里写 `try/finally` 更可靠。

**3）角色为什么同时存在于 token 和数据库**

- token 中的 `role` 用于拦截器的**接口级**快速鉴权，避免每个请求都查库。
- 但 token 一旦签发就无法撤销，**用户角色变更后旧 token 仍携带旧角色**。因此涉及"数据归属"的判断（如还书校验）以**数据库中的最新角色**为准，避免越权。
- 生产环境的完整解法是缩短 token 有效期 + refresh token + 登出黑名单（Redis），本项目未实现。

**4）密码存储的演进**

- 历史版本使用 MD5。**MD5 是快速摘要而非密码哈希**：无盐、计算极快（GPU 每秒数十亿次）、同一密码摘要固定，彩虹表可直接反查。
- 现改为 **PBKDF2-HMAC-SHA256**（随机盐 16 字节、10 万次迭代、256 位输出），存储格式 `pbkdf2$迭代次数$盐$哈希`，自描述、便于将来升级算法。
- 比较时使用 `MessageDigest.isEqual` 做**恒定时间比较**，避免通过响应耗时逐字节推测密码。
- **平滑迁移**：登录时若检测到旧格式（32 位 MD5），校验通过后自动重写为新哈希，用户无感知。

## 接口列表

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | /api/auth/login | 登录，返回 token | 公开 |
| POST | /api/auth/register | 注册 | 公开 |
| GET | /api/user/info | 获取当前登录用户信息 | 登录 |
| GET | /api/book/list | 分页查询图书（支持书名/作者/分类筛选） | 登录 |
| GET | /api/book/{id} | 根据 ID 查询图书 | 登录 |
| POST | /api/book | 新增图书 | ADMIN |
| PUT | /api/book/{id} | 修改图书 | ADMIN |
| DELETE | /api/book/{id} | 删除图书 | ADMIN |
| POST | /api/borrow/borrow | 借书 | 登录 |
| POST | /api/borrow/return | 还书（仅限本人记录，管理员可代还） | 登录 |
| GET | /api/borrow/list | 分页查询全部借阅记录（含用户名/书名） | ADMIN |
| GET | /api/borrow/my | 查询我的借阅记录 | 登录 |

## 接口测试流程

除登录/注册外的所有接口都需要携带 JWT 令牌：

1. 在 `/doc.html` 中找到「认证管理 → 登录」，填入 `admin / 123456` 调用，复制返回结果中的 `data.token`。
2. 点击页面右上角 **「Authorize」**，粘贴 token（可加 `Bearer ` 前缀），确认授权。
3. 之后即可调用所有接口。

**权限验证实验**（建议亲手试一遍）：

- 用 `zhangsan` 登录拿到 token，调用 `DELETE /api/book/1` → 返回 `{"code":403,"message":"无权限"}`。
- 用 `zhangsan` 调用 `GET /api/borrow/list` → 同样返回 403。
- 用 `zhangsan` 调用 `GET /api/borrow/my` → 正常返回自己的借阅记录。

## 统一返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| code | 含义 |
| --- | --- |
| 200 | 成功 |
| 400 | 业务规则拒绝（库存不足、重复借阅、用户名已存在等）或参数校验失败 |
| 401 | 未登录或 token 无效/过期 |
| 403 | 已登录但无权限 |
| 500 | 系统异常（未预期的服务端错误） |

> 说明：本项目所有异常（含未登录、无权限）统一以 HTTP 200 + 业务码返回，由前端根据 `code` 判断。
> 好处是前端只需处理一套响应结构；代价是无法利用 HTTP 语义与浏览器/网关的默认行为（如 401 自动跳登录）。
> 若要改为标准 HTTP 语义，需在拦截器与全局异常处理中显式设置 `response.setStatus(...)`。

## 数据库设计

**表结构**

| 表 | 关键字段 | 说明 |
| --- | --- | --- |
| `user` | id, username(唯一索引), password, nickname, role | 角色 USER / ADMIN |
| `book` | id, title, author, isbn(唯一索引), category, total_count, available_count, status | status: 1 上架 / 0 下架 |
| `borrow_record` | id, user_id, book_id, borrow_time, due_time, return_time, status | status: 0 借出中 / 1 已归还 |

**索引设计**

| 索引 | 服务的查询 |
| --- | --- |
| `uk_username` (user.username) | 登录、注册查重 |
| `uk_isbn` (book.isbn) | ISBN 唯一性约束 |
| `idx_user_status_id` (borrow_record: user_id, status, id) | 「我的借阅记录」：`WHERE user_id=? AND status=? ORDER BY id DESC` |
| `idx_book_status` (borrow_record: book_id, status) | 删除图书前校验是否还有未归还记录 |

### EXPLAIN 执行计划对比

> 请在自己机器上执行下列语句，把真实输出填进表格后再写进简历。**不要写没有实测过的数字。**

未加 `idx_user_status_id` 前：

```sql
EXPLAIN SELECT r.id, r.user_id, r.book_id, r.status
FROM borrow_record r
LEFT JOIN `user` u ON r.user_id = u.id
LEFT JOIN book b ON r.book_id = b.id
WHERE r.user_id = 1 AND r.status = 0
ORDER BY r.id DESC;
```

| 字段 | 加索引前 | 加索引后 |
| --- | --- | --- |
| type | （待填） | （待填） |
| key | （待填，预期 `idx_user_id`） | （待填，预期 `idx_user_status_id`） |
| rows | （待填） | （待填） |
| Extra | （待填，预期含 `Using filesort`） | （待填，预期 filesort 消失） |

**压测建议**：用 JMeter 或 wrk 对 `POST /api/borrow/borrow` 施压（例如 200 线程、库存设为 10），
验证最终 `available_count = 0` 且未归还借阅记录数恰好为 10（即"零超借"）。把实际数字记录下来。

## 已知不足与后续改进方向

| 项 | 现状 | 计划 |
| --- | --- | --- |
| 缓存 | 无。图书列表每次直连数据库 | 引入 Redis 缓存热门图书列表，并处理缓存穿透（空值缓存）、击穿（互斥重建）、雪崩（过期时间加随机） |
| 登出 | JWT 无状态，签发后无法主动失效 | 引入 Redis 黑名单或改为双 token（access + refresh） |
| 热点库存 | 单行 `available_count` 条件更新，极高并发下是瓶颈 | 评估悲观锁 / Redis 预扣减 / 分段库存的吞吐差异 |
| 测试 | 无单元测试与 CI | 补借书、还书、库存不足、重复借阅等关键路径的单测 |
| 技术栈版本 | JDK 8 + Spring Boot 2.7（开源支持已结束） | 升级至 JDK 17 + Spring Boot 3.x（`javax.*` → `jakarta.*`） |
| 接口返回 | 全部以 HTTP 200 + 业务码返回 | 评估改为标准 HTTP 状态码 |
| CORS | `allowedOriginPatterns("*") + allowCredentials(true)` | 生产环境收敛为白名单域名 |

## 常见问题

- **启动报 NPE（springfox）**：`application.yml` 已配置 `spring.mvc.pathmatch.matching-strategy: ant_path_matcher`，请勿删除（springfox 3.0 在 Spring Boot 2.6+ 的已知兼容问题）。
- **登录返回 500 且日志提示 `WeakKeyException`**：`jwt.secret` 长度不足 32 字节，请配置足够长的随机密钥。
- **返回 403**：当前账号角色与接口要求不符（例如用普通用户调用图书管理接口）。
- **启动报 `Access denied for user 'your_username'@'localhost'`**：
  说明 `application.yml` 中的占位符未被覆盖。请创建 `application-local.yml`
  （已加入 .gitignore）配置真实账号密码，并激活 `local` profile
  （`--spring.profiles.active=local` 或 IDEA 的 Active profiles）。
