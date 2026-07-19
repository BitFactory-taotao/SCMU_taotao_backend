# AGENTS.md — SCMU_taotao 项目 AI 编码助手指令

> **最后更新**：2026-07-18
> **CodeGraph 索引**：已初始化 (`.codegraph/codegraph.db`)

---

## 1. Project Overview（项目简介）

**项目名称**：SCMU_taotao（校园淘淘）
**项目类型**：校园二手交易平台
**基础包路径**：`com.bit.scmu_taotao`
**构建工具**：Maven（使用 Maven Wrapper：`mvnw`）
**Context Path**：`/api/v1/campus-taotao`
**服务端口**：默认 8080（Spring Boot）

### 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.5.11 |
| ORM | MyBatis-Plus | 3.5.16 |
| 数据库 | MySQL | 8.x (mysql-connector-j) |
| 缓存 | Redis（Lettuce 连接池 + Commons Pool2） | — |
| 对象存储 | AWS SDK v2 → 对接阿里云 OSS S3 协议 | — |
| JSON | Jackson（jackson-databind + jsr310） | — |
| HTTP 客户端 | Apache HttpClient | 4.5.14 |
| HTML 解析 | Jsoup | 1.18.3 |
| 工具库 | Hutool | 5.8.36 |
| 简化代码 | Lombok | — |
| 文件上传 | Commons FileUpload | 1.5 |
| 校验 | Jakarta Bean Validation（spring-boot-starter-validation） | — |
| WebSocket | spring-boot-starter-websocket（STOMP 协议） | — |
| 测试 | spring-boot-starter-test（JUnit 5 + MockMvc） | — |

### 核心业务模块

- **商品管理**：发布、搜索、分类浏览（宿舍用品/娱乐用品/学习用品/预售）
- **用户系统**：登录认证（Token + Redis）、个人主页、信用体系
- **收藏与黑名单**：软删除模式
- **即时通讯**：WebSocket STOMP 聊天、会话管理
- **管理后台**：商品审核、反馈管理、统计工作台
- **推荐系统**：个性化推荐 + 冷启动热门推荐，基于分类偏好/收藏/点击/时效的加权评分
- **敏感词过滤**：商品发布时校验
- **图片上传**：S3 协议对接阿里云 OSS
- **风险账号审核**：信用分 < 70 自动标记、多维风险指标、查封/消除操作
- **解决事项总台**：聚合商品审核/反馈/账号审核已处理记录的查阅、详情和撤销

---

## 2. Project Structure（目录结构说明）

