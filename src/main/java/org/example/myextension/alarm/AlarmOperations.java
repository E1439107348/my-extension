package org.example.myextension.alarm;

import org.example.myextension.alarm.model.RobotMessageContent;

import java.util.Map;

/**
 * 告警操作接口
 * <p>
 * 定义不同格式的消息发送能力，作为告警服务的抽象层。
 * 实现类负责将消息发送到具体的告警渠道（例如飞书群机器人）。
 * <p>
 * 接口提供了多种发送方式：
 * <ul>
 *   <li>同步发送：阻塞等待发送完成</li>
 *   <li>异步发送：在后台线程中执行，不阻塞调用方</li>
 *   <li>多种消息格式：纯文本、Map 结构化数据、富文本卡片等</li>
 * </ul>
 *
 * @see org.example.myextension.alarm.FeiShuGroupRobotAlarmTemplate 飞书群机器人实现示例
 */
public interface AlarmOperations {

    /**
     * 发送纯文本告警消息（同步方式）
     * <p>
     * 此方法将文本内容作为告警消息发送，调用线程会阻塞直到发送完成。
     * 适用于需要立即确认发送结果的场景。
     *
     * @param text 文本消息内容，不能为 null。空字符串或仅包含空白字符的文本可能被实现类忽略
     * @throws org.example.myextension.exception.BizException 当发送过程出现异常时可能抛出业务异常
     */
    void sendMessage(String text);

    /**
     * 发送自定义 Map 格式告警消息（同步方式）
     * <p>
     * 允许发送结构化的告警数据，Map 中的键值对会被序列化为对应的 JSON 格式。
     * 此方法适用于需要发送复杂结构化告警信息的场景，调用线程会阻塞直到发送完成。
     * <p>
     * 注意：Map 的结构需要符合目标告警渠道的 API 规范。
     *
     * @param content 结构化的告警内容，键值对会被序列化为 JSON。为 null 或空时可能被忽略
     * @throws org.example.myextension.exception.BizException 当发送过程出现异常时可能抛出业务异常
     */
    void sendMessage(Map<String, Object> content);

    /**
     * 发送卡片类型的告警消息（同步方式）
     * <p>
     * 发送富文本卡片消息，支持标题、颜色、@用户等丰富格式。
     * 调用线程会阻塞直到发送完成，适用于需要立即确认发送结果的场景。
     * <p>
     * RobotMessageContent 封装了飞书卡片的完整结构，包括：
     * <ul>
     *   <li>header：卡片标题和颜色</li>
     *   <li>elements：卡片内容元素，支持文本、@用户等</li>
     *   <li>config：卡片显示配置（如宽屏模式等）</li>
     * </ul>
     *
     * @param content 已构建好的机器人消息内容对象，为 null 时可能被忽略
     * @throws org.example.myextension.exception.BizException 当发送过程出现异常时可能抛出业务异常
     */
    void sendMessage(RobotMessageContent content);

    /**
     * 异步发送卡片类型的告警消息
     * <p>
     * 将消息提交到后台线程池执行，调用方不会阻塞等待发送结果。
     * 适用于对实时性要求不高、或批量发送告警的场景。
     * <p>
     * 异步发送的缺点是无法立即获知发送结果，需要通过日志或其他方式监控发送状态。
     *
     * @param content 已构建好的机器人消息内容对象，为 null 时可能被忽略
     */
    void asyncSendMessage(RobotMessageContent content);
}