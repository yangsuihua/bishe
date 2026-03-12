# 前端数据流分析文档 (DATA_FLOW.md)

> 扫描范围：`src/api/`、`src/views/`（及重点关联的核心组件）  
> 生成时间：2026-03-11

---

## 一、API 层总览（`src/api/`）

### 1.1 API 模块划分

| 模块文件 | 领域 | 导出方式 | 涵盖后端服务 |
|---------|------|---------|------------|
| `index.js` | HTTP 客户端基础设施 | `default apiClient` | — |
| `auth.js` | 认证（登录/注册/登出/刷新Token） | `default authAPI` | `video-auth` |
| `video.js` | 视频（上传/发布/推荐/分类/标签） | **具名导出** `videoAPI` | `video-video` |
| `user.js` | 用户（个人信息/关注/点赞/收藏/历史） | `default userAPI` | `video-user`、`video-interaction` |
| `comment.js` | 评论（列表/发表/删除） | **具名导出** `commentAPI` | `video-interaction` |
| `friend.js` | 好友（列表/删除/检查） | `default friendAPI` | `video-interaction` |
| `message.js` | 私信（聊天记录/发送/已读） | `default messageAPI` | `video-interaction` |

> ⚠️ **不规范点 #1**：`video.js` 和 `comment.js` 使用**具名导出**（`export const`），而其余模块使用**默认导出**（`export default`）。导出风格不统一，调用方需要与导入方式精确对应，容易出错。

---

### 1.2 API 拦截器工作流（`src/api/index.js`）

```
所有 API 请求
      │
      ▼
┌─────────────────────────────────────────┐
│          REQUEST INTERCEPTOR            │
│  1. 读取 localStorage.accessToken       │
│  2. 若 URL 不在白名单 → 注入 Authorization: Bearer {token} │
│  3. 非 FormData 请求 → 设 Content-Type: application/json  │
└────────────────┬────────────────────────┘
                 │
                 ▼
         发送 HTTP 请求
                 │
      ┌──────────┴───────────┐
      │ 成功 (2xx)           │ 失败 (非 2xx)
      ▼                      ▼
  直接返回              RESPONSE INTERCEPTOR
  response              │
                        ├─ 若 401:
                        │   ① localStorage 清除 token/user
                        │   ② window.store.commit('CLEAR_AUTH')
                        │   ③ router.push('/login')
                        │   ④ alert('登录已过期')
                        │
                        └─ 其他错误: Promise.reject(error)
```

**白名单接口（无需 Token）：**
- `/api/auth/login`、`/api/auth/register`、`/api/auth/refresh`
- `/api/video/list`、`/api/video/*/detail`、`/api/search/**`

> ⚠️ **不规范点 #2**：响应拦截器中调用了 `window.store.commit('CLEAR_AUTH')` 和 `alert()`。
> - `window.store` 是 `main.js` 手动挂载到全局的，是一种**反模式**（绕过模块系统，造成隐式耦合）。
> - 在拦截器里弹 `alert()` 会**阻塞 UI 线程**，体验差，且难以测试。推荐用自定义通知组件替代。

---

## 二、核心业务数据流（按页面）

---

### 2.1 认证流程（Login.vue / Register.vue）

#### 登录数据流

```
用户填写用户名+密码
        │
        ▼
Login.vue → handleLogin()
        │  1. 前端基本校验（非空）
        │
        ▼
authAPI.login({ username, password })
        │  POST /api/auth/login（白名单，不加 Token）
        │
        ▼
后端响应 → response.data.data
        │  { accessToken, userId }
        │
        ├─ 成功 (code === 200)
        │   ① store.dispatch('login', { token, user })
        │       → commit('SET_TOKEN') → localStorage.setItem('accessToken')
        │       → commit('SET_USER') → localStorage.setItem('user')
        │   ② router.push('/')
        │   ③ alert('登录成功')    ← ⚠️ 见下文问题#5
        │
        └─ 失败
            alert(response.data.message)
```

