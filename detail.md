
# my-extension 任务模块说明（说明 / 作用 / 使用方式 / 配置）

## 1. 总览

`org.example.myextension` 模块结构：

- `alarm`：统一告警（当前实现为飞书群机器人）
- `auth`：认证与权限（管理后台鉴权 + Open OAuth2）
- `config`：自动配置（日志 TraceId、文件日志等）
- `exception`：统一业务异常
- `extension`：基础扩展工具（MyBatis、Redisson、基础转换）
- `health`：应用启动后健康检查
- `limiter`：基于 Redisson 的注解限流
- `logger`：方法日志与开放接口日志
- `message`：消息分发与幂等处理
- `model`：通用 DTO/VO/分页参数
- `script`：在线 Groovy 脚本执行（可配置开关）
- `threadPool`：上下文感知线程池（MDC/Request/SecurityContext 透传）
- `utils`：Spring/事务/Trace/分页工具

---

## 2. 接入方式（建议）

### 2.1 启用扩展组件扫描

在业务系统启动类添加：

```java
@ImportAutoConfiguration(org.example.myextension.ExtensionAutoConfiguration.class)
```

### 2.2 启用日志自动配置（TraceId + 文件日志）

在业务系统启动类添加：

```java
@ImportAutoConfiguration(org.example.myextension.config.LoggingAutoConfiguration.class)
```

说明：
- `LoggingAutoConfiguration` 会尽量保证日志中包含 `TraceId`
- 会优先改造已有 `ConsoleAppender` 的 pattern，缺失时注入 `TraceId:%X{TraceId}`
- 可按配置启用文件滚动日志（异步）

---

## 2.3 Maven 工程治理 

### 说明
- 本项目已按“工程治理优先”补强 Maven 构建规范，目标是降低“本地能编译、CI 失败”风险。

### 作用
- 统一构建环境约束（Maven/JDK 最低版本）
- 统一测试执行入口（单测 / 集成测试）
- 提前暴露依赖冲突与重复依赖声明问题

### 如何使用
1. 本地快速编译：
```bash
mvn -DskipTests compile
```
2. 执行单元测试：
```bash
mvn test
```
3. 执行完整校验（包含 failsafe verify 阶段）：
```bash
mvn verify
```

### 如何配置
`pom.xml` 中已新增/完善：
- `maven-enforcer-plugin`：
  - `requireMavenVersion`：`[3.6.3,)`
  - `requireJavaVersion`：`[1.8,)`
  - `banDuplicatePomDependencyVersions`
  - `dependencyConvergence`
- `maven-surefire-plugin`：统一执行 `*Test.java`
- `maven-failsafe-plugin`：预留执行 `*IT.java`
- `spring-boot-starter-test`（test scope）：统一 JUnit5 测试栈

---

## 3. 模块明细

## 3.1 `config`（日志自动配置）

### 说明
- 核心类：`LoggingAutoConfiguration`、`TraceIdTurboFilter`
- 目标：日志链路统一携带 `TraceId`，并可落地到滚动文件

### 作用
- 在日志输出前自动补齐 MDC 中的 `TraceId`
- 在 root logger 缺失控制台 appender 时补充控制台输出
- 按配置追加异步滚动文件 appender

### 如何使用
1. 启动类上添加：
```java
@ImportAutoConfiguration(org.example.myextension.config.LoggingAutoConfiguration.class)
```
2. 正常打印日志即可（`TraceId` 自动出现）

### 如何配置
- `myextension.logging.files.url`：日志文件完整路径（当前实现使用该字段）
- 示例：`/Users/qy/tmp/log/application.log`

```yaml
myextension:
  logging:
    files:
      url: /Users/qy/tmp/log/application.log
```

---

## 3.2 `auth.open`（开放平台 OAuth2）

### 说明
- 核心类：
  - `OpenOAuth2ClientProperties`：读取 `extension.auth.open.*`
  - `OpenOAuth2ClientRegistry`：应用启动时注册客户端
  - `OpenOAuth2TemplateImpl`：对接 Sa-Token `SaOAuth2Template`
  - `OpenOauth2Controller`：发放 client token 接口
  - `OpenApiInterceptor`：开放接口 token 校验

### 作用
- 支持多租户/多项目客户端配置
- 支持客户端维度 token 策略（如 `clientTokenTimeout`）
- 在应用启动时加载并注册客户端到运行时注册中心，由 Sa-Token 获取使用

