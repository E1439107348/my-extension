package org.example.myextension.model.dto;

import java.io.Serializable;

/**
 * 商品 SKU 属性数据传输对象（DTO）
 * <p>
 * 用于封装商品的 SKU（库存单位）属性信息，包括属性名称、属性 ID、属性值名称和属性值 ID。
 * 此对象通常用于在不同层之间传递商品 SKU 属性数据，如从服务层返回给控制器层。
 * <p>
 * <b>字段说明：</b>
 * <ul>
 *   <li>{@code propName}: 属性名称，如"颜色"、"尺寸"等</li>
 *   <li>{@code propNameId}: 属性名称 ID，用于唯一标识属性</li>
 *   <li>{@code propValueName}: 属性值名称，如"红色"、"XL"等</li>
 *   <li>{@code propValueId}: 属性值 ID，用于唯一标识属性值</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *   <li>商品详情展示：返回商品 SKU 的可选属性</li>
 *   <li>订单商品信息：订单中商品 SKU 的属性明细</li>
 *   <li>SKU 组合管理：管理不同属性组合形成的 SKU</li>
 * </ul>
 *
 * @see OrderItemSkuPropDTO 订单商品 SKU 属性 DTO
 */
public class ItemSkuPropDTO implements Serializable {
    /**
     * 属性名称
     * <p>
     * SKU 属性的名称，如"颜色"、"尺寸"、"材质"等。
     */
    private String propName;

    /**
     * 属性名称 ID
     * <p>
     * 用于唯一标识属性名称，通常对应系统中的属性主键。
     */
    private String propNameId;

    /**
     * 属性值名称
     * <p>
     * SKU 属性对应的具体值，如"红色"、"XL"、"棉麻"等。
     */
    private String propValueName;

    /**
     * 属性值 ID
     * <p>
     * 用于唯一标识属性值，通常对应系统中的属性值主键。
     */
    private String propValueId;

    public String getPropName() {
        return propName;
    }

    public void setPropName(String propName) {
        this.propName = propName;
    }

    public String getPropNameId() {
        return propNameId;
    }

    public void setPropNameId(String propNameId) {
        this.propNameId = propNameId;
    }

    public String getPropValueName() {
        return propValueName;
    }

    public void setPropValueName(String propValueName) {
        this.propValueName = propValueName;
    }

    public String getPropValueId() {
        return propValueId;
    }

    public void setPropValueId(String propValueId) {
        this.propValueId = propValueId;
    }
}