```
src/main/java/com/bit/scmu_taotao/
├── ScmuTaotaoApplication.java   # Spring Boot 启动类（@MapperScan, @EnableScheduling）
├── client/                      # HTTP 客户端（WebVPN 登录）
│   ├── HttpResponseHandler.java / HttpResponseHandlerImpl.java
│   ├── HttpResponseResult.java
│   └── thread/WebVpnLoginThread.java
├── config/                      # Spring @Configuration
│   ├── CorsConfig.java          # 跨域：全局放行 *，允许 Cookie
│   ├── MyBatisPlusConfig.java   # 分页插件 + 自动填充（createTime/updateTime）
│   ├── RedisConfig.java         # GenericJackson2JsonRedisSerializer 序列化
│   ├── WebConfig.java           # 拦截器注册（LoginInterceptor）
│   ├── WebSocketConfig.java     # STOMP WebSocket 配置
│   ├── WebSocketPrincipalHandshakeHandler.java
│   ├── TestOnlyAuthProperties.java
│   └── storage/
│       ├── S3ClientConfig.java
│       └── S3StorageProperties.java
├── controller/
│   ├── GoodsController.java           # 商品 CRUD + 收藏 + 草稿
│   ├── LoginController.java           # WebVPN 登录/登出
│   ├── MessagesController.java        # STOMP WebSocket 消息处理
│   ├── MessagesRestController.java    # 聊天 REST 接口
│   ├── RecommendationController.java  # 推荐系统（@RequestMapping("/goods")）
│   ├── TBlacklistController.java      # 黑名单
│   ├── TEvaluateController.java       # 交易评价
│   ├── TFeedbackController.java       # 用户反馈
│   ├── TradeController.java           # 交易确认/拒绝/撤回
│   ├── UserController.java            # 用户中心
│   ├── FileUploadController.java      # 图片上传
│   ├── admin/
│   │   ├── AdminAuthController.java         # 管理员登录
│   │   ├── AdminFeedbackController.java     # 反馈管理
│   │   ├── AdminGoodsAuditController.java   # 商品审核
│   │   ├── AdminReportController.java       # 举报管理
│   │   ├── AdminRiskUserController.java     # 风险用户管理
│   │   ├── AdminSolvedItemsController.java  # 解决事项总台
│   │   └── AdminStatisticsController.java   # 工作台概览统计
│   └── testonly/
│       └── TestAuthController.java    # 测试环境 Token 签发
├── dto/                           # 按功能分包
│   ├── admin/                     # 管理后台 DTO（20+ 个）
│   ├── chat/                      # 聊天消息 DTO
│   ├── FeedbackDto/               # 反馈 DTO
│   ├── goods/                     # GoodsRequestDTO, GoodsResponseDTO, SearchRequestDTO, PublisherDTO
│   ├── recommend/                 # 推荐系统 DTO
│   ├── testonly/
│   ├── upload/
│   └── *.java                     # 顶层通用 DTO
├── entity/                        # MyBatis-Plus 实体（18 张表）
│   ├── TUser.java, TAdmin.java, TCreditLog.java
│   ├── TGoods.java, TGoodsCategory.java, TGoodsImage.java
│   ├── TTrade.java, TEvaluate.java, TEvaluateImage.java
│   ├── TAccountAuditLog.java
│   ├── TFavorite.java, TBlacklist.java, TFeedback.java, TUserReport.java
│   ├── ChatSession.java, ChatMessage.java
│   ├── RecommendationConfig.java, UserGoodsBrowse.java
├── exception/
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   ├── SensitiveWordException.java
│   └── StorageException.java
├── handler/MessageWebSocketHandler.java
├── interceptor/
│   ├── LoginInterceptor.java        # Token 校验（详见 §9.4）
│   └── WebSocketAuthHandshakeInterceptor.java
├── mapper/                          # MyBatis-Plus Mapper（18 个，extends BaseMapper）
├── service/
│   ├── *.java                       # Service 接口（extends IService）
│   ├── impl/*.java                  # Service 实现
│   ├── storage/                     # ObjectStorageService + S3ObjectStorageServiceImpl
│   ├── upload/                      # ImageUploadService + ImageUploadServiceImpl
│   └── testonly/
└── util/
    ├── common/
    │   ├── Result.java              # 统一响应体 {code, msg, data, total}
    │   └── KeyDescription.java
    ├── TokenUtil.java               # Token 生成/校验/销毁（基于 Redis）
    ├── UserContext.java             # ThreadLocal 用户上下文
    ├── LoginProcessMessage.java
    └── storage/
        ├── ObjectKeyGenerator.java
        └── ObjectKeyParser.java
```

---

## 3. Coding Guidelines（编码规范）

### 3.1 命名规范

- **实体类**：以 `T` 前缀开头（如 `TGoods`, `TUser`），对应数据库表 `t_goods`, `t_user`
- **Service 接口**：`XxxService extends IService<Entity>`
- **Service 实现**：`XxxServiceImpl extends ServiceImpl<Mapper, Entity> implements XxxService`
- **Mapper 接口**：`XxxMapper extends BaseMapper<Entity>`
- **Controller**：`XxxController`，`@RestController` + `@RequestMapping("/xxx")`
- **DTO**：以 `DTO` 或 `Request/Response` 结尾
- **变量与方法**：小驼峰（`goodsId`, `getGoodsImageUrls`）
- **数据库字段**：下划线命名（`goods_id`, `create_time`），MyBatis-Plus 自动映射

### 3.2 注解使用

- 实体类使用 Lombok `@Data`
- Controller/ServiceImpl 使用 `@Slf4j`
- 依赖注入使用 `@Autowired` 字段注入
- 实体主键 `@TableId(type = IdType.AUTO)`，TUser 用 `IdType.ASSIGN_ID`
- 实体表名 `@TableName(value = "t_xxx")`
- 时间字段：`@TableField(fill = FieldFill.INSERT)` + `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")`

### 3.3 代码风格

- **注释使用中文**
- **日志使用 SLF4J**：`log.info/warn/error/debug`，严禁 `System.out.println`
- **统一返回 `Result`**：`Result.ok(data)` / `Result.ok(list, total)` / `Result.fail(msg)`
- **分页查询**用 MyBatis-Plus `Page` 对象
- **Lambda 查询**优先用 `LambdaQueryWrapper`
- **软删除**：所有实体 `is_delete` 字段（0=正常, 1=已删除）
- **参数安全校验**：Controller 层手动校验 `page`/`size`，`Math.min(size, 50)`

