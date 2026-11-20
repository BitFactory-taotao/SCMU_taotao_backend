# SCMU_taotao 项目开发规范

本项目是校园二手交易平台后端，基础包为 `com.bit.scmu_taotao`。编译目标为 Java 17，使用 Spring Boot、MyBatis-Plus 和 Maven，具体依赖版本以 `pom.xml` 为准。

## 改动边界

- 分析、排查、审查和设计默认只读。获准修改后，只改任务相关文件，保留用户已有改动以及文件编码、换行、注释和日志。
- 保持现有分层、方法签名和调用方依赖的行为。接口契约、鉴权规则或数据库结构需要变更时，先向用户确认方案。
- 修改 `pom.xml`（包括依赖和版本）及数据库、Redis 连接配置须得到明确授权。避免无关重构和格式调整。
- 日常改动避开 `.idea/`、`target/`、`.gitignore`、`.gitattributes` 和根目录说明文档。删除文件、暂存、提交、推送、创建或切换分支、合并及部署均须明确授权。

## 代码定位

- 从 `ScmuTaotaoApplication`、相关 Controller 或配置类进入。顺着 Service、Mapper 和 DTO 查看实现，复杂查询同时核对 `src/main/resources/com/bit/scmu_taotao/mapper/` 下的 XML。
- CodeGraph MCP 可用且索引与目标分支匹配时，优先用它定位符号、调用关系和受影响代码。索引缺失、过期或工具不可用时，用 `rg` 和源码核对。初始化、重建索引和修改 MCP 配置须明确授权。
- 接口契约以 Controller、DTO 和现有测试为准。鉴权、配置生效和持久化行为结合拦截器、Spring 配置、Mapper SQL 和运行验证确认。

## Java 与持久化

- 包名小写，方法和变量使用小驼峰，数据库字段使用下划线命名。实体使用 Lombok `@Data`、`@TableName` 和 `@TableId`，`t_` 表的实体使用 `T` 前缀，自增主键使用 `IdType.AUTO`。请求和响应 DTO 按业务分包，使用 `DTO`、`Request` 或 `Response` 后缀。
- Controller 负责接收请求和参数校验，Service 承载业务逻辑，Mapper 负责数据访问。沿用当前模块的接口和实现方式，已有 Service 接口新增业务方法时先声明再实现。
- 依赖注入沿用字段 `@Autowired`，日志使用 `@Slf4j` 和 SLF4J。业务方法使用简短中文注释说明用途。
- 优先复用 MyBatis-Plus 的 `BaseMapper`、`IService`、`LambdaQueryWrapper` 和 `Page`。复杂 SQL 写在 Mapper XML 或注解中，参数使用绑定。对有 `is_delete` 字段的表沿用软删除，保留现有状态条件、事务边界和主键策略。
- 时间字段沿用同类字段的 MyBatis-Plus 自动填充和 Jackson 序列化约定。对外日期保持 `yyyy-MM-dd HH:mm:ss` 格式与已有时区设置，JSON 空值沿用全局 `non_null` 配置。
- 缓存使用 `RedisService`，对象存储使用 `ObjectStorageService`。JSON、HTTP、HTML 解析及通用操作分别复用 Jackson、Apache HttpClient、Jsoup 和 Hutool。

## 接口、安全与配置

- `@RestController` 沿用 `util.common.Result`，通过 `Result.ok()` 和 `Result.fail()` 返回结果，异常交由 `GlobalExceptionHandler` 处理。DTO 使用 Jakarta Bean Validation 校验。分页检查 `page` 和 `size`，新增接口按现有约定将 `size` 封顶为 50。
- HTTP 鉴权由 `LoginInterceptor` 处理，保留 `Authorization` 请求头对 Bearer Token 和纯 Token 的兼容。业务代码通过 `UserContext` 获取当前用户。保留公开路由、管理端权限边界、请求结束时的上下文清理和 WebSocket 握手鉴权。
- 测试 Token 签发入口仅在 `test` profile 启用。商品发布保留敏感词校验，响应体禁止暴露服务端异常堆栈。发现 SQL 注入、XSS 或密钥泄露时，立即暂停相关操作并告知用户。
- 配置放在 `application.yaml` 或 `application-test.yaml` 的对应配置块，沿用 `@ConfigurationProperties` 或 `@Value`。凭据从环境变量等外部配置注入，禁止硬编码。

## 构建与验证

- Unix 或 Git Bash 下用 `sh ./mvnw`。Windows PowerShell 下用本机 Maven `mvn`，仓库仅跟踪 Unix Wrapper。
- 根据改动范围执行最小必要检查。现有测试使用 JUnit 5，控制器集成测试沿用 `@SpringBootTest` 和 MockMvc。运行前核对目标 profile、MySQL 和 Redis 连接，涉及上传时也核对 S3 环境。
- 新建、修改测试或取消已有测试的注释须明确授权。汇报实际执行的检查、结果及未验证的边界。

```powershell
mvn compile
mvn test '-Dtest=GoodsControllerTest'
mvn test '-Dspring.profiles.active=test'
mvn package '-DskipTests'
mvn spring-boot:run
```
