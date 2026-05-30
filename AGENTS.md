# AGENTS.md — SCMU_taotao 项目 AI 编码助手指令

## 1. Project Overview（项目简介）

**项目名称**：SCMU_taotao（校园淘淘）
**项目类型**：校园二手交易平台
**基础包路径**：`com.bit.scmu_taotao`
**构建工具**：Maven（使用 Maven Wrapper：`mvnw`）

### 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.5.11 |
| ORM | MyBatis-Plus | 3.5.16 |
| 数据库 | MySQL | 8.x (mysql-connector-j) |
| 缓存 | Redis（Lettuce 连接池 + Commons Pool2） | — |
| 对象存储 | AWS SDK v1/v2 → 对接阿里云 OSS S3 协议 | — |
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
- **管理后台**：商品审核、反馈管理
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
├── client/                      # HTTP 客户端（WebVPN 登录、HttpResponse 处理）
│   └── thread/                  # 登录线程
├── config/                      # Spring @Configuration 配置类
│   ├── CorsConfig.java          # 跨域配置
│   ├── MyBatisPlusConfig.java   # MyBatis-Plus 分页插件等
│   ├── RedisConfig.java         # Redis 序列化配置
│   ├── WebConfig.java           # 拦截器注册（LoginInterceptor）
│   ├── WebSocketConfig.java     # STOMP WebSocket 配置
│   ├── WebSocketPrincipalHandshakeHandler.java
│   ├── TestOnlyAuthProperties.java
│   └── storage/                 # S3 存储配置
├── controller/                  # REST 控制器（@RestController）
│   ├── GoodsController.java     # 商品相关接口
│   ├── LoginController.java     # 登录接口
│   ├── MessagesController.java / MessagesRestController.java  # 消息/聊天
│   ├── RecommendationController.java  # 推荐系统
│   ├── TBlacklistController.java / TEvaluateController.java  # 黑名单/评价
│   ├── FileUploadController.java
│   ├── admin/                   # 管理后台控制器
│   └── testonly/                # 仅测试环境使用的接口
├── dto/                         # 数据传输对象，按功能分包
│   ├── admin/                   # 管理后台 DTO
│   ├── chat/                    # 聊天相关 DTO
│   ├── FeedbackDto/
│   ├── goods/                   # 商品相关 DTO
│   ├── recommend/               # 推荐系统 DTO
│   ├── testonly/
│   ├── upload/
│   └── *.java                   # 顶层通用 DTO（LoginRequest 等）
├── entity/                      # MyBatis-Plus 实体类（@TableName, @TableId）
│   └── T*.java                  # 表前缀 "t_" 对应实体前缀 "T"
├── exception/                   # 全局异常处理 + 自定义异常
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   ├── SensitiveWordException.java
│   └── StorageException.java
├── handler/                     # WebSocket 消息处理器
├── interceptor/                 # 拦截器
│   ├── LoginInterceptor.java    # Token 校验（ThreadLocal 注入 UserContext）
│   └── WebSocketAuthHandshakeInterceptor.java
├── mapper/                      # MyBatis-Plus Mapper 接口（继承 BaseMapper）
├── service/                     # 业务层接口（继承 IService）
│   ├── impl/                    # Service 实现（继承 ServiceImpl）
│   ├── storage/                 # 对象存储服务
│   ├── upload/                  # 图片上传服务
│   └── testonly/
└── util/                        # 工具类
    ├── Result.java              # 统一响应体（code/msg/data/total）
    ├── TokenUtil.java           # Token 生成/校验
    ├── UserContext.java         # ThreadLocal 用户上下文
    ├── LoginProcessMessage.java
    ├── common/                  # KeyDescription
    └── storage/                 # ObjectKeyGenerator, ObjectKeyParser

src/main/resources/
├── application.yaml             # 主配置（MySQL, Redis, S3, MyBatis-Plus, 敏感词等）
├── application-test.yaml        # test profile 专用配置
└── com/bit/scmu_taotao/mapper/  # MyBatis XML Mapper（与接口同名）

