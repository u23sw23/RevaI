# RevAI - AI智能复习平台
一个基于 Spring Boot + Vue 3 的全栈智能学习平台，集成了 AI 生成笔记、智能考试、间隔重复复习等功能。
## 技术栈
### 后端
- **框架**：Spring Boot 3.2.0
- **ORM**：MyBatis 3.0.3
- **数据库**：MySQL 8.0+
- **Java**：JDK 17+
### 前端
- **框架**：Vue 3.3.4
- **构建工具**：Vite 4.4.9
- **路由**：Vue Router 4.2.5
- **Markdown**：marked 11.1.1
### AI 服务
- **API**：DeepSeek API
- **代理服务**：Node.js + Express

## 环境要求
在开始之前，请确保你的开发环境已安装：
- **JDK 17+**（推荐使用 OpenJDK 或 Oracle JDK）
- **Maven 3.6+**
- **Node.js 16+** 和 **npm 8+**
- **MySQL 8.0+**
- **Git**
## 快速开始
### 1. 克隆项目
```bash
git clone https://github.com/your-username/revai.git
cd revai
```
### 2. 数据库配置
#### 2.1 创建数据库
登录 MySQL，执行以下命令创建数据库：
```sql
CREATE DATABASE demo2 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
#### 2.2 配置数据库连接
编辑 `src/main/resources/application.properties`，修改数据库配置：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/demo2?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的数据库密码
```
>  **提示**：数据库表结构会在项目启动时自动创建（通过 `schema.sql`）

### 3. 配置环境变量
#### 3.1 配置 DeepSeek API Key
项目根目录创建 `.env` 文件（参考 `.env.example`）：
```bash
# Windows
copy .env.example .env
# Linux/Mac
cp .env.example .env
```
编辑 `.env` 文件，填入你的 DeepSeek API Key：
```env
DEEPSEEK_API_KEY=sk-your-api-key-here
```

> **获取 API Key**：访问 [DeepSeek Platform](https://platform.deepseek.com/) 注册并获取 API Key
#### 3.2 配置 AI 代理服务（可选）
如果需要使用 AI 生成功能，还需要配置 AI 代理服务：
```bash
cd server
cp env-example.txt .env
```

编辑 `server/.env` 文件，填入相同的 API Key。

### 4. 启动后端服务
#### 4.1 安装依赖并启动 Spring Boot
```bash
# 安装 Maven 依赖（首次运行）
mvn clean install
# 启动 Spring Boot 应用
mvn spring-boot:run
```

或者使用 IDE 直接运行 `Demo2Application.java`
后端服务将运行在：**http://localhost:8080**

#### 4.2 启动 AI 代理服务（可选）
如果需要使用 AI 生成笔记和考试功能，需要启动 Node.js 代理服务：
```bash
cd server
npm install
npm start
```
AI 代理服务将运行在：**http://localhost:3001**

### 5. 启动前端服务
```bash
cd frontend
npm install
npm run dev
```
前端服务将运行在：**http://localhost:3000**

### 6. 访问应用
打开浏览器访问：**http://localhost:3000**
## 项目结构
```
revai/
├── frontend/                 # Vue 3 前端项目
│   ├── src/
│   │   ├── components/       # 组件
│   │   ├── views/           # 页面视图
│   │   ├── router/          # 路由配置
│   │   └── utils/           # 工具类
│   ├── package.json
│   └── vite.config.js
├── server/                   # AI 代理服务（Node.js）
│   ├── index.js
│   ├── package.json
│   └── .env                 # 环境变量（需自行创建）
├── src/
│   ├── main/
│   │   ├── java/            # Java 源代码
│   │   │   └── com/example/demo/
│   │   │       ├── controller/  # 控制器层
│   │   │       ├── service/     # 服务层
│   │   │       ├── mapper/      # MyBatis Mapper
│   │   │       ├── entity/      # 实体类
│   │   │       └── config/      # 配置类
│   │   └── resources/
│   │       ├── mapper/          # MyBatis XML 映射
│   │       ├── application.properties  # 应用配置
│   │       └── schema.sql      # 数据库初始化脚本
│   └── test/                 # 测试代码
├── .env.example             # 环境变量示例
├── .gitignore
├── pom.xml                  # Maven 配置
└── README.md
```

## 常见问题
### Q: 数据库连接失败？
**A:** 请检查：
1. MySQL 服务是否已启动
2. 数据库名称、用户名、密码是否正确
3. MySQL 端口是否为 3306（默认）

### Q: AI 生成功能无法使用？
**A:** 请确保：
1. 已配置 `DEEPSEEK_API_KEY` 环境变量
2. API Key 有效且有足够余额


## 开发说明
### 数据库迁移

项目使用 Spring Boot 的自动初始化功能，首次启动时会自动执行：
- `schema.sql` - 创建表结构
- `data.sql` - 初始化数据（如果存在）
- `migrate_add_options_to_questions.sql` - 迁移脚本
### API 接口
主要 API 接口：

- **认证**：`/api/auth/register`, `/api/auth/login`
- **用户**：`/api/users/profile`
- **科目**：`/api/subjects`
- **笔记**：`/api/notes`
- **考试**：`/api/exams`
- **群组**：`/api/groups`
- **AI**：`/api/ai/chat`, `/api/ai/generate-note`, `/api/ai/generate-exam`
详细 API 文档请参考代码中的 Controller 注释。