### 3.4 JSON 序列化

- 全局 `default-property-inclusion: non_null`，null 值自动省略
- 日期格式 `yyyy-MM-dd HH:mm:ss`，时区 `GMT+8`

### 3.5 认证与鉴权

- Token 放在 `Authorization` 头，格式 `Bearer <token>` 或纯 token
- `LoginInterceptor.preHandle()` → `UserContext.setUserId(finalId)`
- `afterCompletion` → `UserContext.remove()` 清理 ThreadLocal
- 公开 GET 无需 Token：`/goods`, `/goods/search`, `/goods/{id}`, `/user/{id}/home`
- 管理后台 `/admin/**` 仅允许管理员 Token

---

## 4. Modification Rules（修改约束）

### 4.1 最小改动原则

- 只修改与任务直接相关的文件，不重构无关代码
- 不改变已有注解和配置风格
- 不动 `.idea/`、`target/` 和根目录 Markdown

### 4.2 新功能添加

1. Controller 参考 `GoodsController` 写法，添加中文注释
2. Service **先在 Interface 加方法签名**，再在 Impl 实现
3. 新实体放 `entity/`，遵循 `T` 前缀
4. 新 DTO 按功能放子包，用 `@Data`
5. 新 Mapper 继承 `BaseMapper<Entity>`

### 4.3 禁止行为

- **禁止修改 `pom.xml`**
- **禁止删除文件**
- **禁止重构**（重命名类/方法/变量）
- **禁止使用 `System.out.println`**
- **禁止硬编码敏感信息**
- **禁止关闭登录拦截器或放宽鉴权**

---

## 5. Dependencies（依赖使用规则）

| 场景 | 使用 | 不使用 |
|------|------|--------|
| ORM 查询 | MyBatis-Plus `LambdaQueryWrapper` | 原生 JDBC / JPA |
| JSON 处理 | Jackson `ObjectMapper` | Gson / Fastjson |
| HTTP 请求 | Apache `HttpClient` | OkHttp / RestTemplate |
| HTML 解析 | Jsoup | 正则表达式 |
| 工具方法 | Hutool | Guava |
| S3 存储 | `ObjectStorageService` | 直接 AWS SDK |
| 缓存 | `RedisService` | 直接 RedisTemplate |

---

## 6. Testing（测试）

- JUnit 5 + `@SpringBootTest` + MockMvc
- 测试类命名：`XxxControllerTest` 或 `XxxIntegrationTest`
- 运行：`./mvnw test -Dtest=XxxControllerTest`
- **不主动创建测试文件**，除非用户要求

---

## 7. Common Tasks（常见开发流程）

### 7.1 新增业务接口

1. `dto/` 创建 Request/Response DTO
2. `service/XxxService.java` 添加方法签名
3. `service/impl/XxxServiceImpl.java` 实现
4. `controller/` 添加 `@GetMapping` / `@PostMapping`
5. 复杂 SQL 在 Mapper 注解或 XML 中编写

### 7.2 项目构建与启动

```bash
./mvnw clean compile          # 编译
./mvnw clean package -DskipTests  # 打包
./mvnw spring-boot:run             # 启动（需 MySQL + Redis）
./mvnw spring-boot:run -Dspring-boot.run.profiles=test  # test profile
```

---

## 8. Forbidden Actions（禁止行为）

- 禁止删除文件
- 禁止修改 `pom.xml`
- 禁止修改 `.idea/`、`.gitignore`
- 禁止修改 `application.yaml` 中的数据库/Redis 连接信息
- 禁止重构
- 禁止 `System.out.println`
- 禁止硬编码敏感信息
- 禁止关闭或放宽鉴权
- 禁止绕过敏感词过滤
- 禁止自动 `git commit` / `push` / 创建分支

---

## 9. Architecture Context（AI 架构上下文）

> **面向 AI 助手的项目结构速查，每次大规模改动后需同步更新。**

### 9.1 数据库实体关系（18 张表）

所有实体均软删除（`is_delete`: 0=正常, 1=已删除）。

#### 用户域

| 表 | 实体 | 主键 | 关键字段 |
|----|------|------|----------|
| `t_user` | TUser | userId (String/学号, ASSIGN_ID) | userName, avatar, creditScore, creditStar, **status**(0=正常/1=违规查封/2=有风险), violationReason, createTime |
| `t_admin` | TAdmin | id (Long, AUTO) | adminId, password, nickname |
| `t_credit_log` | TCreditLog | id (Long, AUTO) | userId→TUser, scoreChange, changeType |