src/test/java/com/bit/scmu_taotao/
├── controller/                  # 控制器集成测试（MockMvc + @SpringBootTest）
├── integration/                 # 集成测试（如 S3 上传）
├── images/                      # 测试用图片资源
└── ScmuTaotaoApplicationTests.java
```

---

## 3. Coding Guidelines（编码规范）

### 3.1 命名规范

- **包名**：全小写，按功能分包（如 `dto/goods/`, `service/impl/`）
- **实体类**：以 `T` 前缀开头（如 `TGoods`, `TUser`, `TFavorite`），对应数据库表 `t_goods`, `t_user`, `t_favorite`
- **Service 接口**：`XxxService extends IService<Entity>`
- **Service 实现**：`XxxServiceImpl extends ServiceImpl<Mapper, Entity> implements XxxService`
- **Mapper 接口**：`XxxMapper extends BaseMapper<Entity>`
- **Controller**：`XxxController`，使用 `@RestController` + `@RequestMapping("/xxx")`
- **DTO**：以 `DTO` 或 `Request/Response` 结尾（如 `GoodsRequestDTO`, `GoodsResponseDTO`, `LoginRequest`）
- **变量与方法**：小驼峰（`goodsId`, `getGoodsImageUrls`）
- **数据库字段**：下划线命名（`goods_id`, `create_time`），MyBatis-Plus 自动映射驼峰

### 3.2 注解使用

- 所有实体类使用 Lombok `@Data`
- Controller/ServiceImpl 使用 `@Slf4j` 进行日志记录
- 依赖注入统一使用 `@Autowired` 字段注入（非构造器注入）
- Controller 方法参数使用 Jakarta 校验注解（`@Valid`, `@Min`, `@NotBlank`）
- 实体主键使用 `@TableId(type = IdType.AUTO)`
- 实体表名使用 `@TableName(value = "t_xxx")`
- 时间字段使用 `@TableField(fill = FieldFill.INSERT)` + `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")`
- 配置文件使用 `@ConfigurationProperties` 或 `@Value`（参考 `S3StorageProperties`）

### 3.3 代码风格

- **注释使用中文**，Javadoc 描述类和方法的用途
- **日志使用 SLF4J**：`log.info/warn/error/debug`，严禁 `System.out.println`
- **每个业务方法前添加中文注释**说明用途（参考 `GoodsController` 中注释风格）
- **统一返回** `Result` 对象，不使用原始类型或 Map 裸返：
  - 成功：`Result.ok(data)` 或 `Result.ok(msg, data)` 或 `Result.ok(list, total)`
  - 失败：`Result.fail(errorMsg)` 或 `Result.fail(code, errorMsg)`
- **分页查询**使用 MyBatis-Plus `Page` 对象
- **Lambda 查询**优先使用 `LambdaQueryWrapper`，而非字符串字段名
- **软删除**：所有实体使用 `is_delete` 字段标记（0=正常, 1=已删除），不物理删除
- **常量定义**：在 ServiceImpl 中使用 `private static final` 定义业务常量
- **参数安全校验**：Controller 层手动校验 `page`/`size` 边界值，使用 `Math.min(size, 50)` 防止溢出

### 3.4 JSON 序列化

- 全局配置 `default-property-inclusion: non_null`，null 值自动省略
- 日期统一格式 `yyyy-MM-dd HH:mm:ss`，时区 `GMT+8`

### 3.5 认证与鉴权

- Token 放在 `Authorization` 请求头，格式 `Bearer <token>` 或纯 token
- `LoginInterceptor` 在 `preHandle` 校验并注入 `UserContext.setUserId(finalId)`
- 请求结束后 `afterCompletion` 调用 `UserContext.remove()` 清理 ThreadLocal
- 公开 GET 路由无需 Token（`/goods`, `/goods/search`, `/goods/{id}`, `/user/{id}/home`）
- 管理后台路径 `/admin/**` 仅允许管理员 Token 访问
- 测试 profile 下可通过 `/test-only/auth/**` 获取临时 Token

---

## 4. Modification Rules（修改约束）

### 4.1 最小改动原则（核心）

- **只修改与当前任务直接相关的文件**，不顺手重构无关代码
- **不修改现有方法签名**，除非任务明确要求
- **不改变已有注解和配置风格**，保持与周边代码一致
- **不删除已有注释或日志语句**，除非确认无用
- **不动 .idea/、target/ 和根目录 Markdown 文档**

### 4.2 新功能添加

1. Controller 新增接口时：参考 `GoodsController` 的写法，添加中文注释
2. Service 新增方法时：**先在 Interface 中添加方法签名**，再在 Impl 中实现
3. 新增实体时：放在 `entity/` 包下，命名遵循 `T` 前缀规范，添加 `@TableName` + `@TableId`
4. 新增 DTO 时：按功能放入对应子包（`dto/xxx/`），使用 `@Data`
5. 新增 Mapper 时：继承 `BaseMapper<Entity>`，复杂 SQL 写在 XML 或 `@Select` 注解中
6. 新增异常时：继承 `RuntimeException`，在 `GlobalExceptionHandler` 添加对应 `@ExceptionHandler`

