package org.example.myextension.alarm;

import com.alibaba.fastjson2.JSON;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.example.myextension.alarm.model.Converter;
import org.example.myextension.alarm.model.RobotMessageContent;
import org.example.myextension.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 飞书群机器人告警实现
 * <p>
 * 使用 RestTemplate 向飞书群机器人的 webhook 发送告警通知。
 * 支持同步与异步两种发送方式，并可通过配置控制是否启用告警功能。
 * <p>
 * <b>配置项说明：</b>
 * 在 {@code application.properties} 或 {@code application.yml} 中配置：
 * <ul>
 *   <li>{@code extension.alarm.feishu.webhook}: 飞书群机器人的 webhook 地址（必填）</li>
 *   <li>{@code extension.alarm.feishu.enable}: 是否启用告警（默认为 false）</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>
 * // 同步发送文本消息
 * alarmOperations.sendMessage("告警：服务器 CPU 使用率过高！");
 *
 * // 同步发送卡片消息（带标题和 @用户）
 * RobotMessageContent card = Converter.cardDefaultContentConverter("告警详情...", "系统告警", "user_123");
 * alarmOperations.sendMessage(card);
 *
 * // 异步发送卡片消息
 * alarmOperations.asyncSendMessage(card);
 * </pre>
 *
 * @see AlarmOperations 告警操作接口
 * @see <a href="https://open.feishu.cn/document/ukTMukTMukTM/uUTNz4SN1MjL1UzM">飞书群机器人自定义机器人接入文档</a>
 */
@Service
public class FeiShuGroupRobotAlarmTemplate implements AlarmOperations {

    /**
     * 日志记录器
     * <p>
     * 用于记录告警发送的执行状态、结果和异常信息。
     */
    private static final Logger log = LoggerFactory.getLogger(FeiShuGroupRobotAlarmTemplate.class);

    /**
     * 线程池：用于异步发送告警消息
     * <p>
     * 使用固定大小线程池（2 个线程）处理异步发送请求。
     * 作为 Spring Bean 的一部分运行，生命周期由容器管理，本类不主动关闭该线程池。
     * <p>
     * <b>注意事项：</b>
     * 如果 Spring 容器关闭时需要优雅关闭线程池，建议实现 DisposableBean 接口。
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    /**
     * Spring 的 RestTemplate 实例
     * <p>
     * 用于发起 HTTP 请求，向飞书机器人的 webhook 发送消息。
     * 通过构造器注入，确保 RestTemplate 实例可配置（如超时时间、连接池等）。
     */
    private final RestTemplate restTemplate;

    /**
     * 飞书群机器人的 webhook 地址
     * <p>
     * 从配置文件读取，格式如：{@code https://open.feishu.cn/open-apis/bot/v2/hook/xxxxxx}
     * 为空表示未配置，此时发送告警会直接返回不执行网络请求。
     */
    @Value("${extension.alarm.feishu.webhook:}")
    private String robotWebhook;

    /**
     * 告警功能开关
     * <p>
     * 从配置文件读取，值为 "true"（忽略大小写）时启用告警，否则禁用。
     * 使用 String 类型而非 Boolean 是为了兼容配置文件的读取方式。
     */
    @Value("${extension.alarm.feishu.enable:false}")
    private String enable;

    /**
     * 构造函数：通过依赖注入初始化
     *
     * @param restTemplate 用于发起 HTTP 请求的 RestTemplate 实例
     */
    public FeiShuGroupRobotAlarmTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 判断告警功能是否开启
     *
     * @return 当 {@code enable} 配置为 "true"（忽略大小写）时返回 true，否则返回 false
     */
    private boolean isOpen() {
        return "true".equalsIgnoreCase(enable);
    }


    /**
     * 发送文本类型的告警消息（同步方式）
     * <p>
     * 将文本内容转换为 {@link RobotMessageContent} 后发送到飞书群机器人。
     * 调用线程会阻塞直到发送完成或出现异常。
     * <p>
     * <b>前置检查：</b>
     * <ul>
     *   <li>如果告警未开启（{@code isOpen() == false}），记录日志并直接返回</li>
     *   <li>如果 webhook 未配置，记录警告日志并直接返回</li>
     * </ul>
     *
     * @param text 文本消息内容，可以为 null 或空字符串
     */
    @Override
    public void sendMessage(String text) {
        if (!isOpen()) {
            log.info("飞书机器人通知未开启，跳过发送");
            return;
        }
        if (robotWebhook == null || robotWebhook.trim().isEmpty()) {
            log.warn("飞书机器人 webhook 未配置，无法发送告警");
            return;
        }
        RobotMessageContent content = Converter.textContentConverter(text);
        resultLogger(execute(URI.create(robotWebhook), HttpMethod.POST, content));
    }


