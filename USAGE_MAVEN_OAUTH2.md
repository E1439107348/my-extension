项目检查与使用说明（Maven / 配置 / OAuth2）

1. 概览

该文档检查当前项目作为 Maven 工程的关键点，列出需要注意的 Maven 配置、项目中使用的 @Value 配置项说明，说明 extension 与 utils 包中核心类的作用，并给出 OAuth2（基于 Sa-Token）的使用说明与示例。

2. Maven 配置检测要点（基于 pom.xml）

- parent 版本：spring-boot-starter-parent 采用 2.5.5。
  - 推荐：插件版本与 Spring Boot 父版本保持一致或使用父 POM 继承的默认值，避免不一致带来的插件/依赖管理差异。

- spring-boot-maven-plugin 版本：显式写为 2.6.13（与父 POM 的 2.5.5 不一致）。
  - 建议：将该插件版本移除（使用父 POM 管理）或改为与父版本对齐（2.5.5），否则可能出现构建/打包行为差异。

- spring-boot-maven-plugin 配置中 <skip>true</skip>：
  - 当前配置会跳过 Spring Boot 的 repackage 行为，最终不会生成可执行 fat-jar（java -jar 可直接运行）。如果期望打出可运行的包，请删除或将其设为 false。

- Java 编译目标：source/target 设置为 1.8。
  - 确保本地 JDK >= 1.8。若希望使用更高 JDK（例如 11/17），请根据需要调整 source/target 与兼容性。

- 依赖版本管理：多数 Spring 相关依赖未显式写版本，依赖于父 POM 管理，这是正常做法。但仍需注意自定义 properties（如 fastjson.version、redisson.version）是否与 Spring Boot 版本兼容。

- 建议增加：Maven Wrapper（mvnw），便于在 CI 或开发者环境中统一 Maven 版本。

- 本环境编译测试：在当前容器中未安装 mvn（mvn: command not found），无法在该环境直接执行 mvn package。请在本地环境或 CI 上运行：
  - mvn -DskipTests package
  - 若需要可执行 jar：删除 spring-boot-maven-plugin 中的 <skip>true</skip> 后执行 mvn package

3. 代码中 @Value 配置项说明与示例

通过代码扫描，以下配置项存在于项目中：

- extension.alarm.feishu.webhook (FeiShu 告警 webhook URI)
- extension.alarm.feishu.enable (FeiShu 告警开关，布尔，默认 false)
- extension.msg.dispatch.handlerLifeTime (消息分发幂等 key 存活时间，单位：秒，默认 21600)
- extension.auth.manage.youzanIsvTokenResolverUrl (ISV Token 解析服务地址)
- extension.auth.manage.enable (管理后台认证开关，布尔，默认 false)

示例 application.yml（推荐写法）：

spring:
  application:
    name: my-extension

extension:
  alarm:
    feishu:
      enable: true
      webhook: "https://open.feishu.cn/xxx"
  msg:
    dispatch:
      handlerLifeTime: 21600    # 秒
  auth:
    manage:
      enable: true
      youzanIsvTokenResolverUrl: "https://token-resolver.example.com"

说明：@Value 支持默认值（例如 :21600），当配置缺失时会使用默认值或空字符串（代码中应做好空值校验）。

4. extension 与 utils 包核心文件作用说明（简要）

- src/main/java/org/example/myextension/extension/
  - BaseExtensions.java：通用工具集（JSON mapper、日志获取、对象映射、金额转换、PageInfo 转换等）。
  - MybatisExtensions.java：MyBatis 扩展（提供 PairColumn/TripleColumn 可绑定列、自定义 RenderingStrategy、参数类型自动转换），用于动态 SQL 中的复合列场景。
  - RedissonExtensions.java：基于 Redisson 提供的分布式锁工具方法集合（多种重载，支持返回值/抛异常/超时等场景）。