### 4.3 修改现有功能

- 同层修改：Controller 改 Controller，Service 改 Service，不跨层修改
- SQL 修改优先在 MyBatis XML 或 Mapper 注解中处理，不在 Service 里拼接 SQL
- 配置修改：仅改 `application.yaml` 或 `application-test.yaml`，不新增配置文件
- 如果需改数据库表结构，同时在 `entity/` 实体类中更新对应字段

### 4.4 代码复用优先

- **Result 工具类已存在**：所有接口响应必须使用 `Result.ok()` / `Result.fail()`
- **UserContext 已存在**：获取当前用户用 `UserContext.getUserId()`，不要自己解析 Token
- **MyBatis-Plus 已有方法优先**：`getById()`, `list()`, `page()`, `save()`, `updateById()` 等
- **Hutool 工具库已引入**：字符串、日期、集合操作优先用 Hutool
- **S3 存储服务已封装**：上传下载使用 `ObjectStorageService`，不直接调 AWS SDK

---

## 5. Dependencies（依赖使用规则）

### 5.1 禁止新增依赖

**不得在 `pom.xml` 中新增任何 Maven 依赖**，除非：
- 当前任务使用已有依赖无法实现
- 用户明确要求添加特定依赖

### 5.2 已有依赖的正确使用

| 场景 | 使用 | 不使用 |
|------|------|--------|
| ORM 查询 | MyBatis-Plus `LambdaQueryWrapper` | 原生 JDBC 或 JPA |
| JSON 处理 | Jackson `ObjectMapper` | Gson、Fastjson |
| HTTP 请求 | Apache `HttpClient` | OkHttp、RestTemplate |
| HTML 解析 | Jsoup | 正则表达式 |
| 工具方法 | Hutool | Guava、Apache Commons |
| 简化代码 | Lombok `@Data`, `@Slf4j` | 手写 getter/setter/logger |
| S3 存储 | `ObjectStorageService` | 直接 AWS SDK |
| 缓存 | `RedisService` | 直接 RedisTemplate |

### 5.3 版本管理

- 不修改 `pom.xml` 中任何依赖版本号
- AWS SDK 版本由 `${aws.sdk.version}` 统一管理

---

## 6. Testing（测试方式和要求）

### 6.1 测试框架

- JUnit 5 + `@SpringBootTest` + MockMvc

### 6.2 测试规范

- 测试类命名：`XxxControllerTest` 或 `XxxIntegrationTest`
- 测试方法命名：`testXxx` 或描述性命名
- 使用 `@BeforeEach` 初始化 MockMvc 和 Token
- 使用 `mockMvc.perform()` + `.andExpect(status().isOk())` 验证
- 响应解析使用 `ObjectMapper` 反序列化 `Result` 对象

### 6.3 运行测试

```bash
# 运行全部测试
./mvnw test

# 运行指定测试类
./mvnw test -Dtest=GoodsControllerTest

# 使用 test profile
./mvnw test -Dspring.profiles.active=test

# 跳过测试编译（仅用于验证语法）
./mvnw compile
```

### 6.4 测试限制

- **不创建新测试文件**，除非用户明确要求
- 已注释的测试类（如 `GoodsControllerTest`）不要取消注释，除非用户要求
- 测试依赖真实数据库/Redis 连接，本地运行前需确保服务可用

---

## 7. Common Tasks（常见开发流程）

### 7.1 新增一个业务接口

1. 在 `dto/` 对应子包创建 Request/Response DTO（使用 `@Data`）
2. 在 `service/XxxService.java` 中添加方法签名（返回 `Result`）
3. 在 `service/impl/XxxServiceImpl.java` 中实现方法
4. 在 `controller/` 中创建或扩展 Controller，添加对应的 `@GetMapping` / `@PostMapping` 方法
5. 如需复杂 SQL，在 `mapper/XxxMapper.java` 中添加 `@Select` 方法或在 XML 中编写

### 7.2 修复一个 Bug

