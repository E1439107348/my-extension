package org.example.myextension.alarm.model;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 飞书机器人消息内容模型
 * <p>
 * 对应飞书机器人的消息格式，支持文本消息（text）与交互式卡片（interactive）两种类型。
 * 此类封装了发送给飞书群机器人的完整消息结构，包括消息类型、内容和卡片数据。
 * <p>
 * 消息结构说明：
 * <pre>
 * 文本消息：
 * {
 *   "msg_type": "text",
 *   "content": { "text": "消息内容" }
 * }
 *
 * 卡片消息：
 * {
 *   "msg_type": "interactive",
 *   "card": {
 *     "header": { "title": { "tag": "plain_text", "content": "标题" }, "template": "red" },
 *     "config": { "wide_screen_mode": true },
 *     "elements": [ ... ]
 *   }
 * }
 * </pre>
 *
 * @see <a href="https://open.feishu.cn/document/common-capabilities/message-card/message-cards-content">飞书消息卡片文档</a>
 */
public class RobotMessageContent {

    /**
     * 消息类型，对应飞书的 "msg_type" 字段
     * <p>
     * 使用 @JSONField 和 @JsonProperty 双重注解确保在不同 JSON 序列化框架下都能正确映射字段名。
     */
    @JSONField(name = "msg_type")
    @JsonProperty("msg_type")
    private String type;

    /**
     * 文本类型消息的内容字段
     * <p>
     * 当 {@code msg_type} 为 "text" 时使用此字段。
     */
    private Content content;

    /**
     * 卡片类型消息的内容字段
     * <p>
     * 当 {@code msg_type} 为 "interactive" 时使用此字段。
     * 注意：虽然字段名为 "card"，但在序列化为 JSON 时会映射为飞书 API 要求的格式。
     */
    private Content card;

    /**
     * 构造函数：创建指定类型的消息内容
     *
     * @param type 消息类型枚举（TEXT 或 CARD）
     */
    public RobotMessageContent(MsgType type) {
        this.type = type.getValue();
    }

    /**
     * 消息类型枚举
     * <p>
     * 定义飞书机器人支持的消息类型：
     * <ul>
     *   <li>{@link #TEXT}：纯文本消息，适用于简单通知</li>
     *   <li>{@link #CARD}：交互式卡片消息，支持富文本格式和复杂布局</li>
     * </ul>
     */
    public enum MsgType {
        /** 纯文本消息类型，值为 "text" */
        TEXT("text"),
        /** 交互式卡片消息类型，值为 "interactive" */
        CARD("interactive");

        /** 枚举值对应的字符串表示 */
        private final String value;

        /**
         * 枚举构造函数
         *
         * @param value 消息类型的字符串值
         */
        MsgType(String value) {
            this.value = value;
        }

        /**
         * 获取消息类型的字符串值
         *
         * @return 消息类型字符串（"text" 或 "interactive"）
         */
        public String getValue() {
            return value;
        }
    }

    /**
     * 内容接口
     * <p>
     * 作为文本内容和卡片内容的统一接口类型，
     * 具体实现类包括 {@link TextContent} 和 {@link InteractiveContent}。
     */
    public interface Content {
    }

    // ==================== 文本消息内容 ====================

    /**
     * 文本消息内容
     * <p>
     * 封装纯文本消息的结构，包含一个 text 字段。
     * 对应飞书 API 的文本消息格式：
     * <pre>
     * { "text": "消息内容" }
     * </pre>
     */
    public static class TextContent implements Content {
        /** 消息文本内容 */
        private String text;

        /**
         * 无参构造函数
         */
        public TextContent() {
        }

