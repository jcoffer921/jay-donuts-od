package od.util;

import od.model.Order;
import od.model.OrderItem;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for generating a text-based receipt.
 * Converts the Order object + its items into a formatted String
 * that can be displayed in a dialog or printed.
 */
public class Receipt {

    // Tax rate used for display calculations (should match system-wide)
    private static final BigDecimal TAX_RATE = new BigDecimal("0.06");

    /**
     * Builds a formatted receipt string for the given Order.
     * Includes:
     *  - Header
     *  - Transaction info
     *  - Line items
     *  - Subtotal, tax, and total
     */
    public static String generate(Order order) {
        StringBuilder sb = new StringBuilder();

        // --- Header ---
        sb.append("=========== OAK DONUTS RECEIPT ===========\n");

        // Transaction details
        sb.append("Transaction ID: ").append(order.getTxnId()).append("\n");
        sb.append("Date: ").append(order.getOrderDate()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .append("\n\n");

        // --- Line Items ---
        sb.append("Items:\n");

        for (OrderItem item : order.getItems()) {
            sb.append(" - ")
                    .append(item.getMenuItem().getName())     // Item name
                    .append(" x ").append(item.getQty())       // Quantity
                    .append(" @ $").append(format(item.getUnitPrice())) // Price per item
                    .append(" = $").append(format(item.getLineTotal())) // Line total
                    .append("\n");
        }

        // --- Totals Section ---
        BigDecimal subtotal = order.getTotal();
        BigDecimal tax = subtotal.multiply(TAX_RATE);
        BigDecimal total = subtotal.add(tax);

        sb.append("\nSubtotal: $").append(format(subtotal));
        sb.append("\nTax (6%): $").append(format(tax));
        sb.append("\nTOTAL: $").append(format(total));

        sb.append("\n==========================================\n");

        return sb.toString();
    }

    /**
     * Formats BigDecimal values as currency (2 decimal places).
     */
    private static String format(BigDecimal value) {
        return value.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }
}
