# 项目开发实施计划 (IMPLEMENTATION_PLAN.md)

## 一、 核心设计准则
- **强制约束**：除登录/注册/搜索外，所有核心接口（包括视频流 `feed`）必须携带 JWT 访问。未登录用户由网关直接拦截（401），后端服务默认认为 `userId` 绝对存在。
- **数据一致性**：引入 Elasticsearch 作为高性能“搜索”和“推荐召回”引擎。建立 MySQL 到 ES 的存量与增量同步机制。

## 二、 模块 A：搜索功能增强 (基于 Elasticsearch)
### 2.1 搜索框 UI 联动（前端）
- **触发逻辑**：点击搜索框展开浮层。
- **无输入状态**：展示“全站热搜榜”（数据源：`GET /api/search/hot`，基于 Redis ZSet）。
- **打字状态**：实时展示“搜索建议列表”（数据源：`GET /api/search/suggest`）。
- **搜索执行**：点击建议词、热搜词或按回车 → 携带关键词跳转搜索结果页。

### 2.2 ES 数据同步 (解决现有数据问题)
- **初始化同步**：开发 `SyncTool`，一次性将 MySQL 中现有已发布的视频（标题、描述、标签、播放量等）全量刷入 ES。
- **增量同步**：在 `publishVideo`（发布成功后）和管理后台（审核通过后）逻辑中，增加 ES 写入操作，确保两边数据准实时一致。

### 2.3 新增接口
- **下拉补全**：`GET /api/search/suggest?prefix=xxx`。
  - 使用 ES `Completion Suggester`。
  - 排序逻辑：按 `viewCount`（播放量）降序排列。

## 三、 模块 B：个性化推荐系统 (Kafka + Redis + ES)
### 3.1 用户行为捕获 (埋点)
- **触发源**：改造 `video-interaction` 微服务（已有）。
- **操作项**：当用户点赞、评论、收藏、完播时，Service 层异步向 Kafka 发送行为消息。
- **分值定义**：完播(+1), 点赞(+2), 评论(+3), 收藏(+4)。

### 3.2 兴趣画像构建
- **处理中心**：在 `video-video` 服务中启动 Kafka 消费者。
- **存储介质**：Redis Hash（Key: `user:profile:{userId}`, Field: `tag_name`, Value: `score`）。
- **逻辑**：消费者解析视频所属标签，并累加对应用户的 Redis 标签分数，设置 30 天滑动过期。

### 3.3 视频流接口改造 (GET /api/video/feed)
**三级瀑布流召回算法实现：**
- **第一级：个性化召回 (ES)**
  - 读取该用户 Redis 中前 3 名的兴趣标签。
  - 在 ES 中查询匹配这些标签、且状态正常、且当前用户未看过的视频（目标获取 10 条中的 8 条）。
- **第二级：热点补全 (MySQL)**
  - 若个性化召回不足，查询全站最热但用户未看过的视频补充。
- **第三级：全量兜底 (MySQL)**
  - 若前两级仍不足，按时间倒序拉取用户未看过的任意视频补充。
- **循环逻辑**：
  - 使用 Redis Set (`user:seen:{userId}`) 记录 7 天内看过的 ID。
  - 若三级全过仍无视频，自动清空该 Set，实现“抖音式”重新推荐。

---

## 四、 细化开发清单 (Step-by-Step)

本清单详细描述了 **[搜索增强]** 与 **[个性化推荐]** 两大核心功能的开发步骤。

### 阶段 1：Elasticsearch 地基建设
- [x] **1.1 定义 ES 索引 Mapping**
  - 在 `video-search` 服务中编写初始化代码。
  - 字段规划：`title`(text), `description`(text), `tags`(keyword), `viewCount`(long), `publishedAt`(date)。
  - **核心字段**：`suggestion` (Completion 类型)，用于实现搜索建议。
- [x] **1.2 开发存量数据同步工具**
  - 编写 `MySQLToESSyncJob` 或 Controller 接口。
  - 功能：从 MySQL `video` 表读取所有 `status=1` 的记录，由于 `VideoDTO` 已有相应结构，批量写入 ES。
- [x] **1.3 实现增量数据同步**
  - 修改 `video-video` 服务的 `publishVideo` 方法（或管理后台的审核通过逻辑）。
  - 功能：视频状态变为“已发布”时，同步调用 `video-search` 或直接写入 ES；视频删除时同步移除。

### 阶段 2：搜索接口升级
- [x] **2.1 实现下拉补全接口**
  - 文件：`video-search/SearchService.java`
  - 逻辑：使用 ES `Completion Suggester`，设置 `weight = viewCount` 以确保热门词排在前面。
  - 接口：`GET /api/search/suggest?prefix=xxx`。
- [x] **2.2 优化主搜索排序逻辑**
  - 改造 `SearchService.search()` 现有的 `multiMatch`。
  - 引入 `Function Score Query`：最终分数 = 文本相关性 + (log1p(viewCount) * 权重)。

