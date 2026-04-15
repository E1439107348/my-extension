package org.example.myextension.alarm.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息内容转换工具类
 * <p>
 * 提供将常见文本或卡片内容转换为 {@link RobotMessageContent} 的静态方法，
 * 便于构造发送给飞书机器人的消息体。
 * <p>
 * 此类与 Kotlin 代码中的扩展函数功能对应：
 * <ul>
 *   <li>{@code textContentConverter()} 对应 Kotlin 扩展函数 {@code String?.textContentConverter()}</li>
 *   <li>{@code cardDefaultContentConverter()} 对应 Kotlin 扩展函数 {@code String?.cardDefaultContentConverter()}</li>
 * </ul>
 *
 * @see RobotMessageContent 飞书机器人消息内容模型
 */
public class Converter {

    /**
     * 将 String 转换为文本类型的机器人消息
     * <p>
     * 创建一个纯文本类型的 {@link RobotMessageContent} 对象，包含指定的文本内容。
     * 如果输入文本为 null 或空白字符串，则返回空的文本消息对象。
     * <p>
     * 对应 Kotlin 扩展函数: {@code String?.textContentConverter()}
     *
     * @param receiver 要发送的文本消息内容，可以为 null 或空白字符串
     * @return 类型为 {@link RobotMessageContent.MsgType#TEXT} 的消息内容对象
     */
    public static RobotMessageContent textContentConverter(String receiver) {
        if (receiver == null || receiver.trim().isEmpty()) {
            return new RobotMessageContent(RobotMessageContent.MsgType.TEXT);
        } else {
            RobotMessageContent message = new RobotMessageContent(RobotMessageContent.MsgType.TEXT);
            message.setContent(new RobotMessageContent.TextContent(receiver));
            return message;
        }
    }

    /**
     * 卡片默认内容转换器（使用默认标题和红色主题）
     * <p>
     * 创建一个默认样式的卡片消息：
     * <ul>
     *   <li>标题：默认为"有赞定制告警"</li>
     *   <li>颜色：红色（表示告警/紧急）</li>
     *   <li>@用户：不@任何人</li>
     * </ul>
     *
     * @param receiver 卡片正文内容（支持 lark_md 格式），可以为 null 或空白字符串
     * @return 类型为 {@link RobotMessageContent.MsgType#CARD} 的消息内容对象
     */
    public static RobotMessageContent cardDefaultContentConverter(String receiver) {
        return cardDefaultContentConverter(receiver, "有赞定制告警", null);
    }

    /**
     * 卡片内容转换器（指定标题和 @ 用户，使用红色主题）
     * <p>
     * 创建一个自定义标题和 @ 用户的卡片消息，卡片颜色固定为红色。
     *
     * @param receiver 卡片正文内容（支持 lark_md 格式），可以为 null 或空白字符串
     * @param title    卡片标题，为 null 时使用默认值"有赞定制告警"
     * @param uid      要 @ 的用户 ID（飞书 user_id），为 null 或空白时表示不 @ 任何人
     * @return 类型为 {@link RobotMessageContent.MsgType#CARD} 的消息内容对象
     */
    public static RobotMessageContent cardDefaultContentConverter(String receiver, String title, String uid) {
        String finalTitle = (title != null) ? title : "有赞定制告警";
        return cardContentConverter(receiver, finalTitle, "red", uid);
    }

    /**
     * 卡片内容转换器核心逻辑（完全自定义）
     * <p>
     * 创建一个完全自定义样式的卡片消息，支持设置：
     * <ul>
     *   <li>标题：通过 {@code title} 参数指定</li>
     *   <li>颜色：通过 {@code headerColor} 参数指定（如 "red"、"blue"、"green" 等）</li>
     *   <li>@用户：通过 {@code uid} 参数指定要 @ 的用户</li>
     * </ul>
     * <p>
     * 卡片结构说明：
     * <pre>
     * ┌─────────────────────────────────┐
     * │ 标题（headerColor 颜色）        │
     * ├─────────────────────────────────┤
     * │ @用户（如果有）                  │
     * ├─────────────────────────────────┤
     * │ 正文内容（lark_md 格式）        │
     * └─────────────────────────────────┘
     * </pre>
     *
     * @param receiver    卡片正文内容（支持 lark_md 格式），可以为 null 或空白字符串
     * @param title       卡片标题，不能为 null（建议使用非空白字符串）
     * @param headerColor 卡片标题栏颜色，支持飞书主题色（如 "red"、"blue"、"green" 等）
     * @param uid         要 @ 的用户 ID（飞书 user_id），为 null 或空白时表示不 @ 任何人
     * @return 类型为 {@link RobotMessageContent.MsgType#CARD} 的消息内容对象
     */
    public static RobotMessageContent cardContentConverter(String receiver, String title, String headerColor, String uid) {
        if (receiver == null || receiver.trim().isEmpty()) {
            return new RobotMessageContent(RobotMessageContent.MsgType.CARD);
        }

        // 1. 构建 Header（卡片标题栏）
        RobotMessageContent.InteractiveContent.Header header = new RobotMessageContent.InteractiveContent.Header();
        header.setTitle(new RobotMessageContent.InteractiveContent.Header.HeaderTitle("plain_text", title));
        header.setTemplate(headerColor);

        // 2. 构建 Config（卡片显示配置）
        // 默认开启宽屏模式和多卡片更新模式
        RobotMessageContent.InteractiveContent.Config config = new RobotMessageContent.InteractiveContent.Config();

        // 3. 构建 Elements（卡片内容元素列表）
        List<RobotMessageContent.InteractiveContent.Element> elements = new ArrayList<>();

        // 处理 @ 逻辑：如果提供了 uid，则在卡片中添加 @ 元素
        // 飞书 @ 语法：使用 lark_md 格式的 <at user_id="xxx"></at> 标签
        String at = (uid != null && !uid.trim().isEmpty()) ? "<at user_id=\"" + uid + "\"></at>" : "";
        elements.add(new RobotMessageContent.InteractiveContent.Div(
                new RobotMessageContent.InteractiveContent.Element.Content("lark_md", at)
        ));

        // 处理正文内容：将 receiver 作为 lark_md 格式的文本内容添加到卡片
        RobotMessageContent.InteractiveContent.Div contentDiv = new RobotMessageContent.InteractiveContent.Div();
        contentDiv.setText(new RobotMessageContent.InteractiveContent.Element.Content("lark_md", receiver));
        elements.add(contentDiv);

        // 4. 组装消息并返回
        RobotMessageContent message = new RobotMessageContent(RobotMessageContent.MsgType.CARD);
        message.setCard(new RobotMessageContent.InteractiveContent(header, config, elements));
        return message;
    }
}
