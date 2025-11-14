package od.db;

import java.sql.*;

public class Database {
    private static final String DB_PATH = "jdbc:derby:db/oakdonutsdb;create=true";
    private static Connection conn;

    public static Connection get() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(DB_PATH);
            ensureSchema();
        }
        return conn;
    }

    private static void ensureSchema() throws SQLException {
        try (Statement st = get().createStatement()) {
            // MENU_ITEMS: CRUD target for products
            st.executeUpdate("""
                CREATE TABLE MENU_ITEMS (
                    ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    NAME VARCHAR(100) NOT NULL,
                    CATEGORY VARCHAR(50) NOT NULL,
                    PRICE DECIMAL(10,2) NOT NULL,
                    ACTIVE BOOLEAN NOT NULL DEFAULT TRUE
                )
            """);
        } catch (SQLException ignore) { /* already exists */ }

        try (Statement st = get().createStatement()) {
            // ORDERS: one per transaction
            st.executeUpdate("""
                CREATE TABLE ORDERS (
                    ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    TXN_ID VARCHAR(40) NOT NULL UNIQUE,
                    ORDER_DATE TIMESTAMP NOT NULL
                )
            """);
        } catch (SQLException ignore) { /* already exists */ }

        try (Statement st = get().createStatement()) {
            // ORDER_ITEMS: details (line items)
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
        } catch (SQLException ignore) { /* already exists */ }

        seedMenuIfEmpty();
    }

    private static void seedMenuIfEmpty() throws SQLException {
        try (Statement st = get().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM MENU_ITEMS")) {
            rs.next();
            if (rs.getInt(1) == 0) {
                try (PreparedStatement ps = get().prepareStatement(
                        "INSERT INTO MENU_ITEMS (NAME, CATEGORY, PRICE, ACTIVE) VALUES (?,?,?,TRUE)")) {
                    Object[][] seed = {
                        {"Glazed Donut", "Donut", 1.49},
                        {"Chocolate Frosted", "Donut", 1.79},
                        {"Boston Cream", "Donut", 2.29},
                        {"Iced Coffee (M)", "Drink", 2.49},
                        {"Hot Coffee (M)", "Drink", 1.99}
                    };
                    for (Object[] row : seed) {
                        ps.setString(1, (String) row[0]);
                        ps.setString(2, (String) row[1]);
                        ps.setBigDecimal(3, new java.math.BigDecimal(row[2].toString()));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
        } catch (SQLException e) {
            // If MENU_ITEMS doesn't exist yet, ignore; ensureSchema will create it on next call
        }
    }
}
