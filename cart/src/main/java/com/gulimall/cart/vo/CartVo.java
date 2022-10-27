package com.gulimall.cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车（包含多个购物项）
 */
@Data
public class CartVo {
    private List<CartItemVo> items;
    private Integer countNum; //商品件数
    private Integer countType; //商品种数
    private BigDecimal totalAmount; //总计价格
    private BigDecimal reduce = new BigDecimal("0.00"); //优惠减免

    public List<CartItemVo> getItems() {
        return items;
    }

    public void setItems(List<CartItemVo> items) {
        this.items = items;
    }

    /**
     * @return 计算总数
     */
    public Integer getCountNum() {
        int count = 0;
        if (items != null && items.size() > 0) {
            for (CartItemVo item : items) {
                if (item.getCheck() == true) {
                    count += item.getCount();
                }
            }
        }
        return count;
    }

    /**
     * @return 计算商品种类
     */
    public Integer getCountType() {
        int count = 0;
        if (items != null && items.size() > 0) {
            for (CartItemVo item : items) {
                if (item.getCheck() == true) {
                    count += 1;
                }
            }
        }
        return count;
    }

    /**
     * @return 计算总价
     */
    public BigDecimal getTotalAmount() {
        BigDecimal price = new BigDecimal("0");
        if (items != null && items.size() > 0) {
            for (CartItemVo item : items) {
                if (item.getCheck() == true) {
                    price = price.add(item.getTotalPrice());
                }
            }
        }
        return price.subtract(getReduce());
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
