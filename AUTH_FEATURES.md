# 前端认证功能实现说明

## 功能概述

已完成前端登录与注册功能的完整实现，包括：

1. **API封装**：使用Axios封装了登录和注册接口
2. **状态管理**：通过Vuex管理认证状态（token、用户信息）
3. **请求拦截器**：自动在请求头中添加Authorization Bearer token
4. **页面逻辑**：登录和注册页面集成API调用和状态管理

## 文件结构

### API层
- `src/api/index.js` - Axios实例配置，包含请求/响应拦截器
- `src/api/auth.js` - 认证相关API接口封装

### 状态管理
- `src/store/index.js` - 扩展了Vuex store，添加认证相关状态、mutations、actions和getters

### 工具函数
- `src/utils/auth.js` - 认证相关工具函数

### 路由
- `src/router/index.js` - 添加路由守卫，使用meta属性标记需要认证的路由

### 视图组件
- `src/views/Login.vue` - 登录页面，集成登录逻辑
- `src/views/Register.vue` - 注册页面，集成注册逻辑
- `src/components/layout/Header.vue` - 头部组件，集成登出逻辑

## 核心功能实现

### 1. API请求配置
- 基础URL设置为 `http://localhost:8080`（后端网关地址）
- 请求拦截器：自动添加Authorization header（Bearer token）
- 响应拦截器：处理401未授权错误，自动清除token、跳转登录页并提示用户
- 白名单URL：对无需认证的接口（如登录、注册等）跳过token添加

### 2. 认证状态管理
- `token`：存储访问令牌
- `isAuthenticated`：认证状态标识
- `user`：存储用户信息
- 相关mutations：`SET_TOKEN`、`SET_USER`、`CLEAR_AUTH`
- 相关actions：`login`、`logout`、`updateUser`

### 3. 路由守卫
- 使用meta属性标记：在路由配置中使用 `meta: { requiresAuth: true }` 标记需要认证的页面（包括首页 /）
- 路由跳转前检查：如访问需要认证的路径且无有效token，自动重定向到登录页

### 4. 登录流程
1. 用户输入用户名和密码
2. 调用后端登录API
3. 登录成功后，将token和用户信息保存到store和localStorage
4. 重定向到首页

### 5. 注册流程
1. 用户输入注册信息（用户名、邮箱、密码等）
2. 验证输入信息（密码确认、邮箱格式、长度等）
3. 调用后端注册API
4. 注册成功后自动登录，保存token和用户信息
5. 重定向到首页

### 6. 登出流程
1. 调用后端登出API
2. 清除store中的认证状态
3. 清除localStorage中的token和用户信息
4. 重定向到登录页

### 7. Token过期处理
1. API响应返回401状态码时
2. 自动清除本地认证信息
3. 跳转到登录页
4. 提示用户"登录已过期，请重新登录"

## 使用说明

### 在其他组件中使用认证状态
```javascript
import { mapGetters, mapActions } from 'vuex'

export default {
  computed: {
    ...mapGetters(['isAuthenticated', 'currentUserInfo', 'authToken'])
  },
  methods: {
    ...mapActions(['login', 'logout'])
  }
}
```

### 直接使用认证工具函数
```javascript
import { isAuthenticated, getToken, clearAuth, setAuth } from '../utils/auth'

// 检查是否已认证
if (isAuthenticated()) {
  // 用户已认证
}

// 获取token
const token = getToken();
```

## 路由保护

需要认证的路由可以添加 `meta: { requiresAuth: true }` 属性：

```javascript
{
  path: '/protected',
  name: 'ProtectedPage',
  component: ProtectedPage,
  meta: { requiresAuth: true }
}
```

## 注意事项

1. 前端认证状态依赖于localStorage，清除浏览器数据会丢失认证状态
2. token过期需要后端配合实现刷新机制
3. 生产环境中应使用HTTPS以确保token安全


