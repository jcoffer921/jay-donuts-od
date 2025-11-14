package od.model;

import java.math.BigDecimal;

public class OrderItem {
    private Integer id;
    private Integer orderId;
    private MenuItem menuItem;
    private int qty;
    private BigDecimal unitPrice;

    public OrderItem(Integer id, Integer orderId, MenuItem menuItem, int qty, BigDecimal unitPrice) {
        this.id = id; this.orderId = orderId; this.menuItem = menuItem; this.qty = qty; this.unitPrice = unitPrice;
    }
    public Integer getId() { return id; }
    public Integer getOrderId() { return orderId; }
    public MenuItem getMenuItem() { return menuItem; }
    public int getQty() { return qty; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineTotal() { return unitPrice.multiply(BigDecimal.valueOf(qty)); }
}