> ⚠️ **不规范点 #3**：**登录/注册后用户信息不完整**。
> 两个页面在成功后，仅将 `{ username, id }` 写入 Store，**没有调用 `/api/user/me` 获取完整的用户 Profile（头像、简介、粉丝数等）**。这导致在跳转到首页后，右上角头像等信息可能显示不出来，需要到 `/profile/me` 页才能补全。
>
> ⚠️ **不规范点 #4**：**用户 ID 容错逻辑有隐患**。
> ```js
> id: data.userId || data.id || 'me'  // 兜底为字符串 'me'
> ```
> 如果后端有时不返回 `userId` 字段，ID 会变成字符串 `'me'`，而不是真实 ID，致使后续需要用 ID 调接口的地方全部出错。
>
> ⚠️ **不规范点 #5**：`Login.vue` 和 `Register.vue` 中大量使用原生 `alert()`，应替换为应用内通知组件。

---

### 2.2 首页推荐流（Home.vue）

```
组件 mounted()
        │
        ▼
store.dispatch('fetchVideoFeed')  [Vuex Action]
        │
        ▼
videoAPI.getVideoFeed(page=1, size=10)
        │  GET /api/video/feed?page=1&size=10
        │
        ▼
后端返回视频列表（含 userId、username、avatar 冗余字段）
        │
        ▼  [store/index.js - fetchVideoFeed action]
        ├─① 遍历视频，构建 usersMap
        │   → commit('UPDATE_USERS_DB', usersMap)
        │
        └─② commit('SET_FEED_VIDEOS', videos)
              → state.feedVideos 更新
              → maxLoadableVideos 重置为 5
              → watchedVideoCount 重置为 0

        ▼
Home.vue 通过 mapState(['feedVideos']) 响应式渲染
        │
        ├─ v-for 遍历 feedVideos
        │   :user="getUser(video.userId)"  [Vuex Getter]
        │
        └─ VideoSlide 组件 @video-watched
              → store.dispatch('onVideoWatched')
              → commit('INCREMENT_WATCHED_COUNT')
              → 每累计5个 → maxLoadableVideos += 5（实现渐进加载）
```

> ⚠️ **不规范点 #6**：**视频数据中冗余了用户信息**。  
> 后端在视频对象中直接返回了 `username`/`avatar` 字段（而非通过单独的用户接口查询）。这种设计在推荐列表场景合理（减少请求），但  Store 的 Action 里把这些数据反向写入了 `usersDB`，形成了**视频服务承担用户信息职责**的混淆。

---

### 2.3 视频上传流（Upload.vue）

这是整个前端最复杂的业务流，分三个独立阶段：

#### 阶段 1：视频文件上传

```
用户点击选择视频文件
        │
        ▼
selectVideo() → 动态创建 <input type="file">
        │
        ▼
uploadVideo()
        │  POST /api/video/upload (multipart/form-data)
        │  大文件 timeout = 100000ms
        │
        ▼
后端返回 → response.data.data = videoUrl（MinIO 存储路径）
        │
        └─ 保存 this.videoUrl = videoUrl
```

#### 阶段 2：封面上传（两种方式）

```
方式 A：本地上传图片
        selectCover() → uploadCover(imageFile)
        POST /api/video/upload/cover

方式 B：从视频截帧（独特功能）
        openVideoFrameSelector()
        用户拖动时间轴 → onSliderChange() → videoPlayer.currentTime = time
        captureAndUploadFrame()
          ① canvas.drawImage(video, ...) 截取当前帧
          ② canvas.toDataURL('image/jpeg') → Blob → File
          ③ videoAPI.uploadCover(frameFile)
          ④ 失败时 fallback → captureFrameWithTempVideo()（新建 video 元素重试）
```

#### 阶段 3：视频发布

```
handleSubmit()
        │  校验：title、videoUrl、coverUrl 均必须存在
        │
        ▼
标签处理：
  selectedTags.filter(tag => tag.id) → existingTagNames（有 ID 的老标签）
  selectedTags.filter(tag => !tag.id) → newTagNames（无 ID 的新标签）
  合并为逗号分隔字符串 → allTagNames
        │
        ▼
videoAPI.publishVideo({
  title, description, coverUrl, videoUrl,
  categoryId: parseInt(formData.category),
  tags: "标签A,标签B,新标签C",  // 后端按名称查找或创建
  isPrivate: 0
})
        │  POST /api/video/publish
        │
        ▼
成功 → router.push('/profile/me')
```