#### 商品域

| 表 | 实体 | 主键 | 关键字段 |
|----|------|------|----------|
| `t_goods` | TGoods | goodsId (Long, AUTO) | userId→TUser, categoryId→TGoodsCategory, goodsType(1=出售/2=预购), goodsName, goodsNote, goodsDesc, price(BigDecimal), **useScene, exchangePlace**, goodsStatus(0=在售/1=成交/2=下架/3=审核), **viewCount, isAudited(0=未审/1=已审/2=违规下架), rejectReason**, createTime |
| `t_goods_category` | TGoodsCategory | categoryId (Int, AUTO) | categoryName, sort, isShow(0=展示/1=隐藏) |
| `t_goods_image` | TGoodsImage | imageId (Long, AUTO) | goodsId→TGoods, imageUrl, sort |

#### 交易域

| 表 | 实体 | 主键 | 关键字段 |
|----|------|------|----------|
| `t_trade` | TTrade | tradeId (Long, AUTO) | goodsId→TGoods, sellerId/buyerId→TUser, tradePrice, **tradePlace, tradeTime** |
| `t_evaluate` | TEvaluate | evalId (Long, AUTO) | tradeId→TTrade, goodsId, buyerId, sellerId, descScore, commScore, totalScore, evalContent, isAnonymous |
| `t_evaluate_image` | TEvaluateImage | evalImgId (Long, AUTO) | evalId→TEvaluate, imgUrl |

#### 互动域

| 表 | 实体 | 主键 | 关键字段 |
|----|------|------|----------|
| `t_favorite` | TFavorite | favoriteId (Long, AUTO) | userId, goodsId |
| `t_blacklist` | TBlacklist | blackId (Long, AUTO) | userId, blackUserId |
| `t_feedback` | TFeedback | feedbackId (Long, AUTO) | userId, feedbackContent, feedbackStatus(0=待/1=中/2=复/3=闭), **isRead(0=未读/1=已读)**, replyContent, replyTime |
| `t_user_report` | TUserReport | id (Long, AUTO) | reporterId, targetId, tag(LOW_CREDIT/GOODS_VIOLATION/LANG_VIOLATION/OTHER), content, imgUrls, status(0=待/1=已处理) |

#### 管理审计域

| 表 | 实体 | 主键 | 关键字段 |
|----|------|------|----------|
| `t_account_audit_log` | TAccountAuditLog | id (Long, AUTO) | userId→TUser, **action**(ban/clear), previousStatus, reason |

#### 即时通讯域

| 表 | 实体 | 主键 | 关键字段 |
|----|------|------|----------|
| `chat_session` | ChatSession | chatId (Long, AUTO) | user1Id, user2Id, lastMsg, lastTime(Date), status(1=正常/2=拉黑/3=删除), createdAt |
| `chat_message` | ChatMessage | msgId (Long, AUTO) | chatId→ChatSession, sendId, receiveId, tradeId→TTrade, msgType(0=系统/1=通知/2=沟通), msgContent, **contentType(TEXT/IMAGE/AUDIO), mediaUrl, mediaName, mediaSize, mediaDuration**, isRead |

#### 推荐系统域

| 表 | 实体 | 主键 | 关键字段 |
|----|------|------|----------|
| `recommendation_config` | RecommendationConfig | configId (Int, AUTO) | configKey, configValue(BigDecimal), **description** |
| `user_goods_browse` | UserGoodsBrowse | id (Long, AUTO) | userId, goodsId, browseTime |

### 9.2 实体外键关系

```
TUser ──1:N──> TGoods(发布)            TUser ──1:N──> TFavorite(收藏)
TUser ──1:N──> TBlacklist(拉黑)        TUser ──1:N──> TFeedback(反馈)
TUser ──1:N──> TCreditLog(信用变更)    TUser ──1:N──> UserGoodsBrowse(浏览)
TUser ──1:N──> TUserReport(举报)       TUser ──1:N──> TAccountAuditLog(审计)

TGoods ──1:N──> TGoodsImage(图片)      TGoods ──N:1──> TGoodsCategory(分类)
TGoods ──1:1──> TTrade(交易)

ChatSession ──1:N──> ChatMessage(消息)  ChatMessage ──N:1──> TTrade(关联交易)
TTrade ──1:N──> TEvaluate(评价)          TEvaluate ──1:N──> TEvaluateImage(评价图)
```

### 9.3 Service 依赖关系

