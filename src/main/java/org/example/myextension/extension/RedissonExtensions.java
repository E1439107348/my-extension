package org.example.myextension.extension;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * Redisson 锁工具扩展
 *
 * 提供多个重载方法以便在不同场景下使用分布式锁：
 * - 无超时且不抛出异常的便捷方法（尝试加锁，失败则静默跳过）
 * - 支持等待时长与租赁时长的加锁方式
 * - 支持在加锁失败时通过抛出自定义异常来驱动调用方异常处理流程
 * - 同时提供带返回值的版本
 *
 * 所有方法在释放锁时使用 forceUnlock()，以确保在异常或线程状态异常时也能释放（注意：forceUnlock 会无条件释放，需保证调用方的业务语义允许）。
 */
public class RedissonExtensions {

    /**
     * 尝试获取锁（立即返回），成功则执行 block 并释放锁；失败则静默返回（不抛出异常）。
     * 适用于非关键路径，获取不到锁可以安全跳过的场景。
     *
     * @param client RedissonClient 客户端实例
     * @param lockKey 锁的键
     * @param block 成功获取到锁后执行的逻辑
     */
    public static void lockOperation(RedissonClient client, String lockKey, Runnable block) {
        RLock lock = client.getLock(lockKey);
        if (lock.tryLock()) {
            try {
                block.run();
            } finally {
                lock.forceUnlock();
            }
        }
    }

    /**
     * 在给定等待时间内尝试加锁，若加锁成功则在没有租赁超时时间的情况下执行 block 并释放锁；
     * 若线程被中断则恢复中断状态并静默返回（不抛出异常）。
     *
     * @param client RedissonClient 客户端实例
     * @param lockKey 锁的键
     * @param time 等待时长
     * @param unit 时间单位
     * @param block 成功获取到锁后执行的逻辑
     */
    public static void lockOperation(RedissonClient client, String lockKey, long time, TimeUnit unit, Runnable block) {
        RLock lock = client.getLock(lockKey);
        try {
            if (lock.tryLock(time, unit)) {
                try {
                    block.run();
                } finally {
                    lock.forceUnlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 在给定等待时间内尝试加锁，成功后以 leaseTime 为锁租赁时间，执行完毕后释放锁。
     * 若线程被中断则恢复中断状态并静默返回（不抛出异常）。
     *
     * @param client RedissonClient 客户端实例
     * @param lockKey 锁的键
     * @param waitTime 等待获取锁的最大时长
     * @param leaseTime 锁的租赁时长（自动释放时间）
     * @param unit 时间单位
     * @param block 成功获取到锁后执行的逻辑
     */
    public static void lockOperation(RedissonClient client, String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable block) {
        RLock lock = client.getLock(lockKey);
        try {
            if (lock.tryLock(waitTime, leaseTime, unit)) {
                try {
                    block.run();
                } finally {
                    lock.forceUnlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 立即尝试加锁，若失败则抛出指定异常；成功则执行 block 并释放锁。
     * 适用于必须获取到锁才能继续执行的关键路径。
     *
     * @param client RedissonClient 客户端实例
     * @param lockKey 锁的键
     * @param exception 加锁失败时抛出的异常
     * @param block 成功获取到锁后执行的逻辑
     * @throws Exception 当无法获取锁或 block 执行时抛出异常
     */
    public static void lockOperation(RedissonClient client, String lockKey, Exception exception, Runnable block) throws Exception {
        RLock lock = client.getLock(lockKey);
        if (!lock.tryLock()) {
            throw exception;
        }
        try {
            block.run();
        } finally {
            lock.forceUnlock();
        }
    }

    /**
     * 在给定的超时时间内尝试加锁，若超时仍未获取锁则抛出指定异常；
     * 若线程被中断则恢复中断状态并抛出指定异常。
     *
     * @param client RedissonClient 客户端实例
     * @param lockKey 锁的键
     * @param time 等待时长
     * @param unit 时间单位
     * @param exception 加锁失败时抛出的异常
     * @param block 成功获取到锁后执行的逻辑
     * @throws Exception 当无法获取锁或线程被中断时抛出指定异常
     */
    public static void lockOperation(RedissonClient client, String lockKey, long time, TimeUnit unit, Exception exception, Runnable block) throws Exception {
        RLock lock = client.getLock(lockKey);
        try {
            if (!lock.tryLock(time, unit)) {
                throw exception;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw exception;
        }
        try {
            block.run();
        } finally {
            lock.forceUnlock();
        }
    }

    /**
     * 立即尝试加锁并返回业务结果，若失败则抛出指定异常。
     *
     * @param client RedissonClient 客户端实例
     * @param lockKey 锁的键
     * @param exception 加锁失败时抛出的异常
     * @param block 加锁成功后执行并返回结果的逻辑
     * @param <T> 返回类型
     * @return block 返回的业务结果
     * @throws Exception 当无法获取锁或 block 执行时抛出异常
     */
    public static <T> T lockOperationWithResult(RedissonClient client, String lockKey, Exception exception, Supplier<T> block) throws Exception {
        RLock lock = client.getLock(lockKey);
        if (!lock.tryLock()) {
            throw exception;
        }
        try {
            return block.get();
        } finally {
            lock.forceUnlock();
        }
    }

    /**
     * 在给定的等待时间内尝试加锁并返回业务结果，若失败或线程被中断则抛出指定异常。
     *
     * @param client RedissonClient 客户端实例
     * @param lockKey 锁的键
     * @param time 等待时长
     * @param unit 时间单位
     * @param exception 加锁失败时抛出的异常
     * @param block 加锁成功后执行并返回结果的逻辑
     * @param <T> 返回类型
     * @return block 返回的业务结果
     * @throws Exception 当无法获取锁或线程被中断时抛出指定异常
     */
    public static <T> T lockOperationWithResult(RedissonClient client, String lockKey, long time, TimeUnit unit, Exception exception, Supplier<T> block) throws Exception {
        RLock lock = client.getLock(lockKey);
        try {
            if (!lock.tryLock(time, unit)) {
                throw exception;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw exception;
        }
        try {
            return block.get();
        } finally {
            lock.forceUnlock();
        }
    }

    /**
     * 在给定等待时间与租赁时间下尝试加锁并返回业务结果，若失败或线程被中断则抛出指定异常。
     *
     * @param client RedissonClient 客户端实例
     * @param lockKey 锁的键
     * @param waitTime 等待获取锁的最大时长
     * @param leaseTime 锁的租赁时长（自动释放时间）
     * @param unit 时间单位
     * @param exception 加锁失败时抛出的异常
     * @param block 加锁成功后执行并返回结果的逻辑
     * @param <T> 返回类型
     * @return block 返回的业务结果
     * @throws Exception 当无法获取锁或线程被中断时抛出指定异常
     */
    public static <T> T lockOperationWithResult(RedissonClient client, String lockKey, long waitTime, long leaseTime, TimeUnit unit, Exception exception, Supplier<T> block) throws Exception {
        RLock lock = client.getLock(lockKey);
        try {
            if (!lock.tryLock(waitTime, leaseTime, unit)) {
                throw exception;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw exception;
        }
        try {
            return block.get();
        } finally {
            lock.forceUnlock();
        }
    }
}