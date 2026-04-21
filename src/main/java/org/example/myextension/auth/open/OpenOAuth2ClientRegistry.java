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
 * <p>
 * 设计目标：
 * <ul>
 *   <li>启动期“前置失败”：配置非法时在启动阶段直接失败，避免线上请求期才暴露问题。</li>
 *   <li>可诊断性：错误信息明确指出具体 projectName 与具体字段。</li>
 *   <li>线程安全读取：注册后提供只读查询能力，供 Sa-Token 模板高频读取。</li>
 * </ul>
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

        // 使用 Set 保障 clientId 全局唯一，防止两个项目配置复用了同一个 clientId
        Set<String> clientIdSet = new HashSet<>();
        clients.forEach((projectName, appConfig) -> {
            validateRequiredFields(projectName, appConfig);
            validateTimeoutFields(projectName, appConfig);
            if (!clientIdSet.add(appConfig.getClientId())) {
                throw new IllegalStateException("open oauth2 clientId 重复: " + appConfig.getClientId());
            }

            SaClientModel model = toClientModel(appConfig);
            clientModelRegistry.put(model.getClientId(), model);
            log.info("open oauth2 客户端注册完成, projectName={}, clientId={}", projectName, model.getClientId());
        });

        log.info("open oauth2 客户端注册汇总: total={}", clientModelRegistry.size());
    }

    public SaClientModel getClientModel(String clientId) {
        SaClientModel model = clientModelRegistry.get(clientId);
        if (model == null) {
            return null;
        }
        // 返回副本，避免调用方误修改注册中心中的原始模型
        return copy(model);
    }

    /**
     * 校验必填字段。
     *
     * @param projectName 客户端分组名（即 clients 下的 key）
     * @param appConfig   客户端配置对象
     */
    private void validateRequiredFields(String projectName, OpenOAuth2AppConfig appConfig) {
        if (appConfig == null) {
            throw new IllegalStateException("open oauth2 客户端配置为空, projectName=" + projectName);
        }
        if (!StringUtils.hasText(appConfig.getClientId())) {
            throw new IllegalStateException("open oauth2 缺少 clientId, projectName=" + projectName);
        }
        if (!StringUtils.hasText(appConfig.getClientSecret())) {
            throw new IllegalStateException("open oauth2 缺少 clientSecret, projectName=" + projectName);
        }
    }

    /**
     * 校验超时字段的合法性。
     * <p>
     * 约束策略：当字段被配置时，必须大于 0；未配置（null）表示走 Sa-Token 默认策略。
     *
     * @param projectName 客户端分组名
     * @param appConfig   客户端配置对象
     */
    private void validateTimeoutFields(String projectName, OpenOAuth2AppConfig appConfig) {
        validatePositiveIfPresent("accessTokenTimeout", appConfig.getAccessTokenTimeout(), projectName);
        validatePositiveIfPresent("refreshTokenTimeout", appConfig.getRefreshTokenTimeout(), projectName);
        validatePositiveIfPresent("clientTokenTimeout", appConfig.getClientTokenTimeout(), projectName);
        validatePositiveIfPresent("pastClientTokenTimeout", appConfig.getPastClientTokenTimeout(), projectName);
    }

    /**
     * 通用数值校验：字段存在时必须 > 0。
     *
     * @param fieldName   字段名
     * @param value       字段值
     * @param projectName 客户端分组名
     */
    private void validatePositiveIfPresent(String fieldName, Long value, String projectName) {
        if (value != null && value <= 0) {
            throw new IllegalStateException(
                    "open oauth2 配置非法: " + fieldName + " 必须大于 0, projectName=" + projectName + ", value=" + value
            );
        }
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