```
商品: TGoodsService → TGoodsMapper, TGoodsImageService, TGoodsCategoryService,
      SensitiveWordService, UserGoodsBrowseService, TFavoriteService
      ImageUploadService → ObjectStorageService(S3ObjectStorageImpl)

用户: TUserService → TUserMapper, TFavoriteService, TGoodsService, TTradeService, TokenUtil
      TAdminService → TokenUtil → RedisService
      LoginController → TUserService, WebVpnLoginThread(Apache HttpClient)

消息: ChatSessionService → ChatSessionMapper, ChatMessageService, TGoodsService,
      TTradeService, StompPushService, RedisService(交易意图锁 trade:req:)
      ChatMessageService → ChatMessageMapper, StompPushService, ObjectStorageService(媒体)

推荐: RecommendationService → TGoodsMapper, UserGoodsBrowseMapper, RedisService(缓存),
      RecommendationConfigService(权重)
      RecommendationScheduleService: @ApplicationReady预热 + @Scheduled(每小时刷新 + 每日02:00清理)

管理统计: AdminDashboardService → 聚合待审核商品/风险用户/待处理反馈/待处理举报/已解决事项 + 日同比

风险: AdminRiskUserService → TUserService, TUserReportService, TGoodsService, TBlacklistService,
      TEvaluateService, TCreditLogService, ChatSessionService, ChatMessageMapper, StompPushService
      信用分<70自动标记: TUserReportServiceImpl.verifyReport() → status=2

解决事项: AdminSolvedItemsService → TGoodsService, TGoodsImageService, TUserService,
      TFeedbackService, TUserReportService, TAccountAuditLogMapper
```

### 9.4 认证与拦截链

```
请求 → LoginInterceptor.preHandle()
  OPTIONS → 放行
  GET 公开路由(/goods, /goods/search, /goods/{id}, /user/{id}/home) → 可选注入UserContext
  /ws/** → 放行(WebSocket握手链单独处理)
  /test-only/auth/** → 放行(test profile)
  /login, /admin/login, /error, /static/** → 放行
  其他 → 校验 Authorization Token
    有效 → UserContext.setUserId(finalId)
    无效 → 401 JSON: {code:401, msg:"未登录或登录已过期"}

管理员Token: validateToken返回"ADMIN:xxx"格式，拦截器识别前缀并剥离
请求结束 → afterCompletion → UserContext.remove()

WebSocket握手链:
  /ws/messages → WebSocketAuthHandshakeInterceptor(Token校验) → WebSocketPrincipalHandshakeHandler(包装Principal)
```

### 9.5 全局配置要点

- **Context Path**: `/api/v1/campus-taotao`
- **Redis序列化**: `GenericJackson2JsonRedisSerializer` + `DefaultTyping.NON_FINAL` + `JavaTimeModule`
- **Redis用途**: Token存储(token_前缀, 2h过期)、热门商品缓存(`recommendation:hot_goods_cache`)、交易意图锁(`trade:req:`)、草稿(`goods_draft:`)   
- **MyBatis-Plus自动填充**: INSERT 填 createTime/updateTime/createdAt，UPDATE 填 updateTime
- **S3存储**: AWS SDK v2 → 阿里云OSS；密钥 `${ALI_ACCESS_KEY}/${ALI_SECRET_KEY}`；上传路径 `user/{userId}/{yyyy}/{MM}/{dd}/{uuid}.{ext}`
- **跨域**: `CorsConfig` 全局放行 `*`，允许 Cookie
- **定时任务** (@EnableScheduling, 线程池2): 每日02:00清理浏览记录, 每小时:05刷新热门缓存
- **敏感词**: `goods.sensitive-words` 配置，发布商品时调用 `sensitiveWordService.validateGoods()`
- **Test Profile**: `/test-only/auth/token` + `X-Test-Secret` Header 签发临时 Token
- **全局异常**: `GlobalExceptionHandler` (@RestControllerAdvice) 统一处理

### 9.6 推荐系统架构

双轨策略：
1. **个性化**（有浏览记录）：Top2 偏好分类 → 加权公式（分类偏好 30% + 收藏 20% + 点击量 25% + 发布时效 25%），权重从 `recommendation_config` 表读取
2. **冷启动**（匿名/新用户）：热门 Top50（点击量×0.15 + 收藏数×20 + 发布时效×0.8），Redis 缓存每小时刷新

浏览埋点：商品详情 `GET /goods/{id}` 异步调 `recordBrowseAndUpdateViewCount()`（1h 防刷）

### 9.7 完整 API 路由表

