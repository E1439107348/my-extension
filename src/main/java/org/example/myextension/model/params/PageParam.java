package org.example.myextension.model.params;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 分页查询基础参数类
 * <p>
 * 提供通用的分页查询参数封装，包含页码和每页大小。
 * 此类可作为所有需要分页查询功能的请求参数基类或组合字段。
 * <p>
 * <b>验证规则：</b>
 * <ul>
 *   <li>{@code pageNum}: 不能为 null（默认值为 1）</li>
 *   <li>{@code pageSize}: 不能为 null，且最大值为 100（默认值为 10）</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>
 * // 直接作为请求参数使用
 * &#64;GetMapping("/list")
 * public ApiResult&lt;List&lt;Item&gt;&gt; getList(PageParam pageParam) {
 *     List&lt;Item&gt; items = itemService.list(pageParam.getPageNum(), pageParam.getPageSize());
 *     return ApiResult.success(items);
 * }
 *
 * // 继承扩展使用
 * public class ItemQueryParam extends PageParam {
 *     private String keyword;
 *     // getters and setters...
 * }
 * </pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>设置 {@code pageSize} 最大值为 100 是为了防止一次性查询过多数据导致性能问题</li>
 *   <li>如果需要更大的每页大小，建议在继承类中覆盖 {@code @Max} 注解的值</li>
 *   <li>页码通常从 1 开始计数，部分数据库分页可能需要从 0 开始计数，使用时请注意转换</li>
 * </ul>
 */
public class PageParam implements Serializable {
    /**
     * 页码
     * <p>
     * 表示当前请求的页码，从 1 开始计数。
     * 使用 {@code @NotNull} 注解确保参数不能为空。
     * 默认值为 1，表示请求第一页。
     */
    @NotNull(message = "PageNum不能为空")
    private Integer pageNum = 1;

    /**
     * 每页大小
     * <p>
     * 表示每页返回的记录数量。
     * 使用 {@code @NotNull} 和 {@code @Max(100)} 注解确保参数不能为空且最大值为 100。
     * 默认值为 10。
     * <p>
     * <b>设置最大值的原因：</b>
     * <ul>
     *   <li>防止恶意请求一次性获取大量数据</li>
     *   <li>避免数据库查询返回过多数据导致内存溢出</li>
     *   <li>提升响应速度，减少网络传输时间</li>
     * </ul>
     */
    @Max(value = 100, message = "PageSize不能大于100")
    @NotNull(message = "PageSize不能为空")
    private Integer pageSize = 10;

    /**
     * 获取页码
     *
     * @return 当前页码，从 1 开始
     */
    public Integer getPageNum() {
        return pageNum;
    }

    /**
     * 设置页码
     *
     * @param pageNum 页码，不能为 null
     */
    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    /**
     * 获取每页大小
     *
     * @return 每页记录数，最大值为 100
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * 设置每页大小
     *
     * @param pageSize 每页记录数，不能为 null 且不能大于 100
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
