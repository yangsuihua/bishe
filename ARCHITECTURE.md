# 项目架构文档 (ARCHITECTURE.md)

> 生成时间：2026-03-11  
> 角色定位：架构师视角扫描，覆盖前端用户端、后端微服务、管理后台三大模块

---

## 一、项目整体概览

本项目是一个**视频平台（VideoPlatform - Pro）**，采用前后端分离、后端微服务的架构风格。整体分为三个独立子工程：

| 子工程 | 路径 | 定位 |
|--------|------|------|
| **用户端前端** | `/`（根目录） | 面向普通用户的视频浏览、上传、互动前端 |
| **后端微服务群** | `/backend/` | 提供所有业务 API 的 Spring Cloud 微服务后端 |
| **管理后台** | `/backend_admin/` | 管理员审核视频、管理用户的后台系统（含独立前后端） |

---

## 二、技术栈总览

### 2.1 用户端前端（`/` 根目录）

| 类别 | 技术 | 版本 |
|------|------|------|
| **框架** | Vue 3 | ^3.3.0 |
| **构建工具** | Vite | ^4.3.0 |
| **路由** | Vue Router | ^4.2.0 |
| **状态管理** | Vuex 4（主用） + Pinia 3（安装但未启用） | ^4.1.0 / ^3.0.4 |
| **HTTP 客户端** | Axios | ^1.13.2 |
| **图标库** | lucide-vue-next | ^0.562.0 |
| **样式** | 纯 CSS（全局样式文件） | — |

> ⚠️ **注意**：`package.json` 中同时安装了 Vuex 和 Pinia，但当前代码主体使用的是 **Vuex**，Pinia 未在用户端 `main.js` 中注册。这是一个技术债。

### 2.2 后端微服务群（`/backend/`）

| 类别 | 技术 | 版本 |
|------|------|------|
| **语言/平台** | Java 17 | 17 |
| **基础框架** | Spring Boot | 3.1.5 |
| **微服务框架** | Spring Cloud | 2022.0.4 |
| **服务治理** | Spring Cloud Alibaba (Nacos) | 2022.0.0.0 |
| **API 网关** | Spring Cloud Gateway | — |
| **服务发现/配置** | Nacos Discovery + Nacos Config | — |
| **限流熔断** | Sentinel | — |
| **ORM** | MyBatis Plus | 3.5.3.2 |
| **数据库** | MySQL | 8.0.33 |
| **连接池** | Druid | 1.2.18 |
| **缓存/分布式锁** | Redis + Redisson | 3.23.4 |
| **对象存储** | MinIO | 8.5.5 |
| **搜索引擎** | Elasticsearch | 8.9.0 |
| **消息队列** | Kafka | 3.4.1 |
| **认证** | JWT (JJWT) | 0.11.5 |
| **服务间调用** | OpenFeign + LoadBalancer | 4.0.4 |
| **视频处理** | FFmpeg (Java wrapper) | 0.7.0 |
| **工具库** | Hutool、Lombok、MapStruct、Fastjson2 | — |

### 2.3 管理后台（`/backend_admin/`）

| 模块 | 技术 |
|------|------|
| **前端框架** | Vue 3 + Vite |
| **UI 组件库** | Element Plus（中文本地化） |
| **路由** | Vue Router 4 |
| **状态管理** | **Pinia**（Setup Store 风格） |
| **HTTP** | Axios |
| **后端** | Spring Boot 3.1.5（单体服务，非微服务） |
| **ORM** | MyBatis Plus + MySQL |
| **缓存** | Redis |
| **认证** | JWT |

---

## 三、目录结构

