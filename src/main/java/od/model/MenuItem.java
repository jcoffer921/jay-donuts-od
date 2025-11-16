package od.model;

import java.math.BigDecimal;

/**
 * Represents a single menu item in the donut shop.
 * Examples: "Glazed Donut", "Iced Coffee (M)", etc.
 */
public class MenuItem {

    // Unique ID from the database (null if not saved yet)
    private Integer id;

    // Name of the item (e.g., "Chocolate Frosted")
    private String name;

    // Category (e.g., "Donut", "Drink")
    private String category;

    // Price stored as BigDecimal for accuracy with money
    private BigDecimal price;

    // Whether the item is active/visible on the menu
    private boolean active;

    /**
     * Full constructor (usually used when loading from the database).
     */
    public MenuItem(Integer id, String name, String category, BigDecimal price, boolean active) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.active = active;
    }

    /**
     * Constructor used before inserting into the database (id = null).
     */
    public MenuItem(String name, String category, BigDecimal price, boolean active) {
        this(null, name, category, price, active);
    }

    // --- Getters and Setters ---

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

    /**
     * Used when displaying items in dropdowns or lists.
     * Example: "Glazed Donut ($1.49)"
     */
    @Override
    public String toString() {
        return name + " ($" + price + ")";
    }
}