一、 个性化推荐系统设计思路
在本科毕设中，直接上工业级的深度学习模型（如YouTube DNN）难度过大且难以训练。建议采用**“基于内容的推荐（CB）” + “基于协同过滤（CF）”的混合推荐策略**，并遵循经典的**“召回（Recall） -> 排序（Rank）”**两阶段流程。

1. 数据采集与用户画像（基础）
利用你架构中的 Kafka 进行异步埋点。

行为定义与评分： 制定一套隐式评分标准。
完播：+1分
点赞：+2分
评论：+3分
收藏：+4分
分享：+5分
数据流向：
前端（Vue.js）监听用户行为 -> 调用后端API。
后端发送消息到 Kafka Topic (user-behavior-topic)。
推荐微服务消费 Kafka 消息，更新 Redis 中的用户画像。
用户画像模型（User Profile）：
在 Redis 中维护一个 Hash 结构：User:{userId}:Tags。
内容为：{ "科技": 10.5, "搞笑": 5.0, "美食": 2.0 }。
随着用户行为实时累加分数。
2. 推荐算法实现（核心）
策略 A：基于标签的实时推荐（Content-Based, CB）—— 解决冷启动

原理： 用户喜欢“科技”，就推“科技”标签的视频。
实现：
读取 Redis 中该用户得分最高的 Top 3 标签（例如：科技、编程）。
利用 Elasticsearch 的 Function Score Query 进行查询。
DSL 逻辑：search videos where tags in ("科技", "编程") order by publish_time desc。
优点： 实时性强，用户刚点赞一个视频，刷新就能看到类似的。
策略 B：基于物品的协同过滤（Item-Based CF）—— 发现惊喜

原理： 喜欢视频 A 的人，通常也喜欢视频 B。
实现（简化版）：
离线计算（可以是Spring Schedule定时任务）： 每天凌晨分析前一天的行为表。
构建“视频-视频”相似度矩阵。如果很多用户同时点赞了视频 A 和 视频 B，则 A 和 B 关联度高。
将关联结果存入 Redis：Video:{videoId}:Similar -> [videoId_1, videoId_2...]。
在线推荐： 当用户观看视频 A 时，直接从 Redis 取出相似视频列表推荐。
3. 推荐流程整合（业务逻辑）
在你的 RecommendationService 中：

第一步（过滤）： 获取用户已看过的视频 ID 列表（Bloom Filter 或 Redis Set），避免重复推荐。
第二步（召回）：
80% 数据来自 策略 A（标签匹配，保证精准）。
20% 数据来自 热门视频榜单（Redis ZSet，保证热度）。
第三步（排序）： 对召回的视频列表进行加权排序（例如：发布时间权重 * 0.3 + 视频热度权重 * 0.7）。
第四步： 返回给前端。
===================================================================================================================================

1、
请先阅读下面的提示词内容，然后参考我的项目，最后看看是否可以实现，哪里需要变动，写出工作清单
任务：基于 Redis ZSet 实现动态热搜排行榜
背景：
除了 ES 提供的搜索能力外，我们需要在搜索框默认状态下展示“全站热搜榜”。该榜单需要统计用户的真实搜索行为，按搜索频率高低排序，并具备一定的时效性（如“今日热搜”或“近7日热搜”）。

请检查代码库中 Redis 的配置，并评估以下方案的可行性：

1. 数据结构设计 (Data Structure)
我们需要利用 Redis 的 Sorted Set (ZSet) 数据结构。

工具：Redis ZSet。
Key 的设计策略：
为了保证热搜的时效性，不能只用一个 Key 永久累加。建议采用 “按时间分片” 的策略。
示例 Key：search_hot_rank:{yyyyMMdd} (例如 search_hot_rank:20231027)。
Value (Member)：用户的搜索关键词（需经过归一化处理，如去除首尾空格、转小写）。
Score：搜索次数（热度值）。
2. 写入逻辑 (Write Strategy)
在用户发起搜索请求的接口（Controller/Service）中，植入埋点逻辑：

