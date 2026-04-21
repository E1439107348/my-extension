package org.example.myextension.auth.open;

import cn.dev33.satoken.oauth2.model.SaClientModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link OpenOAuth2ClientRegistry} 单元测试。
 * <p>
 * 设计说明：
 * <ul>
 *   <li>本测试不依赖 Spring 容器，直接实例化注册中心并调用 init()，用于快速验证核心治理逻辑。</li>
 *   <li>重点覆盖“启动期配置校验”与“模型注册正确性”，防止后续重构引发静默回归。</li>
 *   <li>覆盖字段：clientId/clientSecret、各类 timeout、重复 clientId、副本返回策略。</li>
 * </ul>
 */
class OpenOAuth2ClientRegistryTest {

    @Test
    @DisplayName("启动注册成功：应能读取客户端配置并输出可用的 SaClientModel")
    void shouldRegisterClientsSuccessfullyWhenConfigIsValid() {
        // 准备一个完整且合法的客户端配置
        OpenOAuth2ClientProperties properties = new OpenOAuth2ClientProperties();
        Map<String, OpenOAuth2AppConfig> clients = new HashMap<>();

        OpenOAuth2AppConfig appConfig = new OpenOAuth2AppConfig();
        appConfig.setClientId("project-a-client");
        appConfig.setClientSecret("secret-a");
        appConfig.setAllowUrl("https://example.com/callback");
        appConfig.setContractScope("read,write");
        appConfig.setClientTokenTimeout(3600L);
        appConfig.setAccessTokenTimeout(7200L);
        appConfig.setRefreshTokenTimeout(86400L);
        appConfig.setPastClientTokenTimeout(120L);
        appConfig.setIsNewRefresh(true);

        clients.put("projectA", appConfig);
        properties.setClients(clients);

        OpenOAuth2ClientRegistry registry = new OpenOAuth2ClientRegistry(properties);
        registry.init();

        // 断言：可以按 clientId 拿到注册模型，且关键字段映射正确
        SaClientModel model = registry.getClientModel("project-a-client");
        Assertions.assertNotNull(model);
        Assertions.assertEquals("project-a-client", model.getClientId());
        Assertions.assertEquals("secret-a", model.getClientSecret());
        Assertions.assertEquals("https://example.com/callback", model.getAllowUrl());
        Assertions.assertEquals("read,write", model.getContractScope());
        Assertions.assertEquals(3600L, model.getClientTokenTimeout());
        Assertions.assertEquals(7200L, model.getAccessTokenTimeout());
        Assertions.assertEquals(86400L, model.getRefreshTokenTimeout());
        Assertions.assertEquals(120L, model.getPastClientTokenTimeout());
        Assertions.assertEquals(Boolean.TRUE, model.getIsNewRefresh());
        Assertions.assertEquals(Boolean.FALSE, model.getIsAutoMode());
        Assertions.assertEquals(Boolean.TRUE, model.getIsClient());
    }

    @Test
    @DisplayName("注册中心应返回模型副本，避免调用方修改内部注册态")
    void shouldReturnCopyInsteadOfOriginalModel() {
        OpenOAuth2ClientProperties properties = new OpenOAuth2ClientProperties();
        Map<String, OpenOAuth2AppConfig> clients = new HashMap<>();

        OpenOAuth2AppConfig appConfig = new OpenOAuth2AppConfig();
        appConfig.setClientId("copy-client");
        appConfig.setClientSecret("before-change");
        clients.put("projectCopy", appConfig);
        properties.setClients(clients);

        OpenOAuth2ClientRegistry registry = new OpenOAuth2ClientRegistry(properties);
        registry.init();

        // 第一次读取后故意修改返回对象，模拟外部错误写入
        SaClientModel firstRead = registry.getClientModel("copy-client");
        Assertions.assertNotNull(firstRead);
        firstRead.setClientSecret("after-change");

        // 第二次读取应保持原值，证明注册中心做了“副本返回”
        SaClientModel secondRead = registry.getClientModel("copy-client");
        Assertions.assertNotNull(secondRead);
        Assertions.assertEquals("before-change", secondRead.getClientSecret());
    }

    @Test
    @DisplayName("启动期校验：clientId 重复时应立即抛错")
    void shouldThrowWhenClientIdIsDuplicated() {
        OpenOAuth2ClientProperties properties = new OpenOAuth2ClientProperties();
        Map<String, OpenOAuth2AppConfig> clients = new HashMap<>();

        OpenOAuth2AppConfig appConfig1 = new OpenOAuth2AppConfig();
        appConfig1.setClientId("dup-client");
        appConfig1.setClientSecret("secret-1");

        OpenOAuth2AppConfig appConfig2 = new OpenOAuth2AppConfig();
        appConfig2.setClientId("dup-client");
        appConfig2.setClientSecret("secret-2");

        clients.put("project1", appConfig1);
        clients.put("project2", appConfig2);
        properties.setClients(clients);

        OpenOAuth2ClientRegistry registry = new OpenOAuth2ClientRegistry(properties);
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, registry::init);
        Assertions.assertTrue(ex.getMessage().contains("clientId 重复"));
    }

    @Test
    @DisplayName("启动期校验：timeout 小于等于 0 时应立即抛错")
    void shouldThrowWhenTimeoutIsInvalid() {
        OpenOAuth2ClientProperties properties = new OpenOAuth2ClientProperties();
        Map<String, OpenOAuth2AppConfig> clients = new HashMap<>();

        OpenOAuth2AppConfig appConfig = new OpenOAuth2AppConfig();
        appConfig.setClientId("invalid-timeout-client");
        appConfig.setClientSecret("secret-timeout");
        appConfig.setClientTokenTimeout(0L);

        clients.put("projectTimeout", appConfig);
        properties.setClients(clients);

        OpenOAuth2ClientRegistry registry = new OpenOAuth2ClientRegistry(properties);
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, registry::init);
        Assertions.assertTrue(ex.getMessage().contains("clientTokenTimeout"));
    }

    @Test
    @DisplayName("启动期校验：缺少 clientSecret 时应立即抛错")
    void shouldThrowWhenClientSecretIsMissing() {
        OpenOAuth2ClientProperties properties = new OpenOAuth2ClientProperties();
        Map<String, OpenOAuth2AppConfig> clients = new HashMap<>();

        OpenOAuth2AppConfig appConfig = new OpenOAuth2AppConfig();
        appConfig.setClientId("no-secret-client");
        appConfig.setClientSecret("");
        clients.put("projectNoSecret", appConfig);
        properties.setClients(clients);

        OpenOAuth2ClientRegistry registry = new OpenOAuth2ClientRegistry(properties);
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, registry::init);
        Assertions.assertTrue(ex.getMessage().contains("缺少 clientSecret"));
    }
}
