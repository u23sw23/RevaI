# Demo2 - Spring Boot + Vue 全栈项目

## 项目结构

```
demo2/
├── frontend/              # Vue 3 前端项目
│   ├── src/
│   │   ├── components/   # 组件
│   │   ├── views/        # 页面视图
│   │   ├── router/       # 路由配置
│   │   └── utils/        # 工具类
│   ├── package.json
│   └── vite.config.js
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/demo/
│   │   │       ├── controller/  # 控制器层
│   │   │       ├── service/     # 服务层
│   │   │       ├── mapper/      # MyBatis Mapper接口
│   │   │       ├── entity/      # 实体类
│   │   │       └── config/      # 配置类
│   │   └── resources/
│   │       ├── mapper/          # MyBatis XML映射文件
│   │       ├── application.properties
│   │       └── schema.sql       # 数据库初始化脚本
│   └── test/
└── pom.xml
```

## 功能模块

### 1. 用户认证
- 用户注册 (`POST /api/auth/register`)
- 用户登录 (`POST /api/auth/login`)

### 2. 用户管理
- 获取用户信息 (`GET /api/users/profile?userId=xxx`)
- 更新用户信息 (`PUT /api/users/profile?userId=xxx`)
- 搜索用户 (`GET /api/users?q=keyword`)

### 3. 科目管理
- 获取所有科目 (`GET /api/subjects?userId=xxx`) 有?用requestParam接收
- 获取科目详情 (`GET /api/subjects/{id}`) 有{}用pathvariable接收
- 创建科目 (`POST /api/subjects`) 
- 更新科目 (`PUT /api/subjects/{id}`)
- 删除科目 (`DELETE /api/subjects/{id}`)

### 4. 笔记管理
- 获取笔记详情 (`GET /api/notes/{id}`)
- 创建笔记 (`POST /api/notes`)
- 上传文件生成笔记 (`POST /api/notes/upload`)

### 5. 群组管理
- 获取用户群组列表 (`GET /api/groups?userId=xxx`)
- 搜索群组 (`GET /api/groups?name=xxx`)
- 创建群组 (`POST /api/groups?userId=xxx`)
- 加入群组 (`POST /api/groups/join?userId=xxx`)

## 数据库配置

### 1. 创建数据库

执行 `src/main/resources/schema.sql` 文件创建数据库和表结构。

或者手动创建：

```sql
CREATE DATABASE demo2 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后执行 `schema.sql` 中的表创建语句。

### 2. 配置数据库连接

编辑 `src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/demo2?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的数据库密码
```

## 运行项目

### 后端（Spring Boot）

1. 确保 MySQL 数据库已启动并创建了 `demo2` 数据库
2. 修改 `application.properties` 中的数据库配置
3. 运行 `Demo2Application.java` 或使用 Maven：
   ```bash
   mvn spring-boot:run
   ```
4. 后端服务运行在 `http://localhost:8080`

### 前端（Vue）

1. 进入前端目录：
   ```bash
   cd frontend
   ```

2. 安装依赖：
   ```bash
   npm install
   ```

3. 启动开发服务器：
   ```bash
   npm run dev
   ```
4. 前端服务运行在 `http://localhost:3000`

## API 接口说明

### 认证接口

#### 注册
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456"
}
```

#### 登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456"
}
```

### 用户接口

#### 获取用户信息
```http
GET /api/users/profile?userId=1
```

#### 更新用户信息
```http
PUT /api/users/profile?userId=1
Content-Type: application/json

{
  "email": "test@example.com",
  "bio": "个人简介"
}
```

#### 搜索用户
```http
GET /api/users?q=keyword
```

### 科目接口

#### 获取所有科目
```http
GET /api/subjects?userId=1
```

#### 创建科目
```http
POST /api/subjects
Content-Type: application/json

{
  "name": "数学",
  "description": "数学科目",
  "userId": 1
}
```

### 笔记接口

#### 获取笔记详情
```http
GET /api/notes/1
```

#### 创建笔记
```http
POST /api/notes
Content-Type: application/json

{
  "title": "函数与极限",
  "content": "笔记内容...",
  "subjectId": 1,
  "userId": 1
}
```

### 群组接口

#### 获取用户群组
```http
GET /api/groups?userId=1
```

#### 搜索群组
```http
GET /api/groups?name=群组名称
```

#### 创建群组
```http
POST /api/groups?userId=1
Content-Type: application/json

{
  "name": "学习小组",
  "memberIds": [2, 3, 4]
}
```

#### 加入群组
```http
POST /api/groups/join?userId=1
Content-Type: application/json

{
  "groupId": 1,
  "groupName": "学习小组"
}
```

## 注意事项

1. **用户认证**：当前实现使用简单的 MD5 密码加密，生产环境应使用 BCrypt 等更安全的加密方式
2. **Token 管理**：当前返回的是模拟 token，实际项目应使用 JWT 进行身份认证
3. **用户ID传递**：部分接口需要 `userId` 参数，实际项目中应从 JWT token 中解析获取
4. **文件上传**：笔记上传功能（`/api/notes/upload`）当前为模拟实现，需要集成文件存储和 AI 服务
5. **CORS 配置**：已在 Controller 中添加 `@CrossOrigin`，确保前后端可以正常通信

## 技术栈

- **后端**：Spring Boot 3.2.0, MyBatis, MySQL
- **前端**：Vue 3, Vue Router, Vite
- **数据库**：MySQL 8.0+

## 开发建议

1. 添加 JWT 认证机制
2. 实现文件上传功能
3. 集成 AI 服务生成笔记
4. 添加异常处理和统一响应格式
5. 添加参数验证（使用 Bean Validation）
6. 添加单元测试和集成测试