        /**
         * 构造函数：指定文本内容
         *
         * @param text 消息文本内容
         */
        public TextContent(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    // ==================== 交互式卡片内容 ====================

    /**
     * 交互式卡片内容
     * <p>
     * 封装飞书卡片消息的完整结构，包括：
     * <ul>
     *   <li>{@link Header}：卡片头部，包含标题和颜色</li>
     *   <li>{@link Config}：卡片配置，控制显示模式等</li>
     *   <li>{@link Element}：卡片元素列表，如文本分割线、@ 用户等</li>
     * </ul>
     */
    public static class InteractiveContent implements Content {
        /** 卡片头部信息 */
        private Header header;
        /** 卡片配置信息 */
        private Config config;
        /** 卡片元素列表 */
        private List<Element> elements;

        /**
         * 无参构造函数
         */
        public InteractiveContent() {
        }

        /**
         * 构造函数：指定头部、配置和元素
         *
         * @param header   卡片头部
         * @param config   卡片配置
         * @param elements 卡片元素列表
         */
        public InteractiveContent(Header header, Config config, List<Element> elements) {
            this.header = header;
            this.config = config;
            this.elements = elements;
        }

        // ------------------- 内部类: Header -------------------

        /**
         * 卡片头部
         * <p>
         * 定义卡片的标题栏样式，包括标题文本和模板颜色。
         * 对应飞书 API 的 header 结构：
         * <pre>
         * {
         *   "title": { "tag": "plain_text", "content": "标题文本" },
         *   "template": "red"
         * }
         * </pre>
         */
        public static class Header {
            /** 标题对象 */
            private HeaderTitle title;
            /** 模板颜色，支持飞书主题色（如 "red"、"blue"、"green"、"yellow"、"grey" 等） */
            private String template;

            /**
             * 标题对象
             * <p>
             * 封装卡片的标题信息，支持纯文本格式。
             * tag 固定为 "plain_text"，表示普通文本类型。
             */
            public static class HeaderTitle {
                /** 标签类型，固定为 "plain_text" */
                private String tag = "plain_text";
                /** 标题文本内容 */
                private String content;

                /**
                 * 无参构造函数
                 */
                public HeaderTitle() {
                }

                /**
                 * 构造函数：指定标签和内容
                 *
                 * @param tag     标签类型，通常为 "plain_text"
                 * @param content 标题文本内容
                 */
                public HeaderTitle(String tag, String content) {
                    this.tag = tag;
                    this.content = content;
                }

                public String getTag() {
                    return tag;
                }

                public void setTag(String tag) {
                    this.tag = tag;
                }

                public String getContent() {
                    return content;
                }

                public void setContent(String content) {
                    this.content = content;
                }
            }

            public HeaderTitle getTitle() {
                return title;
            }

            public void setTitle(HeaderTitle title) {
                this.title = title;
            }

            public String getTemplate() {
                return template;
            }

            public void setTemplate(String template) {
                this.template = template;
            }
        }

        // ------------------- 内部类: Config -------------------

        /**
         * 卡片配置
         * <p>
         * 控制卡片的显示模式和更新行为。
         * 使用 @JSONField 注解将驼峰命名转换为下划线命名，以符合飞书 API 规范。
         */
        public static class Config {
            /**
             * 宽屏模式开关
             * <p>
             * 为 true 时，卡片在宽屏模式下显示，占用更大的横向空间。
             * 使用 "wide_screen_mode" 作为 JSON 字段名。
             */
            @JSONField(name = "wide_screen_mode")
            private boolean wideScreenMode = true;

            /**
             * 多卡片更新开关
             * <p>
             * 为 true 时，支持同一消息的多张卡片更新。
             * 使用 "update_multi" 作为 JSON 字段名。
             */
            @JSONField(name = "update_multi")
            private boolean updateMulti = true;

            public boolean isWideScreenMode() {
                return wideScreenMode;
            }

            public void setWideScreenMode(boolean wideScreenMode) {
                this.wideScreenMode = wideScreenMode;
            }

            public boolean isUpdateMulti() {
                return updateMulti;
            }

            public void setUpdateMulti(boolean updateMulti) {
                this.updateMulti = updateMulti;
            }
        }

        // ------------------- 内部类: Element -------------------

        /**
         * 卡片元素基类
         * <p>
         * 卡片中的所有元素都继承此类，包含一个 tag 字段标识元素类型。
         */
        public static class Element {
            /** 元素类型标签，如 "div" 表示文本分割线 */
            private String tag;

            /**
             * 构造函数：指定元素类型
             *
             * @param tag 元素类型标签
             */
            public Element(String tag) {
                this.tag = tag;
            }

            public String getTag() {
                return tag;
            }

            /**
             * 元素内容
             * <p>
             * 封装卡片元素中的文本内容，支持 lark_md 格式。
             * lark_md 是飞书的 Markdown 变种，支持 @用户、链接等特殊语法。
             */
            public static class Content {
                /** 内容格式，固定为 "lark_md" 表示飞书 Markdown 格式 */
                private String tag = "lark_md";
                /** 实际文本内容 */
                private String content;

                /**
                 * 无参构造函数
                 */
                public Content() {
                }

                /**
                 * 构造函数：指定标签和内容
                 *
                 * @param tag     内容格式，通常为 "lark_md"
                 * @param content 文本内容
                 */
                public Content(String tag, String content) {
                    this.tag = tag;
                    this.content = content;
                }

                public String getTag() {
                    return tag;
                }

                public void setTag(String tag) {
                    this.tag = tag;
                }

                public String getContent() {
                    return content;
                }

                public void setContent(String content) {
                    this.content = content;
                }
            }
        }

        /**
         * 文本分割线元素
         * <p>
         * 用于在卡片中显示一段文本，支持 lark_md 格式。
         * 继承自 Element，tag 固定为 "div"。
         */
        public static class Div extends Element {
            /** 文本内容对象 */
            private Element.Content text;

            /**
             * 无参构造函数
             */
            public Div() {
                super("div");
            }

            /**
             * 构造函数：指定文本内容
             *
             * @param text 文本内容对象
             */
            public Div(Element.Content text) {
                super("div");
                this.text = text;
            }

            public Element.Content getText() {
                return text;
            }

            public void setText(Element.Content text) {
                this.text = text;
            }
        }

        // ------------------- InteractiveContent Getter & Setter -------------------

        public Header getHeader() {
            return header;
        }

        public void setHeader(Header header) {
            this.header = header;
        }

        public Config getConfig() {
            return config;
        }

        public void setConfig(Config config) {
            this.config = config;
        }

        public List<Element> getElements() {
            return elements;
        }

        public void setElements(List<Element> elements) {
            this.elements = elements;
        }
    }

    // ==================== RobotMessageContent Getter & Setter ====================

    /**
     * 获取消息类型
     *
     * @return 消息类型字符串（"text" 或 "interactive"）
     */
    public String getType() {
        return type;
    }

    /**
     * 设置消息类型
     *
     * @param type 消息类型字符串
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取文本内容
     * <p>
     * 当消息类型为 TEXT 时使用此字段。
     *
     * @return 文本内容对象
     */
    public Content getContent() {
        return content;
    }

    /**
     * 设置文本内容
     * <p>
     * 当消息类型为 TEXT 时使用此字段。
     *
     * @param content 文本内容对象
     */
    public void setContent(Content content) {
        this.content = content;
    }

    /**
     * 获取卡片内容
     * <p>
     * 当消息类型为 CARD 时使用此字段。
     *
     * @return 卡片内容对象
     */
    public Content getCard() {
        return card;
    }

    /**
     * 设置卡片内容
     * <p>
     * 当消息类型为 CARD 时使用此字段。
     *
     * @param card 卡片内容对象
     */
    public void setCard(Content card) {
        this.card = card;
    }
}