    /**
     * 发送自定义 Map 格式的告警消息（同步方式）
     * <p>
     * 将 Map 序列化为 JSON 字符串后发送到飞书群机器人。
     * 调用线程会阻塞直到发送完成或出现异常。
     * <p>
     * <b>注意事项：</b>
     * Map 的结构需要符合飞书机器人 API 规范，否则可能发送失败。
     *
     * @param content 按飞书 API 要求组织好的 Map 内容，会被序列化为 JSON。为 null 或空时可能被忽略
     */
    @Override
    public void sendMessage(Map<String, Object> content) {
        if (!isOpen()) {
            log.info("飞书机器人通知未开启，跳过发送");
            return;
        }
        if (robotWebhook == null || robotWebhook.trim().isEmpty()) {
            log.warn("飞书机器人 webhook 未配置，无法发送告警");
            return;
        }
        resultLogger(execute(URI.create(robotWebhook), HttpMethod.POST, JSON.toJSONString(content)));
    }


    /**
     * 发送卡片类型的告警消息（同步方式）
     * <p>
     * 将已构建好的 {@link RobotMessageContent} 对象发送到飞书群机器人。
     * 调用线程会阻塞直到发送完成或出现异常。
     * <p>
     * <b>前置检查：</b>
     * <ul>
     *   <li>如果告警未开启（{@code isOpen() == false}），记录日志并直接返回</li>
     *   <li>如果 webhook 未配置，记录警告日志并直接返回</li>
     * </ul>
     *
     * @param content 已构建好的消息内容对象，可以为 null
     */
    @Override
    public void sendMessage(RobotMessageContent content) {
        if (!isOpen()) {
            log.info("飞书机器人通知未开启，跳过发送");
            return;
        }
        if (robotWebhook == null || robotWebhook.trim().isEmpty()) {
            log.warn("飞书机器人 webhook 未配置，无法发送告警");
            return;
        }
        resultLogger(execute(URI.create(robotWebhook), HttpMethod.POST, content));
    }


    /**
     * 异步发送卡片类型的告警消息
     * <p>
     * 将消息序列化后提交到线程池执行，调用方不会阻塞等待发送结果。
     * 适用于对实时性要求不高、或批量发送告警的场景。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>异步发送无法立即获知发送结果，需要通过日志监控</li>
     *   <li>如果任务提交失败，异常信息会记录在日志中</li>
     * </ul>
     *
     * @param content 要发送的消息内容对象，可以为 null
     */
    @Override
    public void asyncSendMessage(RobotMessageContent content) {
        if (!isOpen()) {
            log.info("飞书机器人通知未开启，跳过异步发送");
            return;
        }
        if (robotWebhook == null || robotWebhook.trim().isEmpty()) {
            log.warn("飞书机器人 webhook 未配置，无法异步发送告警");
            return;
        }
        String message = JSON.toJSONString(content);
        executorService.submit(() -> {
            resultLogger(execute(URI.create(robotWebhook), HttpMethod.POST, message));
        });
    }

    /**
     * 记录发送结果日志
     * <p>
     * 根据飞书返回的响应结构判定是否成功：
     * <ul>
     *   <li>成功条件：{@code code == 0} 且 {@code message == "success"}</li>
     *   <li>失败条件：响应为 null、code 不为 0、或 message 不为 "success"</li>
     * </ul>
     *
     * @param response 飞书机器人返回的响应对象，可以为 null
     */
    private void resultLogger(Response response) {
        if (response == null || response.getCode() == null || response.getCode() != 0
                || !"success".equalsIgnoreCase(response.getMessage())) {
            String msg = (response == null) ? "null" : response.getMessage();
            log.warn("告警发送失败: {}", msg);
            return;
        }
        log.info("告警发送成功");
    }

