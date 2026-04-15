package org.example.myextension.auth.open;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.oauth2.error.SaOAuth2ErrorCode;
import cn.dev33.satoken.oauth2.exception.SaOAuth2Exception;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.router.SaRouter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * 开放 API 拦截器配置
 * <p>
 * 用于校验开放 API 请求中 Header 中的 {@code client_token} 是否有效。
 * 基于 Sa-Token OAuth2 框架实现，对所有开放 API 请求进行 Token 验证。
 * <p>
 * <b>拦截路径：</b>
 * <ul>
 *   <li>{@code /open/**}</li>
 *   <li>{@code /api/open/**}</li>
 * </ul>
 * <p>
 * <b>豁免路径：</b>
 * <ul>
 *   <li>{@code /open/oauth2/clientToken}：获取 Client-Token 的接口，无需验证</li>
 * </ul>
 * <p>
 * <b>Token 获取方式：</b>
 * 从 HTTP 请求头中获取，支持多种命名格式（忽略大小写和连接符）：
 * <ul>
 *   <li>{@code ClientToken}</li>
 *   <li>{@code clienttoken}</li>
 *   <li>{@code client_token}</li>
 *   <li>{@code client-token}</li>
 *   <li>{@code Client-Token}</li>
 *   <li>{@code Client_token}</li>
 * </ul>
 * <p>
 * <b>启用条件：</b>
 * 需要配置 {@code extension.auth.open.enable=true} 才会启用此拦截器。
 *
 * @see OpenOAuth2Controller OAuth2 控制器
 * @see OpenOAuth2ClientProperties OAuth2 客户端配置
 * @see OpenOAuth2TemplateImpl OAuth2 模板实现
 * @see cn.dev33.satoken.oauth2 Sa-Token OAuth2
 */
@Configuration
@ConditionalOnProperty(name = "extension.auth.open.enable", havingValue = "true")
public class OpenApiInterceptor implements WebMvcConfigurer {

    /**
     * 拦截的路径模式
     * <p>
     * 定义需要拦截的 URL 路径模式，符合这些模式的请求将进行 Token 验证。
     */
    private final List<String> patterns = Arrays.asList("/open/**", "/api/open/**");

    /**
     * 可能存在的 ClientToken 请求头 Key（忽略大小写和不同格式）
     * <p>
     * 为了兼容不同客户端的传参方式，支持多种命名格式。
     * 遍历此列表，找到第一个非空的 Token 值进行验证。
     */
    private final List<String> headerPatterns = Arrays.asList(
            "ClientToken", "clienttoken", "client_token", "client-token", "Client-Token", "Client_token"
    );

    /**
     * 添加拦截器配置
     * <p>
     * 注册拦截器，配置拦截路径和豁免路径，并设置拦截器的处理逻辑。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        for (String pattern : patterns) {
            registry.addInterceptor(new SaInterceptor(handler -> {
                // 路由匹配：除了获取 token 的接口外，其他接口都需要校验 token
                SaRouter.match(pattern, "/open/oauth2/clientToken", () -> {
                    // 从 Header 中获取第一个非空的 client_token
                    String requestClientToken = null;
                    for (String headerName : headerPatterns) {
                        requestClientToken = SaHolder.getRequest().getHeader(headerName);
                        if (requestClientToken != null) {
                            break;
                        }
                    }

                    // 校验 Token 有效性
                    Object clientTokenObj = SaOAuth2Util.getClientToken(requestClientToken);

                    // 如果 Token 无效则抛出异常
                    SaOAuth2Exception.throwBy(
                            clientTokenObj == null,
                            "无效的client_token: " + requestClientToken,
                            SaOAuth2ErrorCode.CODE_30107
                    );
                });
            })).addPathPatterns(pattern);
        }
    }
}