> ⚠️ **不规范点 #7**：**分类是前端硬编码的**。
> ```html
> <option value="1">生活</option>
> <option value="2">娱乐</option>
> ...
> ```
> 分类列表直接写死在模板中（8个固定分类），而 `src/api/video.js` 中已有 `getCategories()` 接口。如果后端分类有变动，前端必须手动修改代码。  
> **改进方案**：组件 `mounted()` 时调用 `getCategories()` 动态拉取分类列表。

> ⚠️ **不规范点 #8**：**`Upload.vue` 中存在大量重复代码**。
> `captureAndUploadFrame()` 和 `captureFrameWithTempVideo()` 内部包含几乎相同的"上传封面"逻辑块，应抽取为 `doUploadCoverFile(file)` 复用方法。

> ⚠️ **不规范点 #9**：**上传进度反馈缺失**。
> 视频上传使用 `alert('视频上传成功')` 作为唯一反馈，没有进度条。大文件上传时（timeout 设置 100 秒），用户全程无任何进度感知，体验极差。

---

### 2.4 用户主页（Profile.vue）

#### 数据加载策略（二态逻辑）

```
路由参数: /profile/:userId
        │
        ├─ userId === 'me'（自己的主页）
        │   mounted() → loadCurrentUser()
        │     fetch('/api/user/me', { Authorization: Bearer })  ← ⚠️ 见下文
        │     → store.dispatch('updateUser', result.data)
        │     → Vuex state.user 更新
        │     → computed profileUser 自动重新计算
        │
        └─ userId !== 'me'（他人主页）
            watch userId → loadUserProfile(userId)
              fetch(`/api/user/profile/${userId}`, ...)  ← ⚠️ 见下文
              若 result.data.isFollowing 为空
                → userAPI.isFollowing(userId)（补充查询关注状态）
              → commit('UPDATE_USERS_DB', { [userId]: {...} })
              → computed profileUser = getUser(userId)（从 Vuex 读取）
```

> ⚠️ **不规范点 #10（严重）**：**直接使用原生 `fetch` 而非封装好的 Axios 实例**。
> `Profile.vue` 中的 `loadCurrentUser()` 和 `loadUserProfile()` 均使用原生 `fetch` 并手动拼接 `Authorization` Header，**没有走 `src/api/index.js` 中的 Axios 实例**。
>
> 这造成了以下问题：  
> ① 401 自动处理逻辑（跳转登录）失效——需要手动实现  
> ② 请求拦截器（自动注入 Token）逻辑绕过  
> ③ 与项目其他 API 调用方式完全不一致，维护成本高  
>
> **正确做法**：使用 `userAPI.getCurrentUser()` 和 `userAPI.getUserProfile(userId)`，统一走 Axios 实例。

> ⚠️ **不规范点 #11**：**`methods` 属性被重复定义了两次**。
> `Profile.vue` 的 `<script>` 中，`methods` 块出现了两次（第 99 行和第 179 行），后面的会覆盖前面的。这会导致前面 `methods` 中定义的 `handleToggleFollow` 和 `handleFollowStatusUpdate` 被后面的同名方法覆盖，而 `loadUserProfile` 和 `loadCurrentUser` 被合并进来。虽然结果可能正常，但这是**严重的代码结构错误**，应合并为一个 `methods` 块。

---

### 2.5 视频分类页（Category.vue）

这是功能**最重、最独立**的页面，自身实现了完整的互动闭环。

#### 视频列表加载

```
mounted() → loadCategoryVideos()
        │
        ├─ videoAPI.getVideosByCategory(categoryId, 1, 20)
        │   GET /api/video/category/{id}?page=1&size=20
        │
        └─ videoAPI.getCategories()
            GET /api/video/categories
            → 根据 categoryId 查找 → 设置 categoryName
```

#### 视频播放弹窗与互动闭环

