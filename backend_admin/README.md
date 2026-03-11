# 管理后台系统

统一的管理端系统，包含完整的后端API和前端界面。

## 项目结构

```
backend_admin/
├── backend/          # Spring Boot 后端应用
└── frontend/         # Vue 3 前端应用
```

## 技术栈

### 后端
- Spring Boot 3.1.5
- MyBatis Plus 3.5.3
- MySQL 8.0
- Redis
- JWT认证

### 前端
- Vue 3 + Composition API
- Vite 4.3.0
- Vue Router 4.2.0
- Pinia 3.0.4
- Element Plus
- Axios

## 功能模块

- ✅ 视频审核：待审核视频列表、审核通过/拒绝
- ✅ 视频管理：视频列表、搜索、筛选、删除
- ✅ 标签管理：标签CRUD操作
- ✅ 分类管理：分类树形结构、父子分类管理
- ✅ 用户管理：用户列表、状态管理、角色管理
- ✅ 仪表盘：系统统计信息

## 启动说明

### 后端启动

1. 确保MySQL和Redis已启动
2. 修改 `backend/src/main/resources/application.yml` 中的数据库配置
3. 进入 `backend` 目录
4. 运行：
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
5. 后端服务将在 `http://localhost:8087` 启动

### 前端启动

1. 进入 `frontend` 目录
2. 安装依赖：
   ```bash
   npm install
   ```
3. 启动开发服务器：
   ```bash
   npm run dev
   ```
4. 前端应用将在 `http://localhost:3001` 启动

## 使用说明

1. 访问前端地址：`http://localhost:3001`
2. 使用管理员账号登录（role='admin'）
3. 登录成功后进入管理后台

## API接口

所有API接口都需要JWT Token认证（除了登录接口）。

- 登录：`POST /api/auth/login`
- 统计：`GET /api/admin/stats`
- 视频审核：`/api/admin/audit/*`
- 视频管理：`/api/admin/videos/*`
- 标签管理：`/api/admin/tags/*`
- 分类管理：`/api/admin/categories/*`
- 用户管理：`/api/admin/users/*`

## 注意事项

1. 确保数据库表已创建（使用主项目的SQL脚本）
2. 需要至少一个role='admin'的用户才能登录管理后台
3. 后端独立运行在8087端口，不经过Gateway网关