### 如何使用
1. 打开开关：`extension.auth.open.enable=true`
2. 配置 `extension.auth.open.clients.<项目名>.*` 比如extension.auth.open.clients.自定义项目名称.clientId
3. 调用 `/open/oauth2/clientToken` 获取 token
4. 访问开放接口时传 Header：`ClientToken` / `client_token` 等

### 如何配置（重点）
> 本次已支持：客户端级别自定义属性，并在启动时自动读取注册。

```yaml
extension:
  auth:
    open:
      enable: true
      clients:
        projectA:
          clientId: project-a-client
          clientSecret: ${PROJECT_A_SECRET}
          allowUrl: "*"
          contractScope: "*"
          clientTokenTimeout: 7200
          accessTokenTimeout: 7200
          refreshTokenTimeout: 2592000
          pastClientTokenTimeout: 120
          isNewRefresh: true
        projectB:
          clientId: project-b-client
          clientSecret: ${PROJECT_B_SECRET}
          clientTokenTimeout: 3600
```

字段说明：
- `clientId`：客户端唯一 ID（必填）
- `clientSecret`：客户端密钥（必填）
- `allowUrl`：允许回调地址，默认 `*`
- `contractScope`：授权范围，默认 `*`
- `clientTokenTimeout`：客户端 token 有效期（秒）
- `accessTokenTimeout`：access_token 有效期（秒）
- `refreshTokenTimeout`：refresh_token 有效期（秒）
- `pastClientTokenTimeout`：历史 token 保留窗口（秒）
- `isNewRefresh`：是否每次刷新后签发新 refresh_token

启动注册行为：
- 启动时遍历 `clients` 配置并进行校验
- 校验 `clientId/clientSecret` 非空、`clientId` 不重复
- 校验超时参数（`accessTokenTimeout/refreshTokenTimeout/clientTokenTimeout/pastClientTokenTimeout`）若配置则必须 `> 0`
- 注册后由 `OpenOAuth2TemplateImpl#getClientModel` 直接提供给 Sa-Token

---

## 3.3 `auth.manage`（管理后台权限）

### 说明
- 核心：`@ManageAuthPermission` + `ManageAuthPermissionAspect` + `ManageAuthIsvTokenUtil`

### 作用
- 基于 Cookie（`isv-token`、`kdt_id`、`mobile`）做管理端权限校验
- 支持按店铺/角色控制接口访问

### 如何使用
```java
@ManageAuthPermission(permissions = {ManagePermissionEnum.Admin}, shopMatch = true)
public void doSomething() {}
```

### 如何配置
```yaml
extension:
  auth:
    manage:
      enable: true
      youzanIsvTokenResolverUrl: https://xxx.example.com
```

---

## 3.4 `alarm`（飞书告警）

### 说明
- 核心：`AlarmOperations`、`FeiShuGroupRobotAlarmTemplate`

### 作用
- 支持文本、结构化、卡片消息发送
- 支持同步和异步告警

### 如何使用
```java
@Autowired
private AlarmOperations alarmOperations;

alarmOperations.sendMessage("告警内容");
```

### 如何配置
```yaml
extension:
  alarm:
    feishu:
      enable: true
      webhook: https://open.feishu.cn/open-apis/bot/v2/hook/xxx
```

---

## 3.5 `limiter`（接口限流）

### 说明
- 核心：`@RateLimit` + `RateLimitAspect`

### 作用
- 基于 Redisson `RRateLimiter` 实现分布式限流
- 支持 SpEL 动态 key

### 如何使用
```java
@RateLimit(
    key = "api:order:create:#userId",
    rate = 20,
    rateInterval = 1,
    rateIntervalUnit = RateIntervalUnit.SECONDS
)
public void create(Long userId) {}
```

### 如何配置
- 无独立配置项；依赖业务系统已有 Redis/Redisson 配置

---

## 3.6 `logger`（方法日志/开放接口日志）

### 说明
- 核心：`@MethodLogger`、`@OpenApiLog`

### 作用
- 自动记录方法入参/出参/耗时
- 自动记录开放接口调用信息和客户端 IP

### 如何使用
```java
@MethodLogger(title = "创建订单")
public Order create(OrderReq req) {}

@OpenApiLog(name = "获取Token", version = "1.0.0", url = "/open/oauth2/clientToken")
public Object clientToken(...) {}
```

### 如何配置
- 无独立配置项（日志输出受 logback 配置控制）

---

## 3.7 `message`（消息分发）

### 说明
- 核心：`MessageDispatcher` + `MessageDispatchHandler`