```
用户点击视频卡片
        │
        ▼
openVideoModal(video)
  ① 停止所有预览视频
  ② this.selectedVideo = video（乐观设置）
  ③ 禁止背景滚动 (document.body.style.overflow = 'hidden')
  ④ $nextTick 中：
      a. videoAPI.getVideoDetail(video.id) → 刷新最新数据
      b. checkLikeStatus() → userAPI.checkVideoLiked(videoId)
      c. checkFavoriteStatus() → userAPI.checkVideoFavorited(videoId)
      d. loadComments() → commentAPI.getComments(videoId)

        ▼
视频开始播放 @play → onModalVideoPlay()
  → 若已登录 → userAPI.recordWatchHistory(videoId, 0, 0)（初始记录）

        ▼
视频播放中 @timeupdate → onModalVideoTimeUpdate()
  → 每 3000~5000ms（随机间隔）更新一次观看进度
  → userAPI.recordWatchHistory(videoId, currentTime, progress%)

        ▼
互动操作（点赞/收藏/评论）
  ├─ toggleLike()
  │   ├─ 点赞: userAPI.likeVideo(videoId) → localLikeCount++
  │   └─ 取消: userAPI.unlikeVideo(videoId) → localLikeCount--
  │
  ├─ toggleFavorite()
  │   ├─ 收藏: userAPI.favoriteVideo(videoId) → localFavoriteCount++
  │   └─ 取消: userAPI.unfavoriteVideo(videoId) → localFavoriteCount--
  │
  └─ submitComment()
      → commentAPI.postComment({ videoId, content, parentId, replyUserId })
      → 乐观更新本地评论列表
```

> ⚠️ **不规范点 #12**：**`Category.vue` 体量过大（1588 行）**。  
> 该页面将视频列表、弹窗播放器、点赞/收藏/评论/观看历史等所有逻辑全部写在一个 `.vue` 文件中，违反了单一职责原则。
> 极大影响可读性和可维护性。应拆分为：
> - `VideoModal.vue`（弹窗容器）
> - `VideoInteractionPanel.vue`（点赞/收藏/分享）
> - `CommentSection.vue`（评论区）
>
> ⚠️ **不规范点 #13**：**作者粉丝数硬编码**。
> ```html
> <div class="author-stats">1.2万 粉丝</div>  <!-- 硬编码！ -->
> ```
> 弹窗内作者信息区域的粉丝数是写死的字符串，没有调用 API 获取。

---

## 三、数据流向全局总结图

```
                   ┌─────────────────────────────────┐
                   │        Vuex Store                │
                   │  state.feedVideos                │
                   │  state.usersDB                   │
                   │  state.token / state.user        │
                   │  state.maxLoadableVideos         │
                   └──────┬──────────────────┬────────┘
                          │ mapState/         │ commit/dispatch
                          │ mapGetters        │
          ┌───────────────▼──────────────┐   │
          │           Views              │   │
          │  ┌────────┐  ┌───────────┐  │   │
          │  │Home    │  │Profile    │  │   │
          │  │(读Store)│  │(混合:     │   │
          │  │        │  │Store+fetch)│  │   │
          │  └───┬────┘  └─────┬─────┘  │   │
          │  ┌───▼────┐  ┌─────▼─────┐  │   │
          │  │Upload  │  │Category   │  │   │
          │  │(不涉及 │  │(完全本地  │  │   │
          │  │Store)  │  │state管理) │  │   │
          │  └───┬────┘  └─────┬─────┘  │   │
          └──────┼─────────────┼────────┘   │
                 │  直接调用    │            │
                 ▼             ▼            ▼
          ┌──────────────────────────────────────┐
          │           src/api/*.js               │
          │  auth / video / user / comment /     │
          │  friend / message                    │
          └────────────────┬─────────────────────┘
                           │ Axios
                           ▼
                  api/index.js (拦截器)
                           │
                           ▼
                    API 网关 :8080
                           │
                    Spring Cloud 微服务群
```

---

## 四、不规范问题汇总清单

