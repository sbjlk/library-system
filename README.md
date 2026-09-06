# 图书管理系统

基于 **Spring Boot 2.7 + MyBatis-Plus + MySQL + Knife4j (Swagger)** 的图书管理系统，包含用户、图书、借阅记录三张表，实现了登录、图书 CRUD、借书还书接口，并集成 Knife4j 接口文档，启动后可通过 `/doc.html` 直接在线测试所有接口。

## 技术栈

| 技术 | 版本 |
| --- | --- |
| Spring Boot | 2.7.18 |
| MyBatis-Plus | 3.5.3.1 |
| Knife4j (Swagger) | 3.0.3 |
| MySQL | 8.x |
| JWT (jjwt) | 0.11.5 |
| Lombok | 1.18.x |

## 项目结构

```
library-system
├── sql/library.sql                 # 建库建表 + 初始化数据
├── src/main/java/com/example/library
│   ├── LibraryApplication.java     # 启动类
│   ├── common/                     # 统一返回、异常、用户上下文
│   ├── config/                     # MyBatis-Plus / Knife4j / Web 配置
│   ├── interceptor/                # JWT 登录拦截器
│   ├── util/                       # JWT 工具类
│   ├── entity/                     # 实体类 (User / Book / BorrowRecord)
│   ├── mapper/                     # Mapper 接口
│   ├── dto/                        # 请求参数对象
│   ├── vo/                         # 返回视图对象
│   ├── service/                    # 业务接口 + 实现
│   └── controller/                 # 控制器
└── src/main/resources/application.yml
```

## 快速开始

1. **初始化数据库**：在 MySQL 中执行 `sql/library.sql`（会自动创建 `library` 库和三张表，并插入测试数据）。

2. **修改数据库连接**：编辑 `src/main/resources/application.yml`，将 `username` / `password` 改为你自己的 MySQL 账号密码。

3. **启动项目**：在 IDEA 中运行 `LibraryApplication`，或命令行执行：
   ```bash
   mvn spring-boot:run
   ```

4. **访问接口文档**：浏览器打开
   ```
   http://localhost:8080/doc.html
   ```

## 测试账号（密码均为 `123456`）

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| admin | 123456 | ADMIN |
| zhangsan | 123456 | USER |

## 接口测试流程（重要）

除登录/注册外的所有接口都需要携带 JWT 令牌。测试步骤如下：

1. 在 `/doc.html` 中找到「认证管理 → 登录」接口，填入 `admin / 123456` 调用，从返回结果中复制 `data.token`。
2. 点击页面右上角的 **「Authorize」** 按钮，把 token 粘贴进去（直接粘贴 token 即可，也可加 `Bearer ` 前缀），点击授权。
3. 之后即可正常调用图书、借阅等所有接口。

## 接口列表

| 方法 | 路径 | 说明 | 是否需要登录 |
| --- | --- | --- | --- |
| POST | /api/auth/login | 登录，返回 token | 否 |
| POST | /api/auth/register | 注册 | 否 |
| GET | /api/user/info | 获取当前登录用户信息 | 是 |
| GET | /api/book/list | 分页查询图书（支持书名/作者/分类筛选） | 是 |
| GET | /api/book/{id} | 根据 ID 查询图书 | 是 |
| POST | /api/book | 新增图书 | 是 |
| PUT | /api/book/{id} | 修改图书 | 是 |
| DELETE | /api/book/{id} | 删除图书 | 是 |
| POST | /api/borrow/borrow | 借书 | 是 |
| POST | /api/borrow/return | 还书 | 是 |
| GET | /api/borrow/list | 分页查询借阅记录（含用户名/书名） | 是 |
| GET | /api/borrow/my | 查询我的借阅记录 | 是 |

## 统一返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

- `code = 200` 成功；`401` 未登录或 token 过期；`500` 业务/系统异常。

## 常见问题

- **启动报 NPE（springfox）**：`application.yml` 已配置 `spring.mvc.pathmatch.matching-strategy: ant_path_matcher`，请勿删除。
- **密码如何加密**：本项目使用 MD5 加密（`org.springframework.util.DigestUtils`），生产环境建议改用 BCrypt。
- **令牌有效期**：默认 24 小时，可在 `application.yml` 的 `jwt.expire` 调整。
