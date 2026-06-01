# SCMU_taotao 安全与体验审查报告（校园项目版）

**扫描日期**: 2026-05-31（第三次更新）  
**扫描范围**: 全仓库（232 files, 3648 nodes）  
**扫描类型**: 上线前安全 + 用户体验复审  
**项目定位**: 校园二手交易平台（非企业级）

---

## 执行摘要

本次复审在原安全审查基础上，补充了 **用户使用视角** 的检查（默认头像、接口易用性、错误提示、推荐展示等）。

共发现：
- **1 项严重**（上线阻断）
- **2 项高危**（含本次新增的管理接口越权风险）
- **3 项中危**
- **4 项低危**
- **6 项用户使用体验改进建议**

其中 **2 项已修复 ✅**，**7 项安全问题待处理**，**6 项体验问题待优化**。

---

## 修复进度总览（安全）

| 状态 | 编号 | 问题 | 原等级 |
|------|------|------|--------|
| ✅ 已修复 | HIGH-01 | 配置文件硬编码密码 → 已改为环境变量 `${DB_PASSWORD}` / `${REDIS_PASSWORD}` | 高危 |
| ✅ 已修复 | MEDIUM-03 | 编辑商品缺少敏感词校验 → 已添加 `sensitiveWordService.validateGoods()` | 中危 |
| ❌ 待修复 | CRITICAL-01 | demo-mode 仍然为 `true`（上线阻断） | 严重 |
| ❌ 待修复 | HIGH-02 | `/goods/cache/refresh-hot` 缺少管理员鉴权（本次新增） | 高危 |
| ❌ 待修复 | MEDIUM-01 | Token 生成算法偏简单 | 中危 |
| ❌ 待修复 | MEDIUM-02 | CORS 仍为 `*` | 中危 |
| ❌ 待修复 | MEDIUM-04 | WebSocket Token URL 传递未改 | 中危 |
| ❌ 待修复 | LOW-01 | Token 仍输出到日志 | 低危 |
| ❌ 待修复 | LOW-02 | SQL 仍输出到 stdout | 低危 |
| ⚪ 可选 | LOW-05 | 管理员密码明文（不影响上线） | 低危 |

---

## 本次新增问题

### [HIGH-02] 管理端缓存刷新接口缺少鉴权 ❌ 未修复

- **文件**: `src/main/java/com/bit/scmu_taotao/controller/RecommendationController.java:85`
- **严重性**: 🔴 高危

**问题**: `POST /goods/cache/refresh-hot` 用于刷新热门推荐缓存，但当前 **任意已登录学生都可以调用**，可被恶意频繁触发，造成缓存抖动和性能风险。

**修复建议（最小改动）**:
在 `refreshHotCache()` 中增加管理员 Token 校验（与 `LoginInterceptor` 的管理员判断逻辑一致）：
```java
// 校验管理员 Token（rawValue 以 "ADMIN:" 开头）
String header = request.getHeader("Authorization");
if (header == null || header.isBlank()) return Result.fail(403, "无权限");
String raw = tokenUtil.validateToken(header.startsWith("Bearer ") ? header.substring(7) : header);
if (raw == null || !raw.replace("\"","").startsWith("ADMIN:")) return Result.fail(403, "无权限");
```

---

## 用户使用体验问题（本次新增）

> 以下问题不影响系统安全，但会直接影响 **用户使用感受和前端展示效果**。

### [UX-01] 用户头像为空时缺少默认头像 ⚡ 建议修复

- **文件**: `src/main/java/com/bit/scmu_taotao/service/impl/TUserServiceImpl.java:238, 1051`
- **影响**: 用户未上传头像时，`avatar` 字段返回 `null` 或空字符串，前端如果没有兜底会显示空白或裂图。

**修复建议**:
```java
private static final String DEFAULT_AVATAR = "/static/default-avatar.png";

// 登录返回
userInfo.put("avatar", user.getAvatar() != null && !user.getAvatar().isBlank()
        ? user.getAvatar() : DEFAULT_AVATAR);

// 发布者信息
publisher.setAvatar(user.getAvatar() != null && !user.getAvatar().isBlank()
        ? user.getAvatar() : DEFAULT_AVATAR);
```
同时在 `src/main/resources/static/` 下放入一张默认头像图片。

---

