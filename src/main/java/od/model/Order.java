package od.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class Order {
    private Integer id;
    private String txnId;
    private LocalDateTime orderDate;
    private List<OrderItem> items = new ArrayList<>();

    public Order(Integer id, String txnId, LocalDateTime orderDate) {
        this.id = id; this.txnId = txnId; this.orderDate = orderDate;
    }

    public Integer getId() { return id; }
    public String getTxnId() { return txnId; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public List<OrderItem> getItems() { return items; }
    public void addItem(OrderItem item) { items.add(item); }

    public BigDecimal getTotal() {
        return items.stream()
            .map(OrderItem::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
