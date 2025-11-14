package od.dao;

import od.db.Database;
import od.model.MenuItem;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

public class MenuItemDAO {
    public List<MenuItem> findAll() throws SQLException {
        List<MenuItem> out = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(
                "SELECT ID, NAME, CATEGORY, PRICE, ACTIVE FROM MENU_ITEMS ORDER BY CATEGORY, NAME");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        }
        return out;
    }

    public MenuItem insert(MenuItem m) throws SQLException {
        try (PreparedStatement ps = Database.get().prepareStatement(
                "INSERT INTO MENU_ITEMS (NAME, CATEGORY, PRICE, ACTIVE) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getCategory());
            ps.setBigDecimal(3, m.getPrice());
            ps.setBoolean(4, m.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) m.setId(keys.getInt(1));
            }
        }
        return m;
    }

    public void update(MenuItem m) throws SQLException {
        if (m.getId() == null) throw new IllegalArgumentException("MenuItem id is null");
        try (PreparedStatement ps = Database.get().prepareStatement(
                "UPDATE MENU_ITEMS SET NAME=?, CATEGORY=?, PRICE=?, ACTIVE=? WHERE ID=?")) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getCategory());
            ps.setBigDecimal(3, m.getPrice());
            ps.setBoolean(4, m.isActive());
            ps.setInt(5, m.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = Database.get().prepareStatement(
                "DELETE FROM MENU_ITEMS WHERE ID=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public MenuItem findById(int id) throws SQLException {
        try (PreparedStatement ps = Database.get().prepareStatement(
                "SELECT ID, NAME, CATEGORY, PRICE, ACTIVE FROM MENU_ITEMS WHERE ID=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    private MenuItem map(ResultSet rs) throws SQLException {
        return new MenuItem(
            rs.getInt("ID"),
            rs.getString("NAME"),
            rs.getString("CATEGORY"),
            rs.getBigDecimal("PRICE"),
            rs.getBoolean("ACTIVE")
        );
    }
}