| 编号 | 文件 | 严重度 | 问题描述 | 建议 |
|------|------|--------|---------|------|
| #1 | `api/video.js`, `api/comment.js` | 低 | 具名导出与其他模块默认导出风格不统一 | 统一为 `export default` |
| #2 | `api/index.js` | 中 | 拦截器中使用 `window.store` 反模式 + `alert()` 阻塞 UI | 用 EventBus/通知组件替代 |
| #3 | `Login.vue`, `Register.vue` | 中 | 登录/注册成功后不获取完整用户信息 | 成功后调用 `/api/user/me` 补充用户数据 |
| #4 | `Login.vue`, `Register.vue` | 中 | 用户 ID 兜底为字符串 `'me'` | 明确后端返回字段名称，不设字符串兜底 |
| #5 | `Login.vue`, `Register.vue` | 低 | 全部交互反馈使用原生 `alert()` | 使用应用内 Toast/通知组件 |
| #6 | `store/index.js` | 低 | 视频接口返回用户信息写回 `usersDB`，职责混淆 | 考虑用户数据来源统一走 user-service |
| #7 | `Upload.vue` | 中 | 分类列表硬编码在模板中 | 改为 `mounted()` 动态调用 `getCategories()` |
| #8 | `Upload.vue` | 低 | 封面上传逻辑重复两次 | 抽取 `doUploadCoverFile(file)` 方法 |
| #9 | `Upload.vue` | 高 | 大文件上传无进度条 | 使用 Axios `onUploadProgress` 回调实现进度条 |
| #10 | `Profile.vue` | **严重** | 直接使用原生 `fetch` 绕过 Axios 拦截器 | 改用 `userAPI.getCurrentUser()` 等封装方法 |
| #11 | `Profile.vue` | **严重** | `methods` 选项被重复声明两次 | 合并为一个 `methods` 块 |
| #12 | `Category.vue` | 高 | 单文件 1588 行，严重违反单一职责 | 拆分为 `VideoModal`、`CommentSection` 等子组件 |
| #13 | `Category.vue` | 中 | 作者粉丝数硬编码为 `1.2万 粉丝` | 从视频详情或用户接口获取真实数据 |

---

---

## 五、后端完整 API 端点清单

> **路由拼接规则**（主微服务群）：  
> 网关统一监听 `:8080`，各服务注册到 Nacos 后网关做路由转发。  
> 前端请求 `/api/{服务前缀}/...` → 网关剔除 `/api` 前缀转发给对应微服务。  
> 例：`POST /api/auth/login` → `video-auth` 服务 → `AuthController@POST /auth/login`

---

### 5.1 认证服务（`video-auth` → 前缀 `/api/auth`）

| 方法 | 路径 | 鉴权 | 业务说明 | 请求体 / 返回 |
|------|------|------|---------|---------------|
| `POST` | `/api/auth/login` | ⬜ 无需（白名单） | 用户登录 | `{ username, password }` → `{ accessToken, refreshToken, userId }` |
| `POST` | `/api/auth/register` | ⬜ 无需（白名单） | 用户注册 | `{ username, email, password }` → `{ accessToken, refreshToken, userId }` |
| `POST` | `/api/auth/logout` | 🔐 需要（X-User-Id Header） | 退出登录（使 Token 失效） | 无 Body → 无 |
| `POST` | `/api/auth/refresh` | ⬜ 无需（白名单） | 刷新 AccessToken | Body: `refreshToken` 字符串 → `TokenResponse` |

---

### 5.2 用户服务（`video-user` → 前缀 `/api/user`）

#### 5.2.1 用户信息（`UserController`）

| 方法 | 路径 | 鉴权 | 业务说明 | 说明 |
|------|------|------|---------|------|
| `GET` | `/api/user/profile/{userId}` | 🔐 需要 | 获取指定用户的公开信息 | 返回 `UserProfileVO` |
| `GET` | `/api/user/me` | 🔐 需要（X-User-Id） | 获取当前登录用户信息 | 同上，userId 从 Header 取 |
| `PUT` | `/api/user/update` | 🔐 需要（X-User-Id） | 更新用户资料（昵称/简介/生日等） | `UserUpdateDTO` |
| `POST` | `/api/user/avatar` | 🔐 需要（X-User-Id） | 更新用户头像 | `multipart/form-data`, field=`avatar` → 返回头像 URL |
| `POST` | `/api/user/batch` | 🔐 需要 | 批量获取用户信息 | `[userId1, userId2, ...]` → `List<UserProfileVO>` |
| `POST` | `/api/user/update-like-count` | 🔐 内部调用 | 更新用户获赞总数（被互动服务调用） | `?userId=&delta=` |