### [UX-02] 推荐商品列表缺少推荐理由 ⚡ 建议优化

- **文件**: `src/main/java/com/bit/scmu_taotao/service/impl/TGoodsServiceImpl.java:452-466`
- **影响**: 推荐接口只返回商品信息，用户 **不知道为什么被推荐**，体验较差。

**修复建议**:
在推荐列表的每一项中增加 `reason` 字段（如“因为你浏览过【宿舍用品】”、“热门商品”等），提升推荐可信度。

---

### [UX-03] 推荐接口同时暴露 `recommendScore` 和 `isFavorited` ⚡ 建议优化

- **文件**: `src/main/java/com/bit/scmu_taotao/dto/recommend/RecommendGoodsDTO.java`
- **影响**: `recommendScore` 是内部排序分，暴露给前端无实际意义且可能造成困惑；`isFavorited` 建议在详情接口返回即可。

**修复建议**:
推荐列表接口中 `recommendScore` 设为 `null`（或不序列化），减少前端噪音。

---

### [UX-04] 商品发布接口参数校验不够严格 ⚡ 建议优化

- **文件**: `src/main/java/com/bit/scmu_taotao/controller/GoodsController.java:94-135`
- **影响**: `publishGoods` 直接使用 `Map<String, Object>` 接收参数，缺少 `@Valid` 和字段长度限制，若前端传空 `name`/负数 `price` 会得到不友好的错误提示。

**修复建议**:
复用已有的 `GoodsRequestDTO` 并加上 `@Valid`，同时在 DTO 中添加字段约束：
```java
@NotBlank(message = "商品名称不能为空")
@Size(max = 50, message = "商品名称不能超过50字")
private String name;
```

---

### [UX-05] 用户主页公开信息可能暴露过多 ⚡ 建议优化

- **文件**: `src/main/java/com/bit/scmu_taotao/service/impl/TUserServiceImpl.java:260-266`
- **影响**: `getUserInfo()` 直接返回 `creditScore`（具体分数）和 `studentId`，对陌生用户来说信息量偏大。

**修复建议**:
公开主页接口只返回 `creditStar`（信誉星级）和脱敏后的 ID（如 `2023***01`），减少信息泄露风险。

---

### [UX-06] 会话列表最后一条消息类型不明确 ⚡ 建议优化

- **文件**: `src/main/java/com/bit/scmu_taotao/service/impl/ChatSessionServiceImpl.java:127-130`
- **影响**: `lastContent` 直接返回文本，如果最后一条消息是图片或系统通知，前端无法区分展示。

**修复建议**:
在会话列表中额外返回 `lastMsgType`（TEXT/IMAGE/SYSTEM）和 `lastContentType`，方便前端做差异化展示。

---

## 待修复问题详情（安全）

### [CRITICAL-01] 演示模式绕过身份认证 ❌ 未修复

- **文件**: `src/main/resources/application.yaml:84`
- **严重性**: ⚠️ 严重（上线阻断）

**问题**: `app.auth.demo-mode: true` 仍然开启。任何人只要知道学号就能直接获取该用户的 Token，冒充其身份发布商品、聊天、交易。

**修复（5分钟）**:
```yaml
app:
  auth:
    demo-mode: false
```

---

### [MEDIUM-01] Token 生成算法偏简单 ❌ 未修复

- **文件**: `src/main/java/com/bit/scmu_taotao/util/TokenUtil.java:37, 52`
- **严重性**: 🟡 中危

**问题**: Token 用 `当前时间戳 + 6位随机数` 生成。理论上可枚举猜测，但校园项目用户量有限，实际被利用的概率不高。

**修复（5分钟）**:
```java
String token = TOKEN_PREFIX + UUID.randomUUID().toString().replace("-", "");
```

---

### [MEDIUM-02] CORS 允许所有域名 ❌ 未修复

- **文件**: `src/main/java/com/bit/scmu_taotao/config/CorsConfig.java:19`
- **严重性**: 🟡 中危

**问题**: `addAllowedOriginPattern("*")` 允许任何网站向 API 发请求。

**修复（5分钟）**:
```java
config.addAllowedOriginPattern("http://localhost:5173");
config.addAllowedOriginPattern("https://your-domain.com");
```

---

