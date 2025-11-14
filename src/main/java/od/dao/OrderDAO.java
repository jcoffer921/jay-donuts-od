package od.dao;

import od.db.Database;
import od.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

public class OrderDAO {
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();

    public Order createNew(Order order) throws SQLException {
        try {
            Database.get().setAutoCommit(false);

            // Insert order header
            try (PreparedStatement ps = Database.get().prepareStatement(
                    "INSERT INTO ORDERS (TXN_ID, ORDER_DATE) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, order.getTxnId());
                ps.setTimestamp(2, Timestamp.valueOf(order.getOrderDate()));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int orderId = keys.getInt(1);
                        // Insert lines
                        try (PreparedStatement line = Database.get().prepareStatement(
                                "INSERT INTO ORDER_ITEMS (ORDER_ID, MENU_ITEM_ID, QTY, UNIT_PRICE) VALUES (?,?,?,?)")) {
                            for (OrderItem oi : order.getItems()) {
                                line.setInt(1, orderId);
                                line.setInt(2, oi.getMenuItem().getId());
                                line.setInt(3, oi.getQty());
                                line.setBigDecimal(4, oi.getUnitPrice());
                                line.addBatch();
                            }
                            line.executeBatch();
                        }
                    }
                }
            }

            Database.get().commit();
            return findByTxn(order.getTxnId());
        } catch (SQLException e) {
            Database.get().rollback();
            throw e;
        } finally {
            Database.get().setAutoCommit(true);
        }
    }

    public Order findByTxn(String txnId) throws SQLException {
        Order order = null;
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
                } else return null;
            }
        }
        // load items
        try (PreparedStatement ps = Database.get().prepareStatement("""
            SELECT OI.ID, OI.QTY, OI.UNIT_PRICE, MI.ID AS MI_ID
            FROM ORDER_ITEMS OI
            JOIN MENU_ITEMS MI ON MI.ID = OI.MENU_ITEM_ID
            WHERE OI.ORDER_ID=?
        """)) {
            ps.setInt(1, order.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MenuItem mi = menuItemDAO.findById(rs.getInt("MI_ID"));
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

    public void deleteByTxn(String txnId) throws SQLException {
        try {
            Database.get().setAutoCommit(false);
            Integer orderId = null;

            try (PreparedStatement ps = Database.get().prepareStatement(
                    "SELECT ID FROM ORDERS WHERE TXN_ID=?")) {
                ps.setString(1, txnId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) orderId = rs.getInt(1);
                }
            }
            if (orderId == null) { Database.get().rollback(); return; }

            try (PreparedStatement ps = Database.get().prepareStatement(
                    "DELETE FROM ORDER_ITEMS WHERE ORDER_ID=?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }
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

    public static String generateTxnId() {
        // Simple: YYYYMMDD-HHMMSS-rand4
        LocalDateTime now = LocalDateTime.now();
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.format("%04d%02d%02d-%02d%02d%02d-%04d",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                now.getHour(), now.getMinute(), now.getSecond(), rand);
    }
}
