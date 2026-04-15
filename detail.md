项目详细说明（detail.md）

目的
----
本文件为项目的详细说明文档：
1. 检测作为 Maven 项目可能存在的问题与建议；
2. 逐个说明 extension 与 utils 包中核心类的作用与使用要点；
3. 列出项目中 @Value 注入的配置项并给出说明与示例；
4. 重点说明 OAuth2（基于 Sa-Token）的使用方法与示例；
5. 提供构建与运行建议及调试步骤。

一、Maven 配置检查（摘要）
----------------------------
- parent: spring-boot-starter-parent 版本为 2.5.5，建议插件/依赖与该版本保持一致。
- spring-boot-maven-plugin 显式写为 2.6.13，版本与父 POM 不一致。建议：删除 <version>（让父 POM 管理）或改为与父对齐。
- <skip>true</skip> 配置会跳过 repackage（默认会生成可执行 fat-jar）。如需可执行 jar，请将其移除或设为 false。
- 建议加入 Maven Wrapper（mvnw）到仓库，便于统一构建环境。
- 当前环境未安装 mvn（mvn: command not found），请在本地或 CI 环境执行 mvn -DskipTests package 进行验证。

二、extension 包详解
---------------------
1) BaseExtensions.java
- 作用：提供通用工具方法集合。
  - jsonMapper(): 返回已配置的 ObjectMapper（忽略空属性、时区 GMT+8、忽略未知属性）。
  - logger(Class): 获取 slf4j Logger 实例。
  - then/otherwise：布尔链式执行工具，便于链式判断。
  - paramHex(...): 将参数序列化为 JSON 后计算 MD5，用于签名或缓存 key。
  - selfBean/getBean：通过 SpringContextUtil 获取 Spring Bean（注意：存在全局状态风险）。
  - getCookieKdtId/getCookieKdtIdNullable：从 HttpServletRequest 的 Cookie 中读取 kdt_id。
  - mapType/mapTypeList：对象与集合拷贝（BeanUtils）。
  - fen2Yuan/yuan2fen：金额单位转换工具。
  - convert(PageInfo)：PageHelper 的 PageInfo 类型转换工具。

2) MybatisExtensions.java
- 作用：为 MyBatis Dynamic SQL 扩展功能。
  - 提供 PairColumn/TripleColumn：实现 BindableColumn，允许把多列组合为一个可绑定列，用于复杂 SQL 场景（如复合列比较）。
  - autoConvertParameterType：根据 SqlColumn 的 JDBCType 对参数进行格式化（如字符串加单引号、日期格式化）。
  - OriginalRenderingStrategy：自定义 RenderingStrategy，生成项目约定的占位符格式。

3) RedissonExtensions.java
- 作用：提供常用的 Redisson 分布式锁封装。
  - 多种重载支持：立即尝试加锁（静默失败）、带等待时间/租赁时间、加锁失败抛自定义异常、带返回值版本等。
  - 所有释放操作使用 forceUnlock()（注意：forceUnlock 会无条件释放，使用时需确保业务语义允许）。

三、utils 包详解
------------------
1) SpringContextUtil.java
- 作用：实现 ApplicationContextAware 并保存静态 ApplicationContext 引用，便于在静态上下文获取 Bean。
- 风险：静态持有 ApplicationContext 会引入全局状态，影响测试隔离与类加载，需谨慎使用。

2) TransactionUtil.java
- 作用：静态事务执行工具。
  - 保存注入的 TransactionTemplate 的实例，提供 transactionOperation、transactionAfterCommit、transactionActiveAfterCommit 等方法。
  - transactionActiveAfterCommit 在没有活跃事务时直接执行回调，兼容无事务场景。

3) TraceUtil.java
- 作用：对 SLF4J MDC 中的 TraceId 进行读写与临时替换。
  - getTraceId/setTraceId：安全处理 null（移除 MDC 键）。
  - runWithTrace(traceId, block)：在指定 TraceId 环境中运行代码并恢复原 TraceId，适用于线程池/异步场景。