### 作用
- 分发消息到多个处理器
- 基于 Redis `setIfAbsent` 防重复消费（幂等）

### 如何使用
1. 实现处理器接口 `MessageDispatchHandler<T>`
2. 注入 `MessageDispatcher` 调用 `handle/handleOrderly`

### 如何配置
```yaml
extension:
  msg:
    dispatch:
      handlerLifeTime: 21600
```

---

## 3.8 `script`（在线脚本）

### 说明
- 核心：`GroovyScriptController`

### 作用
- 在线执行 Groovy 脚本（高风险能力）

### 如何使用
- 开关开启后访问：
  - `GET /script/groovy/index`
  - `POST /script/groovy/exec`
  - `POST /script/groovy/exec/file`

### 如何配置
```yaml
extension:
  script:
    enable: true
```

---

## 3.9 `health`（健康检查）

### 说明
- 核心：`HealthChecker`、`HealthCheckRunner`

### 作用
- 应用启动完成后自动执行健康检查并输出结果日志

### 如何使用
1. 实现 `HealthChecker`
2. 标记为 Spring Bean，启动后自动执行

### 如何配置
- 当前无独立配置项

---

## 3.10 `threadPool`（上下文线程池）

### 说明
- 核心：`ContextAwaredThreadPoolExecutor`、`ThreadPoolUtils`

### 作用
- 异步线程自动透传：
  - MDC（TraceId）
  - RequestAttributes
  - SecurityContext

### 如何使用
```java
ContextAwaredThreadPoolExecutor pool =
    ThreadPoolUtils.contextAwarePool(8, 16, 120, 100, "biz-worker");
pool.submit(() -> {
    // 可直接读取 TraceId / SecurityContext
});
```

### 如何配置
- 无固定配置项，按业务代码构建线程池参数

---

## 3.11 `extension` / `utils` / `model` / `exception`

### 说明与作用
- `extension.BaseExtensions`：对象转换、金额转换、PageInfo 转换、IP/cookie 工具
- `extension.MybatisExtensions`：Pair/Triple 复合列、渲染策略扩展
- `extension.RedissonExtensions`：分布式锁工具封装
- `utils.TraceUtil`：TraceId 读写与上下文执行
- `utils.TransactionUtil`：事务模板封装
- `utils.SpringContextUtil`：ApplicationContext 静态获取
- `model`：统一数据对象（分页参数、统一返回体等）
- `exception.BizException`：业务异常基础类

### 如何使用
- 按工具类静态方法直接调用
- `BizException` 在业务异常场景直接抛出

### 如何配置
- 依赖业务系统基础配置（数据库、Redis、Spring）

---

## 4. 配置清单（汇总）

```yaml
myextension:
  logging:
    files:
      url: /Users/qy/tmp/log/application.log

extension:
  auth:
    open:
      enable: true
      clients:
        customProject:
          clientId: your-client-id
          clientSecret: your-client-secret
          clientTokenTimeout: 7200
          allowUrl: "*"
          contractScope: "*"
    manage:
      enable: false
      youzanIsvTokenResolverUrl: ""
  alarm:
    feishu:
      enable: false
      webhook: ""
  msg:
    dispatch:
      handlerLifeTime: 21600
  script:
    enable: false
```

---

## 5. 重点结论（本次场景）

你提到的场景已落地支持：

1. 可配置客户端级属性：
   - `extension.auth.open.clients.<自定义项目名>.clientTokenTimeout`
   - `extension.auth.open.clients.<自定义项目名>.clientSecret`
   - `extension.auth.open.clients.<自定义项目名>.clientId`
   - 以及 `accessTokenTimeout/refreshTokenTimeout/pastClientTokenTimeout/isNewRefresh`
2. 应用启动时自动读取并注册到 `OpenOAuth2ClientRegistry`
3. Sa-Token 在鉴权与签发 token 时通过 `OpenOAuth2TemplateImpl` 获取已注册客户端模型

---

## 6. 测试与回归建议（新增）

### 已补齐
- 新增单测：`OpenOAuth2ClientRegistryTest`
- 覆盖场景：
  - 合法配置注册成功
  - 返回副本防止内部状态被篡改
  - `clientId` 重复启动失败
  - timeout 非法（<=0）启动失败
  - `clientSecret` 缺失启动失败

### 发布前建议
1. 执行 `mvn test`，确认治理规则与核心单测通过
2. 在预发环境用真实配置验证 `/open/oauth2/clientToken`
3. 检查日志中是否出现 `open oauth2 客户端注册汇总`，确认启动注册数量符合预期
