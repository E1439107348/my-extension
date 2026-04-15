package org.example.myextension.model.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * 订单商品 SKU 属性数据传输对象（DTO）
 * <p>
 * 用于封装订单中商品的 SKU（库存单位）属性信息，包括属性名称（k）、属性 ID（k_id）、属性值（v）和属性值 ID（v_id）。
 * 此对象通常用于从订单系统获取商品 SKU 属性数据，或向外部系统传递订单商品属性信息。
 * <p>
 * <b>字段说明：</b>
 * <ul>
 *   <li>{@code k}: 属性名称（key），如"颜色"、"尺寸"等</li>
 *   <li>{@code k_id}: 属性名称 ID（key_id），用于唯一标识属性</li>
 *   <li>{@code v}: 属性值（value），如"红色"、"XL"等</li>
 *   <li>{@code v_id}: 属性值 ID（value_id），用于唯一标识属性值</li>
 * </ul>
 * <p>
 * <b>与 ItemSkuPropDTO 的区别：</b>
 * <ul>
 *   <li>此对象使用 k/k_id/v/v_id 字段名，兼容订单系统常用的字段命名方式</li>
 *   <li>使用了 {@code @JSONField} 和 {@code @JsonProperty} 注解，确保在不同 JSON 序列化框架下都能正确映射字段名</li>
 *   <li>此对象专门用于订单场景，而 {@code ItemSkuPropDTO} 适用于通用商品场景</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *   <li>订单详情展示：显示订单中商品 SKU 的属性明细</li>
 *   <li>订单导出：将订单商品属性信息导出到其他系统</li>
 *   <li>订单同步：与外部系统同步订单商品属性数据</li>
 * </ul>
 *
 * @see ItemSkuPropDTO 通用商品 SKU 属性 DTO
 */
public class OrderItemSkuPropDTO implements Serializable {
    /**
     * 属性名称（key）
     * <p>
     * SKU 属性的名称，如"颜色"、"尺寸"等。
     * 使用单字母 "k" 是为了兼容订单系统的字段命名习惯。
     */
    private String k;

    /**
     * 属性名称 ID（key_id）
     * <p>
     * 用于唯一标识属性名称，通常对应系统中的属性主键。
     * 使用 {@code @JSONField(name = "k_id")} 和 {@code @JsonProperty("k_id")} 双重注解，
     * 确保在不同 JSON 序列化框架下都能正确映射为下划线命名。
     */
    @JSONField(name = "k_id")
    @JsonProperty("k_id")
    private Long kId;

    /**
     * 属性值（value）
     * <p>
     * SKU 属性对应的具体值，如"红色"、"XL"等。
     * 使用单字母 "v" 是为了兼容订单系统的字段命名习惯。
     */
    private String v;

    /**
     * 属性值 ID（value_id）
     * <p>
     * 用于唯一标识属性值，通常对应系统中的属性值主键。
     * 使用 {@code @JSONField(name = "v_id")} 和 {@code @JsonProperty("v_id")} 双重注解，
     * 确保在不同 JSON 序列化框架下都能正确映射为下划线命名。
     */
    @JSONField(name = "v_id")
    @JsonProperty("v_id")
    private Long vId;

    public String getK() {
        return k;
    }

    public void setK(String k) {
        this.k = k;
    }

    public Long getKId() {
        return kId;
    }

    public void setKId(Long kId) {
        this.kId = kId;
    }

    public String getV() {
        return v;
    }

    public void setV(String v) {
        this.v = v;
    }

    public Long getVId() {
        return vId;
    }

    public void setVId(Long vId) {
        this.vId = vId;
    }
}