> **所有路径均需加 Context Path 前缀**：`/api/v1/campus-taotao` + 路径。
> **认证列**：✅ 必须登录 | ⬜ 公开 | 🔑 管理员

#### 登录认证

| 方法 | 路径 | 认证 | 说明 | 请求体/参数 |
|------|------|------|------|-------------|
| POST | `/login` | ⬜ | WebVPN 登录 | `LoginRequest {userId, password}` |
| POST | `/logout` | ⬜ | 登出 | Header: `Authorization` |
| GET | `/userInfo` | ⬜(可选认证) | 当前用户信息 | — |

#### 商品（GoodsController）

| 方法 | 路径 | 认证 | 说明 | 请求体/参数 |
|------|------|------|------|-------------|
| GET | `/goods` | ⬜ | 首页 Tab 商品列表 | `?tab=&category=&page=1&size=10` |
| GET | `/goods/search` | ⬜ | 模糊搜索 | `?keyword=&page=1&size=10` |
| POST | `/goods` | ✅ | **发布商品**（核心） | `Map {name, desc, remark, price, purpose, exchangeAddr, imgUrls[], type(sell/buy), category(分类名), draftId?}` |
| GET | `/goods/{goodsId}` | ⬜ | 商品详情（自动记录浏览） | — |
| POST | `/goods/{goodsId}/favorite` | ✅ | 收藏商品 | — |
| DELETE | `/goods/{goodsId}/favorite` | ✅ | 取消收藏 | — |
| GET | `/goods/{goodsId}/trade` | ✅ | 联系卖家（创建会话） | — |
| POST | `/goods/draft` | ✅ | 保存草稿 | `Map {draftId?, name, desc, ...}` |
| GET | `/goods/draft` | ✅ | 草稿列表 | — |
| GET | `/goods/draft/{draftId}` | ✅ | 草稿详情 | — |
| DELETE | `/goods/draft/{draftId}` | ✅ | 删除草稿（含图片清理） | — |

#### 推荐系统（RecommendationController，RequestMapping="/goods"）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/goods/preference/categories` | ✅ | 用户偏好分类 Top2 |
| POST | `/goods/cache/refresh-hot` | ⬜(TODO:加管理员校验) | 刷新热门缓存 |
| GET | `/goods/statistics` | ⬜ | 推荐系统运行统计 |

#### 用户中心（UserController）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/user/info` | ✅ | 当前用户信息 |
| GET | `/user/favorites` | ✅ | 我的收藏 `?page=1&size=10` |
| GET | `/user/goods/sell` | ✅ | 我发布的 `?page=1&size=10&goodsStatus=0/1/2` |
| GET | `/user/goods/bought` | ✅ | 我买到的 `?page=1&size=10` |
| GET | `/user/goods/buy` | ✅ | 我的预购 `?page=1&size=10&status=online/offline` |
| PUT | `/user/goods/{goodsId}` | ✅ | 编辑商品 `GoodsEditRequest` |
| PUT | `/user/goods/{goodsId}/offline` | ✅ | 下架商品 |
| DELETE | `/user/goods/{goodsId}` | ✅ | 删除商品 |
| GET | `/user/{userId}/home` | ⬜ | 他人主页 |
| POST | `/user/{userId}/blacklist` | ✅ | 拉黑用户 |
| GET | `/user/blacklist` | ✅ | 黑名单列表 `?page=1&size=10` |
| DELETE | `/user/{userId}/blacklist` | ✅ | 取消拉黑 |
| POST | `/user/{userId}/report` | ✅ | 举报用户 `UserReportRequest` |

#### 交易（TradeController）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| DELETE | `/trade/request/{goodsId}` | ✅ | 买家撤回交易请求 |
| POST | `/trade/reject/{goodsId}/{buyerId}` | ✅ | 卖家拒绝交易 |
| POST | `/trade/confirm/{goodsId}/{buyerId}` | ✅ | 卖家确认交易（创建交易记录） |

#### 评价

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/evaluate/trade` | ✅ | 提交交易评价 `TradeEvaluateSubmitRequest` |

#### 反馈

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/feedback` | ✅ | 提交反馈 `FeedbackSubmitRequest` |
| GET | `/feedback` | ✅ | 我的反馈列表 `?page=1&size=10` |