#### 5.2.2 关注关系（`FollowController`）

| 方法 | 路径 | 鉴权 | 业务说明 |
|------|------|------|----------|
| `POST` | `/api/user/follow/{targetUserId}` | 🔐 需要 | 关注指定用户 |
| `DELETE` | `/api/user/follow/{targetUserId}` | 🔐 需要 | 取消关注指定用户 |
| `GET` | `/api/user/follow/check/{targetUserId}` | 🔐 需要 | 检查是否已关注某用户 → `Boolean` |
| `GET` | `/api/user/follow/following` | 🔐 需要（X-User-Id） | 获取**自己**的关注列表 |
| `GET` | `/api/user/follow/following/{targetUserId}` | 🔐 需要 | 获取**他人**的关注列表 |
| `GET` | `/api/user/follow/followers` | 🔐 需要（X-User-Id） | 获取**自己**的粉丝列表 |
| `GET` | `/api/user/follow/followers/{targetUserId}` | 🔐 需要 | 获取**他人**的粉丝列表 |

---

### 5.3 视频服务（`video-video` → 前缀 `/api/video`）

| 方法 | 路径 | 鉴权 | 业务说明 | 备注 |
|------|------|------|---------|------|
| `GET` | `/api/video/feed` | 🔐 需要 | 获取推荐视频流 | `?page=1&size=10` |
| `GET` | `/api/video/{videoId}` | ⬜ 白名单（`/api/video/*/detail` 例外） | 获取视频详情 | 注意：白名单匹配有歧义，见下方说明 |
| `POST` | `/api/video/upload` | 🔐 需要（X-User-Id） | 上传视频文件 | `multipart/form-data`, field=`file` → 返回 MinIO URL |
| `POST` | `/api/video/upload/cover` | 🔐 需要（X-User-Id） | 上传封面图片 | `multipart/form-data`, field=`file` → 返回封面 URL |
| `POST` | `/api/video/publish` | 🔐 需要（X-User-Id） | 发布视频（提交完整信息） | `VideoPublishDTO` → 返回 `videoId` |
| `GET` | `/api/video/user/{userId}` | 🔐 需要 | 获取指定用户的视频列表 | `?page&size`；本人可见所有状态，他人仅见已过审 |
| `DELETE` | `/api/video/{videoId}` | 🔐 需要（X-User-Id） | 删除视频（仅本人） | — |
| `GET` | `/api/video/categories` | 🔐 需要 | 获取视频分类列表 | 返回所有分类（管理后台写入） |
| `GET` | `/api/video/category/{categoryId}` | 🔐 需要 | 按分类获取视频列表 | `?page&size` |
| `GET` | `/api/video/tags` | 🔐 需要 | 获取所有视频标签 | — |
| `GET` | `/api/video/batch` | 🔐 需要 | 批量获取视频详情 | `?ids=1,2,3` |
| `GET` | `/api/video/{videoId}/reject-reason` | 🔐 需要 | 获取视频最新审核拒绝原因 | 供用户查看审核结果 |

> ⚠️ **白名单歧义说明**：网关白名单配置为 `/api/video/*/detail`，而 `VideoController` 实际路径是 `/api/video/{videoId}`（无 `/detail` 后缀），因此视频详情接口**实际上并不在白名单中**，需要登录才能访问。前后端白名单定义不一致。

---

### 5.4 互动服务（`video-interaction` → 前缀 `/api/interaction`）

#### 5.4.1 点赞（`LikeController`）

| 方法 | 路径 | 业务说明 |
|------|------|----------|
| `POST` | `/api/interaction/like/{videoId}` | 点赞视频 |
| `DELETE` | `/api/interaction/like/{videoId}` | 取消点赞 |
| `GET` | `/api/interaction/like/check/{videoId}` | 检查是否已点赞 → `Boolean` |
| `GET` | `/api/interaction/like/user` | 获取**自己**点赞的视频列表（X-User-Id） |
| `GET` | `/api/interaction/like/user/{targetUserId}` | 获取**指定用户**点赞的视频列表 |

#### 5.4.2 收藏（`FavoriteController`）

