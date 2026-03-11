# 视频平台后端微服务架构

## 📁 项目结构

```
backend/
├── sql/                                    # 数据库脚本
│   └── video_platform.sql                 # 完整数据库表结构
│
├── video-common/                           # 公共模块
│   └── src/main/java/com/videoplatform/common/
│       ├── result/                         # 统一返回结果
│       │   ├── Result.java                # 返回结果封装
│       │   └── ResultCode.java            # 状态码枚举
│       ├── exception/                      # 异常处理
│       │   ├── BusinessException.java     # 业务异常
│       │   └── GlobalExceptionHandler.java# 全局异常处理器
│       ├── utils/                          # 工具类
│       │   ├── JwtUtil.java               # JWT工具
│       │   ├── RedisUtil.java             # Redis工具
│       │   └── SnowflakeIdWorker.java     # 雪花ID生成器
│       └── constants/                      # 常量定义
│           └── RedisConstants.java        # Redis键常量
│
├── video-gateway/                          # 网关服务 (8080)
│   └── src/main/java/com/videoplatform/gateway/
│       ├── filter/                         # 网关过滤器
│       │   ├── AuthGlobalFilter.java      # 认证过滤器
│       │   └── LogGlobalFilter.java       # 日志过滤器
│       ├── config/                         # 配置类
│       │   └── CorsConfig.java            # 跨域配置
│       └── GatewayApplication.java        # 启动类
│
├── video-auth/                             # 认证授权服务 (8081)
│   └── src/main/java/com/videoplatform/auth/
│       ├── controller/                     # 控制层
│       │   └── AuthController.java        # 认证接口
│       ├── service/                        # 服务层
│       │   ├── AuthService.java           # 认证服务接口
│       │   └── impl/
│       │       └── AuthServiceImpl.java   # 认证服务实现
│       └── AuthApplication.java           # 启动类
│
├── video-user/                             # 用户服务 (8082)
│   └── src/main/java/com/videoplatform/user/
│       ├── controller/                     # 控制层
│       │   ├── UserController.java        # 用户接口
│       │   └── FollowController.java      # 关注接口
│       ├── service/                        # 服务层
│       │   ├── UserService.java
│       │   ├── FollowService.java
│       │   └── impl/
│       ├── mapper/                         # 数据访问层
│       │   ├── UserMapper.java
│       │   └── UserFollowMapper.java
│       ├── entity/                         # 实体类
│       │   ├── User.java
│       │   └── UserFollow.java
│       └── dto/                            # 数据传输对象
│           ├── UserDTO.java
│           └── UserProfileVO.java
│
├── video-video/                            # 视频服务 (8083)
│   └── src/main/java/com/videoplatform/video/
│       ├── controller/
│       │   ├── VideoController.java       # 视频接口
│       │   ├── UploadController.java      # 上传接口
│       │   └── CategoryController.java    # 分类接口
│       ├── service/
│       │   ├── VideoService.java
│       │   ├── UploadService.java         # MinIO上传服务
│       │   └── impl/
│       ├── mapper/
│       │   ├── VideoMapper.java
│       │   ├── VideoTagMapper.java
│       │   └── VideoCategoryMapper.java
│       └── entity/
│           ├── Video.java
│           ├── VideoTag.java
│           └── VideoCategory.java
│
├── video-interaction/                      # 互动服务 (8084)
│   └── src/main/java/com/videoplatform/interaction/
│       ├── controller/
│       │   ├── LikeController.java        # 点赞接口
│       │   ├── FavoriteController.java    # 收藏接口
│       │   └── CommentController.java     # 评论接口
│       ├── service/
│       │   ├── LikeService.java
│       │   ├── FavoriteService.java
│       │   ├── CommentService.java
│       │   └── impl/
│       ├── mapper/
│       │   ├── UserLikeMapper.java
│       │   ├── UserFavoriteMapper.java
│       │   └── CommentMapper.java
│       └── consumer/                       # Kafka消费者
│           └── InteractionConsumer.java   # 互动行为消费
│
├── video-search/                           # 搜索服务 (8085)
│   └── src/main/java/com/videoplatform/search/
│       ├── controller/
│       │   └── SearchController.java      # 搜索接口
│       ├── service/
│       │   └── ElasticsearchService.java  # ES搜索服务
│       └── document/
│           └── VideoDocument.java         # 视频文档
│
└── video-admin/                            # 管理后台服务 (8086)
    └── src/main/java/com/videoplatform/admin/
        ├── controller/
        │   ├── AdminController.java        # 管理后台接口
        │   └── AuditController.java        # 审核管理接口
        ├── service/
        │   ├── AdminService.java           # 管理服务
        │   └── AuditService.java           # 审核服务
        ├── mapper/
        │   ├── UserMapper.java             # 用户Mapper
        │   ├── VideoMapper.java            # 视频Mapper
        │   └── AuditLogMapper.java         # 审核日志Mapper
        ├── entity/
        │   └── AuditLog.java               # 审核日志实体
        └── config/
            └── AdminConfig.java            # 管理配置
```