    /**
     * 执行发送（对象重载方法）
     * <p>
     * 将对象序列化为 JSON 字符串后，委托到字符串重载方法执行实际发送。
     *
     * @param uri        webhook 的 URI
     * @param httpMethod HTTP 方法（通常为 POST）
     * @param content    要发送的消息对象
     * @return 解析后的响应对象
     */
    private Response execute(URI uri, HttpMethod httpMethod, RobotMessageContent content) {
        return execute(uri, httpMethod, JSON.toJSONString(content));
    }

    /**
     * 执行发送（字符串重载方法）
     * <p>
     * 负责构造 HTTP 请求并调用 RestTemplate 发起网络请求。
     * <p>
     * <b>流程说明：</b>
     * <ol>
     *   <li>记录发送日志（包含 webhook URI、开关状态和消息内容）</li>
     *   <li>进行防御性检查（开关和 webhook URI）</li>
     *   <li>构造 HTTP 请求头（Content-Type: application/json;charset=UTF-8）</li>
     *   <li>使用 RestTemplate 发送 POST 请求</li>
     *   <li>记录响应日志（包含响应体和耗时）</li>
     *   <li>解析响应并返回</li>
     * </ol>
     * <p>
     * <b>异常处理：</b>
     * 捕获所有异常，记录错误日志并返回包含错误信息的 Response 对象。
     *
     * @param uri        webhook 的 URI
     * @param httpMethod HTTP 方法（通常为 POST）
     * @param message    已序列化的 JSON 字符串
     * @return 解析后的 Response 对象（若异常则返回包含错误信息的 Response）
     */
    private Response execute(URI uri, HttpMethod httpMethod, String message) {
        log.info("发送飞书机器人通知, uri:{}, switch:{}\nmessage:{}", uri, isOpen(), message);

        if (!isOpen()) {
            return new Response(0, "未开启机器人通知");
        }

        if (uri == null || uri.toString().trim().isEmpty()) {
            log.warn("发送失败：webhook URI 为空");
            return new Response(500, "webhook 未配置或为空");
        }

        long startTime = Instant.now().toEpochMilli();
        try {
            // 构造 HTTP 请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            HttpEntity<String> entity = new HttpEntity<>(message, headers);

            // 发送 POST 请求
            ResponseEntity<String> responseEntity = restTemplate.exchange(uri, httpMethod, entity, String.class);
            String body = responseEntity.getBody();

            // 记录响应日志和耗时
            log.info("发送飞书机器人通知响应, uri:{}\nresponse:{}, 耗时:{}ms",
                    uri, body, (Instant.now().toEpochMilli() - startTime));

            // 判断响应状态并解析
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                return JSON.parseObject(body, Response.class);
            } else {
                throw new BizException(BizException.SYSTEM_FAILED, "通知异常, status: " + responseEntity.getStatusCode());
            }
        } catch (Exception e) {
            log.error("发送飞书机器人通知异常", e);
            return new Response(500, e.getMessage());
        }
    }

    /**
     * 响应内部类
     * <p>
     * 用于解析飞书机器人返回的简单响应结构。
     * 响应格式：
     * <pre>
     * {
     *   "code": 0,
     *   "msg": "success"
     * }
     * </pre>
     * <p>
     * <b>响应码说明：</b>
     * <ul>
     *   <li>{@code code == 0}：表示发送成功</li>
     *   <li>{@code code != 0}：表示发送失败，具体原因见 {@code message} 字段</li>
     * </ul>
     */
    public static class Response {
        /**
         * 返回码
         * <p>
         * 飞书成功通常返回 0，非 0 表示失败。
         * 常见错误码：
         * <ul>
         *   <li>0：成功</li>
         *   <li>99991663：webhook 不存在</li>
         *   <li>19024：缺少必填参数</li>
         * </ul>
         */
        private Integer code;

        /**
         * 返回信息
         * <p>
         * 成功时通常为 "success"，失败时为错误描述信息。
         */
        private String message;

        /**
         * 无参构造函数
         */
        public Response() {}

        /**
         * 构造函数：指定返回码和信息
         *
         * @param code    返回码
         * @param message 返回信息
         */
        public Response(Integer code, String message) {
            this.code = code;
            this.message = message;
        }

        public Integer getCode() { return code; }
        public void setCode(Integer code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