| 方法 | 路径 | 业务说明 |
|------|------|----------|
| `POST` | `/api/interaction/favorite/{videoId}` | 收藏视频 |
| `DELETE` | `/api/interaction/favorite/{videoId}` | 取消收藏 |
| `GET` | `/api/interaction/favorite/check/{videoId}` | 检查是否已收藏 → `Boolean` |
| `GET` | `/api/interaction/favorite/user` | 获取**自己**的收藏列表（X-User-Id） |
| `GET` | `/api/interaction/favorite/user/{targetUserId}` | 获取**指定用户**的收藏列表 |

#### 5.4.3 评论（`CommentController`）

| 方法 | 路径 | 业务说明 |
|------|------|----------|
| `GET` | `/api/interaction/comment/{videoId}` | 获取视频评论列表（含分页） |
| `POST` | `/api/interaction/comment` | 发表评论或回复 `{ videoId, content, parentId, replyUserId }` |
| `DELETE` | `/api/interaction/comment/{commentId}` | 删除评论（仅本人） |

#### 5.4.4 观看历史（`HistoryController`）

| 方法 | 路径 | 业务说明 |
|------|------|----------|
| `POST` | `/api/interaction/history` | 记录/更新观看历史 `{ videoId, watchDuration, watchProgress }` |
| `GET` | `/api/interaction/history` | 获取自己的观看历史列表（X-User-Id） |
| `DELETE` | `/api/interaction/history/{historyId}` | 删除某条历史记录 |

#### 5.4.5 好友关系（`FriendshipController`）

| 方法 | 路径 | 业务说明 | 备注 |
|------|------|---------|------|
| `POST` | `/api/interaction/friendship` | 添加好友 `?targetUserId=` | 互相关注时自动触发 |
| `DELETE` | `/api/interaction/friendship/{targetUserId}` | 删除好友 | 同时异步取关（在新线程中执行） |
| `GET` | `/api/interaction/friendship/check` | 检查是否为好友 `?targetUserId=` |
| `GET` | `/api/interaction/friendship/list` | 获取原始好友关系记录列表 |
| `GET` | `/api/interaction/friendship/friend-list` | 获取好友列表（含用户信息） | 前端实际调用此接口 |

#### 5.4.6 私信（`MessageController`）

| 方法 | 路径 | 业务说明 |
|------|------|----------|
| `GET` | `/api/interaction/message` | 获取消息列表 `?page&size&isRead` |
| `POST` | `/api/interaction/message` | 发送消息 `{ toUserId, type, content, relatedId }` |
| `POST` | `/api/interaction/message/{messageId}/read` | 标记消息为已读 |
| `GET` | `/api/interaction/message/chat/{targetUserId}` | 获取与指定用户的聊天记录 |

---

### 5.5 搜索服务（`video-search` → 前缀 `/api/search`）

> ⚠️ **注意**：`SearchController` 类上标注的是 `@RequestMapping("/api/search")`（含 `/api` 前缀），与其他微服务不同（其他服务类上只有业务前缀，`/api` 由网关路由规则拼接）。这可能导致路由冲突或双重前缀问题，需核查网关路由配置。

| 方法 | 路径 | 鉴权 | 业务说明 |
|------|------|------|----------|
| `GET` | `/api/search` | ⬜ 白名单 | 视频全文搜索 `?keyword=&page=&size=&sortBy=viewCount/createdAt&sortOrder=asc/desc` |
| `GET` | `/api/search/hot` | ⬜ 白名单 | 获取热搜榜 `?type=today/recent&days=3&top=10`（基于 Redis ZSet） |

---

### 5.6 管理后台服务（`admin-backend` → 独立服务，约 `:8081`）

> 管理后台是独立的 Spring Boot 单体服务，有自己的 JWT 验证（`admin_token`），不经过主系统的 Nacos 网关。

#### 5.6.1 管理员认证（`AuthController`）

| 方法 | 路径 | 鉴权 | 业务说明 |
|------|------|------|----------|
| `POST` | `/api/auth/login` | ⬜ 无需 | 管理员登录（校验 role='admin'）→ 返回 `{ token, userId, username, role }` |

#### 5.6.2 仪表盘统计（`AdminController`）

