package chinook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;

/**
 * NotificationsPanel — Tab 4
 * --------------------------
 * Two sections:
 *   TOP    → Customer CRUD (Create, Read, Update, Delete)
 *   BOTTOM → Inactive Customers (searchable)
 *
 * CONCEPT 1 — CRUD:
 *   CREATE → INSERT INTO Customer VALUES (...)
 *   READ   → SELECT * FROM Customer
 *   UPDATE → UPDATE Customer SET ... WHERE CustomerId = ?
 *   DELETE → DELETE FROM Customer WHERE CustomerId = ?
 *
 *   All use PreparedStatement with ? placeholders (no SQL injection).
 *   MAX(CustomerId)+1 used for new ID (no AUTO_INCREMENT in Chinook).
 *
 * CONCEPT 2 — Inactive Customers (Advanced SQL):
 *   Uses LEFT JOIN + GROUP BY + HAVING — more advanced than WHERE.
 *
 *   LEFT JOIN keeps customers with NO invoices (they appear with NULLs).
 *   GROUP BY collapses multiple invoice rows into one row per customer.
 *   HAVING filters AFTER aggregation (WHERE filters before aggregation).
 *
 *   HAVING MAX(i.InvoiceDate) IS NULL
 *     → customer has zero invoices (all joined values are NULL)
 *   OR MAX(i.InvoiceDate) < DATE_SUB(NOW(), INTERVAL 2 YEAR)
 *     → most recent invoice is older than 2 years from today
 *
 *   DATE_SUB(NOW(), INTERVAL 2 YEAR) dynamically calculates
 *   the date exactly 2 years ago — no hardcoded dates needed.
 *
 * CONCEPT 3 — FK Constraint on DELETE:
 *   Customers link to Invoices via FK. Deleting a customer who
 *   has invoices violates referential integrity — MySQL rejects it.
 *   We catch this gracefully and show a helpful error message.
 */
public class NotificationsPanel extends JPanel {

    // CRUD section
    private DefaultTableModel customerModel;
    private JTable customerTable;
    private JLabel customerCountLabel;

    // Inactive section
    private DefaultTableModel inactiveModel;
    private JTable inactiveTable;
    private TableRowSorter<DefaultTableModel> inactiveSorter;
    private JTextField inactiveSearch;
    private JLabel inactiveCountLabel;

    private static final String[] CUSTOMER_COLS = {
        "ID", "First Name", "Last Name", "Email", "Phone", "Country"
    };
    private static final String[] INACTIVE_COLS = {
        "ID", "First Name", "Last Name", "Email", "Last Invoice Date"
    };

    public NotificationsPanel() {
        setLayout(new GridLayout(2, 1, 0, 12));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(buildCrudPanel());
        add(buildInactivePanel());

        loadCustomers();
        loadInactiveCustomers();
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 1 — CUSTOMER CRUD
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildCrudPanel() {
        JPanel outer = new JPanel(new BorderLayout(5, 8));
        outer.setBackground(Color.WHITE);
        outer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Customer Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(52, 152, 219));
        header.add(title, BorderLayout.WEST);
        customerCountLabel = new JLabel("");
        customerCountLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        customerCountLabel.setForeground(new Color(108, 117, 125));
        header.add(customerCountLabel, BorderLayout.EAST);
        outer.add(header, BorderLayout.NORTH);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        JButton createBtn = makeButton("＋ Create", new Color(39, 174, 96));
        JButton updateBtn = makeButton("✎  Update", new Color(52, 152, 219));
        JButton deleteBtn = makeButton("✕  Delete", new Color(192, 57, 43));
        JButton refreshBtn = makeButton("↻", new Color(108, 117, 125));
        btnRow.add(createBtn);
        btnRow.add(updateBtn);
        btnRow.add(deleteBtn);
        btnRow.add(refreshBtn);
        outer.add(btnRow, BorderLayout.CENTER);

        // Table
        customerModel = new DefaultTableModel(CUSTOMER_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        customerTable = new JTable(customerModel) {
            @Override public Component prepareRenderer(
                    javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 252));
                return c;
            }
        };
        styleTable(customerTable);
        outer.add(new JScrollPane(customerTable), BorderLayout.SOUTH);

