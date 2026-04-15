package org.example.myextension.utils;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * 分页工具类
 *
 * 封装常见的基于 PageHelper 的分页消费/遍历/批量处理模式，避免在业务代码中重复编写分页循环逻辑。
 */
public class PageUtil {

    /**
     * 分页消费数据（基础版）
     *
     * 根据 pageSize 分页获取数据，直到没有下一页为止。每页数据由 supplier 提供，consumer 处理。
     *
     * @param pageSize 页大小
     * @param supplier 数据提供者（无参，返回当前页数据列表）
     * @param consumer 数据消费者（接收每页的列表进行处理）
     */
    public static <E> void pageConsume(int pageSize, Supplier<List<E>> supplier, Consumer<List<E>> consumer) {
        int pageNum = 1;
        boolean hasNextPage;
        do {
            PageMethod.startPage(pageNum++, pageSize);
            List<E> data = supplier.get();
            if (!data.isEmpty()) {
                consumer.accept(data);
            }
            hasNextPage = new PageInfo<>(data).isHasNextPage();
        } while (hasNextPage);
    }

    /**
     * 分页遍历元素（按元素回调）
     *
     * 与 pageConsume 类似，但对每个元素单独处理，适用于元素级别的操作。
     *
     * @param pageSize 页大小
     * @param supplier 数据提供者
     * @param action 对每个元素的处理逻辑
     */
    public static <E> void iterablePageConsume(int pageSize, Supplier<List<E>> supplier, Consumer<E> action) {
        int pageNum = 1;
        boolean hasNextPage;
        do {
            PageMethod.startPage(pageNum++, pageSize);
            List<E> data = supplier.get();
            if (!data.isEmpty()) {
                data.forEach(action);
            }
            hasNextPage = new PageInfo<>(data).isHasNextPage();
        } while (hasNextPage);
    }

    /**
     * 分页消费数据（限制最大条数）
     *
     * 当处理大量数据时，希望限制最多处理 maxSize 条记录，避免一次性处理过多。
     *
     * @param pageSize 页大小
     * @param maxSize 最大处理条数
     * @param supplier 数据提供者
     * @param consumer 数据消费者
     */
    public static <E> void pageConsume(int pageSize, int maxSize, Supplier<List<E>> supplier, Consumer<List<E>> consumer) {
        int pageNum = 1;
        boolean hasNextPage;
        do {
            PageMethod.startPage(pageNum++, pageSize);
            List<E> data = supplier.get();
            if (!data.isEmpty()) {
                consumer.accept(data);
            }
            hasNextPage = (pageNum - 1) * pageSize < maxSize && new PageInfo<>(data).isHasNextPage();
        } while (hasNextPage);
    }

    /**
     * 分页消费数据（自定义分页查询）
     *
     * 支持外部自定义的分页查询函数，函数接收 int[]{pageNum, pageSize}，返回 Pair(total, list)。
     * 这样可以在不依赖 PageHelper 的情况下复用分页循环逻辑。
     *
     * @param pageSize 页大小
     * @param supplier 自定义分页查询（参数：pageNum, pageSize；返回：total + 数据列表）
     * @param consumer 数据消费者
     */
    public static <T> void pageConsume(int pageSize, Function<int[], Pair<Long, List<T>>> supplier, Consumer<List<T>> consumer) {
        int pageNum = 1;
        boolean hasNextPage;
        do {
            Pair<Long, List<T>> result = supplier.apply(new int[]{pageNum++, pageSize});
            long total = result.getFirst();
            List<T> list = result.getSecond();
            if (!list.isEmpty()) {
                consumer.accept(list);
            }
            hasNextPage = ((pageNum - 1) * pageSize) < total && !list.isEmpty();
        } while (hasNextPage);
    }

    /**
     * 循环删除数据（直到删除行数为0）
     *
     * 常用于批量删除需要分批执行的场景，使用 deleter 返回每次删除的行数，直到为 0 停止。
     *
     * @param deleter 删除操作（返回删除行数）
     */
    public static void nextDelete(IntSupplier deleter) {
        int deletedRows;
        do {
            deletedRows = deleter.getAsInt();
        } while (deletedRows > 0);
    }

    /**
     * 循环更新数据（直到更新行数为0）
     *
     * 常用于需要分批提交更新的场景，直到没有可更新的记录为止。
     *
     * @param updater 更新操作（返回更新行数）
     */
    public static void nextUpdate(IntSupplier updater) {
        int updateRows;
        do {
            updateRows = updater.getAsInt();
        } while (updateRows > 0);
    }

    // 自定义Pair类（替代Kotlin的Pair，也可使用Apache Commons的Pair）
    public static class Pair<F, S> {
        private final F first;
        private final S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public F getFirst() {
            return first;
        }

        public S getSecond() {
            return second;
        }
    }
}