| 方法 | 路径 | 鉴权 | 业务说明 |
|------|------|------|----------|
| `GET` | `/api/admin/stats` | 🔐 需要 | 获取系统统计数据（用户数、视频数、待审核数等）→ `AdminStatsVO` |

#### 5.6.3 视频审核（`VideoAuditController`）

| 方法 | 路径 | 业务说明 |
|------|------|----------|
| `GET` | `/api/admin/audit/pending` | 获取待审核视频列表（分页） |
| `POST` | `/api/admin/audit/{videoId}/approve` | 审核通过视频 `{ reason }` |
| `POST` | `/api/admin/audit/{videoId}/reject` | 审核拒绝视频 `{ reason }` （reason 必填）|
| `GET` | `/api/admin/audit/logs` | 获取审核日志（分页，可按 videoId 筛选） |
| `POST` | `/api/admin/audit/logs/{logId}/approve` | 从审核日志重新标记通过 |
| `POST` | `/api/admin/audit/logs/{logId}/reject` | 从审核日志重新标记拒绝 |
| `GET` | `/api/admin/audit/logs/latest/{videoId}` | 获取视频最新审核记录 |
| `GET` | `/api/admin/audit/video/{videoId}` | 获取视频审核详情 |

#### 5.6.4 视频管理（`VideoManageController`）

| 方法 | 路径 | 业务说明 |
|------|------|----------|
| `GET` | `/api/admin/videos` | 获取视频列表（多条件筛选分页） |
| `GET` | `/api/admin/videos/{id}` | 获取视频详情 |
| `PUT` | `/api/admin/videos/{id}` | 修改视频信息 |
| `DELETE` | `/api/admin/videos/{id}` | 删除视频 |
| `POST` | `/api/admin/videos/{id}/status` | 更新视频状态 `?status=` |

#### 5.6.5 用户管理（`UserManageController`）

| 方法 | 路径 | 业务说明 |
|------|------|----------|
| `GET` | `/api/admin/users` | 获取用户列表（多条件筛选分页） |
| `GET` | `/api/admin/users/{id}` | 获取用户详情 |
| `PUT` | `/api/admin/users/{id}` | 修改用户信息 |
| `POST` | `/api/admin/users/{id}/status` | 封禁/解封用户 `?status=` |
| `POST` | `/api/admin/users/{id}/role` | 修改用户角色 `?role=` |
| `GET` | `/api/admin/users/{id}/videos` | 获取指定用户的视频列表 |

#### 5.6.6 分类管理（`CategoryManageController`）

| 方法 | 路径 | 业务说明 |
|------|------|----------|
| `GET` | `/api/admin/categories` | 获取分类树 |
| `POST` | `/api/admin/categories` | 创建分类 |
| `PUT` | `/api/admin/categories/{id}` | 更新分类 |
| `DELETE` | `/api/admin/categories/{id}` | 删除分类 |
| `PUT` | `/api/admin/categories/{id}/sort` | 更新分类显示排序 `?sort=` |

#### 5.6.7 标签管理（`TagManageController`）

| 方法 | 路径 | 业务说明 |
|------|------|----------|
| `GET` | `/api/admin/tags` | 获取标签列表（分页） |
| `GET` | `/api/admin/tags/{id}` | 获取标签详情 |
| `POST` | `/api/admin/tags` | 创建标签 |
| `PUT` | `/api/admin/tags/{id}` | 更新标签 |
| `DELETE` | `/api/admin/tags/{id}` | 删除标签 |

---

### 5.7 API 统计汇总

| 服务 | Controller 数 | 接口总数 | 覆盖领域 |
|------|-------------|---------|----------|
| `video-auth` | 1 | 4 | 登录/注册/登出/刷新 |
| `video-user` | 2 | 13 | 用户信息 + 关注关系 |
| `video-video` | 1 | 11 | 视频全生命周期 |
| `video-interaction` | 6 | 20 | 点赞/收藏/评论/历史/好友/私信 |
| `video-search` | 1 | 2 | 搜索 + 热搜榜 |
| `admin-backend` | 6 | 27 | 管理后台全套 |
| **合计** | **17** | **77** | — |

---

*本文档由架构师扫描代码后自动生成。*