4) PageUtil.java
- 作用：封装基于 PageHelper 的分页循环与批量更新/删除逻辑。
  - pageConsume/iterablePageConsume：按页获取并消费数据。
  - nextDelete/nextUpdate：循环执行删除/更新直到无剩余行。
  - 支持自定义分页查询的回调版本（返回 total + list）。

四、项目中 @Value 配置项（扫描结果与说明）
-------------------------------------------
以下为源码扫描到的 @Value 配置项：

- extension.alarm.feishu.webhook
  - 说明：飞书机器人 webhook 地址，字符串，若为空则不发送告警。
  - 示例：extension.alarm.feishu.webhook: "https://open.feishu.cn/..."

- extension.alarm.feishu.enable
  - 说明：是否启用飞书告警（boolean），默认 false。

- extension.msg.dispatch.handlerLifeTime
  - 说明：消息分发幂等 key 的过期时间（秒），默认 21600（6小时）。
  - 使用处：MessageDispatcher 中用来设置 Redis 键的过期时间，防止重复消费。

- extension.auth.manage.youzanIsvTokenResolverUrl
  - 说明：ISV Token 解析服务地址（外部服务），用于管理后台的 token 校验与用户信息解析。
  - 建议：设置为可信的解析服务 URL，例如 https://token-resolver.example.com

- extension.auth.manage.enable
  - 说明：管理后台认证开关（boolean），默认 false。
  - 使用处：ManageAuthPermissionAspect 根据此配置决定是否进行真认证或使用模拟用户。

示例 application.yml 段落：

extension:
  alarm:
    feishu:
      enable: true
      webhook: "https://open.feishu.cn/xxx"
  msg:
    dispatch:
      handlerLifeTime: 21600
  auth:
    manage:
      enable: true
      youzanIsvTokenResolverUrl: "https://token-resolver.example.com"

五、OAuth2（Sa-Token）核心使用说明（详细）
-------------------------------------------
项目依赖：sa-token-spring-boot-starter、sa-token-oauth2、sa-token-redis-jackson（用于 Redis 存储）。

1) 功能位置与接口
- OpenOauth2Controller.clientToken 提供客户端凭证（Client Credentials）获取 ClientToken 的接口，路径：/open/oauth2/clientToken。
- 调用参数：grantType（必须为 client_credentials）、clientId、clientSecret、scope（可选）。
- 返回：ApiResult 包装的 ClientToken 信息（controller 将 SaToken 返回结构转换为驼峰命名的 map）。

2) Sa-Token 配置要点（在 application.yml / application.properties 或环境变量设置）
- 在 application.yml 中配置 sa-token 的 redis 存储或默认内存存储：

# 示例（Redis 存储）
sa-token:
  token-name: satoken
  timeout: 7200
  is-share: false
  # 若使用 redis，可配置存储方式，或引入 sa-token-redis-jackson

- OAuth2 客户端注册：需要在 Sa-Token 的配置或初始化代码中注册客户端（clientId / clientSecret / scope / expires 等）。具体示例见 Sa-Token 官方文档。

3) 环境变量命名规范与生效规则（关键）
- 基本规则：Spring Boot 使用 relaxed binding，将属性名转换为大写并用下划线连接。点（.）、短横（-）和驼峰均会被容错映射。
- 转换步骤示例：
  1. 原始属性：extension.msg.dispatch.handlerLifeTime
  2. 转为 kebab-case（推荐读法）：extension.msg.dispatch.handler-life-time
  3. 环境变量形式：EXTENSION_MSG_DISPATCH_HANDLER_LIFE_TIME
- 其他示例：
  - sa-token.token-name -> SA_TOKEN_TOKEN_NAME
  - extension.auth.manage.enable -> EXTENSION_AUTH_MANAGE_ENABLE
  - extension.auth.manage.youzanIsvTokenResolverUrl -> EXTENSION_AUTH_MANAGE_YOUZAN_ISV_TOKEN_RESOLVER_URL