#### 聊天（REST）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/messages` | ✅ | 会话列表 `?page=1&size=10` |
| GET | `/messages/{chatId}` | ✅ | 聊天记录 `?page=1&size=10` |
| POST | `/messages/{chatId}` | ✅ | 发送文本消息 `MessageSendRequest` |
| POST | `/messages/{chatId}/media` | ✅ | 发送媒体消息 `MessageSendMediaRequest` |
| POST | `/messages/{goodsId}/trade` | ✅ | 发起交易意图 |
| POST | `/messages/goods/{goodsId}/contact` | ✅ | 通过商品联系卖家（同 `/goods/{id}/trade`） |
| PUT | `/messages/unread/clear` | ✅ | 清除所有未读 |
| PUT | `/messages/{chatId}/read` | ✅ | 标记会话已读 |

#### 图片上传

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/file/upload/images` | ✅ | 上传图片 `MultipartFile[]` → 返回 URL 列表 |
| DELETE | `/file/delete/images` | ✅ | 删除图片 `{urls: [...]}` |

#### 管理后台（全部 🔑 管理员 Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin/login` | 管理员登录 `AdminLoginRequest` |
| GET | `/admin/auth/profile` | 管理员信息 |
| POST | `/admin/logout` | 管理员登出 |
| GET | `/admin/statistics/overview` | **工作台概览**（待审核商品/风险用户/待处理反馈/待处理举报/已解决事项+日同比） |
| GET | `/admin/goods/audit/list` | 待审核商品列表 |
| GET | `/admin/goods/audit/{goodsId}` | 审核商品详情 |
| PUT | `/admin/goods/audit/approve` | 审核通过 `GoodsAuditApproveRequest {goodsIds[]}` |
| PUT | `/admin/goods/audit/reject` | 审核驳回 `GoodsAuditRejectRequest {goodsIds[], reason}` |
| GET | `/admin/feedback/list` | 反馈列表 |
| GET | `/admin/feedback/{feedbackId}` | 反馈详情 |
| PUT | `/admin/feedback/mark-unread` | 标记未读 |
| POST | `/admin/feedback/{feedbackId}/resolve` | 处理反馈 |
| GET | `/admin/reports` | 举报列表 |
| GET | `/admin/reports/{reportId}` | 举报详情 |
| PUT | `/admin/reports/{reportId}/verify` | 核实举报 |
| GET | `/admin/users/risk/list` | 风险用户列表 |
| GET | `/admin/users/risk/{userId}/metrics` | 风险指标详情 |
| PUT | `/admin/users/risk/handle` | 处理风险用户（查封/消除） |
| GET | `/admin/solved-items/list` | 已解决事项列表 |
| GET | `/admin/solved-items/detail` | 解决事项详情 `?type=&id=` |
| GET | `/admin/solved-items/count` | 各类解决事项数量统计 |
| PUT | `/admin/solved-items/revoke` | 撤销解决事项 |

#### 测试接口（仅 test profile + X-Test-Secret）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/test-only/auth/token` | 签发临时 Token `TestTokenRequest {userId}` |

---

## 10. Agent Integration Guide（Agent 集成开发指南）

> **本节为 Campus Trading Agent（Python/LangGraph）项目的 AI 助手提供完整的后端对接信息。**

### 10.1 关键接口速查（Agent 最常用）

| 用途 | 方法 | 完整路径 | 认证 |
|------|------|----------|------|
| 验证 Token | POST | `/api/v1/campus-taotao/test-only/auth/token` 或复用现有拦截器 | X-Test-Secret |
| 发布商品 | POST | `/api/v1/campus-taotao/goods` | ✅ Bearer Token |
| 搜索商品 | GET | `/api/v1/campus-taotao/goods/search?keyword=&page=1&size=10` | ⬜ |
| 商品详情 | GET | `/api/v1/campus-taotao/goods/{goodsId}` | ⬜ |
| 商品列表 | GET | `/api/v1/campus-taotao/goods?tab=&category=&page=1&size=10` | ⬜ |
| 用户信息 | GET | `/api/v1/campus-taotao/user/info` | ✅ |
| 我的商品 | GET | `/api/v1/campus-taotao/user/goods/sell?page=1&size=10&goodsStatus=0` | ✅ |
| 我的收藏 | GET | `/api/v1/campus-taotao/user/favorites?page=1&size=10` | ✅ |
| 我的交易 | 需组合 `/user/goods/bought` 和 `/messages` | ✅ |
| 图片上传 | POST | `/api/v1/campus-taotao/file/upload/images` | ✅ multipart |
| 分类列表 | 需查询 TGoodsCategory 表（当前无直接 API） | — |

### 10.2 发布商品接口详解（最重要）

**POST** `/api/v1/campus-taotao/goods`

Headers: `Authorization: Bearer <token>`, `Content-Type: application/json`

