package od.ui;

import od.dao.MenuItemDAO;
import od.dao.OrderDAO;
import od.model.*;
import od.model.MenuItem;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class MainFrame extends JFrame {
    private final MenuItemDAO menuDAO = new MenuItemDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    private JComboBox<String> categoryFilter;
    private JTextField searchField;
    private JComboBox<String> icingBox;
    private JComboBox<String> fillingBox;
    private JSpinner qtySpinner;
    private JLabel unitLabel;
    private JList<MenuItem> menuList;
    private DefaultListModel<MenuItem> menuListModel;

    private JTable orderTable;
    private DefaultTableModel orderModel;
    private JLabel subtotalLabel, taxLabel, totalLabel;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.06");

    public MainFrame() {
        super("Oak Donuts – Ordering System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(950, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        setJMenuBar(createMenuBar());
        add(createLeftPanel(), BorderLayout.WEST);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createRightPanel(), BorderLayout.EAST);

        refreshMenuList();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu categoryMenu = new JMenu("Category");
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(fileMenu);
        menuBar.add(categoryMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JPanel createLeftPanel() {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Oak Donuts");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        left.add(title);

        left.add(Box.createVerticalStrut(10));

        // Filters
        JPanel filters = new JPanel(new GridLayout(4, 1, 5, 5));
        filters.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Filters", TitledBorder.LEFT, TitledBorder.TOP));

        filters.add(new JLabel("Category:"));
        categoryFilter = new JComboBox<>(new String[]{"All", "Donut", "Drink"});
        filters.add(categoryFilter);

        filters.add(new JLabel("Search:"));
        searchField = new JTextField();
        filters.add(searchField);

        left.add(filters);
        left.add(Box.createVerticalStrut(10));

        // Item Options
        JPanel options = new JPanel(new GridLayout(4, 1, 5, 5));
        options.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Item Options", TitledBorder.LEFT, TitledBorder.TOP));

        options.add(new JLabel("Icing:"));
        icingBox = new JComboBox<>(new String[]{"None", "Chocolate", "Vanilla", "Strawberry"});
        options.add(icingBox);

        options.add(new JLabel("Filling:"));
        fillingBox = new JComboBox<>(new String[]{"None", "Custard", "Jelly", "Cream"});
        options.add(fillingBox);

        left.add(options);
        left.add(Box.createVerticalStrut(10));

        // Qty + Add
        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        qtyPanel.add(new JLabel("Qty:"));
        qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        qtyPanel.add(qtySpinner);

        unitLabel = new JLabel("Unit: $0.00");
        qtyPanel.add(unitLabel);

        JButton addButton = new JButton("Add to Order");
        addButton.addActionListener(e -> addToOrder());
        qtyPanel.add(addButton);

        left.add(qtyPanel);

        return left;
    }

    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(5, 5));
        center.setBorder(BorderFactory.createTitledBorder("Menu"));

        menuListModel = new DefaultListModel<>();
        menuList = new JList<>(menuListModel);
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuList.addListSelectionListener(e -> updateUnitLabel());
        center.add(new JScrollPane(menuList), BorderLayout.CENTER);

        return center;
    }

    private JPanel createRightPanel() {
        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setBorder(BorderFactory.createTitledBorder("Order"));

        orderModel = new DefaultTableModel(new Object[]{"Item", "Options", "Qty", "Price", "Total"}, 0);
        orderTable = new JTable(orderModel);
        right.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        // Totals + Buttons
        JPanel bottom = new JPanel(new BorderLayout());

        JPanel totalsPanel = new JPanel(new GridLayout(3, 2));
        totalsPanel.add(new JLabel("Subtotal:"));
        subtotalLabel = new JLabel("$0.00");
        totalsPanel.add(subtotalLabel);
        totalsPanel.add(new JLabel("Tax (6%):"));
        taxLabel = new JLabel("$0.00");
        totalsPanel.add(taxLabel);
        totalsPanel.add(new JLabel("Total:"));
        totalLabel = new JLabel("$0.00");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        totalsPanel.add(totalLabel);
        bottom.add(totalsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearOrder());
        JButton checkoutButton = new JButton("Checkout");
        checkoutButton.addActionListener(e -> checkout());
        buttonPanel.add(clearButton);
        buttonPanel.add(checkoutButton);
        bottom.add(buttonPanel, BorderLayout.SOUTH);

        right.add(bottom, BorderLayout.SOUTH);

        return right;
    }

    private void refreshMenuList() {
        try {
            menuListModel.clear();
            List<MenuItem> items = menuDAO.findAll();
            for (MenuItem m : items) {
                if (m.isActive()) {
                    menuListModel.addElement(m);
                }
            }
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void updateUnitLabel() {
        MenuItem selected = menuList.getSelectedValue();
        if (selected != null) {
            unitLabel.setText("Unit: $" + selected.getPrice());
        }
    }

    private void addToOrder() {
        MenuItem selected = menuList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select an item from the menu.");
            return;
        }

        int qty = (int) qtySpinner.getValue();
        String icing = (String) icingBox.getSelectedItem();
        String filling = (String) fillingBox.getSelectedItem();

        String options = "";
        if (!"None".equals(icing)) options += "Icing: " + icing;
        if (!"None".equals(filling)) {
            if (!options.isEmpty()) options += ", ";
            options += "Filling: " + filling;
        }
        if (options.isEmpty()) options = "-";

        BigDecimal price = selected.getPrice();
        BigDecimal total = price.multiply(BigDecimal.valueOf(qty));

        orderModel.addRow(new Object[]{selected, options, qty, price, total});
        updateTotals();
    }

    private void updateTotals() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < orderModel.getRowCount(); i++) {
            subtotal = subtotal.add((BigDecimal) orderModel.getValueAt(i, 4));
        }
        BigDecimal tax = subtotal.multiply(TAX_RATE);
        BigDecimal total = subtotal.add(tax);

        subtotalLabel.setText(String.format("$%.2f", subtotal));
        taxLabel.setText(String.format("$%.2f", tax));
        totalLabel.setText(String.format("$%.2f", total));
    }

    private void clearOrder() {
        orderModel.setRowCount(0);
        updateTotals();
    }

    private void checkout() {
        if (orderModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Your order is empty.");
            return;
        }

        BigDecimal subtotal = new BigDecimal(subtotalLabel.getText().replace("$", ""));
        BigDecimal tax = new BigDecimal(taxLabel.getText().replace("$", ""));
        BigDecimal total = new BigDecimal(totalLabel.getText().replace("$", ""));

        try {
            String txn = OrderDAO.generateTxnId();
            Order order = new Order(null, txn, LocalDateTime.now());

            for (int i = 0; i < orderModel.getRowCount(); i++) {
                MenuItem m = (MenuItem) orderModel.getValueAt(i, 0);
                int qty = (int) orderModel.getValueAt(i, 2);
                BigDecimal price = (BigDecimal) orderModel.getValueAt(i, 3);
                order.addItem(new OrderItem(null, null, m, qty, price));
            }

            orderDAO.createNew(order);

            JOptionPane.showMessageDialog(this,
                    "Thank you!\nTotal due: $" + total + "\n(This is a mockup – no payment processed.)",
                    "Checkout", JOptionPane.INFORMATION_MESSAGE);

            clearOrder();

        } catch (Exception e) {
            showError(e);
        }
    }

    private void showError(Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
