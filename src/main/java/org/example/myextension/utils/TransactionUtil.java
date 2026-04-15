package org.example.myextension.utils;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.annotation.PostConstruct;


/**
 * 事务工具类
 *
 * 为了在非 Spring 管理的静态上下文中也能方便地执行事务操作，
 * 本类在启动时通过 @PostConstruct 保存自身实例，暴露静态方法供全局使用。
 *
 * 说明：
 * - transactionOperation：在新的事务上下文中执行给定逻辑（通过 TransactionTemplate）。
 * - transactionAfterCommit：注册事务提交后的回调（仅在存在事务同步时生效）。
 * - transactionActiveAfterCommit：若当前不存在事务同步，则直接执行回调；否则注册为事务提交后的回调。
 *
 * 注意：使用静态 ApplicationContext/工具类会引入全局状态，需谨慎使用以免影响测试。
 */
@Component
public class TransactionUtil {

    // 保存的单例实例，用于静态方法调用时访问注入的 TransactionTemplate
    private static TransactionUtil transactionUtil;

    @Autowired
    private TransactionTemplate injectedTemplate;

    @PostConstruct
    public void init() {
        TransactionUtil.transactionUtil = this;
    }

    /**
     * 在事务中执行给定的 Runnable。内部使用注入的 TransactionTemplate。
     *
     * 使用场景：需要手动在代码中包装事务逻辑但无法直接通过注入获得 TransactionTemplate 的场景。
     *
     * @param block 在事务中执行的逻辑（不返回值）
     */
    public static void transactionOperation(Runnable block) {
        transactionUtil.injectedTemplate.execute(status -> {
            block.run();
            return null;
        });
    }

    /**
     * 在当前事务提交后执行回调（仅在事务同步活动时注册）。
     *
     * @param block 提交后执行的逻辑
     */
    public static void transactionAfterCommit(Runnable block) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                block.run();
            }
        });
    }

    /**
     * 在事务提交后执行回调。如果当前没有活动的事务同步，则直接执行回调，保证在无事务场景下也能生效。
     *
     * 使用场景：有些操作希望在事务成功提交后执行（例如发布事件、异步通知），但也兼容在非事务环境直接执行。
     *
     * @param block 提交后执行的逻辑或在无事务时直接执行
     */
    public static void transactionActiveAfterCommit(Runnable block) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    block.run();
                }
            });
        } else {
            // 无活动事务，同步执行
            block.run();
        }
    }
}