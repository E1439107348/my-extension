package org.example.myextension.message;

public interface MessageDispatchHandler<T> {

    /**
     * 处理条件，默认返回 true
     */
    default boolean condition(T msgData) {
        return true;
    }

    /**
     * 核心业务处理逻辑
     */
    void handle(T msgData);
}