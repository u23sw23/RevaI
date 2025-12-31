# 5分钟快速开始指南
## 前置条件检查
```bash
# 检查 Java 版本（需要 17+）
java -version

# 检查 Maven 版本
mvn -version

# 检查 Node.js 版本（需要 16+）
node -v
npm -v

# 检查 MySQL 是否运行
mysql --version
```

## 一键启动脚本
```powershell
# 1. 配置环境变量
copy .env.example .env
# 编辑 .env 文件，填入 DEEPSEEK_API_KEY

# 2. 创建数据库
mysql -u root -p
# 执行: CREATE DATABASE demo2 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 3. 配置数据库密码
# 编辑 src/main/resources/application.properties，修改 spring.datasource.password

# 4. 启动后端
mvn spring-boot:run

# 5. 新开终端，启动 AI 服务
cd server
npm install
npm start

# 6. 新开终端，启动前端
cd frontend
npm install
npm run dev
```

### Linux/Mac

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env 文件，填入 DEEPSEEK_API_KEY

# 2. 创建数据库
mysql -u root -p -e "CREATE DATABASE demo2 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. 配置数据库密码
# 编辑 src/main/resources/application.properties，修改 spring.datasource.password

# 4. 启动后端（后台运行）
mvn spring-boot:run &

# 5. 启动 AI 服务（后台运行）
cd server && npm install && npm start &

# 6. 启动前端
cd frontend && npm install && npm run dev
```