        // Wire events
        createBtn.addActionListener(e -> openCustomerDialog(null));
        updateBtn.addActionListener(e -> {
            int row = customerTable.getSelectedRow();
            if (row < 0) { warn("Please select a customer to update."); return; }
            openCustomerDialog(row);
        });
        deleteBtn.addActionListener(e -> deleteCustomer());
        refreshBtn.addActionListener(e -> { loadCustomers(); loadInactiveCustomers(); });

        return outer;
    }

    /** Loads all customers from the database */
    private void loadCustomers() {
        customerModel.setRowCount(0);
        String sql = """
            SELECT CustomerId, FirstName, LastName, Email, Phone, Country
            FROM Customer
            ORDER BY LastName, FirstName
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                customerModel.addRow(new Object[]{
                    rs.getInt("CustomerId"),
                    rs.getString("FirstName"),
                    rs.getString("LastName"),
                    rs.getString("Email"),
                    rs.getString("Phone"),
                    rs.getString("Country")
                });
                count++;
            }
            customerCountLabel.setText(count + " customers  ");
        } catch (SQLException ex) { showError("Error loading customers", ex); }
    }

    /**
     * Opens Create or Update dialog.
     * selectedRow == null → Create mode (empty form)
     * selectedRow != null → Update mode (pre-filled form)
     */
    private void openCustomerDialog(Integer selectedRow) {
        boolean isUpdate = selectedRow != null;
        JDialog dlg = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            isUpdate ? "Update Customer" : "Create New Customer", true);
        dlg.setSize(420, 340);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.getContentPane().setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        JTextField firstF   = new JTextField(22);
        JTextField lastF    = new JTextField(22);
        JTextField emailF   = new JTextField(22);
        JTextField phoneF   = new JTextField(22);
        JTextField countryF = new JTextField(22);

        if (isUpdate) {
            firstF.setText(  nullSafe(customerModel.getValueAt(selectedRow, 1)));
            lastF.setText(   nullSafe(customerModel.getValueAt(selectedRow, 2)));
            emailF.setText(  nullSafe(customerModel.getValueAt(selectedRow, 3)));
            phoneF.setText(  nullSafe(customerModel.getValueAt(selectedRow, 4)));
            countryF.setText(nullSafe(customerModel.getValueAt(selectedRow, 5)));
        }

        addRow(form, gbc, 0, "First Name *:", firstF);
        addRow(form, gbc, 1, "Last Name *:",  lastF);
        addRow(form, gbc, 2, "Email *:",      emailF);
        addRow(form, gbc, 3, "Phone:",        phoneF);
        addRow(form, gbc, 4, "Country:",      countryF);
        dlg.add(form, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnRow.setBackground(new Color(248, 249, 252));
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(222, 226, 230)));
        JButton saveBtn   = makeButton("Save", new Color(39, 174, 96));
        JButton cancelBtn = makeButton("Cancel", new Color(108, 117, 125));

        saveBtn.addActionListener(e -> {
            if (firstF.getText().trim().isEmpty() || lastF.getText().trim().isEmpty()
                    || emailF.getText().trim().isEmpty()) {
                warn("First Name, Last Name and Email are required.");
                return;
            }
            if (isUpdate) {
                int id = (int) customerModel.getValueAt(selectedRow, 0);
                updateCustomer(id, firstF.getText().trim(), lastF.getText().trim(),
                    emailF.getText().trim(), phoneF.getText().trim(), countryF.getText().trim());
            } else {
                createCustomer(firstF.getText().trim(), lastF.getText().trim(),
                    emailF.getText().trim(), phoneF.getText().trim(), countryF.getText().trim());
            }
            dlg.dispose();
            loadCustomers();
            loadInactiveCustomers();
        });
        cancelBtn.addActionListener(e -> dlg.dispose());
        btnRow.add(saveBtn);
        btnRow.add(cancelBtn);
        dlg.add(btnRow, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    /**
     * CREATE — INSERT a new customer.
     *
     * SQL:
     *   INSERT INTO Customer (CustomerId, FirstName, LastName, Email, Phone, Country)
     *   VALUES (?, ?, ?, ?, ?, ?)
     *
     * We first get MAX(CustomerId)+1 because Chinook has no AUTO_INCREMENT.
     */
    private void createCustomer(String first, String last, String email,
                                 String phone, String country) {
        String getMax = "SELECT COALESCE(MAX(CustomerId), 0) + 1 FROM Customer";
        String insert = """
            INSERT INTO Customer (CustomerId, FirstName, LastName, Email, Phone, Country)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection()) {
            int nextId;
            try (PreparedStatement ps = conn.prepareStatement(getMax);
                 ResultSet rs = ps.executeQuery()) {
                rs.next(); nextId = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setInt(1, nextId);
                ps.setString(2, first);
                ps.setString(3, last);
                ps.setString(4, email);
                ps.setString(5, phone.isEmpty() ? null : phone);
                ps.setString(6, country.isEmpty() ? null : country);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Customer created successfully!");
        } catch (SQLException ex) { showError("Error creating customer", ex); }
    }

    /**
     * UPDATE — Modify an existing customer by ID.
     *
     * SQL:
     *   UPDATE Customer
     *   SET FirstName=?, LastName=?, Email=?, Phone=?, Country=?
     *   WHERE CustomerId=?
     */
    private void updateCustomer(int id, String first, String last,
                                 String email, String phone, String country) {
        String sql = """
            UPDATE Customer
            SET FirstName=?, LastName=?, Email=?, Phone=?, Country=?
            WHERE CustomerId=?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, first);
            ps.setString(2, last);
            ps.setString(3, email);
            ps.setString(4, phone.isEmpty() ? null : phone);
            ps.setString(5, country.isEmpty() ? null : country);
            ps.setInt(6, id);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Customer updated successfully!");
        } catch (SQLException ex) { showError("Error updating customer", ex); }
    }

    /**
     * DELETE — Remove a customer after confirmation.
     *
     * SQL: DELETE FROM Customer WHERE CustomerId=?
     *
     * If the customer has invoices, MySQL will throw a FK constraint error.
     * We catch this and show a friendly message — we do NOT force-delete
     * invoices as that would destroy financial records.
     */
    private void deleteCustomer() {
        int row = customerTable.getSelectedRow();
        if (row < 0) { warn("Please select a customer to delete."); return; }

        int id     = (int) customerModel.getValueAt(row, 0);
        String name = customerModel.getValueAt(row, 1) + " " + customerModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete:\n" + name + "?\n\nThis cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM Customer WHERE CustomerId=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Customer deleted successfully.");
            loadCustomers();
            loadInactiveCustomers();
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("foreign key")) {
                JOptionPane.showMessageDialog(this,
                    "Cannot delete " + name + ".\n\n" +
                    "This customer has invoice records linked to their account.\n" +
                    "Deleting financial records is not permitted.",
                    "Delete Blocked — Foreign Key Constraint",
                    JOptionPane.WARNING_MESSAGE);
            } else {
                showError("Error deleting customer", ex);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION 2 — INACTIVE CUSTOMERS
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildInactivePanel() {
        JPanel outer = new JPanel(new BorderLayout(5, 8));
        outer.setBackground(Color.WHITE);
        outer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(192, 57, 43), 2),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Inactive Customers");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(192, 57, 43));
        header.add(title, BorderLayout.WEST);
        inactiveCountLabel = new JLabel("");
        inactiveCountLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        inactiveCountLabel.setForeground(new Color(108, 117, 125));
        header.add(inactiveCountLabel, BorderLayout.EAST);
        outer.add(header, BorderLayout.NORTH);

        // Search bar
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchRow.setOpaque(false);
        searchRow.add(new JLabel("🔍  Search: "));
        inactiveSearch = new JTextField(25);
        inactiveSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        inactiveSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        searchRow.add(inactiveSearch);
        searchRow.add(Box.createHorizontalStrut(8));
        JLabel hint = new JLabel("(no invoices OR last invoice > 2 years ago)");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(new Color(108, 117, 125));
        searchRow.add(hint);
        outer.add(searchRow, BorderLayout.CENTER);

        // Table
        inactiveModel = new DefaultTableModel(INACTIVE_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        inactiveTable = new JTable(inactiveModel) {
            @Override public Component prepareRenderer(
                    javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(255, 248, 248));
                return c;
            }
        };
        styleTable(inactiveTable);
        inactiveSorter = new TableRowSorter<>(inactiveModel);
        inactiveTable.setRowSorter(inactiveSorter);
        outer.add(new JScrollPane(inactiveTable), BorderLayout.SOUTH);

        // Real-time search filter
        inactiveSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { filterInactive(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { filterInactive(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterInactive(); }
        });

        return outer;
    }

    /**
     * Loads inactive customers.
     *
     * FULL SQL EXPLAINED:
     *
     *   FROM Customer c
     *   LEFT JOIN Invoice i ON c.CustomerId = i.CustomerId
     *     → LEFT JOIN: keeps ALL customers even those with no invoices.
     *       Customers with no invoices get NULL in all Invoice columns.
     *
     *   GROUP BY c.CustomerId, c.FirstName, c.LastName, c.Email
     *     → One row per customer (collapses multiple invoice rows)
     *
     *   HAVING MAX(i.InvoiceDate) IS NULL
     *     → No invoices at all (LEFT JOIN gave us NULLs, MAX of NULLs = NULL)
     *
     *   OR MAX(i.InvoiceDate) < DATE_SUB(NOW(), INTERVAL 2 YEAR)
     *     → Most recent invoice older than 2 years from today
     *
     *   COALESCE(MAX(i.InvoiceDate), 'Never')
     *     → Shows 'Never' for customers with no invoices instead of NULL
     *
     * NOTE: Because Chinook data only goes up to 2013, ALL customers
     * will appear inactive by the 2-year rule. This is correct behaviour —
     * the SQL is right, the data is just old.
     */
    private void loadInactiveCustomers() {
        inactiveModel.setRowCount(0);

        String sql = """
            SELECT
                c.CustomerId,
                c.FirstName,
                c.LastName,
                c.Email,
                COALESCE(CAST(MAX(i.InvoiceDate) AS CHAR), 'Never') AS LastInvoice
            FROM Customer c
            LEFT JOIN Invoice i ON c.CustomerId = i.CustomerId
            GROUP BY c.CustomerId, c.FirstName, c.LastName, c.Email
            HAVING MAX(i.InvoiceDate) IS NULL
                OR MAX(i.InvoiceDate) < DATE_SUB(NOW(), INTERVAL 2 YEAR)
            ORDER BY LastInvoice ASC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                inactiveModel.addRow(new Object[]{
                    rs.getInt("CustomerId"),
                    rs.getString("FirstName"),
                    rs.getString("LastName"),
                    rs.getString("Email"),
                    rs.getString("LastInvoice")
                });
                count++;
            }
            inactiveCountLabel.setText(count + " inactive  ");
        } catch (SQLException ex) { showError("Error loading inactive customers", ex); }
    }

    private void filterInactive() {
        String t = inactiveSearch.getText().trim();
        if (t.isEmpty()) { inactiveSorter.setRowFilter(null); }
        else {
            try { inactiveSorter.setRowFilter(RowFilter.regexFilter("(?i)" + t, 1, 2, 3)); }
            catch (java.util.regex.PatternSyntaxException ignored) {}
        }
    }

    // ── Shared helpers ─────────────────────────────────────────────────────

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        p.add(field, gbc);
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setRowHeight(26);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.getTableHeader().setBackground(new Color(44, 62, 80));
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setPreferredSize(new Dimension(0, 32));
        t.setSelectionBackground(new Color(213, 232, 252));
        t.setGridColor(new Color(222, 226, 230));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Notice", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String msg, SQLException ex) {
        JOptionPane.showMessageDialog(this, msg + ":\n" + ex.getMessage(),
            "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    private String nullSafe(Object val) {
        return val == null ? "" : val.toString();
    }
}
