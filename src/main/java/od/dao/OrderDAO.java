package od.dao;

import od.db.Database;
import od.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

public class OrderDAO {
    // DAO used to look up menu items by ID when rebuilding an order
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();

    /**
     * Creates a brand-new order in the database.
     * Steps:
     * 1. Insert the order header (ORDERS table)
     * 2. Insert each line item (ORDER_ITEMS table)
     * 3. Wrap everything in a transaction (commit or rollback)
     */
    public Order createNew(Order order) throws SQLException {
        try {
            // Begin transaction
            Database.get().setAutoCommit(false);

            // Insert into ORDERS table
            try (PreparedStatement ps = Database.get().prepareStatement(
                    "INSERT INTO ORDERS (TXN_ID, ORDER_DATE) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, order.getTxnId());
                ps.setTimestamp(2, Timestamp.valueOf(order.getOrderDate()));
                ps.executeUpdate();

                // Grab the auto-generated ORDER ID
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int orderId = keys.getInt(1);

                        // Insert every ORDER_ITEM row for this order
                        try (PreparedStatement line = Database.get().prepareStatement(
                                "INSERT INTO ORDER_ITEMS (ORDER_ID, MENU_ITEM_ID, QTY, UNIT_PRICE) VALUES (?,?,?,?)")) {

                            for (OrderItem oi : order.getItems()) {
                                line.setInt(1, orderId);
                                line.setInt(2, oi.getMenuItem().getId());
                                line.setInt(3, oi.getQty());
                                line.setBigDecimal(4, oi.getUnitPrice());
                                line.addBatch(); // Batch insert = faster
                            }

                            line.executeBatch();
                        }
                    }
                }
            }

            // Commit transaction
            Database.get().commit();

            // Reload the finished order from DB (now it has IDs)
            return findByTxn(order.getTxnId());

        } catch (SQLException e) {
            // Something went wrong → undo everything
            Database.get().rollback();
            throw e;
        } finally {
            Database.get().setAutoCommit(true);
        }
    }

    /**
     * Finds a single order using its transaction ID (TXN_ID).
     * Steps:
     * 1. Load the order header
     * 2. Load all its items
     * 3. For each item, fetch the MenuItem from MenuItemDAO
     */
    public Order findByTxn(String txnId) throws SQLException {
        Order order = null;

        // Fetch order header
        try (PreparedStatement ps = Database.get().prepareStatement(
                "SELECT ID, TXN_ID, ORDER_DATE FROM ORDERS WHERE TXN_ID=?")) {

            ps.setString(1, txnId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    order = new Order(
                            rs.getInt("ID"),
                            rs.getString("TXN_ID"),
                            rs.getTimestamp("ORDER_DATE").toLocalDateTime()
                    );
                } else {
                    return null; // Order not found
                }
            }
        }

        // Fetch order line items
        try (PreparedStatement ps = Database.get().prepareStatement("""
            SELECT OI.ID, OI.QTY, OI.UNIT_PRICE, MI.ID AS MI_ID
            FROM ORDER_ITEMS OI
            JOIN MENU_ITEMS MI ON MI.ID = OI.MENU_ITEM_ID
            WHERE OI.ORDER_ID=?
        """)) {

            ps.setInt(1, order.getId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Look up actual MenuItem object using its ID
                    MenuItem mi = menuItemDAO.findById(rs.getInt("MI_ID"));

                    // Add to the order object
                    order.addItem(new OrderItem(
                            rs.getInt("ID"),
                            order.getId(),
                            mi,
                            rs.getInt("QTY"),
                            rs.getBigDecimal("UNIT_PRICE")
                    ));
                }
            }
        }

        return order;
    }

    /**
     * Returns a list of all orders in the system,
     * sorted with newest orders first.
     */
    public List<Order> findAll() throws SQLException {
        List<Order> out = new ArrayList<>();

        try (PreparedStatement ps = Database.get().prepareStatement(
                "SELECT ID, TXN_ID, ORDER_DATE FROM ORDERS ORDER BY ORDER_DATE DESC");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(new Order(
                        rs.getInt("ID"),
                        rs.getString("TXN_ID"),
                        rs.getTimestamp("ORDER_DATE").toLocalDateTime()
                ));
            }
        }

        return out;
    }

    /**
     * Deletes an order by its transaction ID.
     * Steps:
     * 1. Find its ORDER.ID
     * 2. Delete its line items first (ORDER_ITEMS)
     * 3. Delete the order header (ORDERS)
     * Uses a transaction so both deletes succeed or fail together.
     */
    public void deleteByTxn(String txnId) throws SQLException {
        try {
            Database.get().setAutoCommit(false);

            Integer orderId = null;

            // Look up order ID from the TXN_ID
            try (PreparedStatement ps = Database.get().prepareStatement(
                    "SELECT ID FROM ORDERS WHERE TXN_ID=?")) {

                ps.setString(1, txnId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) orderId = rs.getInt(1);
                }
            }

            // No such order → exit early
            if (orderId == null) {
                Database.get().rollback();
                return;
            }

            // Delete line items first (foreign key)
            try (PreparedStatement ps = Database.get().prepareStatement(
                    "DELETE FROM ORDER_ITEMS WHERE ORDER_ID=?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            // Delete the main order record
            try (PreparedStatement ps = Database.get().prepareStatement(
                    "DELETE FROM ORDERS WHERE ID=?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            Database.get().commit();

        } catch (SQLException e) {
            Database.get().rollback();
            throw e;
        } finally {
            Database.get().setAutoCommit(true);
        }
    }

    /**
     * Generates a unique transaction ID:
     * Format: YYYYMMDD-HHMMSS-rand4
     * Example: 20251116-154220-4821
     */
    public static String generateTxnId() {
        LocalDateTime now = LocalDateTime.now();
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);

        return String.format("%04d%02d%02d-%02d%02d%02d-%04d",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                now.getHour(), now.getMinute(), now.getSecond(),
                rand);
    }
}
