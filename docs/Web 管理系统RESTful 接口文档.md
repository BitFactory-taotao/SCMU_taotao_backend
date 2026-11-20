# 校园淘淘 Web 管理系统 RESTful 接口

本文供管理系统前端开发与联调使用，由后端维护。学生端及实时消息见 [学生端接口](二手市场接口文档.md)。数据存储见 [数据库表结构](表结构设计.md)。

## 接口索引

按模块跳转。每个模块下的接口名链接到请求与响应详情。

| 模块 | 接口范围 |
| --- | --- |
| [管理员认证](#管理员认证) | 登录、资料与退出 |
| [工作台](#工作台) | 管理端总览指标 |
| [商品巡检](#商品巡检) | 列表、详情、通过与驳回 |
| [风险账号](#风险账号) | 风险列表、指标及批量处置 |
| [反馈管理](#反馈管理) | 列表、详情、标记未读与处理 |
| [举报审核](#举报审核) | 待审核列表、详情和审核结论 |
| [已处理事项](#已处理事项) | 列表、详情、计数与撤销 |

[请求体结构索引](#请求体结构索引) · [响应数据结构索引](#响应数据结构索引)

## 接入约定

| 项目 | 约定 |
| --- | --- |
| HTTP 基路径 | `/api/v1/campus-taotao`。接口路径相对于此基路径，使用部署环境的服务地址。 |
| 鉴权 | `/admin/login` 可匿名。其他路由传 `Authorization: Bearer <admin-token>`，也支持纯管理员 Token。学生 Token 访问管理路由时会被拒绝。 |
| 凭证有效期 | Token 由 Redis 管理，有效期 2 小时。校验成功会续期。 |
| 请求体 | 有 JSON 请求体时使用 `Content-Type: application/json`。 |
| 参数位置 | `Path` 是路径占位符，`Query` 拼接在 URL 上，JSON 字段放在请求体。 |
| 业务类型 | `int32` 和 `int64` 为相应宽度的整数，`number` 为 JSON 数值，`boolean` 为 `true` 或 `false`，`[]` 表示数组。 |

### HTTP 响应结构

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | `int32` | 业务码，`200` 表示成功。 |
| `msg` | `string` | 业务提示或失败原因。 |
| `data` | 按接口定义 | 成功的业务数据。值为空时可能省略。 |
| `total` | `int64` | 使用顶层总数分页时的总记录数。 |

鉴权失败由拦截器返回 HTTP `401` 和业务 `401`。Controller 或全局异常处理器返回 `Result` 时，HTTP 状态通常为 `200`，前端需检查业务 `code`。已识别的参数绑定和校验错误通常返回业务 `400`，其他错误按各接口说明处理。JSON 全局采用 `non_null` 配置。

### 无数据响应

```json
{
  "code": 200,
  "msg": "操作成功"
}
```

### 分页对象响应

`data.total` 为总记录数（`int64`），`data.pages` 为总页数（`int64`），`data.list` 为当前页数组。每个列表项的结构在对应接口中链接。

```json
{
  "code": 200,
  "msg": "请求成功",
  "data": {
    "total": 0,
    "pages": 0,
    "list": []
  }
}
```

### 顶层总数分页响应

`data` 为当前页数组，响应顶层 `total` 为总记录数（`int64`）。举报列表和风险账号列表使用此形状。

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [],
  "total": 0
}
```

## 分页请求参数

分页参数都放在 Query 中，类型为 `int32`。页码和每页条数均可省略，默认值分别为 `1` 和 `10`。各入口通过下表规则区分校验与回退行为。

| 规则 | 参数 | 页码 | 每页条数 |
| --- | --- | --- | --- |
| 每页最多 50 条 | `page`、`size` | 大于等于 1。 | 取值 1 到 50。 |
| 风险账号分页 | `page`、`pageSize` | 大于等于 1。 | 取值 1 到 50。 |

## 管理员认证

| 方法与路径 | 接口详情 | 鉴权 |
| --- | --- | --- |
| `POST /admin/login` | [管理员登录](#管理员登录) | 匿名 |
| `GET /admin/auth/profile` | [读取管理员资料](#读取管理员资料) | 管理员 Token |
| `POST /admin/logout` | [退出管理端](#退出管理端) | 管理员 Token |

### 管理员登录

`POST /admin/login` · 管理端 · 匿名

**请求**

JSON 请求体见 [管理员登录请求](#管理员登录请求)。

**响应**

成功时 `data` 为 [管理员登录结果](#管理员登录结果)。

**行为与错误**

请求体或字段为空 `400`，账号或密码错误 `401`。登录会更新最后登录时间。

### 读取管理员资料

`GET /admin/auth/profile` · 管理端 · 管理员 Token

**请求**

无业务参数。

**响应**

成功时 `data` 为 [管理员资料](#管理员资料)。

**行为与错误**

管理员资料不存在 `404`。

### 退出管理端

`POST /admin/logout` · 管理端 · 管理员 Token

**请求**

无业务参数。

**响应**

见[无数据响应](#无数据响应)。

**行为与错误**

使本次 Token 失效。过期或已退出的 Token 在拦截阶段返回 HTTP 401。

后端实现见 [AdminAuthController](../src/main/java/com/bit/scmu_taotao/controller/admin/AdminAuthController.java)、[TAdminServiceImpl](../src/main/java/com/bit/scmu_taotao/service/impl/TAdminServiceImpl.java)。

## 工作台

| 方法与路径 | 接口详情 | 鉴权 |
| --- | --- | --- |
| `GET /admin/statistics/overview` | [读取工作台总览](#读取工作台总览) | 管理员 Token |

### 读取工作台总览

`GET /admin/statistics/overview` · 管理端 · 管理员 Token

**请求**

无业务参数。

**响应**

成功时 `data` 为 [工作台统计数据](#工作台统计数据)。

**行为与错误**

总览缓存有效期 10 秒。昨日值为 0 且今日大于 0 时趋势返回 `+100%`。查询失败返回业务 `500`。

后端实现见 [AdminStatisticsController](../src/main/java/com/bit/scmu_taotao/controller/admin/AdminStatisticsController.java)、[AdminDashboardServiceImpl](../src/main/java/com/bit/scmu_taotao/service/impl/AdminDashboardServiceImpl.java)。

## 商品巡检

批量操作去除空 ID 和重复 ID。已查到的商品中存在不处于待巡检状态的记录时，整批返回业务 `400`。个别缺失 ID 没有单独的响应项。

| 方法与路径 | 接口详情 | 鉴权 |
| --- | --- | --- |
| `GET /admin/goods/audit/list` | [列出巡检商品](#列出巡检商品) | 管理员 Token |
| `GET /admin/goods/audit/{goodsId}` | [读取商品巡检详情](#读取商品巡检详情) | 管理员 Token |
| `PUT /admin/goods/audit/approve` | [通过商品巡检](#通过商品巡检) | 管理员 Token |
| `PUT /admin/goods/audit/reject` | [驳回商品巡检](#驳回商品巡检) | 管理员 Token |

### 列出巡检商品

`GET /admin/goods/audit/list` · 管理端 · 管理员 Token

**请求**

| 位置 | 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- | --- |
| Query | `auditStatus` | `int32` | 选填 | `0` | 0 待巡检、1 已通过、2 已驳回。0 同时匹配历史空状态。 |
| Query | `category` | `string` | 选填 | — | dormitory、entertainment 或 study。 |
| Query | `keyword` | `string` | 选填 | — | 按商品名称、摘要或发布者 ID 模糊匹配。 |

Query 分页规则见[分页请求参数](#分页请求参数)中的 **每页最多 50 条**。

**响应**

返回[分页对象响应](#分页对象响应)。`data.list` 元素为 [商品摘要](二手市场接口文档.md#商品摘要)。

**行为与错误**

过滤已删除商品，按发布时间倒序。分类或状态不合法返回业务 `400`。

### 读取商品巡检详情

`GET /admin/goods/audit/{goodsId}` · 管理端 · 管理员 Token

**请求**

| 位置 | 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- | --- |
| Path | `goodsId` | `int64` | 必填 | — | 大于等于 1 的商品 ID。 |

**响应**

成功时 `data` 为 [商品详情数据](二手市场接口文档.md#商品详情数据)。

**行为与错误**

无商品 `404`，主键须大于 0。

### 通过商品巡检

`PUT /admin/goods/audit/approve` · 管理端 · 管理员 Token

**请求**

JSON 请求体字段如下。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `goodsIds` | `int64[]` | 必填 | 非空商品 ID 数组，单条也通过数组提交。 |

**响应**

见[无数据响应](#无数据响应)。

**行为与错误**

把待巡检商品设为 `isAudited=1`、在线。状态不符 `400`，未查到任何商品 `404`。

### 驳回商品巡检

`PUT /admin/goods/audit/reject` · 管理端 · 管理员 Token

**请求**

JSON 请求体字段如下。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `goodsIds` | `int64[]` | 必填 | 非空商品 ID 数组。 |
| `reason` | `string` | 必填 | 非空白的驳回原因。 |

```json
{
  "goodsIds": [
    101,
    102
  ],
  "reason": "商品描述需要补充"
}
```

**响应**

见[无数据响应](#无数据响应)。

**行为与错误**

把待巡检商品设为 `isAudited=2`、下架，保存原因并通知发布者。状态不符 `400`，未查到任何商品 `404`。

后端实现见 [AdminGoodsAuditController](../src/main/java/com/bit/scmu_taotao/controller/admin/AdminGoodsAuditController.java)、[TGoodsServiceImpl](../src/main/java/com/bit/scmu_taotao/service/impl/TGoodsServiceImpl.java)。

## 风险账号

| 方法与路径 | 接口详情 | 鉴权 |
| --- | --- | --- |
| `GET /admin/users/risk/list` | [列出风险账号](#列出风险账号) | 管理员 Token |
| `GET /admin/users/risk/{userId}/metrics` | [读取风险账号指标](#读取风险账号指标) | 管理员 Token |
| `PUT /admin/users/risk/handle` | [处置风险账号](#处置风险账号) | 管理员 Token |

### 列出风险账号

`GET /admin/users/risk/list` · 管理端 · 管理员 Token

**请求**

| 位置 | 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- | --- |
| Query | `keyword` | `string` | 选填 | — | 按姓名或学号模糊匹配。 |

Query 分页规则见[分页请求参数](#分页请求参数)中的 **风险账号分页**。

**响应**

返回[顶层总数分页响应](#顶层总数分页响应)。`data` 元素为 [风险账号列表项](#风险账号列表项)。

**行为与错误**

返回 `status=2`、信誉分低于 70 且未删除的用户，按注册时间倒序。

### 读取风险账号指标

`GET /admin/users/risk/{userId}/metrics` · 管理端 · 管理员 Token

**请求**

| 位置 | 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- | --- |
| Path | `userId` | `string` | 必填 | — | 用户的学号或工号。 |

**响应**

成功时 `data` 为 [风险账号详情](#风险账号详情)。

**行为与错误**

无有效用户 `404`。

### 处置风险账号

`PUT /admin/users/risk/handle` · 管理端 · 管理员 Token

**请求**

JSON 请求体见 [风险账号处置请求](#风险账号处置请求)。

**响应**

成功时 `data` 的类型为 `string`。返回 `操作成功 N 条数据`，`N` 为处理条数。

**行为与错误**

`BAN` 查封账号并记录原因。`CLEAR` 恢复账号正常、清空违规原因，信誉分取原有分数与 80 的较大值。跳过不存在或已删除的账号。成功处置写入信誉流水和审核日志，并发系统通知。请求为空或清理后没有 ID 返回业务 `400`。

后端实现见 [AdminRiskUserController](../src/main/java/com/bit/scmu_taotao/controller/admin/AdminRiskUserController.java)、[AdminRiskUserServiceImpl](../src/main/java/com/bit/scmu_taotao/service/impl/AdminRiskUserServiceImpl.java)。

## 反馈管理

反馈的处理进度 `status` 与后台已读标记 `is_read` 独立。

| 方法与路径 | 接口详情 | 鉴权 |
| --- | --- | --- |
| `GET /admin/feedback/list` | [列出后台反馈](#列出后台反馈) | 管理员 Token |
| `GET /admin/feedback/{feedbackId}` | [读取后台反馈详情](#读取后台反馈详情) | 管理员 Token |
| `PUT /admin/feedback/mark-unread` | [标记反馈未读](#标记反馈未读) | 管理员 Token |
| `POST /admin/feedback/{feedbackId}/resolve` | [回复并处理反馈](#回复并处理反馈) | 管理员 Token |

### 列出后台反馈

`GET /admin/feedback/list` · 管理端 · 管理员 Token

**请求**

| 位置 | 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- | --- |
| Query | `keyword` | `string` | 选填 | — | 按反馈正文或用户姓名模糊匹配。 |
| Query | `status` | `string` | 选填 | — | pending 待处理或 processed 已处理。缺省或其他值不增加状态筛选。 |

Query 分页规则见[分页请求参数](#分页请求参数)中的 **每页最多 50 条**。

**响应**

返回[分页对象响应](#分页对象响应)。`data.list` 元素为 [管理端反馈列表项](#管理端反馈列表项)。

**行为与错误**

过滤已删除反馈，按提交时间倒序。

### 读取后台反馈详情

`GET /admin/feedback/{feedbackId}` · 管理端 · 管理员 Token

**请求**

| 位置 | 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- | --- |
| Path | `feedbackId` | `int64` | 必填 | — | 反馈记录 ID。列表响应中的 `id` 可是同一编号的十进制字符串。 |

**响应**

成功时 `data` 为 [管理端反馈详情](#管理端反馈详情)。

**行为与错误**

获取未读反馈时写入已读状态，并返回 `is_read=read`。未找到用 `404`。

### 标记反馈未读

`PUT /admin/feedback/mark-unread` · 管理端 · 管理员 Token

**请求**

JSON 请求体字段如下。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `feedbackIds` | `int64[]` | 必填 | 反馈 ID 数组，1 到 20 项。 |

**响应**

见[无数据响应](#无数据响应)。

**行为与错误**

设置为未读，`msg` 包含实际更新条数。

### 回复并处理反馈

`POST /admin/feedback/{feedbackId}/resolve` · 管理端 · 管理员 Token

**请求**

| 位置 | 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- | --- |
| Path | `feedbackId` | `int64` | 必填 | — | 反馈记录 ID。列表响应中的 `id` 可是同一编号的十进制字符串。 |

JSON 请求体字段如下。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `replyContent` | `string` | 必填 | 非空白回复内容，最多 2500 字符。 |

**响应**

见[无数据响应](#无数据响应)。

**行为与错误**

仅在当前状态待处理时写入回复并通知用户。无记录 `404`，状态已改变 `409`。

后端实现见 [AdminFeedbackController](../src/main/java/com/bit/scmu_taotao/controller/admin/AdminFeedbackController.java)、[TFeedbackServiceImpl](../src/main/java/com/bit/scmu_taotao/service/impl/TFeedbackServiceImpl.java)。

## 举报审核

| 方法与路径 | 接口详情 | 鉴权 |
| --- | --- | --- |
| `GET /admin/reports` | [列出待审核举报](#列出待审核举报) | 管理员 Token |
| `GET /admin/reports/{reportId}` | [读取举报详情](#读取举报详情) | 管理员 Token |
| `PUT /admin/reports/{reportId}/verify` | [提交举报审核结论](#提交举报审核结论) | 管理员 Token |

### 列出待审核举报

`GET /admin/reports` · 管理端 · 管理员 Token

**请求**

| 位置 | 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- | --- |
| Query | `keyword` | `string` | 选填 | — | 匹配被举报用户的姓名或学号。 |

Query 分页规则见[分页请求参数](#分页请求参数)中的 **每页最多 50 条**。

**响应**

返回[顶层总数分页响应](#顶层总数分页响应)。`data` 元素为 [举报列表项](#举报列表项)。

**行为与错误**

只查询待处理举报，按创建时间倒序。

### 读取举报详情

`GET /admin/reports/{reportId}` · 管理端 · 管理员 Token

**请求**

| 位置 | 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- | --- |
| Path | `reportId` | `int64` | 必填 | — | 举报记录 ID。 |

**响应**

成功时 `data` 为 [举报详情](#举报详情)。

**行为与错误**

未找到用 `404`。

### 提交举报审核结论

`PUT /admin/reports/{reportId}/verify` · 管理端 · 管理员 Token

**请求**

| 位置 | 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- | --- |
| Path | `reportId` | `int64` | 必填 | — | 举报记录 ID。 |

JSON 请求体见 [举报审核请求](#举报审核请求)。

**响应**

成功时 `data` 为 [举报审核结果](#举报审核结果)。

**行为与错误**

属实时扣分最低到 0。低于 70 时触发风险标记，并写入信誉流水及通知。不属实时保留信誉分并通知举报人。两种动作都会把举报标记为已处理。已处理或非法扣分用业务 `400`，举报不存在或属实时找不到目标用户用 `404`。

后端实现见 [AdminReportController](../src/main/java/com/bit/scmu_taotao/controller/admin/AdminReportController.java)、[TUserReportServiceImpl](../src/main/java/com/bit/scmu_taotao/service/impl/TUserReportServiceImpl.java)。

## 已处理事项

| 方法与路径 | 接口详情 | 鉴权 |
| --- | --- | --- |
| `GET /admin/solved-items/list` | [列出已处理事项](#列出已处理事项) | 管理员 Token |
| `GET /admin/solved-items/detail` | [读取已处理事项详情](#读取已处理事项详情) | 管理员 Token |
| `GET /admin/solved-items/count` | [统计已处理事项数量](#统计已处理事项数量) | 管理员 Token |
| `PUT /admin/solved-items/revoke` | [撤销已处理事项](#撤销已处理事项) | 管理员 Token |

### 列出已处理事项

`GET /admin/solved-items/list` · 管理端 · 管理员 Token

**请求**

| 位置 | 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- | --- |
| Query | `type` | `string` | 必填 | — | goods 商品、feedback 反馈或 user 账号。 |
| Query | `status` | `string` | 选填 | — | goods 支持 APPROVED、REJECTED。user 支持 BAN、CLEAR。feedback 按已处理状态查询。 |
| Query | `keyword` | `string` | 选填 | — | 商品按名称、摘要或发布者匹配。反馈按内容或用户匹配。账号按姓名或 ID 匹配。 |

Query 分页规则见[分页请求参数](#分页请求参数)中的 **每页最多 50 条**。

**响应**

返回[分页对象响应](#分页对象响应)。按请求的 `type` 选择 `data.list` 的元素结构。

| type | 列表项 |
| --- | --- |
| `goods` | [已处理商品列表项](#已处理商品列表项) |
| `feedback` | [已处理反馈列表项](#已处理反馈列表项) |
| `user` | [已处理账号列表项](#已处理账号列表项) |

**行为与错误**

类型非法返回业务 `400`。查询账号事项时，有 `status` 筛选会先按动作查询，再取每个用户最新有效记录。

### 读取已处理事项详情

`GET /admin/solved-items/detail` · 管理端 · 管理员 Token

**请求**

| 位置 | 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- | --- |
| Query | `type` | `string` | 必填 | — | goods 商品、feedback 反馈或 user 账号。 |
| Query | `id` | `string` | 必填 | — | goods 和 feedback 为实体编号的十进制字符串，user 为用户 ID。 |

**响应**

成功时 `data` 为 [已处理事项详情](#已处理事项详情)。

**行为与错误**

`goods` 和 `feedback` 的 `id` 为数字字符串，`user` 的 `id` 为学号。不存在或已撤销 `400`。

### 统计已处理事项数量

`GET /admin/solved-items/count` · 管理端 · 管理员 Token

**请求**

无业务参数。

**响应**

成功时 `data` 为对象，字段如下。

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `goodsCount` | `int64` | 总是 | 已巡检且未删除的商品数。 |
| `feedbackCount` | `int64` | 总是 | 已处理且未删除的反馈数。 |
| `userCount` | `int64` | 总是 | 按用户去重后的有效账号审核记录数。 |
| `totalCount` | `int64` | 总是 | 以上三项的合计。 |

**行为与错误**

统计已巡检商品、已处理反馈、每个用户的最新有效审核记录，`totalCount` 为三者之和。

### 撤销已处理事项

`PUT /admin/solved-items/revoke` · 管理端 · 管理员 Token

**请求**

JSON 请求体见 [已处理事项撤销请求](#已处理事项撤销请求)。

**响应**

见[无数据响应](#无数据响应)。

**行为与错误**

- `goods` 恢复待巡检。原为驳回时清空原因，保留商品上下架状态。
- `feedback` 恢复待处理并清空回复内容、回复时间，保留已读状态。
- `user` 恢复最近有效审核记录保存的原状态。恢复正常时清空违规原因，审核记录标记删除。信誉分保持原值。
- 事项不存在、已撤销、类型非法或状态异常时返回业务 `400`。

后端实现见 [AdminSolvedItemsController](../src/main/java/com/bit/scmu_taotao/controller/admin/AdminSolvedItemsController.java)、[AdminSolvedItemsServiceImpl](../src/main/java/com/bit/scmu_taotao/service/impl/AdminSolvedItemsServiceImpl.java)、[TAccountAuditLogMapper.xml](../src/main/resources/com/bit/scmu_taotao/mapper/TAccountAuditLogMapper.xml)。

## 请求体结构索引

[管理员登录请求](#管理员登录请求) · [风险账号处置请求](#风险账号处置请求) · [举报审核请求](#举报审核请求) · [已处理事项撤销请求](#已处理事项撤销请求)

### 管理员登录请求

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `adminId` | `string` | 必填 | 非空白管理员账号。 |
| `password` | `string` | 必填 | 非空白登录密码。 |

### 风险账号处置请求

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userIds` | `string[]` | 必填 | 非空用户 ID 数组。服务端去除空白并去重。 |
| `action` | `string` | 必填 | BAN 查封，CLEAR 消除风险。 |
| `reason` | `string` | 选填 | 处置原因，空白按未填写处理。 |

### 举报审核请求

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `action` | `string` | 必填 | PASS 属实，REJECT 不属实。 |
| `deductScore` | `int32` | 条件必填 | PASS 时必填，范围 1 到 100。REJECT 可省略。 |

```json
{
  "action": "PASS",
  "deductScore": 10
}
```

### 已处理事项撤销请求

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `type` | `string` | 必填 | goods、feedback 或 user。 |
| `id` | `string` | 必填 | 商品或反馈使用对应 ID 的十进制字符串，账号使用用户 ID。 |

```json
{
  "type": "goods",
  "id": "101"
}
```

## 响应数据结构索引

[管理员登录结果](#管理员登录结果) · [管理员资料](#管理员资料) · [工作台统计数据](#工作台统计数据) · [工作台单项指标](#工作台单项指标) · [风险账号列表项](#风险账号列表项)

[风险账号详情](#风险账号详情) · [风险账号指标](#风险账号指标) · [管理端反馈列表项](#管理端反馈列表项) · [管理端反馈详情](#管理端反馈详情) · [被举报用户摘要](#被举报用户摘要)

[举报列表项](#举报列表项) · [举报详情](#举报详情) · [举报审核结果](#举报审核结果) · [已处理商品列表项](#已处理商品列表项) · [已处理反馈列表项](#已处理反馈列表项)

[已处理账号列表项](#已处理账号列表项) · [已处理事项详情](#已处理事项详情) · [已处理商品详情](#已处理商品详情) · [已处理商品发布者](#已处理商品发布者) · [已处理反馈详情](#已处理反馈详情)

[已处理账号详情](#已处理账号详情)

### 管理员登录结果

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `token` | `string` | 总是 | 用于管理端鉴权的 Token。 |
| `nickname` | `string` | 有值时 | 管理员昵称。 |
| `lastLogin` | `string` | 总是 | 本次登录时间，格式 yyyy-MM-dd HH:mm:ss。 |

### 管理员资料

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `adminId` | `string` | 总是 | 管理员登录账号。 |
| `nickname` | `string` | 有值时 | 管理员昵称。 |
| `lastLogin` | `string` | 有值时 | 最近登录时间，格式 yyyy-MM-dd HH:mm:ss。 |

### 工作台统计数据

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `pendingGoods` | [工作台单项指标](#工作台单项指标) 对象 | 总是 | 待巡检且未删除的商品。按 is_audited=0 计数。 |
| `riskUsers` | [工作台单项指标](#工作台单项指标) 对象 | 总是 | 风险待审核、信誉分低于 70 且未删除的账号。 |
| `pendingFeedback` | [工作台单项指标](#工作台单项指标) 对象 | 总是 | 待处理且未删除的反馈。 |
| `pendingReports` | [工作台单项指标](#工作台单项指标) 对象 | 总是 | 待审核的举报。 |
| `solvedItems` | [工作台单项指标](#工作台单项指标) 对象 | 总是 | 已处理事项总数，与已处理事项计数共用逻辑。 |
| `snapshotTime` | `string` | 总是 | 本次返回的快照时间，格式 yyyy-MM-dd HH:mm:ss。 |

### 工作台单项指标

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `count` | `int64` | 总是 | 指标数量。 |
| `trend` | `string` | 总是 | 与昨日 Redis 快照比较的百分比字符串，例如 +100%、-20% 或 0%。 |

### 风险账号列表项

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `userId` | `string` | 总是 | 用户 ID。 |
| `userName` | `string` | 有值时 | 用户姓名。 |
| `avatar` | `string` | 有值时 | 头像 URL。 |
| `riskLevel` | `string` | 总是 | 60 到 69 为低度风险，40 到 59 为中度风险，0 到 39 为高度风险，其余为未知。 |
| `registerTime` | `string` | 总是 | 账号创建时间，格式 yyyy-MM-dd HH:mm:ss。缺少时间时为空字符串。 |

### 风险账号详情

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `userId` | `string` | 总是 | 用户 ID。 |
| `userName` | `string` | 有值时 | 用户姓名。 |
| `avatar` | `string` | 有值时 | 头像 URL。 |
| `creditScore` | `int32` | 有值时 | 当前信誉分。 |
| `creditStar` | `number` | 有值时 | 信誉星级。 |
| `metrics` | [风险账号指标](#风险账号指标) 对象 | 总是 | 当前统计数据。 |

### 风险账号指标

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `reportCount` | `int64` | 总是 | 已处理举报数量，包含属实和不属实的已处理记录。 |
| `itemViolationCount` | `int64` | 总是 | 商品巡检驳回数。 |
| `blacklistCount` | `int64` | 总是 | 有效被拉黑记录数。 |
| `langViolationCount` | `int64` | 总是 | 已处理的语言类举报数。 |
| `lowRatingCount` | `int64` | 总是 | 当前统计条件为 total_score < 40。学生端每项评分范围为 0 到 5。 |

### 管理端反馈列表项

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `id` | `string` | 总是 | 反馈 ID 的字符串形式。 |
| `userId` | `string` | 总是 | 反馈人 ID。 |
| `userName` | `string` | 有值时 | 反馈人姓名。 |
| `avatar` | `string` | 有值时 | 反馈人头像。 |
| `content` | `string` | 总是 | 反馈内容。 |
| `submitTime` | `string` | 有值时 | 提交时间，格式 yyyy-MM-dd HH:mm:ss。 |
| `status` | `string` | 总是 | pending 待处理或 processed 已处理。 |
| `is_read` | `string` | 总是 | unread 未读或 read 已读。 |

### 管理端反馈详情

包含 [管理端反馈列表项](#管理端反馈列表项) 的全部字段，另有下列字段。

详情包含列表项的全部字段。已读标记反映此次读取后的状态。

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `replyContent` | `string` | 有值时 | 管理员回复内容。 |

### 被举报用户摘要

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `id` | `string` | 总是 | 被举报用户 ID。 |
| `name` | `string` | 有值时 | 被举报用户姓名。 |
| `avatar` | `string` | 有值时 | 头像 URL。 |

### 举报列表项

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `reportId` | `int64` | 总是 | 举报记录 ID。 |
| `targetUser` | [被举报用户摘要](#被举报用户摘要) 对象 | 有值时 | 被举报用户资料。 |
| `tag` | `string` | 有值时 | 举报分类。 |
| `tagDesc` | `string` | 总是 | 分类名称。 |
| `createTime` | `string` | 总是 | 创建时间，格式 yyyy-MM-dd HH:mm:ss。 |
| `status` | `int32` | 总是 | 当前列表只返回待处理记录，值为 0。 |

### 举报详情

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `reportId` | `int64` | 总是 | 举报记录 ID。 |
| `targetUser` | [被举报用户摘要](#被举报用户摘要) 对象 | 有值时 | 被举报用户资料。 |
| `tag` | `string` | 有值时 | 举报分类。 |
| `tagDesc` | `string` | 总是 | 分类名称。 |
| `content` | `string` | 有值时 | 举报正文。 |
| `imgUrls` | `string[]` | 总是 | 证据图 URL 数组。 |
| `createTime` | `string` | 总是 | 创建时间，格式 yyyy-MM-dd HH:mm:ss。 |

### 举报审核结果

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `reportId` | `int64` | 总是 | 处理的举报 ID。 |
| `targetUserId` | `string` | 总是 | 被举报用户 ID。 |
| `newCreditScore` | `int32` | 有值时 | 处理后的信誉分。驳回且查不到用户资料时可省略。 |
| `status` | `string` | 总是 | 固定为 PROCESSED。 |

### 已处理商品列表项

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `goodsId` | `int64` | 总是 | 商品 ID。 |
| `name` | `string` | 有值时 | 商品名称。 |
| `remark` | `string` | 有值时 | 商品摘要。 |
| `price` | `number` | 有值时 | 商品价格，单位元。 |
| `imgUrl` | `string` | 有值时 | 主图 URL。 |
| `publishTime` | `string` | 总是 | 发布时间，格式 yyyy-MM-dd HH:mm:ss。缺少时间时为空串。 |
| `handleStatus` | `string` | 总是 | APPROVED 已通过或 REJECTED 已驳回。 |
| `handleTime` | `string` | 总是 | 处理时间取商品更新时间。缺少时间时为空串。 |

### 已处理反馈列表项

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `feedbackId` | `int64` | 总是 | 反馈 ID。 |
| `userId` | `string` | 总是 | 反馈人 ID。 |
| `userName` | `string` | 有值时 | 反馈人姓名。 |
| `avatar` | `string` | 有值时 | 反馈人头像。 |
| `replyContent` | `string` | 有值时 | 处理时的回复内容。 |
| `submitTime` | `string` | 总是 | 提交时间，格式 yyyy-MM-dd HH:mm:ss。缺少时为空串。 |
| `replyTime` | `string` | 总是 | 回复时间，格式 yyyy-MM-dd HH:mm:ss。缺少时为空串。 |

### 已处理账号列表项

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `userId` | `string` | 总是 | 账号 ID。 |
| `userName` | `string` | 有值时 | 用户姓名。 |
| `avatar` | `string` | 有值时 | 头像 URL。 |
| `reason` | `string` | 有值时 | 审核记录中的处置原因。 |
| `registerTime` | `string` | 有值时 | 账号注册时间，格式 yyyy-MM-dd HH:mm:ss。 |
| `handleStatus` | `string` | 总是 | BAN 查封或 CLEAR 消除风险。 |

### 已处理事项详情

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `type` | `string` | 总是 | goods、feedback 或 user。 |
| `handleStatus` | `string` | 总是 | goods 为 APPROVED 或 REJECTED，feedback 为 PROCESSED，user 为 BAN 或 CLEAR。 |
| `handleTime` | `string` | 总是 | 格式 yyyy-MM-dd HH:mm:ss。商品取更新时间，反馈取回复时间，账号取审核记录创建时间。 |
| `goodsDetail` | [已处理商品详情](#已处理商品详情) 对象 | 条件返回 | type=goods 时返回。 |
| `feedbackDetail` | [已处理反馈详情](#已处理反馈详情) 对象 | 条件返回 | type=feedback 时返回。 |
| `userDetail` | [已处理账号详情](#已处理账号详情) 对象 | 条件返回 | type=user 时返回。 |

### 已处理商品详情

此处 `goodsType` 与学生端发布和管理端巡检的 `type` 口径不同。返回值以当前已处理事项实现为准。

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `goodsId` | `int64` | 总是 | 商品 ID。 |
| `name` | `string` | 有值时 | 商品名称。 |
| `desc` | `string` | 有值时 | 商品详情。 |
| `remark` | `string` | 有值时 | 商品摘要。 |
| `price` | `number` | 有值时 | 商品价格，单位元。 |
| `purpose` | `string` | 有值时 | 商品用途。 |
| `exchangeAddr` | `string` | 有值时 | 交易地点。 |
| `imgUrls` | `string[]` | 总是 | 图片 URL 数组。 |
| `publishTime` | `string` | 总是 | 发布时间，格式 yyyy-MM-dd HH:mm:ss。缺少时为空串。 |
| `publisher` | [已处理商品发布者](#已处理商品发布者) 对象 | 有值时 | 发布者资料。 |
| `goodsType` | `string` | 总是 | 当前实现按 0 返回 sell、1 返回 buy，其余非空值转字符串。无值时为空串。 |
| `rejectReason` | `string` | 有值时 | 商品驳回原因。 |

### 已处理商品发布者

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `userId` | `string` | 总是 | 发布者用户 ID。 |
| `userName` | `string` | 有值时 | 发布者姓名。 |
| `avatar` | `string` | 有值时 | 头像 URL。 |
| `creditScore` | `int32` | 有值时 | 信誉分。 |
| `creditStar` | `number` | 有值时 | 信誉星级。 |

### 已处理反馈详情

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `feedbackId` | `int64` | 总是 | 反馈 ID。 |
| `userId` | `string` | 总是 | 反馈人 ID。 |
| `userName` | `string` | 有值时 | 反馈人姓名。 |
| `avatar` | `string` | 有值时 | 反馈人头像。 |
| `submitTime` | `string` | 总是 | 提交时间，格式 yyyy-MM-dd HH:mm:ss。 |
| `content` | `string` | 总是 | 反馈正文。 |
| `replyContent` | `string` | 有值时 | 管理员回复。 |

### 已处理账号详情

| 字段 | 类型 | 返回条件 | 说明 |
| --- | --- | --- | --- |
| `userId` | `string` | 总是 | 账号 ID。 |
| `userName` | `string` | 有值时 | 用户姓名。 |
| `avatar` | `string` | 有值时 | 头像 URL。 |
| `creditScore` | `int32` | 有值时 | 当前信誉分。 |
| `creditStar` | `number` | 有值时 | 当前信誉星级。 |
| `reasonCategory` | `string` | 总是 | 出现最多的已处理举报分类。没有相关记录时为空字符串。 |
| `violationReason` | `string` | 有值时 | 审核流水保存的处置原因。 |