- src/main/java/org/example/myextension/utils/
  - SpringContextUtil.java：保存 Spring ApplicationContext 的静态工具，便于静态上下文获取 Bean（注意：引入全局状态）。
  - TransactionUtil.java：静态事务工具（使用注入的 TransactionTemplate，在静态上下文执行事务或注册提交后回调）。
  - TraceUtil.java：MDC TraceId 管理工具，用于跨线程/异步保留 TraceId。
  - PageUtil.java：封装基于 PageHelper 的分页循环、批量删除/更新等工具。

- 其它相关类（示例）：
  - OpenOauth2Controller.java：开放 OAuth2 接口（clientToken），基于 Sa-Token-OAuth2 实现客户端凭证授权。
  - ManageAuthIsvTokenUtil.java & ManageAuthPermissionAspect.java：用于管理后台的 ISV Token 校验与权限注入（调用外部 Token 解析服务）。

5. OAuth2（Sa-Token）核心使用说明（针对 OpenOauth2Controller）

项目内集成了 sa-token-oauth2（见 pom.xml），OpenOauth2Controller 暴露了 /open/oauth2/clientToken 接口（注解：@OpenApiLog），用于 Client Credentials 流程。

- 接口路径：/open/oauth2/clientToken
- 请求方法：GET 或 POST
- 必填参数：grantType（必须为 client_credentials）、clientId、clientSecret
- 可选参数：scope

示例请求：

GET /open/oauth2/clientToken?grantType=client_credentials&clientId=myapp&clientSecret=secret123

响应：统一封装为 ApiResult（项目内部类型），成功时 data 字段包含生成的 ClientToken 信息（字段已做下划线到驼峰的转换）。

典型使用流程：
1. 在 Sa-Token 配置中注册 OAuth2 客户端信息（clientId/clientSecret、scope 等）。
2. 客户端调用 /open/oauth2/clientToken 获取 client-token（access token）。
3. 客户端后续调用开放 API 时需在请求中携带该 token（具体 header 名称取决于上层调用约定，项目中客户端调用拦截器可能会查看 ClientToken header）。

注意事项与建议：
- Sa-Token 的具体配置（如 ClientDetails 注册、token 存储策略等）需在项目配置中补充。pom 已包含 sa-token-spring-boot-starter 与 sa-token-oauth2 依赖。
- OpenOauth2Controller 内部会捕获 SaTokenException 并返回对应错误码与消息；其他异常会记录日志并返回通用错误消息。
- 若需对 token 颁发策略（有效期、刷新、签名等）做定制，请参考 sa-token 官方文档并在 application.yml 中配置对应属性。

6. 检查建议与下一步验证

- 在本地或 CI 运行编译：
  - mvn -DskipTests package
  - 若希望生成可执行 jar：移除 spring-boot-maven-plugin 中的 <skip>true</skip> 配置
- 对于插件版本不一致问题：对齐 spring-boot-maven-plugin 版本或删除显式版本由父 POM 管理
- 建议提交一个 Maven Wrapper（mvnw）到仓库，方便在无全局 mvn 的环境中构建
- 在构建/运行前，补充以下必要配置（application.yml）：
  - extension.auth.manage.youzanIsvTokenResolverUrl
  - extension.auth.manage.enable
  - extension.msg.dispatch.handlerLifeTime
  - FeiShu 告警相关（若使用）

7. 附：快速调试与排查步骤

- 若本地 mvn 报错依赖下载失败：检查网络、私服设置或 pom 中的仓库配置
- 若运行时报错 ClassNotFound：确认依赖版本兼容性，检查是否需要添加额外 spring-boot-starter 或 starter 依赖
- 若 Sa-Token OAuth2 无法颁发 token：检查 Sa-Token 的 OAuth2 配置和客户端注册

---

如需，我可以：
- 在本地构建日志（你粘贴构建输出）基础上逐条定位并修复问题；
- 生成更详尽的配置示例（application.yml）并在项目中添加样例配置文件；
- 将本 MD 拆分为更详细的 README 章节或 wiki 页面。