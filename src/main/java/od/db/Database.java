package od.db;

import java.sql.*;

public class Database {

    // Path to the Derby embedded database
    // "create=true" means it will be created automatically if it doesn't exist
    private static final String DB_PATH = "jdbc:derby:db/oakdonutsdb;create=true";

    // Single shared connection instance (simple singleton approach)
    private static Connection conn;

    /**
     * Returns a database connection.
     * If none exists yet, or if the previous one was closed,
     * it opens a new connection and ensures the schema/tables exist.
     */
    public static Connection get() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(DB_PATH);
            ensureSchema(); // Create tables if this is the first run
        }
        return conn;
    }

    /**
     * Ensures all required tables exist.
     */
    private static void ensureSchema() throws SQLException {

        // MENU_ITEMS table — holds all products sold in the shop
        try (Statement st = get().createStatement()) {
            st.executeUpdate("""
                CREATE TABLE MENU_ITEMS (
                    ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    NAME VARCHAR(100) NOT NULL,
                    CATEGORY VARCHAR(50) NOT NULL,
                    PRICE DECIMAL(10,2) NOT NULL,
                    ACTIVE BOOLEAN NOT NULL DEFAULT TRUE
                )
            """);
        } catch (SQLException ignore) {}

        // ORDERS table — header
        try (Statement st = get().createStatement()) {
            st.executeUpdate("""
                CREATE TABLE ORDERS (
                    ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    TXN_ID VARCHAR(40) NOT NULL UNIQUE,
                    ORDER_DATE TIMESTAMP NOT NULL
                )
            """);
        } catch (SQLException ignore) {}

        // ORDER_ITEMS table — line items
        try (Statement st = get().createStatement()) {
            st.executeUpdate("""
                CREATE TABLE ORDER_ITEMS (
                    ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    ORDER_ID INTEGER NOT NULL,
                    MENU_ITEM_ID INTEGER NOT NULL,
                    QTY INTEGER NOT NULL,
                    UNIT_PRICE DECIMAL(10,2) NOT NULL,
                    FOREIGN KEY (ORDER_ID) REFERENCES ORDERS(ID),
                    FOREIGN KEY (MENU_ITEM_ID) REFERENCES MENU_ITEMS(ID)
                )
            """);
        } catch (SQLException ignore) {}

        // Insert items if missing
        seedMenuItems();
    }

    /**
     * Seeds menu items, but ONLY inserts items that do NOT already exist.
     * This avoids duplicates and allows updating the seed list anytime.
     */
    private static void seedMenuItems() throws SQLException {

        Object[][] seed = {
                // --- Classic Donuts ---
                {"Glazed Donut", "Donut", 1.49},
                {"Chocolate Frosted", "Donut", 1.79},
                {"Boston Cream", "Donut", 2.29},
                {"Strawberry Frosted", "Donut", 1.89},
                {"Powdered Sugar Donut", "Donut", 1.59},
                {"Old Fashioned Donut", "Donut", 1.69},
                {"Blueberry Donut", "Donut", 1.99},

                // --- Premium Donuts ---
                {"Oreo Crumble Donut", "Donut", 2.79},
                {"Red Velvet Donut", "Donut", 2.49},
                {"Cinnamon Twist", "Donut", 2.19},

                // --- Drinks (M removed) ---
                {"Iced Coffee", "Drink", 2.49},
                {"Hot Coffee", "Drink", 1.99},
                {"Hot Chocolate", "Drink", 3.49},
                {"Latte", "Drink", 3.99},
                {"Cappuccino", "Drink", 3.69},
                {"Caramel Iced Latte", "Drink", 4.29},
                {"Chai Tea Latte", "Drink", 3.79},

                // --- Cold Drinks ---
                {"Bottled Water", "Drink", 1.25},
                {"Orange Juice", "Drink", 2.29},
                {"Milk", "Drink", 1.49}
        };


        for (Object[] row : seed) {
            try (PreparedStatement ps = get().prepareStatement("""
                INSERT INTO MENU_ITEMS (NAME, CATEGORY, PRICE, ACTIVE)
                SELECT ?, ?, ?, TRUE
                FROM SYSIBM.SYSDUMMY1
                WHERE NOT EXISTS (
                    SELECT 1 FROM MENU_ITEMS WHERE NAME = ?
                )
            """)) {

                ps.setString(1, (String) row[0]); // NAME
                ps.setString(2, (String) row[1]); // CATEGORY
                ps.setBigDecimal(3, new java.math.BigDecimal(row[2].toString())); // PRICE
                ps.setString(4, (String) row[0]); // NAME check

                ps.executeUpdate();
            }
        }
    }
}