```json
{
  "name": "MacBook Pro 14英寸 M3 Pro",
  "desc": "2024年购入，外观完好，电池循环12次",
  "remark": "带原装充电器",
  "price": 8500,
  "purpose": "编程开发",
  "exchangeAddr": "南区宿舍",
  "imgUrls": ["https://oss.xxx.com/user/xxx/2026/07/18/uuid.jpg"],
  "type": "sell",
  "category": "学习用品",
  "draftId": null
}
```

**关于 `category` 参数**：
- 值为**分类的名称**（如"学习用品""宿舍用品""娱乐用品""预售"），不是 ID
- Controller 通过 `categoryName` 查询 `t_goods_category` 表获取 `categoryId`
- 如果 Agent 需要获取全部分类列表，需要确认是否有 API 或直接查表

**`type` 字段**：`"sell"` → `goodsType=1`（出售），`"buy"` → `goodsType=2`（预购）

**响应**：
```json
{
  "code": 200,
  "msg": "发布成功，平台将进行合规审核",
  "data": {
    "goodsId": 123,
    "imgUrls": ["https://..."]
  },
  "total": null
}
```

### 10.3 Token 认证流程

1. **用户登录** `POST /login` → 返回 token
2. **后续请求** Header: `Authorization: Bearer <token>`
3. **Token 校验**：Redis 双向映射（token ↔ userId），2h 过期，校验时自动续期
4. **管理员 Token**：Redis 值带 `ADMIN:` 前缀

**Agent 项目建议**：
- Agent 服务作为中间层，前端传 token 给 Agent，Agent 透传给 Java 后端
- 或新增 `/internal/auth/verify` 接口供 Agent 服务验证 token 并获取 userId
- 测试环境可用 `/test-only/auth/token` + `X-Test-Secret` 签发临时 token

### 10.4 Result 响应格式

所有接口统一返回：
```json
{
  "code": 200,       // 200=成功, 400=参数错误, 401=未登录, 500=服务异常
  "msg": "操作成功",
  "data": { ... },   // 任意类型，null 时不序列化
  "total": null      // 分页时传总数
}
```

### 10.5 图片上传流程

Agent 需要处理用户上传图片的场景：

1. Agent 前端将图片文件以 `multipart/form-data` 发送到 Agent 服务
2. Agent 服务将图片转发到 `POST /api/v1/campus-taotao/file/upload/images`
   - 字段名 `file`，支持多文件 `MultipartFile[]`
   - 需要携带用户的 Token
3. 返回的 `imgUrls` 列表传给发布接口

### 10.6 商品搜索接口详解

**GET** `/api/v1/campus-taotao/goods/search?keyword=Java笔记本&page=1&size=10`

- 模糊匹配 `goodsName` / `goodsDesc` / `goodsNote`
- 无需认证
- 返回分页商品列表（含图片 URL、发布者信息）

### 10.7 分类体系

当前分类（存储在 `t_goods_category` 表）：
- 宿舍用品
- 娱乐用品  
- 学习用品
- 预售

`isShow=0` 表示在首页展示。

### 10.8 敏感词过滤

- 配置在 `application.yaml` 的 `goods.sensitive-words`
- 调用链：`GoodsController.publishGoods()` → `sensitiveWordService.validateGoods(name, desc, remark, purpose, exchangeAddr)`
- 包含敏感词时抛出 `SensitiveWordException`，`GlobalExceptionHandler` 捕获返回错误

### 10.9 Agent 项目建议的 Internal API 扩展点

如果 Agent 需要绕过公开 API 的限制（如批量查询、内部数据聚合），建议在 Java 端新增：

```
controller/internal/AgentInternalController.java
  POST /internal/auth/verify        → 验证 token 返回 userId
  GET  /internal/goods/search       → 增强搜索（价格区间、分类过滤等）
  GET  /internal/goods/{id}/detail  → 商品完整信息（含浏览/收藏数）
  GET  /internal/user/profile       → 用户完整信息
  GET  /internal/categories         → 所有分类列表
  GET  /internal/goods/history-price → 同类商品历史成交价
```

这些接口应使用 `127.0.0.1` 限制或内部密钥保护。

### 10.10 维护说明

> **当新增业务模块或对现有模块进行大规模改动时，必须：**
> 1. 运行 `codegraph init -i` 重建 CodeGraph 索引
> 2. 更新 §9 中的实体关系、Service 依赖、Controller 路由
> 3. 更新 §10 中的 API 速查表（如有接口变更）