```
dasd-main/                          # 项目根目录
├── index.html                      # 用户端前端 SPA 入口
├── package.json                    # 用户端依赖配置
├── vite.config.js                  # Vite 构建 & 代理配置
├── AUTH_FEATURES.md                # 认证功能说明文档
│
├── src/                            # 用户端前端源码
│   ├── main.js                     # 应用入口，挂载 Vue + Router + Vuex
│   ├── App.vue                     # 根组件（含布局判断逻辑）
│   ├── router/
│   │   └── index.js                # 路由表 + 全局导航守卫
│   ├── store/
│   │   └── index.js                # Vuex Store（状态、mutations、actions、getters）
│   ├── api/
│   │   ├── index.js                # Axios 实例 + 请求/响应拦截器
│   │   ├── auth.js                 # 认证 API（登录/注册/刷新/登出）
│   │   ├── video.js                # 视频 API（上传/发布/推荐流/分类）
│   │   ├── user.js                 # 用户 API（个人信息/关注/粉丝）
│   │   ├── comment.js              # 评论 API
│   │   ├── friend.js               # 好友 API
│   │   └── message.js              # 消息 API
│   ├── views/                      # 页面级组件
│   │   ├── Home.vue                # 首页（视频推荐流）
│   │   ├── Upload.vue              # 视频上传页
│   │   ├── Profile.vue             # 用户主页
│   │   ├── Category.vue            # 视频分类页
│   │   ├── Login.vue               # 登录页
│   │   └── Register.vue            # 注册页
│   ├── components/
│   │   ├── layout/
│   │   │   ├── Header.vue          # 顶部导航栏
│   │   │   └── Sidebar.vue         # 左侧边栏
│   │   ├── common/
│   │   │   ├── VideoCard.vue       # 视频卡片
│   │   │   ├── VideoGrid.vue       # 视频网格布局
│   │   │   ├── ChatWindow.vue      # 聊天窗口
│   │   │   ├── MessageSidebar.vue  # 消息侧边栏
│   │   │   ├── ForwardModal.vue    # 转发弹窗
│   │   │   └── UserList.vue        # 用户列表
│   │   ├── home/
│   │   │   └── VideoSlide.vue      # 首页视频滑动播放器
│   │   └── profile/
│   │       ├── ProfileContent.vue  # 个人主页内容区
│   │       ├── ProfileHeader.vue   # 个人主页头部
│   │       └── ProfileTabs.vue     # 个人主页标签页
│   ├── layouts/
│   │   └── BlankLayout.vue         # 空白布局（用于登录/注册页）
│   ├── utils/
│   │   └── auth.js                 # 认证工具函数（Token 读写/清除）
│   └── assets/
│       └── styles/
│           └── global.css          # 全局 CSS 样式
│
├── backend/                         # 后端微服务父工程
│   ├── pom.xml                      # Maven 父 POM（统一依赖版本管理）
│   ├── video-common/                # 公共模块（Result 封装、工具类等）
│   ├── video-gateway/               # API 网关（统一入口、JWT 鉴权过滤器）
│   ├── video-auth/                  # 认证服务（登录/注册/Token）
│   ├── video-user/                  # 用户服务（个人信息/关注关系）
│   ├── video-video/                 # 视频服务（上传/发布/推荐）
│   ├── video-interaction/           # 互动服务（点赞/评论/收藏/私信）
│   ├── video-search/                # 搜索服务（Elasticsearch）
│   └── sql/
│       └── 123.sql                  # 数据库初始化 SQL 脚本
│
└── backend_admin/                   # 管理后台（独立子系统）
    ├── README.md
    ├── create_admin_token.java      # 手动生成管理员 Token 的工具脚本
    ├── backend/                     # 管理后台后端（Spring Boot 单体）
    │   └── pom.xml
    └── frontend/                    # 管理后台前端（Vue3 + Element Plus）
        ├── package.json
        ├── vite.config.js
        └── src/
            ├── main.js              # 入口（Pinia + Element Plus）
            ├── App.vue
            ├── router/index.js      # 管理后台路由表
            ├── store/auth.js        # Pinia Auth Store
            ├── api/                 # 管理后台 API 封装
            ├── components/layout/   # AdminLayout 布局组件
            └── views/               # Dashboard/VideoAudit/UserManage 等
```

---

## 四、核心架构模式

### 4.1 用户端前端：布局路由二态模式

`App.vue` 中通过路由 `meta.blankLayout` 标志控制两种布局：

```
路由 meta.blankLayout = true  →  全屏空白布局（登录页、注册页）
路由 meta.blankLayout = false →  标准布局（Header + Sidebar + 内容区 + 聊天侧边栏）
```

### 4.2 用户端前端：API 层架构

```
用户端组件 / Vuex Actions
    ↓
src/api/*.js（业务 API 模块，按领域拆分）
    ↓
src/api/index.js（Axios 实例 + 拦截器中心）
    ├─ 请求拦截：自动注入 Bearer Token，自动设置 Content-Type
    └─ 响应拦截：401 全局处理 → 清除 Token → 跳转登录页
    ↓
Vite Dev Server Proxy（/api/* → http://localhost:8080）
    ↓
后端 API 网关
```

### 4.3 用户端前端：认证状态管理（双轨并存问题）

当前认证状态存在**双轨存储**：