- 布尔值与数值：环境变量值以字符串形式提供，Spring 会根据目标类型转换。布尔可用 true/false/on/off/1/0，数值直接写数字字符串。
- 生效优先级（常用简化版）：命令行参数（-D 或 --） > SPRING_APPLICATION_JSON > 环境变量 > application.properties / application.yml（打包内）> 默认值。

4) 设置示例（Shell / Docker / Java）
- Shell（Unix）导出环境变量：
  export EXTENSION_AUTH_MANAGE_ENABLE=true
  export EXTENSION_AUTH_MANAGE_YOUZAN_ISV_TOKEN_RESOLVER_URL="https://token-resolver.example.com"
  export EXTENSION_MSG_DISPATCH_HANDLER_LIFE_TIME=21600
  export SA_TOKEN_TOKEN_NAME=satoken

- 通过 java 命令行传参（命令优先级更高）：
  java -jar app.jar --extension.auth.manage.enable=true --sa-token.token-name=satoken
  或使用系统属性：java -Dextension.auth.manage.enable=true -jar app.jar

- Docker run：
  docker run -e EXTENSION_AUTH_MANAGE_ENABLE=true -e SA_TOKEN_TOKEN_NAME=satoken myimage

- Kubernetes Deployment（片段）：
  env:
    - name: EXTENSION_AUTH_MANAGE_ENABLE
      value: "true"
    - name: EXTENSION_AUTH_MANAGE_YOUZAN_ISV_TOKEN_RESOLVER_URL
      valueFrom:
        secretKeyRef:
          name: token-resolver-secret
          key: resolver_url

5) Sa-Token OAuth2 使用示例（完整流程）
- 注册客户端（初始化代码示例，伪代码）：
  SaOAuth2Client client = new SaOAuth2Client();
  client.setClientId("myapp");
  client.setClientSecret("secret123");
  client.setScope("read,write");
  client.setExpires(3600);
  SaOAuth2Util.registerClient(client);

- 发起获取 token（客户端请求）：
  GET /open/oauth2/clientToken?grantType=client_credentials&clientId=myapp&clientSecret=secret123
  或者 POST 表单方式提交 client_id/client_secret

- 使用 token：
  - 将获得的 access_token 放在请求头（例如 Authorization: Bearer <token>）或项目约定的位置，后端通过 Sa-Token 的拦截器/过滤器进行校验。

6) 安全与运维建议
- 不要在镜像或代码仓库中明文保存 clientSecret 或其他密钥；优先使用环境变量、Kubernetes Secret、Vault 等安全存储。
- 在集群中使用 secret 管理并挂载为环境变量或文件，避免直接在 Deployment YAML 中明文写入。
- 若使用 Redis 存储 token，确保 Redis 连接安全（鉴权、私有网络）。

7) 排查提示（补充）
- 若 env 设置后未生效：检查容器/进程是否重启；确认环境变量名称与属性映射规则一致（使用大写与下划线）。
- 若类型转换失败（例如数字或布尔）：检查值是否有效字符串形式。
- 若仍然无法生效：在启动时加入 --debug 可查看 Spring Environment 的 property sources 与最终解析的属性值。

9) 具体示例：clients.* 映射规则与代码处理（以 pousheng 为例）

- 场景说明：希望在配置中添加客户端级别的自定义属性，例如
  extension.auth.open.clients.自定义项目名称.clientTokenTimeout、
  extension.auth.open.clients.自定义项目名称.clientSecret、
  extension.auth.open.clients.自定义项目名称.clientId 等，并让应用在启动时自动读取并注册到 Sa-Token。

- 完整 YAML 示例（可复制到 application.yml）：

extension:
  auth:
    open:
      enable: true
      clients:
        pousheng:
          clientId: "pousheng_id"
          clientSecret: "secret123"
          clientTokenTimeout: 3600
          allowUrl: "https://pousheng.example/callback"
          contractScope: "read,write"

