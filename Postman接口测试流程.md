# Postman 接口测试流程文档

**文档用途**：基于Postman进行推荐系统接口的完整测试  
**测试环境**：本地开发环境 (localhost:8080)  
**前置条件**：应用已启动，数据库和Redis已就绪  
**预计耗时**：30分钟（完整测试）

---

## 📋 目录

1. [环境准备](#环境准备)
2. [Postman集合设置](#postman集合设置)
3. [测试用例详解](#测试用例详解)
4. [测试流程](#测试流程)
5. [断言验证](#断言验证)
6. [常见错误处理](#常见错误处理)

---

## 环境准备

### 前置条件检查

```bash
# 1. 应用是否启动
curl http://localhost:8080/api/v1/campus-taotao/goods?tab=recommend&page=1&size=1

# 2. 数据库是否就绪
mysql -u root -p taotao_db -e "SELECT COUNT(*) FROM t_goods;"

# 3. Redis是否就绪
redis-cli PING
# 预期输出: PONG
```

### Postman 导入准备

**步骤1：创建新集合**
- 打开Postman
- 点击 "File" → "New" → "Collection"
- 命名为 "SCMU_taotao_推荐系统"

**步骤2：创建环境变量**
- 点击 "Environments" → "Create New Environment"
- 命名为 "Development"

**步骤3：添加环境变量**

| 变量名 | 类型 | 初始值 | 说明 |
|---|---|---|---|
| `baseUrl` | string | `http://localhost:8080/api/v1/campus-taotao` | API基地址 |
| `userId` | string | `20240001` | 测试用户ID |
| `token` | string | `（待设置）` | 登录token |
| `goodsId` | string | `1001` | 测试商品ID |
| `categoryId` | string | `1` | 电子产品分类ID |

**步骤4：设置活跃环境**
- 右上角选择 "Development" 环境

---

## Postman 集合设置

### 集合结构

```
SCMU_taotao_推荐系统
├── 1. 环境检查
│   ├── 1.1 获取推荐商品列表（冷启动）
│   ├── 1.2 数据库连接验证
│   └── 1.3 Redis连接验证
├── 2. 首页功能
│   ├── 2.1 获取推荐列表（tab=recommend）
│   ├── 2.2 获取宿舍用品（tab=dormitory）
│   ├── 2.3 获取娱乐用品（tab=entertainment）
│   ├── 2.4 获取学习用品（tab=study）
│   └── 2.5 获取悬赏商品（tab=pre-order）
├── 3. 商品详情 + 埋点
│   ├── 3.1 获取商品详情
│   ├── 3.2 验证浏览记录保存
│   ├── 3.3 验证防刷机制
│   └── 3.4 验证点击量更新
├── 4. 用户偏好
│   ├── 4.1 获取用户偏好分类
│   └── 4.2 验证偏好分类准确性
├── 5. 缓存管理
│   ├── 5.1 查看系统统计
│   ├── 5.2 手动刷新热门缓存
│   └── 5.3 验证缓存刷新
└── 6. 性能测试
    ├── 6.1 单个请求响应时间
    ├── 6.2 批量请求并发
    └── 6.3 缓存命中率
```

---

## 测试用例详解

### Test Set 1: 环境检查

#### 测试1.1：获取推荐商品列表（冷启动）

**请求**：
```http
GET {{baseUrl}}/goods?tab=recommend&page=1&size=10
```

**Headers**：无需身份验证（冷启动）

**Postman设置**：
```javascript
// Pre-request Script: 无

// Tests（断言）
pm.test("状态码为200", function() {
    pm.expect(pm.response.code).to.equal(200);
});

pm.test("响应包含data字段", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("data");
});

pm.test("列表不为空", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.list.length).to.be.greaterThan(0);
});

pm.test("响应时间 < 300ms", function() {
    pm.expect(pm.response.responseTime).to.be.below(300);
});
```

**预期结果**：
- ✅ status: 200
- ✅ code: 200
- ✅ data.list 包含至少1件商品
- ✅ 响应时间 < 300ms

**验证点**：
- 冷启动推荐是否正常工作
- 热门商品缓存是否有效
- 列表格式是否正确

---

### Test Set 2: 首页功能

#### 测试2.1：获取推荐列表（tab=recommend）

**请求**：
```http
GET {{baseUrl}}/goods?tab=recommend&page=1&size=10
```

**Postman Tests**：
```javascript
pm.test("推荐列表返回成功", function() {
    pm.expect(pm.response.code).to.equal(200);
});

pm.test("返回total/pages/list字段", function() {
    var data = pm.response.json().data;
    pm.expect(data).to.have.property("total");
    pm.expect(data).to.have.property("pages");
    pm.expect(data).to.have.property("list");
});

pm.test("商品包含必要字段", function() {
    var list = pm.response.json().data.list;
    if (list.length > 0) {
        var item = list[0];
        pm.expect(item).to.have.property("id");
        pm.expect(item).to.have.property("name");
        pm.expect(item).to.have.property("price");
        pm.expect(item).to.have.property("imgUrl");
        pm.expect(item).to.have.property("publisherName");
    }
});
```

**预期结果**：
```json
{
  "code": 200,
  "msg": "请求成功",
  "data": {
    "total": 128,
    "pages": 13,
    "list": [
      {
        "id": 1001,
        "name": "商品名称",
        "remark": "商品备注",
        "price": 99.90,
        "imgUrl": "https://...",
        "publishTime": "2026-04-04 10:30:00",
        "publisherName": "张三",
        "publisherId": "20240001"
      }
    ]
  }
}
```

---

#### 测试2.2-2.5：按分类获取商品（tab切换）

**请求模板**：
```http
GET {{baseUrl}}/goods?tab={{TAB_VALUE}}&page=1&size=10
```

**TAB值测试表**：

| 用例 | tab值 | 预期goods_type | 预期goods_status |
|---|---|---|---|
| 2.2 | dormitory | 1 | 0 |
| 2.3 | entertainment | 1 | 0 |
| 2.4 | study | 1 | 0 |
| 2.5 | pre-order | 2 | 0 |

**Postman Tests（相同）**：
```javascript
pm.test("分类查询成功", function() {
    pm.expect(pm.response.code).to.equal(200);
});

pm.test("返回商品列表", function() {
    var list = pm.response.json().data.list;
    pm.expect(list).to.be.an("array");
    // 至少有一条数据（取决于测试数据）
});

pm.test("响应时间 < 200ms", function() {
    pm.expect(pm.response.responseTime).to.be.below(200);
});
```

---

### Test Set 3: 商品详情 + 埋点

#### 测试3.1：获取商品详情

**请求**：
```http
GET {{baseUrl}}/goods/{{goodsId}}
Authorization: Bearer {{token}}
```

**Headers**：
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Postman Tests**：
```javascript
pm.test("商品详情获取成功", function() {
    pm.expect(pm.response.code).to.equal(200);
});

pm.test("返回完整商品信息", function() {
    var data = pm.response.json().data;
    pm.expect(data).to.have.property("id");
    pm.expect(data).to.have.property("name");
    pm.expect(data).to.have.property("desc");
    pm.expect(data).to.have.property("price");
    pm.expect(data).to.have.property("imgUrls");
    pm.expect(data).to.have.property("publisher");
});

pm.test("发布者信息完整", function() {
    var publisher = pm.response.json().data.publisher;
    pm.expect(publisher).to.have.property("id");
    pm.expect(publisher).to.have.property("name");
    pm.expect(publisher).to.have.property("creditScore");
    pm.expect(publisher).to.have.property("creditStar");
});
```

**预期结果**：
```json
{
  "code": 200,
  "msg": "请求成功",
  "data": {
    "id": 1001,
    "name": "iPad Air 256GB",
    "desc": "原装港版，无拆无修...",
    "price": 2999.00,
    "purpose": "日常使用",
    "exchangeAddr": "校园内",
    "publishTime": "2026-04-04 10:30:00",
    "type": "sell",
    "imgUrls": ["https://...", "https://..."],
    "publisher": {
      "id": "20240001",
      "name": "张三",
      "creditScore": 95,
      "creditStar": 4.8
    }
  }
}
```

**埋点验证**（后台自动处理）：
- ✅ 浏览记录已保存（检查DB）
- ✅ 防刷规则已应用（检查Redis）
- ✅ 点击量已更新（检查DB）

---

#### 测试3.2：验证浏览记录保存

**手动验证步骤**：

1. **获取商品详情**
   ```
   GET /goods/1001
   ```
   记录响应时间戳 T1

2. **查询数据库**
   ```sql
   SELECT * FROM user_goods_browse 
   WHERE user_id='20240001' AND goods_id=1001
   ORDER BY browse_time DESC LIMIT 1;
   ```

3. **验证**
   - ✅ 浏览记录存在
   - ✅ browse_time >= T1
   - ✅ user_id 正确
   - ✅ goods_id 正确

---

#### 测试3.3：验证防刷机制

**测试流程**：

```javascript
// 第1次请求
GET /goods/1001
// 预期：点击量 +1

// 立即第2次请求（同一用户，同一商品，1小时内）
GET /goods/1001
// 预期：点击量不变（防刷触发）

// 验证Redis中的防刷Key
redis-cli GET "recommendation:click_anti_spam:20240001:1001"
// 预期：存在，TTL = 3600秒
```

**Postman自动化测试**：
```javascript
// 第1次请求前记录点击量
var getViewCount = function(goodsId) {
    var url = pm.environment.get("baseUrl") + "/goods/" + goodsId;
    pm.sendRequest(url, function(err, response) {
        var viewCount = response.json().data.viewCount;
        pm.environment.set("viewCount_before", viewCount);
    });
};

// 执行请求后验证
pm.test("防刷机制生效", function() {
    var viewCountBefore = pm.environment.get("viewCount_before");
    var viewCountAfter = pm.response.json().data.viewCount;
    // 第2次应该不增加
    pm.expect(viewCountAfter).to.equal(viewCountBefore);
});
```

---

### Test Set 4: 用户偏好

#### 测试4.1：获取用户偏好分类

**请求**：
```http
GET {{baseUrl}}/goods/preference/categories
Authorization: Bearer {{token}}
```

**Postman Tests**：
```javascript
pm.test("偏好分类查询成功", function() {
    pm.expect(pm.response.code).to.equal(200);
});

pm.test("返回Top2分类", function() {
    var data = pm.response.json().data;
    pm.expect(data.top2Categories).to.be.an("array");
    pm.expect(data.top2Categories.length).to.be.lte(2);
});

pm.test("分类包含必要字段", function() {
    var categories = pm.response.json().data.top2Categories;
    if (categories.length > 0) {
        var cat = categories[0];
        pm.expect(cat).to.have.property("categoryId");
        pm.expect(cat).to.have.property("categoryName");
        pm.expect(cat).to.have.property("browseCount");
        pm.expect(cat).to.have.property("percentage");
    }
});
```

**预期结果**：
```json
{
  "code": 200,
  "msg": "获取偏好分类成功",
  "data": {
    "userId": "20240001",
    "top2Categories": [
      {
        "categoryId": 1,
        "categoryName": "电子产品",
        "browseCount": 42,
        "percentage": 66.67
      },
      {
        "categoryId": 2,
        "categoryName": "图书",
        "browseCount": 21,
        "percentage": 33.33
      }
    ]
  }
}
```

---

### Test Set 5: 缓存管理

#### 测试5.1：获取系统统计

**请求**：
```http
GET {{baseUrl}}/goods/statistics
```

**Postman Tests**：
```javascript
pm.test("系统统计获取成功", function() {
    pm.expect(pm.response.code).to.equal(200);
});

pm.test("统计信息完整", function() {
    var data = pm.response.json().data;
    pm.expect(data).to.have.property("hotCacheValid");
    pm.expect(data).to.have.property("hotCacheCount");
    pm.expect(data).to.have.property("lastCacheRefreshTime");
    pm.expect(data).to.have.property("status");
});

pm.test("缓存状态正常", function() {
    var data = pm.response.json().data;
    pm.expect(data.hotCacheValid).to.be.true;
    pm.expect(data.status).to.equal("normal");
});
```

**预期结果**：
```json
{
  "code": 200,
  "msg": "获取统计信息成功",
  "data": {
    "hotCacheValid": true,
    "hotCacheCount": 50,
    "lastCacheRefreshTime": "2026-04-04T13:35:00",
    "status": "normal",
    "message": "推荐系统正常运行"
  }
}
```

---

#### 测试5.2：手动刷新缓存

**请求**：
```http
POST {{baseUrl}}/goods/cache/refresh-hot
```

**Postman Tests**：
```javascript
pm.test("缓存刷新成功", function() {
    pm.expect(pm.response.code).to.equal(200);
});

pm.test("返回刷新时间戳", function() {
    var data = pm.response.json().data;
    pm.expect(data).to.have.property("refreshTime");
    pm.expect(data.refreshTime).to.match(/\d{4}-\d{2}-\d{2}/);
});

// 保存刷新时间，用于后续验证
var refreshTime = pm.response.json().data.refreshTime;
pm.environment.set("lastRefreshTime", refreshTime);
```

**预期结果**：
```json
{
  "code": 200,
  "msg": "热门缓存刷新成功",
  "data": {
    "refreshTime": "2026-04-04 13:36:00"
  }
}
```

---

### Test Set 6: 性能测试

#### 测试6.1：单个请求响应时间

**Postman Tests**：
```javascript
pm.test("响应时间 < 100ms（缓存命中）", function() {
    pm.expect(pm.response.responseTime).to.be.below(100);
});

pm.test("响应时间 < 200ms（实时计算）", function() {
    pm.expect(pm.response.responseTime).to.be.below(200);
});

// 记录响应时间
pm.environment.set("lastResponseTime", pm.response.responseTime);
console.log("响应时间: " + pm.response.responseTime + "ms");
```

**性能基准**：

| 接口 | 缓存场景 | 预期时间 |
|---|---|---|
| GET /goods?tab=recommend | 缓存命中 | < 100ms |
| GET /goods/{id} | 数据库查询 | < 150ms |
| GET /goods?tab=dormitory | 数据库查询 | < 200ms |
| GET /goods/statistics | 内存操作 | < 50ms |

---

## 测试流程

### 完整测试执行步骤

**步骤1：环境准备（5分钟）**
1. 启动应用 `mvn spring-boot:run`
2. 确认MySQL正在运行
3. 确认Redis正在运行
4. 导入测试数据 `mysql ... < 测试数据生成脚本.sql`

**步骤2：环境验证（3分钟）**
- 执行测试集合 "1. 环境检查"
- 所有3个测试用例通过

**步骤3：首页功能测试（5分钟）**
- 执行测试集合 "2. 首页功能"
- 验证5个tab都返回正确结果
- 检查响应格式

**步骤4：商品详情 + 埋点测试（8分钟）**
- 执行测试用例 3.1（获取详情）
- 执行测试用例 3.2（验证浏览记录）
- 执行测试用例 3.3（验证防刷机制）
- 执行测试用例 3.4（验证点击量）

**步骤5：用户偏好测试（3分钟）**
- 执行测试集合 "4. 用户偏好"
- 验证偏好分类准确性

**步骤6：缓存管理测试（3分钟）**
- 执行测试集合 "5. 缓存管理"
- 手动刷新缓存
- 验证缓存状态

**步骤7：性能测试（3分钟）**
- 执行测试集合 "6. 性能测试"
- 检查响应时间指标
- 验证缓存效果

---

## 断言验证

### 关键断言清单

#### 功能断言
- [ ] HTTP 状态码为 200
- [ ] 返回JSON格式正确
- [ ] data.total > 0
- [ ] data.pages 计算正确
- [ ] data.list 不为空
- [ ] 商品包含id/name/price等必要字段
- [ ] 发布者信息完整

#### 业务逻辑断言
- [ ] 推荐得分 >= 0
- [ ] 防刷Key存在于Redis
- [ ] 浏览记录保存到数据库
- [ ] 点击量更新正确
- [ ] 用户偏好Top2分类正确
- [ ] 缓存有效期为1小时

#### 性能断言
- [ ] 响应时间 < 300ms
- [ ] 缓存命中时 < 100ms
- [ ] 实时查询时 < 200ms

---

## 常见错误处理

### 错误1：401 Unauthorized

**症状**：
```json
{
  "code": 401,
  "msg": "用户未登录"
}
```

**原因**：Authorization header缺失或token无效

**解决方案**：
```javascript
// 在Postman中添加Authorization header
Headers: {
  "Authorization": "Bearer {{token}}"
}
```

---

### 错误2：400 tab参数非法

**症状**：
```json
{
  "code": 400,
  "msg": "tab参数非法"
}
```

**原因**：tab值不在枚举内

**解决方案**：
```
有效的tab值：
- recommend
- dormitory
- entertainment
- study
- pre-order
```

---

### 错误3：500 服务异常

**症状**：
```json
{
  "code": 500,
  "msg": "服务异常"
}
```

**排查步骤**：
1. 检查应用日志 `tail -100 logs/taotao.log`
2. 检查数据库连接 `mysql -u root -p taotao_db -e "SELECT 1"`
3. 检查Redis连接 `redis-cli PING`
4. 检查网络连通性 `curl http://localhost:8080/health`

---

### 错误4：防刷机制不生效

**症状**：连续两次请求都更新了点击量

**原因**：Redis未连接或防刷Key过期

**排查步骤**：
```bash
# 1. 检查Redis连接
redis-cli PING

# 2. 检查防刷Key
redis-cli GET "recommendation:click_anti_spam:userId:goodsId"

# 3. 检查TTL
redis-cli TTL "recommendation:click_anti_spam:userId:goodsId"
# 预期：3600（秒）
```

---

### 错误5：缓存过期导致查询变慢

**症状**：响应时间从100ms增加到300ms

**原因**：热门缓存过期，转为数据库查询

**解决方案**：
```http
POST /goods/cache/refresh-hot
```

手动刷新缓存，重新加载到Redis

---

## Postman 导出和共享

### 导出集合

1. 点击集合名称 → 三点菜单 → "Export"
2. 选择 Postman v2.1 格式
3. 保存为 `SCMU_taotao_推荐系统.postman_collection.json`

### 导出环境

1. 点击环境名称 → 三点菜单 → "Export"
2. 保存为 `Development.postman_environment.json`

### 导入到其他Postman

1. 点击 "Import" → "Upload Files"
2. 选择导出的JSON文件
3. 自动导入集合和环境

---

## 总结检查清单

完成测试后，请逐一检查：

- [ ] Test Set 1 全部通过（3/3）
- [ ] Test Set 2 全部通过（5/5）
- [ ] Test Set 3 全部通过（4/4）
- [ ] Test Set 4 全部通过（2/2）
- [ ] Test Set 5 全部通过（2/2）
- [ ] Test Set 6 全部通过（3/3）
- [ ] 所有响应时间在预期范围内
- [ ] 数据库中的浏览记录正确保存
- [ ] Redis中的防刷Key正确生成
- [ ] 缓存刷新正常工作

**测试完成后**：
✅ 推荐系统已验证可用  
✅ 所有接口工作正常  
✅ 性能指标符合预期  
✅ 防刷机制生效  
✅ 缓存策略有效  

🎉 **推荐系统可以上线！**


