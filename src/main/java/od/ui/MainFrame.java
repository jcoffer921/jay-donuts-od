package od.ui;

import od.dao.MenuItemDAO;
import od.dao.OrderDAO;
import od.model.*;
import od.model.MenuItem;
import od.util.Receipt;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Main application window for Oak Donuts ordering system.
 * Contains:
 *  - Menu list
 *  - Filters
 *  - Item customization
 *  - Order summary
 *  - Checkout workflow
 */
public class MainFrame extends JFrame {

    // --- DAOs for DB operations ---
    private final MenuItemDAO menuDAO = new MenuItemDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    // --- UI components for filters and options ---
    private JComboBox<String> categoryFilter;
    private JTextField searchField;
    private JComboBox<String> sizeBox;
    private JComboBox<String> icingBox;
    private JComboBox<String> fillingBox;
    private JSpinner qtySpinner;
    private JLabel unitLabel;
    private JList<MenuItem> menuList;
    private DefaultListModel<MenuItem> menuListModel;

    // --- Right-panel order table ---
    private JTable orderTable;
    private DefaultTableModel orderModel;
    private JLabel subtotalLabel, taxLabel, totalLabel;

    // Tax rate constant (6%)
    private static final BigDecimal TAX_RATE = new BigDecimal("0.06");
    private static final BigDecimal SMALL_MULT = new BigDecimal("0.90");
    private static final BigDecimal MEDIUM_MULT = new BigDecimal("1.00");
    private static final BigDecimal LARGE_MULT = new BigDecimal("1.20");


    /**
     * Constructs the main JFrame and builds the entire UI.
     */
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

        // Load menu items from DB
        refreshMenuList();
    }


    /** Top application menu bar (File, Category, Help). */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new JMenu("File"));
        menuBar.add(new JMenu("Category"));
        menuBar.add(new JMenu("Help"));
        return menuBar;
    }

    /** Builds left panel: Filters, options, and add-to-order section. */
    private JPanel createLeftPanel() {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel title = new JLabel("Oak Donuts");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        left.add(title);
        left.add(Box.createVerticalStrut(10));

        // --- Filters section ---
        JPanel filters = new JPanel(new GridLayout(6, 1, 5, 5));
        filters.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Filters", TitledBorder.LEFT, TitledBorder.TOP));

        filters.add(new JLabel("Category:"));
        categoryFilter = new JComboBox<>(new String[]{"All", "Donut", "Drink"});
        filters.add(categoryFilter);

        filters.add(new JLabel("Search:"));
        searchField = new JTextField();
        filters.add(searchField);

        filters.add(new JLabel("Size:"));
        sizeBox = new JComboBox<>(new String[]{"Small", "Medium", "Large"});
        filters.add(sizeBox);

        // Update unit price when size changes
        sizeBox.addActionListener(e -> updateUnitLabel());

        left.add(filters);
        left.add(Box.createVerticalStrut(10));

        // --- Item customization options ---
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

        // --- Quantity + Add to order ---
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

    /** Center panel: Displays menu items in a scrollable list. */
    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(5, 5));
        center.setBorder(BorderFactory.createTitledBorder("Menu"));

        menuListModel = new DefaultListModel<>();
        menuList = new JList<>(menuListModel);
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // When you click an item, update the unit price display
        menuList.addListSelectionListener(e -> updateUnitLabel());

        center.add(new JScrollPane(menuList), BorderLayout.CENTER);
        return center;
    }

    /** Right panel: Order table, totals, and checkout buttons. */
    private JPanel createRightPanel() {
        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setBorder(BorderFactory.createTitledBorder("Order"));

        orderModel = new DefaultTableModel(new Object[]{"Item", "Options", "Qty", "Price", "Total"}, 0);
        orderTable = new JTable(orderModel);
        right.add(new JScrollPane(orderTable), BorderLayout.CENTER);

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

    /** Loads menu items from DB into list. */
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

    /** Updates unit price when item or size changes. */
    private void updateUnitLabel() {
        MenuItem selected = menuList.getSelectedValue();
        if (selected != null) {
            BigDecimal price = applySizePrice(selected.getPrice());
            unitLabel.setText("Unit: $" + price);
        }
    }

    /** Returns item price adjusted for selected size. */
    private BigDecimal applySizePrice(BigDecimal basePrice) {
        String size = (String) sizeBox.getSelectedItem();
        return switch (size) {
            case "Small" -> basePrice.multiply(SMALL_MULT);
            case "Large" -> basePrice.multiply(LARGE_MULT);
            default -> basePrice.multiply(MEDIUM_MULT);
        };
    }

    /** Adds selected menu item to the order table. */
    private void addToOrder() {
        MenuItem selected = menuList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select an item from the menu.");
            return;
        }

        int qty = (int) qtySpinner.getValue();
        String icing = (String) icingBox.getSelectedItem();
        String filling = (String) fillingBox.getSelectedItem();
        String size = (String) sizeBox.getSelectedItem();

        // Correctly formatted options text
        String options = "Size: " + size;
        if (!"None".equals(icing)) options += ", Icing: " + icing;
        if (!"None".equals(filling)) options += ", Filling: " + filling;

        BigDecimal price = applySizePrice(selected.getPrice());
        BigDecimal total = price.multiply(BigDecimal.valueOf(qty));

        orderModel.addRow(new Object[]{selected, options, qty, price, total});
        updateTotals();
    }

    /** Recalculates subtotal, tax, total. */
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

    /** Clears the order table. */
    private void clearOrder() {
        orderModel.setRowCount(0);
        updateTotals();
    }

    /** Checkout routine: builds order, saves to DB, shows receipt. */
    private void checkout() {
        if (orderModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Your order is empty.");
            return;
        }

        BigDecimal subtotal = new BigDecimal(subtotalLabel.getText().replace("$", ""));
        BigDecimal tax      = new BigDecimal(taxLabel.getText().replace("$", ""));
        BigDecimal total    = new BigDecimal(totalLabel.getText().replace("$", ""));

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

            String receiptText = Receipt.generate(order);

            JTextArea area = new JTextArea(receiptText);
            area.setEditable(false);
            area.setFont(new Font("Monospaced", Font.PLAIN, 14));

            JScrollPane scrollPane = new JScrollPane(area);
            scrollPane.setPreferredSize(new Dimension(420, 500));

            JOptionPane.showMessageDialog(
                    this,
                    scrollPane,
                    "Receipt",
                    JOptionPane.INFORMATION_MESSAGE
            );

            clearOrder();

        } catch (Exception e) {
            showError(e);
        }
    }

    /** Displays an error message dialog. */
    private void showError(Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