1. 先用 `rg "关键词"` 定位相关代码文件
2. 确定影响的层级（Controller / Service / Mapper / Config）
3. 仅修改出问题的文件和方法
4. 不改动方法签名（除非 Bug 由签名引起）
5. 如果有日志，考虑添加 `log.debug/warn` 以便排查

### 7.3 添加新的配置项

1. 在 `application.yaml` 中添加配置（放到已有相关配置块下，不新建顶级键）
2. 如需 Spring 注入，在 `config/` 创建 `@ConfigurationProperties` 类（参考 `S3StorageProperties`）
3. test profile 专属配置放到 `application-test.yaml`

### 7.4 项目构建与启动

```bash
# 编译
./mvnw clean compile

# 打包
./mvnw clean package -DskipTests

# 启动（需要 MySQL + Redis 服务可用）
./mvnw spring-boot:run

# 使用 test profile 启动
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

---

## 8. Forbidden Actions（禁止行为）

### 8.1 文件操作

- **禁止删除任何文件**，除非用户明确要求
- **禁止修改 `pom.xml`**（新增/删除/升级依赖、修改版本）
- **禁止修改 `.idea/` 目录下的任何文件**
- **禁止修改 `.gitignore`、`.gitattributes`**
- **禁止修改 `application.yaml` / `application-test.yaml` 中的数据库/Redis 连接信息**
- **禁止在 `src/main/resources/` 下新增配置文件**（除非用户要求）

### 8.2 代码操作

- **禁止重构**：不重命名类、方法、变量（除非任务明确要求）
- **禁止改变代码格式**：不调整缩进、空行、import 排序（保持原样）
- **禁止修改已存在的方法签名**（参数类型、返回值类型、方法名）
- **禁止删除或注释已有代码**（包括已注释的测试类）
- **禁止使用 `System.out.println`** 代替日志
- **禁止硬编码敏感信息**（密码、密钥放到环境变量或配置中引用 `${...}`）
- **禁止在项目中引入新的编码范式或设计模式**，遵循现有分层架构

### 8.3 安全红线

- **禁止关闭登录拦截器或放宽鉴权规则**
- **禁止在 Controller 返回异常堆栈给前端**（GlobalExceptionHandler 已统一处理）
- **禁止绕过敏感词过滤**
- **发现安全风险（SQL 注入、XSS、密钥泄露）时立即中断并提示用户**

### 8.4 Git 操作

- **禁止自动 `git commit`**
- **禁止自动创建分支或推送**
- **禁止自动合并代码**

### 8.5 依赖操作

- **禁止新增 Maven 依赖**（`pom.xml` 中 `<dependency>`）
- **禁止新增 Gradle 依赖**（项目不使用 Gradle）


---

## 9. Architecture Context（AI 架构上下文）

> **本节面向 AI 助手，用于快速建立对项目结构和运行时依赖关系的全局理解。**
> 每次新增业务模块或对现有模块进行大规模改动时，**必须同步更新本节和 CodeGraph 索引**（``codegraph init -i``）。

### 9.1 数据库实体关系

**18 张表，按业务域分组（所有实体均软删除 ``is_delete``）：**

#### 用户域
- ``t_user``(userId/String/学号) → TUser：userName, avatar, creditScore, creditStar, status(0=正常/1=违规查封/2=有风险需审核), violationReason
- ``t_admin``(id/Long) → TAdmin：adminId, password, nickname
- ``t_credit_log``(id/Long) → TCreditLog：userId → TUser, scoreChange, changeType

#### 商品域
- ``t_goods``(goodsId/Long) → TGoods：userId → TUser, categoryId → TGoodsCategory, goodsType(0普通/1预售), price, goodsStatus(0在售/1已售), viewCount, isAudited
- ``t_goods_category``(categoryId/Int) → TGoodsCategory：categoryName, sort, isShow
- ``t_goods_image``(imageId/Long) → TGoodsImage：goodsId → TGoods, imageUrl, sort

#### 交易域
- ``t_trade``(tradeId/Long) → TTrade：goodsId → TGoods, sellerId/buyerId → TUser, tradePrice
- ``t_evaluate``(evalId/Long) → TEvaluate：tradeId → TTrade, goodsId, buyerId, sellerId, descScore/commScore/totalScore
- ``t_evaluate_image``(evalImgId/Long) → TEvaluateImage：evalId → TEvaluate, imgUrl

#### 管理审计域
- `t_account_audit_log`(id/Long) → TAccountAuditLog：userId → TUser, action(ban/clear), previousStatus, reason

#### 互动域
- ``t_favorite``(favoriteId/Long) → TFavorite：userId, goodsId
- ``t_blacklist``(blackId/Long) → TBlacklist：userId, blackUserId
- ``t_feedback``(feedbackId/Long) → TFeedback：userId, feedbackContent, feedbackStatus, replyContent
- ``t_user_report``(id/Long) → TUserReport：reporterId, targetId, tag, status

#### 即时通讯域
- ``chat_session``(chatId/Long) → ChatSession：user1Id, user2Id, lastMsg, lastTime, status
- ``chat_message``(msgId/Long) → ChatMessage：chatId → ChatSession, sendId/receiveId, tradeId → TTrade, msgType(0系统/1通知/2沟通), contentType(TEXT/IMAGE/AUDIO), mediaUrl, isRead

#### 推荐系统域
- ``recommendation_config``(configId/Int) → RecommendationConfig：configKey, configValue(BigDecimal) — 存储推荐权重，可动态调整
- ``user_goods_browse``(id/Long) → UserGoodsBrowse：userId, goodsId, browseTime — 浏览记录，每日凌晨清理 30 天前数据

### 9.2 实体外键关系图

```
TUser ──1:N──> TGoods(发布)           TUser ──1:N──> TFavorite(收藏)
TUser ──1:N──> TBlacklist(拉黑)       TUser ──1:N──> TFeedback(反馈)
TUser ──1:N──> TCreditLog(信用变更)   TUser ──1:N──> UserGoodsBrowse(浏览)
TUser ──1:N──> TUserReport(举报)
TUser ──1:N──> TAccountAuditLog(账号审核操作)

