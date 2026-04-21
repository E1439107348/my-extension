package org.example.myextension.auth.open;

import cn.dev33.satoken.oauth2.model.SaClientModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Open OAuth2 客户端注册中心。
 * <p>
 * 在应用启动时读取 {@code extension.auth.open.clients.*} 配置，构建并缓存 Sa-Token 所需的客户端模型。
 */
@Component
@ConditionalOnProperty(name = "extension.auth.open.enable", havingValue = "true")
public class OpenOAuth2ClientRegistry {

    private static final Logger log = LoggerFactory.getLogger(OpenOAuth2ClientRegistry.class);

    private final OpenOAuth2ClientProperties clientProperties;
    private final Map<String, SaClientModel> clientModelRegistry = new ConcurrentHashMap<>();

    public OpenOAuth2ClientRegistry(OpenOAuth2ClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    @PostConstruct
    public void init() {
        Map<String, OpenOAuth2AppConfig> clients = clientProperties.getClients();
        if (clients == null || clients.isEmpty()) {
            log.warn("open oauth2: 未读取到任何客户端配置，请检查 extension.auth.open.clients");
            return;
        }

        Set<String> clientIdSet = new HashSet<>();
        clients.forEach((projectName, appConfig) -> {
            if (appConfig == null) {
                throw new IllegalStateException("open oauth2 客户端配置为空, projectName=" + projectName);
            }
            if (!StringUtils.hasText(appConfig.getClientId())) {
                throw new IllegalStateException("open oauth2 缺少 clientId, projectName=" + projectName);
            }
            if (!StringUtils.hasText(appConfig.getClientSecret())) {
                throw new IllegalStateException("open oauth2 缺少 clientSecret, projectName=" + projectName);
            }
            if (!clientIdSet.add(appConfig.getClientId())) {
                throw new IllegalStateException("open oauth2 clientId 重复: " + appConfig.getClientId());
            }

            SaClientModel model = toClientModel(appConfig);
            clientModelRegistry.put(model.getClientId(), model);
            log.info("open oauth2 客户端注册完成, projectName={}, clientId={}", projectName, model.getClientId());
        });
    }

    public SaClientModel getClientModel(String clientId) {
        SaClientModel model = clientModelRegistry.get(clientId);
        if (model == null) {
            return null;
        }
        return copy(model);
    }

    private SaClientModel toClientModel(OpenOAuth2AppConfig appConfig) {
        SaClientModel model = new SaClientModel()
                .setClientId(appConfig.getClientId())
                .setClientSecret(appConfig.getClientSecret())
                .setAllowUrl(StringUtils.hasText(appConfig.getAllowUrl()) ? appConfig.getAllowUrl() : "*")
                .setContractScope(StringUtils.hasText(appConfig.getContractScope()) ? appConfig.getContractScope() : "*")
                .setIsAutoMode(false)
                .setIsClient(true);

        if (appConfig.getAccessTokenTimeout() != null) {
            model.setAccessTokenTimeout(appConfig.getAccessTokenTimeout());
        }
        if (appConfig.getRefreshTokenTimeout() != null) {
            model.setRefreshTokenTimeout(appConfig.getRefreshTokenTimeout());
        }
        if (appConfig.getClientTokenTimeout() != null) {
            model.setClientTokenTimeout(appConfig.getClientTokenTimeout());
        }
        if (appConfig.getPastClientTokenTimeout() != null) {
            model.setPastClientTokenTimeout(appConfig.getPastClientTokenTimeout());
        }
        if (appConfig.getIsNewRefresh() != null) {
            model.setIsNewRefresh(appConfig.getIsNewRefresh());
        }
        return model;
    }

    private SaClientModel copy(SaClientModel source) {
        return new SaClientModel()
                .setClientId(source.getClientId())
                .setClientSecret(source.getClientSecret())
                .setAllowUrl(source.getAllowUrl())
                .setContractScope(source.getContractScope())
                .setIsCode(source.getIsCode())
                .setIsImplicit(source.getIsImplicit())
                .setIsPassword(source.getIsPassword())
                .setIsClient(source.getIsClient())
                .setIsAutoMode(source.getIsAutoMode())
                .setIsNewRefresh(source.getIsNewRefresh())
                .setAccessTokenTimeout(source.getAccessTokenTimeout())
                .setRefreshTokenTimeout(source.getRefreshTokenTimeout())
                .setClientTokenTimeout(source.getClientTokenTimeout())
                .setPastClientTokenTimeout(source.getPastClientTokenTimeout());
    }
}