- 等效环境变量（示例，Unix shell）：

export EXTENSION_AUTH_OPEN_ENABLE=true
export EXTENSION_AUTH_OPEN_CLIENTS_POUSHENG_CLIENTID=pousheng_id
export EXTENSION_AUTH_OPEN_CLIENTS_POUSHENG_CLIENTSECRET=secret123
export EXTENSION_AUTH_OPEN_CLIENTS_POUSHENG_CLIENT_TOKEN_TIMEOUT=3600

说明映射规则（严格版）：
1. Spring Boot 的 relaxed binding 会将配置键做以下规范化：点（.）分隔层级，属性名中的驼峰会被转换为 kebab-case（clientTokenTimeout -> client-token-timeout），最终环境变量形式为大写并用下划线替换分隔符和连字符。
   - clientTokenTimeout -> client-token-timeout -> CLIENT_TOKEN_TIMEOUT
   - 因此最终环境变量为：EXTENSION_AUTH_OPEN_CLIENTS_POUSHENG_CLIENT_TOKEN_TIMEOUT
2. 对于 Map 结构（ConfigurationProperties 声明 Map<String, OpenOAuth2AppConfig> clients），Binder 会将 clients 下的每个子节点名（如 pousheng）作为 Map 的 key，并把子节点内容绑定为 OpenOAuth2AppConfig 实例。
   - 属性路径 extension.auth.open.clients.自定义项目名称.clientId 会被绑定到 clients.get("自定义项目名称").getClientId()
3. 只有在 OpenOAuth2AppConfig 中声明了相应的字段（例如 clientTokenTimeout）并提供 getter/setter（或使用 Lombok @Data）时，Spring 才能把配置值注入到对象上。
4. ConditionalOnProperty(name = "extension.auth.open.enable", havingValue = "true") 会保证配置类与相关 Bean 仅在 enable 为 true 时才加载。

- Java 代码示例（生产可用的注册器，实现属性读取并注册 SaOAuth2Client）：

package org.example.myextension.auth.open;

import cn.dev33.satoken.oauth2.model.SaOAuth2Client;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpenOAuth2ClientRegistrar implements InitializingBean {

    @Autowired
    private OpenOAuth2ClientProperties clientProperties;

    @Override
    public void afterPropertiesSet() {
        clientProperties.getClients().forEach((name, cfg) -> {
            SaOAuth2Client client = new SaOAuth2Client();
            // 优先使用配置内的 clientId，若未配置则回退到 map key
            String clientId = cfg.getClientId() != null ? cfg.getClientId() : name;
            client.setClientId(clientId);
            client.setClientSecret(cfg.getClientSecret());
            client.setScope(cfg.getContractScope());
            // 自定义过期时间（秒）
            if (cfg instanceof OpenOAuth2AppConfig) {
                try {
                    // 允许 OpenOAuth2AppConfig 新增字段 clientTokenTimeout (Integer)
                    Integer timeout = ((OpenOAuth2AppConfig) cfg).getClientTokenTimeout();
                    client.setExpires(timeout != null ? timeout : 3600);
                } catch (Exception e) {
                    client.setExpires(3600);
                }
            } else {
                client.setExpires(3600);
            }
            SaOAuth2Util.registerClient(client);
        });
    }
}

- 如何让自定义属性生效（checklist）：
  1. 在 OpenOAuth2AppConfig 中声明对应字段（例如 private Integer clientTokenTimeout;）并生成 getter/setter（Lombok @Data 可自动生成）。
  2. 在 application.yml 或通过环境变量/命令行设置对应键值（遵循 relaxed binding）。
  3. 确认 extension.auth.open.enable=true，以便配置类与 registrar 被加载。
  4. 重启应用（或使用动态配置刷新机制）以使新配置生效。

参考：
- Sa-Token 官方文档（客户端注册与 OAuth2 部分）
- Spring Boot 属性绑定（relaxed binding 与 Environment 优先级）

 