| 存储位置 | 读写方 | 说明 |
|----------|--------|------|
| `localStorage.accessToken` | `api/index.js`、`router/index.js`、`utils/auth.js` | 直接操作 localStorage |
| `Vuex store.state.token` | `store/index.js` | 同步到 Store，同时写 localStorage |

路由守卫（`router/index.js`）**直接读取 localStorage**，而非从 Vuex 读取，导致认证状态不完全由 Vuex 统一管理。

### 4.4 用户端前端：视频渐进加载机制

Vuex Store 中实现了一个视频分批加载控制器：

```
state.maxLoadableVideos（初始 = 5）：允许渲染的最大视频数
state.watchedVideoCount：已观看视频计数器

每观看 5 个视频 → INCREMENT_WATCHED_COUNT → maxLoadableVideos 增加 5
Getter: isVideoLoadable(index) → index < maxLoadableVideos
```

### 4.5 后端：Spring Cloud 微服务架构

```
客户端请求
    ↓
video-gateway（:8080）
    ├─ [GlobalFilter] AuthGlobalFilter：JWT 验证
    │     ├─ 白名单路径直接放行
    │     ├─ 解析 JWT → 提取 userId → 注入 X-User-Id 请求头
    │     └─ 401 → 返回统一 JSON 错误响应
    │
    ├─ Nacos 服务发现 → LoadBalancer 负载均衡
    │
    └─ 路由转发至各微服务
         ├─ video-auth（认证服务）
         ├─ video-user（用户服务）
         ├─ video-video（视频服务，包含 MinIO 存储、FFmpeg 处理）
         ├─ video-interaction（互动服务，含 Kafka 异步消息）
         └─ video-search（搜索服务，Elasticsearch）
```

**微服务间通信**：使用 OpenFeign + Nacos + LoadBalancer  
**分布式缓存**：Redis（Redisson 分布式锁）  
**配置中心**：Nacos Config  
**限流熔断**：Sentinel（网关层）

### 4.6 管理后台：轻量单体架构

管理后台（`backend_admin`）是一个**相对独立的轻量子系统**，不接入 Nacos/Kafka 等重型中间件：

- **后端**：Spring Boot 单体，直连 MySQL + Redis，使用独立 JWT（`admin_token`）
- **前端**：Vue 3 + Element Plus，状态管理使用 Pinia（Setup Store 风格），路由为嵌套结构
- **认证**：与用户端完全独立，Token 存储于 `admin_token` localStorage key

---

## 五、关键架构决策与技术债

| # | 问题 | 影响等级 | 说明 |
|---|------|---------|------|
| 1 | **Vuex 与 Pinia 共存** | 中 | `package.json` 同时声明了两者，用户端用 Vuex，管理端用 Pinia，未来统一时需迁移 |
| 2 | **路由守卫直读 localStorage** | 中 | `router/index.js` 绕过 Vuex 直接读取 `localStorage.accessToken`，测试困难 |
| 3 | **Store 挂载到 window** | 低-中 | `main.js` 将 `store` 挂载到 `window.store`，供 API 拦截器访问，是循环依赖的变通方式 |
| 4 | **Store 中存在硬编码模拟数据** | 低 | `store/index.js` 的 `usersDB` 和 `feedVideos` 初始值包含 Mock 数据，应全部来自 API |
| 5 | **视频服务 admin 模块** | 低 | `pom.xml` 中声明了 `video-admin` 子模块，但 `/backend/` 下未见对应目录，可能缺失 |
| 6 | **管理员 Token 硬生成** | 中 | `create_admin_token.java` 为手工工具脚本，生产环境不应以此方式创建管理员凭证 |

---

## 六、服务端口规划（开发环境）

| 服务 | 默认端口 | 说明 |
|------|---------|------|
| 用户端前端（Vite） | 5173 | Vite 默认端口 |
| API 网关 | 8080 | 所有前端请求的统一入口 |
| 管理后台后端 | 8081（推测） | 独立 Spring Boot 服务 |
| 管理后台前端 | 5174（推测） | 独立 Vite 实例 |
| Nacos | 8848 | 服务注册与配置 |
| MySQL | 3306 | 主数据库 |
| Redis | 6379 | 缓存 |
| MinIO | 9000 | 对象存储 |
| Elasticsearch | 9200 | 搜索服务 |
| Kafka | 9092 | 消息队列 |

---

*本文档由架构师扫描代码后自动生成，如有业务变更请同步更新。*
