package org.example.myextension.auth.open;

import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.hutool.core.util.StrUtil;
import org.example.myextension.logger.annotation.OpenApiLog;
import org.example.myextension.model.vo.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 开放 OAuth2 控制器
 * <p>
 * 提供 OAuth2 接口，用于客户端获取 Access Token（Client-Token）。
 * 遵循 OAuth2 的 Client Credentials 授权模式。
 * <p>
 * <b>接口说明：</b>
 * <ul>
 *   <li>接口路径：{@code /open/oauth2/clientToken}</li>
 *   <li>请求方法：GET 或 POST</li>
 *   <li>请求参数：grantType、clientId、clientSecret、scope（可选）</li>
 *   <li>响应格式：JSON，包含 access_token、expires_in 等信息</li>
 * </ul>
 * <p>
 * <b>请求参数：</b>
 * <table border="1">
 *   <tr><th>参数</th><th>类型</th><th>必填</th><th>说明</th></tr>
 *   <tr><td>grantType</td><td>String</td><td>是</td><td>授权类型，必须为 "client_credentials"</td></tr>
 *   <tr><td>clientId</td><td>String</td><td>是</td><td>客户端 ID</td></tr>
 *   <tr><td>clientSecret</td><td>String</td><td>是</td><td>客户端秘钥</td></tr>
 *   <tr><td>scope</td><td>String</td><td>否</td><td>授权范围，默认为 "*"</td></tr>
 * </table>
 * <p>
 * <b>响应示例：</b>
 * <pre>
 * {
 *   "code": 200,
 *   "success": true,
 *   "message": "成功",
 *   "timestamp": 1234567890,
 *   "data": {
 *     "accessToken": "xxxxx",
 *     "tokenName": "satoken",
 *     "expiresIn": 7200,
 *     "clientType": "c"
 *   }
 * }
 * </pre>
 * <p>
 * <b>使用示例：</b>
 * <pre>
 * // 请求示例
 * GET /open/oauth2/clientToken?grantType=client_credentials&amp;clientId=myapp&amp;clientSecret=secret123
 *
 * // 使用获取到的 Token 调用其他 API
 * GET /open/api/items
 * Headers:
 *   ClientToken: xxxxx
 * </pre>
 * <p>
 * <b>启用条件：</b>
 * 需要配置 {@code extension.auth.open.enable=true} 才会启用此控制器。
 * <p>
 * <b>错误码：</b>
 * <ul>
 *   <li>400：参数错误（缺少必填参数或 grant_type 无效）</li>
 *   <li>401：客户端 ID 或秘钥错误</li>
 *   <li>403：scope 范围超出授权</li>
 *   <li>500：系统繁忙</li>
 * </ul>
 *
 * @see OpenOAuth2AppConfig OAuth2 应用配置模型
 * @see OpenOAuth2ClientProperties OAuth2 客户端属性配置类
 * @see OpenApiInterceptor 开放 API 拦截器
 */
@RestController
@RequestMapping("/open/oauth2")
@ConditionalOnProperty(name = "extension.auth.open.enable", havingValue = "true")
public class OpenOauth2Controller {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(OpenOauth2Controller.class);

    /**
     * 获取 Client Token
     * <p>
     * 实现 OAuth2 Client Credentials 授权模式，为客户端颁发 Access Token。
     * 客户端使用此 Token 可以调用其他开放 API。
     *
     * @param grantType    授权类型，必须为 "client_credentials"
     * @param clientId     客户端 ID
     * @param clientSecret 客户端秘钥
     * @param scope        授权范围（可选）
     * @return 统一响应对象，包含 Token 信息或错误信息
     */
    @RequestMapping("/clientToken")
    @OpenApiLog(name = "Oauth2 Client Token", version = "1.0.0", url = "/v1/oauth2/clientToken")
    public Object clientToken(
            @RequestParam(required = false) String grantType,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String clientSecret,
            @RequestParam(required = false) String scope) {

        // 1. 参数校验
        if (StrUtil.isBlank(grantType)) {
            return ApiResult.fail("缺少参数: grantType");
        }
        if (!"client_credentials".equals(grantType)) {
            return ApiResult.fail("无效grant_type: " + grantType);
        }
        if (StrUtil.isBlank(clientId)) {
            return ApiResult.fail("缺少参数: clientId");
        }
        if (StrUtil.isBlank(clientSecret)) {
            return ApiResult.fail("缺少参数: clientSecret");
        }

        try {
            // 2. 校验权限范围 (Scope)
            // 检查客户端请求的 scope 是否在授权范围内
            SaOAuth2Util.checkContract(clientId, scope);

            // 3. 校验秘钥 (ClientSecret)
            // 验证客户端提供的秘钥是否正确
            SaOAuth2Util.checkClientSecret(clientId, clientSecret);

            // 4. 生成 Client-Token
            // 为客户端生成访问令牌
            cn.dev33.satoken.oauth2.model.ClientTokenModel ct = SaOAuth2Util.generateClientToken(clientId, scope);

            // 5. 转换结果为驼峰命名法返回
            // Sa-Token 默认使用下划线命名，这里转换为驼峰命名方便前端使用
            Map<String, Object> result = new HashMap<>();
            ct.toLineMap().forEach((k, v) -> result.put(StrUtil.toCamelCase(k), v));

            return ApiResult.success(result);
        } catch (SaTokenException saEx) {
            // Sa-Token 异常：权限不足、秘钥错误等
            log.error("获取 oauth2 客户端 token 失败", saEx);
            return ApiResult.fail(saEx.getCode(), saEx.getMessage());
        } catch (Exception e) {
            // 其他未知异常
            log.error("clientToken 接口发生未知错误", e);
            return ApiResult.fail("系统繁忙");
        }
    }
}