动作：每次有效搜索，对当天的 Redis ZSet 执行 ZINCRBY 操作，步长为 1。
异步处理：评估当前项目是否支持异步事件（如 Spring Event 或 消息队列），建议将“记录热搜”的动作异步化，避免阻塞主搜索接口的响应速度。
数据清洗：在写入 Redis 前，需要判断关键词是否违禁、是否过短，防止无效词霸榜。
3. 读取与聚合逻辑 (Read & Aggregation)
前端请求“热搜榜”接口时的逻辑：

场景 A：仅展示今日热搜
直接对当天的 Key 使用 ZREVRANGE 获取 Score 最高的 Top 10。
场景 B：展示近 N 天热搜（更平滑，推荐）
工具：ZUNIONSTORE (并集操作)。
逻辑：将过去 N 天（如近3天）的 Key 进行合并计算，生成一个临时的 temp_rank_key，然后从临时 Key 中读取 Top 10。
缓存策略：由于聚合计算耗时，计算结果应该在应用层做短时间缓存（如缓存 5-10 分钟）。
4. 生命周期管理 (TTL)
过期时间：每天生成的 Key (search_hot_rank:20231027) 必须设置过期时间（如 7 天后过期），防止 Redis 内存无限膨胀。

2、请先阅读下面的提示词内容，然后参考我的项目，最后看看是否可以实现，哪里需要变动，写出工作清单
任务：基于 Elasticsearch 实现带权重排序的视频搜索与下拉提示
背景：
我们需要在现有的视频业务中集成 Elasticsearch，实现两个核心功能：

主搜索列表：支持文本匹配，并根据“播放量”和“发布时间”进行排序或加权。
搜索下拉提示 (Autocomplete)：用户输入前缀时提示补全词，且提示词顺序必须由“播放量”决定（热度高的排前面）。
请阅读我的代码库，检查现有的实体类（Entity/Model），并评估以下实施方案的可行性。

1. 索引结构设计 (Index Mapping)
我们需要在 ES 索引中额外构建以下特定字段，请规划 Mapping 结构：

基础文本字段：
title (Text, Analyzer: ik_max_word)：用于全文检索。
keyword (Keyword)：用于精确匹配或聚合。
C类排序/权重字段 (额外构建)：
view_count (Type: Long/Integer)：
用途：用于搜索结果列表的“硬排序”（按热度）和“综合排序”（Function Score 加权）。
来源：对应数据库中的播放量字段。
create_time (Type: Date/Long)：
用途：用于搜索结果列表的“按最新发布”排序。
提示词字段 (额外构建)：
suggestion (Type: Completion)：
用途：专用于下拉自动补全（Suggester）。
特殊要求：该字段结构包含 input (提示词文本) 和 weight (权重值)。
2. 数据同步逻辑 (Data Sync)
在编写从 MySQL 同步数据到 ES 的逻辑（Logstash 或 Java 代码）时，需要处理以下转换：

常规映射：将 DB 的 title, description 同步到 ES。
分离权重逻辑：
将 DB 的播放量直接赋值给 ES 的 view_count 字段。
关键步骤：构建 suggestion 对象时，必须将 DB 的播放量赋值给 suggestion.weight 属性。
目的：确保下拉提示出来的词，是已经在索引阶段就按热度排好序的。
3. 查询接口实现 (Query Implementation)
接口 A：主搜索 (Search)

工具：Bool Query + Function Score Query。
逻辑：
必须包含 multi_match 对标题和描述的检索。
综合排序：使用 field_value_factor，引入 view_count 字段对相关性得分进行加权（建议使用 log1p 平滑处理），实现“标题匹配度高且播放量高”的排前面。
硬排序：支持前端传参，直接对 view_count 或 create_time 进行 sort。
接口 B：下拉提示 (Autocomplete)

工具：Completion Suggester。
逻辑：
针对 suggestion 字段进行前缀查询。
注意：不需要在查询时动态指定排序，因为我们在“数据同步”阶段已经通过 weight 属性定义好了顺序。
开启 skip_duplicates: true 去除重复提示。