### [MEDIUM-04] WebSocket Token 通过 URL 参数传递 ❌ 未修复

- **文件**: `src/main/java/com/bit/scmu_taotao/interceptor/WebSocketAuthHandshakeInterceptor.java:82-92`
- **严重性**: 🟡 中危

**问题**: WebSocket 握手时支持 `?token=xxx` URL 参数，Token 会被记录到浏览器历史和服务器日志中。

**说明**: 如果前端 WebSocket 库（如 SockJS）确实无法设置 Header，可以暂时保留，但建议 Nginx 日志对 Token 脱敏。

---

### [LOW-01] 请求日志中输出了完整 Token ❌ 未修复

- **文件**: `src/main/java/com/bit/scmu_taotao/interceptor/LoginInterceptor.java:42`
- **严重性**: 🟢 低危

**修复（1分钟）**:
```java
String preview = token != null && token.length() > 8 ? token.substring(0,8) + "***" : "***";
log.info("收到请求：{}，path={}，Token={}", method, path, preview);
```

---

### [LOW-02] MyBatis SQL 全量输出到控制台 ❌ 未修复

- **文件**: `src/main/resources/application.yaml:44`
- **严重性**: 🟢 低危

**修复（1分钟）**:
```yaml
mybatis-plus:
  configuration:
    # log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

---

### [LOW-05] 管理员密码明文存储 ⚪ 可选优化

- **文件**: `src/main/java/com/bit/scmu_taotao/service/impl/TAdminServiceImpl.java:47`
- **严重性**: 🟢 低危（不影响上线）

**说明**: 校园项目管理员就几个人，数据库访问权在你手里，实际被利用的风险很低。有精力时改成 BCrypt 哈希即可，不需要注册模块。

---

## 做得好的地方 ✅

- ✅ **商品编辑/删除**有完整的 owner 权限校验，防止越权操作
- ✅ **商品编辑**已加入敏感词校验（本次修复）
- ✅ **聊天消息**有会话参与者校验，A 不能查看 B 和 C 的聊天
- ✅ **文件上传**有类型、大小、扩展名三重校验
- ✅ **文件删除**有 `userPrefix` 前缀校验，不能删别人的图
- ✅ **交易流程**有买卖家身份校验
- ✅ **管理后台**有 ADMIN Token 前缀拦截，学生进不去
- ✅ **WebSocket 握手**有 Token 校验
- ✅ **推荐系统**已实现黑名单过滤
- ✅ **软删除**模式，数据不物理丢失
- ✅ **数据库/Redis 密码**已改用环境变量（本次修复）
- ✅ **会话列表**已计算未读消息数（体验加分）

---

## 修复优先级总览

| 优先级 | 编号 | 问题 | 预估耗时 | 状态 |
|--------|------|------|----------|------|
| 🔴 上线前必修 | CRITICAL-01 | 关闭 demo-mode | 5 分钟 | ❌ |
| 🔴 上线前必修 | HIGH-02 | 管理缓存接口加鉴权 | 15 分钟 | ❌ |
| 🟡 建议修复 | MEDIUM-01 | Token 改用 UUID | 5 分钟 | ❌ |
| 🟡 建议修复 | MEDIUM-02 | CORS 改具体域名 | 5 分钟 | ❌ |
| 🟡 建议修复 | MEDIUM-04 | WebSocket Token URL | 视前端 | ❌ |
| ✅ 已完成 | HIGH-01 | 密码改用环境变量 | — | ✅ |
| ✅ 已完成 | MEDIUM-03 | 编辑商品加敏感词校验 | — | ✅ |
| 🟢 用户体验 | UX-01~06 | 默认头像/推荐理由/校验优化 | 30~60 分钟 | ❌ |
| 🟢 上线后修 | LOW-01 | Token 日志脱敏 | 1 分钟 | ❌ |
| 🟢 上线后修 | LOW-02 | SQL stdout 注释掉 | 1 分钟 | ❌ |
| 🟢 可选 | LOW-05 | 管理员密码哈希 | 30 分钟 | ⚪ |

**上线前必修还剩 2 件事：关闭 `demo-mode`（5 分钟）+ 管理缓存接口加鉴权（15 分钟）。**

---

*报告生成时间: 2026-05-31（第三次更新）*  
*报告版本: 校园项目版（安全 + 用户体验）*

