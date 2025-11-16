package od.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * Represents a full customer order (one transaction).
 * Contains:
 *  - Order header info (txnId, date)
 *  - A list of line items (OrderItem objects)
 */
public class Order {

    // Primary key from the database (null until inserted)
    private Integer id;

    // Human-readable transaction ID (e.g., "20251116-142530-4821")
    private String txnId;

    // Date + time the order was created
    private LocalDateTime orderDate;

    // All line items that belong to this order
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Full constructor — used when loading from the database.
     */
    public Order(Integer id, String txnId, LocalDateTime orderDate) {
        this.id = id;
        this.txnId = txnId;
        this.orderDate = orderDate;
    }

    // --- Getters ---

    public Integer getId() { return id; }
    public String getTxnId() { return txnId; }
    public LocalDateTime getOrderDate() { return orderDate; }

    /**
     * Returns the list of items for this order.
     * The DAO will populate this list after loading the order.
     */
    public List<OrderItem> getItems() { return items; }

    /**
     * Adds a single line item to the order.
     */
    public void addItem(OrderItem item) {
        items.add(item);
    }

    /**
     * Computes the total cost of the order.
     * Sums: (unit price * qty) for every line item.
     */
    public BigDecimal getTotal() {
        return items.stream()
                .map(OrderItem::getLineTotal)       // convert each item -> its total
                .reduce(BigDecimal.ZERO, BigDecimal::add); // add all item totals together
    }
}
