package od.model;

import java.math.BigDecimal;

/**
 * Represents a single line item inside an order.
 * Example: 2 × "Glazed Donut" @ $1.49 each
 */
public class OrderItem {

    // Primary key in ORDER_ITEMS table (null until inserted)
    private Integer id;

    // Foreign key pointing to the parent order (ORDERS.ID)
    private Integer orderId;

    // The actual menu item being purchased
    private MenuItem menuItem;

    // Quantity ordered (e.g., 2 donuts)
    private int qty;

    // The price per item at the time of purchase
    // (stored separately in case menu prices change later)
    private BigDecimal unitPrice;

    /**
     * Full constructor used when loading from or inserting into the database.
     */
    public OrderItem(Integer id, Integer orderId, MenuItem menuItem, int qty, BigDecimal unitPrice) {
        this.id = id;
        this.orderId = orderId;
        this.menuItem = menuItem;
        this.qty = qty;
        this.unitPrice = unitPrice;
    }

    // --- Getters ---

    public Integer getId() { return id; }
    public Integer getOrderId() { return orderId; }
    public MenuItem getMenuItem() { return menuItem; }
    public int getQty() { return qty; }
    public BigDecimal getUnitPrice() { return unitPrice; }

    /**
     * Returns the total price for this line:
     *     unitPrice × qty
     */
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(qty));
    }
}