TGoods ──1:N──> TGoodsImage(图片)     TGoods ──N:1──> TGoodsCategory(分类)
TGoods ──1:1──> TTrade(交易)

ChatSession ──1:N──> ChatMessage(消息)  ChatMessage ──N:1──> TTrade(关联交易)
TTrade ──1:N──> TEvaluate(评价)         TEvaluate ──1:N──> TEvaluateImage(评价图)
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
      TTradeService, StompPushService, RedisService(交易意图锁)
      ChatMessageService → ChatMessageMapper, StompPushService, ObjectStorageService(媒体)

推荐: RecommendationService → TGoodsMapper, UserGoodsBrowseMapper, RedisService(缓存),
      RecommendationConfigService(权重)
      RecommendationScheduleService → @ApplicationReady预热, @Scheduled每小时刷新, 每日清理

管理: AdminGoodsAuditController → TGoodsService → TUserService(通知被下架用户)
      AdminFeedbackController → TFeedbackService
      AdminAuthController → TAdminService
      AdminReportController → TUserReportService, TUserService, TCreditLogService
      TUserReportService → ChatSessionService, ChatMessageMapper, StompPushService(系统消息推送)

风险: AdminRiskUserService → TUserService, TUserReportService, TGoodsService, TBlacklistService,
      TEvaluateService, TCreditLogService, ChatSessionService, ChatMessageMapper, StompPushService
      TAccountAuditLogMapper(审计日志)
      信用分<70自动标记: TUserReportServiceImpl.verifyReport() → 检查newScore<70 → status=2

解决事项: AdminSolvedItemsService → TGoodsService, TGoodsImageService, TUserService,
      TFeedbackService, TUserReportService, TAccountAuditLogMapper, TGoodsMapper, TFeedbackMapper
      AdminSolvedItemsController → AdminSolvedItemsService (聚合商品审核/反馈/账号审核已处理记录)