### 阶段 3：前端 UI 增强
- [x] **3.1 实现搜索浮层组件**
  - 修改 `src/components/common/Header.vue`。
  - 逻辑：
    - Input Focus 时展示浮层。
    - Input 为空：调用 `GET /api/search/hot` 显示热搜。
    - Input 有字：调用 `GET /api/search/suggest` 显示建议。
- [x] **3.2 路由联动**
  - 点击建议词/热搜词：`router.push({ name: 'Search', query: { q: keyword } })`。

### 阶段 4：行为捕获与消息驱动 (Kafka Integration)
- [ ] **4.1 定义行为消息协议**
  - 消息体：`{ userId, videoId, behaviorType, score, timestamp }`。
- [ ] **4.2 接入点赞/收藏埋点**
  - 修改 `video-interaction` 服务：在点赞、收藏、评论成功后，异步向 Kafka 发送消息。
- [ ] **4.3 接入完播埋点**
  - 修改 `HistoryService`：当 `watchProgress >= 80%` 时，触发完播消息发送。

### 阶段 5：用户画像构建 (User Profiling)
- [ ] **5.1 编写 Kafka 行为消费者**
  - 在 `video-video` 服务中监听行为 Topic。
  - 逻辑：根据 `videoId` 查 MySQL 获取该视频的标签（`VideoTag`）。
- [ ] **5.2 实时画像存储 (Redis)**
  - 存储结构：Redis Hash `user:profile:{userId}`。
  - 逻辑：对该用户画像中的每个标签进行分值累加（`HINCRBYFLOAT`），设置 30 天滑动过期。

### 阶段 6：高级推荐算法实现 (Feed API 改造)
- [ ] **6.1 引入“已看”去重机制**
  - 在 `getVideoFeed` 前置逻辑中。
  - 存储：Redis Set `user:seen:{userId}`，记录近期推送过的 ID，设置 7 天过期。
- [ ] **6.2 编写三级召回逻辑**
  - 在 `VideoServiceImpl.getVideoFeed()` 中：
    - **Step 1 (个性化)**：从用户画像取 Top 3 标签 -> ES 搜索同标签视频 (排除 seen 列表)。
    - **Step 2 (热门)**：若不足 10 条，从 MySQL 取高播放量视频 (排除 seen 列表)。
    - **Step 3 (兜底)**：若仍不足，按时间倒序取全量视频 (排除 seen 列表)。
- [ ] **6.3 循环策略与原子性重置**
  - 逻辑：当三级召回结果总数 < 3 时，判断为“用户已刷完全平台视频”。
  - 动作：删除 Redis `user:seen:{userId}`，重新开始推荐。

---

## 🗺️ 整体开发路线图 (Timeline)

1. **第一天 (地基)**：ES 索引建立 + 存量数据同步。
2. **第二天 (搜索)**：补全接口开发 + 前端搜索浮层实现。
3. **第三天 (画像)**：Kafka 埋点 + Redis 画像分值计算。
4. **第四天 (推荐)**：Feed 瀑布流逻辑编写 + 已看去重逻辑。
5. **第五天 (联调)**：全链路测试与排序参数微调。

---

## 🏆 搜索功能总结 (Stage A Finish)

### 1. 功能描述
- **高精准搜索**：支持对视频标题和描述的全文检索。采用 `AND` 逻辑过滤，确保搜索结果与关键词高度相关，有效排除干扰项。
- **全场联想补全**：在搜索框输入时，实时推荐相关的视频标题。支持“中段匹配”（如输入“摩天轮”可联想出“游乐场的摩天轮”），匹配范围涵盖标题与标签。
- **实时热搜榜**：当搜索框聚焦且为空时，自动展示全站热搜关键词，支持点击跳转。
- **智能排序**：搜索结果基于“相关性得分 + 播放量权重”进行综合排序，确保优质内容优先展示。
- **极简 UI 体效**：采用 Glassmorphism（玻璃拟态）设计搜索预览层，支持退格自动回退热搜状态，交互流畅。

### 2. 简要实现方式
- **后端 (Elasticsearch)**:
  - **分词器**：集成 `IK Analyzer` 实现中文深度分词。
  - **搜索算法**：使用 `MultiMatch` + `Operator.And` 提升精度；使用 `MatchPhrasePrefix` 实现比原生 Suggester 更灵活的补全联想。
  - **得分增强**：通过 `FunctionScore` 引入 `viewCount` 因子，使用 `Ln2p` 平滑处理播放量对排名的贡献。
  - **容错处理**：在 DTO 层通过 `@JsonIgnoreProperties` 解决 ES 动态字段与 Java 对象的反序列化冲突问题。
- **数据同步**:
  - 实现了 `DataSyncService`，支持从 MySQL 全量同步视频数据至 ES，并集成了 `UserFeignClient` 实时补全视频作者的头像和昵称。
- **前端 (Vue.js)**:
  - 基于 `Debounce`（防抖）技术优化联想接口请求频率。
  - 监听搜索框 `focus` 和 `input` 事件，实现热搜榜与建议列表的无缝切换。