## 🔧 技术栈详情

### 核心框架
- **Spring Boot 3.1.5** - 基础框架
- **Spring Cloud 2022.0.4** - 微服务框架
- **Spring Cloud Alibaba 2022.0.0.0** - 阿里微服务组件

### 服务治理
- **Nacos** - 服务注册发现 & 配置中心
- **Gateway** - API网关
- **Sentinel** - 流量控制 & 熔断降级
- **LoadBalancer** - 客户端负载均衡

### 数据存储
- **MySQL 8.0** - 关系型数据库
- **Redis** - 缓存 & 分布式锁
- **Elasticsearch 8.9** - 搜索引擎
- **MinIO** - 对象存储(视频文件)

### 消息队列
- **Kafka 3.4** - 异步消息处理

### ORM & 数据库
- **MyBatis Plus 3.5.3** - ORM框架
- **Druid** - 数据库连接池

### 安全认证
- **JWT** - Token认证
- **BCrypt** - 密码加密

### 工具库
- **Hutool** - Java工具集
- **Lombok** - 简化代码
- **Fastjson2** - JSON处理
- **MapStruct** - 对象映射

## 🚀 服务端口分配

| 服务名称 | 端口 | 说明 |
|---------|------|------|
| video-gateway | 8080 | API网关 |
| video-auth | 8081 | 认证授权 |
| video-user | 8082 | 用户服务 |
| video-video | 8083 | 视频服务 |
| video-interaction | 8084 | 互动服务 |
| video-search | 8085 | 搜索服务 |
| video-admin | 8086 | 管理后台 |

## 📊 数据库表设计

### 核心表
- **user** - 用户表
- **user_follow** - 关注关系表
- **video** - 视频表
- **video_tag** - 视频标签表
- **video_category** - 视频分类表
- **user_like** - 点赞表
- **user_favorite** - 收藏表
- **comment** - 评论表
- **user_history** - 观看历史表
- **message** - 消息表

### 管理表
- **audit_log** - 审核记录

## 🔄 业务流程

### 1. 用户注册登录
```
用户 -> Gateway -> Auth服务 -> 生成JWT -> 返回Token
```

### 2. 视频上传
```
用户 -> Gateway -> Video服务 -> MinIO存储 -> 入库 -> Kafka异步审核
```

### 3. 视频互动(点赞/收藏)
```
用户 -> Gateway -> Interaction服务 -> Redis缓存 -> Kafka异步落库
```

### 4. 视频搜索
```
用户 -> Gateway -> Search服务 -> Elasticsearch -> 返回结果
```

### 5. 推荐流
```
用户 -> Gateway -> Video服务 -> Redis缓存 -> 算法推荐 -> 返回列表
```

## 📝 配置说明

### Nacos配置中心
每个服务需要在Nacos配置中心配置：
- `application-{env}.yml` - 环境配置
- `datasource.yml` - 数据源配置
- `redis.yml` - Redis配置
- `kafka.yml` - Kafka配置

### 环境变量
```properties
# Nacos
NACOS_SERVER=localhost:8848
NACOS_NAMESPACE=video-platform

# MySQL
MYSQL_HOST=localhost:3306
MYSQL_DATABASE=video_platform
MYSQL_USERNAME=root
MYSQL_PASSWORD=123456

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Elasticsearch
ES_HOST=localhost
ES_PORT=9200
```

## 🛠️ 开发指南

### 1. 环境准备
```bash
# 安装MySQL 8.0
# 安装Redis
# 安装Nacos
# 安装Kafka
# 安装Elasticsearch
# 安装MinIO
```

### 2. 数据库初始化
```bash
mysql -u root -p < sql/video_platform.sql
```

### 3. 启动服务
```bash
# 1. 启动Nacos
# 2. 启动各个微服务
mvn spring-boot:run
```

### 4. 访问地址
- 前端页面: http://localhost:3000
- 后端网关: http://localhost:8080
- Nacos控制台: http://localhost:8848/nacos

## 📮 API接口规范

### 统一返回格式
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1234567890
}
```

### 请求Header
```
Authorization: Bearer {token}
Content-Type: application/json
```

## 🔐 安全措施

1. **JWT认证** - 所有接口需要Token验证
2. **BCrypt加密** - 密码加密存储
3. **Sentinel限流** - 防止恶意请求
4. **XSS防护** - 输入过滤
5. **SQL注入防护** - MyBatis预编译

## 📈 性能优化

1. **Redis缓存** - 热点数据缓存
2. **Kafka异步** - 削峰填谷
3. **ES搜索** - 快速检索
4. **CDN加速** - 静态资源加速
5. **数据库索引** - 查询优化

## 🎯 后续扩展

- [ ] 实现推荐算法
- [ ] 添加实时弹幕功能
- [ ] 接入第三方登录
- [ ] 添加支付功能
- [ ] 实现直播功能
- [ ] 数据分析大屏