```

### 9.4 认证与拦截链

```
请求 → LoginInterceptor.preHandle()
  OPTIONS → 放行
  公共 GET(/goods, /goods/search, /goods/{id}, /user/{id}/home) → 放行(可选注入UserContext)
  /ws/** → 放行(交给WebSocket拦截器)
  /test-only/auth/** → 放行(仅test profile)
  /login, /admin/login, /error, /static/** → 放行
  其他 → 校验Authorization Token
    有效 → UserContext.setUserId(finalId)
    无效 → 401 JSON

Token: Redis双向映射(token↔userId), 2小时过期, 校验时自动续期
管理员Token值带"ADMIN:"前缀, 拦截器区分学生/管理员权限
请求结束 → afterCompletion → UserContext.remove() 清理ThreadLocal

WebSocket握手链:
  /ws/messages → WebSocketAuthHandshakeInterceptor(Token校验, 注入userId到attributes)
    → WebSocketPrincipalHandshakeHandler(userId包装为Principal, 供STOMP /user/路由)
```

### 9.5 全局配置要点

- **Context Path**: ``/api/v1/campus-taotao``
- **Redis序列化**: ``GenericJackson2JsonRedisSerializer`` + ``DefaultTyping.NON_FINAL`` + ``JavaTimeModule``
- **Redis用途**: Token存储、热门商品缓存(``recommendation:hot_goods_cache``)、交易意图锁(``trade:req:``前缀, 48h过期)
- **MyBatis-Plus自动填充**: ``MyBatisPlusConfig``(MetaObjectHandler) → INSERT时填createTime/updateTime/createdAt, UPDATE时填updateTime
- **S3存储**: ``S3ClientConfig``→AWS SDK v2 S3Client→阿里云OSS; 密钥```${ALI_ACCESS_KEY}```/```${ALI_SECRET_KEY}```; 上传路径``user/{userId}/{yyyy}/{MM}/{dd}/{uuid}.{ext}``
- **跨域**: ``CorsConfig`` 全局放行``*``, 允许Cookie
- **定时任务**(@EnableScheduling, 线程池2): 每日02:00清理浏览记录, 每小时:05刷新热门缓存, 启动时预热
- **敏感词**: ``goods.sensitive-words``配置, 发布商品时校验
- **Test Profile**: ``/test-only/auth/token`` + ``X-Test-Secret`` Header签发临时Token
- **全局异常**: ``GlobalExceptionHandler``(@RestControllerAdvice) 统一处理StorageException/SensitiveWord/Validation/STOMP异常等

### 9.6 推荐系统架构

双轨策略:
1. **个性化**(有浏览记录): Top2偏好分类 → 加权公式(分类偏好30 + 收藏20 + 点击量25 + 发布时效25), 权重从recommendation_config表读取, 排除黑名单
2. **冷启动**(匿名/新用户): 热门Top50(点击量×0.15 + 收藏数×20 + 发布时效×0.8), Redis缓存每小时刷新

浏览记录写入: 用户查看商品详情时 → ``user_goods_browse``

### 9.7 Controller 路由速查

| 路径 | Controller | 认证 |
|------|-----------|------|
| ``/login`` | LoginController | 无(WebVPN登录) |
| ``/goods`` | GoodsController | GET列表/搜索/详情无需; 发布/编辑需认证 |
| ``/recommend`` | RecommendationController | 可选(匿名支持冷启动) |
| ``/messages``(REST) | MessagesRestController | 需认证 |
| ``/app/messages/*``(STOMP) | MessagesController | WebSocket握手认证 |
| ``/trade/*`` | TradeController | 需认证 |
| ``/favorites`` | GoodsController | 需认证 |
| ``/blacklist`` | TBlacklistController | 需认证 |
| ``/evaluate`` | TEvaluateController | 需认证 |
| ``/feedback`` | TFeedbackController | 需认证 |
| ``/user`` | UserController | 需认证 |
| ``/user/{id}/home`` | UserController | 无需(公开主页) |
| ``/file`` | FileUploadController | 需认证 |
| ``/admin/login,logout,profile`` | AdminAuthController | 管理员Token |
| ``/admin/goods/audit/*`` | AdminGoodsAuditController | 管理员Token |
| ``/admin/feedback/*`` | AdminFeedbackController | 管理员Token |
| ``/admin/reports`` | AdminReportController | 管理员Token |
| ``/admin/users/risk/list`` | AdminRiskUserController | 管理员Token |
| ``/admin/users/risk/{userId}/metrics`` | AdminRiskUserController | 管理员Token |
| ``/admin/users/risk/handle`` | AdminRiskUserController | 管理员Token |
| `/admin/solved-items/list` | AdminSolvedItemsController | 管理员Token |
| `/admin/solved-items/detail` | AdminSolvedItemsController | 管理员Token |
| `/admin/solved-items/count` | AdminSolvedItemsController | 管理员Token |
| `/admin/solved-items/revoke` | AdminSolvedItemsController | 管理员Token |
| ``/test-only/auth/token`` | TestAuthController | test profile + X-Test-Secret |

### 9.8 维护说明

> **当新增业务模块或对现有模块进行大规模改动时，必须：**
> 1. 运行 ``codegraph init -i`` 重建 CodeGraph 索引
> 2. 更新本节（§9）中的实体关系、Service 依赖、Controller 路由等对应段落


