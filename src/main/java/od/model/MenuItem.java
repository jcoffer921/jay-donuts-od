package od.model;

import java.math.BigDecimal;

public class MenuItem {
    private Integer id;
    private String name;
    private String category;
    private BigDecimal price;
    private boolean active;

    public MenuItem(Integer id, String name, String category, BigDecimal price, boolean active) {
        this.id = id; this.name = name; this.category = category; this.price = price; this.active = active;
    }
    public MenuItem(String name, String category, BigDecimal price, boolean active) {
        this(null, name, category, price, active);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public boolean isActive() { return active; }
    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setActive(boolean active) { this.active = active; }

    @Override public String toString() { return name + " ($" + price + ")"; }
}
