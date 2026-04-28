package chinook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;

/**
 * EmployeesPanel — Tab 1
 * ----------------------
 * Shows a table of all employees with columns:
 *   First Name, Last Name, Title, City, Country, Phone, Supervisor, Active/Inactive
 *
 * CONCEPT 1 — Self-Join (for Supervisor column):
 *   The Employee table has a column called ReportsTo which stores
 *   the EmployeeId of that employee's manager. Both the employee
 *   and the manager are in the SAME table.
 *
 *   To get the manager's name we join Employee to itself:
 *     FROM Employee e              ← 'e' = the employee
 *     LEFT JOIN Employee m         ← 'm' = the manager (same table, different alias)
 *       ON e.ReportsTo = m.EmployeeId
 *
 *   LEFT JOIN is used so employees with NO manager (the top boss)
 *   still appear — their supervisor shows as 'None'.
 *
 * CONCEPT 2 — Active/Inactive status:
 *   The Chinook schema has no "IsActive" column for employees.
 *   We determine activity by checking if any customer is assigned
 *   to this employee as their support representative.
 *
 *   LEFT JOIN Customer c ON c.SupportRepId = e.EmployeeId
 *   COUNT(c.CustomerId) > 0 → Active (has customers assigned)
 *   COUNT(c.CustomerId) = 0 → Inactive (no customers assigned)
 *
 *   CASE WHEN is SQL's version of an if-else statement.
 *
 * CONCEPT 3 — Real-time filter:
 *   We use TableRowSorter + RowFilter. This does NOT re-query
 *   the database — it just hides rows in the view. Much faster.
 */
public class EmployeesPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;

    private static final String[] COLUMNS = {
        "First Name", "Last Name", "Title", "City", "Country", "Phone", "Supervisor", "Status"
    };

    public EmployeesPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);

        loadData();
    }

    // ── Top panel: title + search ──────────────────────────────────────────

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setOpaque(false);

        // Title
        JLabel title = new JLabel("Employee Directory");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(26, 32, 44));
        panel.add(title, BorderLayout.NORTH);

        // Search bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchPanel.setOpaque(false);

        JLabel searchIcon = new JLabel("🔍  Filter by Name or City:  ");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchPanel.add(searchIcon);

        searchField = new JTextField(28);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218)),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        searchPanel.add(searchField);

        searchPanel.add(Box.createHorizontalStrut(10));

        JButton clearBtn = makeButton("Clear", new Color(108, 117, 125));
        clearBtn.addActionListener(e -> { searchField.setText(""); applyFilter(); });
        searchPanel.add(clearBtn);

        panel.add(searchPanel, BorderLayout.CENTER);

        // Stats label
        return panel;
    }

    // ── Table panel ────────────────────────────────────────────────────────

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel) {
            // Alternating row colours for readability
            @Override public Component prepareRenderer(
                    javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 252));
                }
                // Colour the Status column green/red
                if (col == 7) {
                    String val = (String) getValueAt(row, col);
                    if ("Active".equals(val))   c.setForeground(new Color(39, 174, 96));
                    else                         c.setForeground(new Color(192, 57, 43));
                    ((JLabel) c).setFont(new Font("Segoe UI", Font.BOLD, 12));
                } else {
                    c.setForeground(new Color(33, 37, 41));
                    ((JLabel) c).setFont(new Font("Segoe UI", Font.PLAIN, 13));
                }
                return c;
            }
        };

        styleTable(table);
        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);

        // Real-time filter as user types
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Loads all employees using:
     * 1. Self-join to get supervisor name
     * 2. LEFT JOIN Customer to determine Active/Inactive status
     * 3. CASE WHEN to convert count to readable label
     * 4. GROUP BY because we use COUNT() aggregation
     *
     * Full SQL:
     *   SELECT e.FirstName, e.LastName, e.Title, e.City, e.Country, e.Phone,
     *          COALESCE(CONCAT(m.FirstName,' ',m.LastName), 'None') AS Supervisor,
     *          CASE WHEN COUNT(c.CustomerId) > 0 THEN 'Active' ELSE 'Inactive' END AS Status
     *   FROM Employee e
     *   LEFT JOIN Employee m  ON e.ReportsTo    = m.EmployeeId
     *   LEFT JOIN Customer c  ON c.SupportRepId = e.EmployeeId
     *   GROUP BY e.EmployeeId, e.FirstName, e.LastName, e.Title,
     *            e.City, e.Country, e.Phone, m.FirstName, m.LastName
     *   ORDER BY e.LastName, e.FirstName
     */
    private void loadData() {
        tableModel.setRowCount(0);

        String sql = """
            SELECT
                e.FirstName,
                e.LastName,
                e.Title,
                e.City,
                e.Country,
                e.Phone,
                COALESCE(CONCAT(m.FirstName, ' ', m.LastName), 'None') AS Supervisor,
                CASE
                    WHEN COUNT(c.CustomerId) > 0 THEN 'Active'
                    ELSE 'Inactive'
                END AS Status
            FROM Employee e
            LEFT JOIN Employee m  ON e.ReportsTo    = m.EmployeeId
            LEFT JOIN Customer c  ON c.SupportRepId = e.EmployeeId
            GROUP BY e.EmployeeId, e.FirstName, e.LastName, e.Title,
                     e.City, e.Country, e.Phone, m.FirstName, m.LastName
            ORDER BY e.LastName, e.FirstName
            """;

        // try-with-resources: connection closes automatically when block ends
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getString("FirstName"),
                    rs.getString("LastName"),
                    rs.getString("Title"),
                    rs.getString("City"),
                    rs.getString("Country"),
                    rs.getString("Phone"),
                    rs.getString("Supervisor"),
                    rs.getString("Status")
                });
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Error loading employees:\n" + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Applies filter to visible rows only — no DB query needed.
     * RowFilter.regexFilter("(?i)" + text, 0, 1, 3) means:
     *   (?i) = case insensitive
     *   columns 0, 1, 3 = FirstName, LastName, City
     */
    private void applyFilter() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            try {
                rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1, 3));
            } catch (java.util.regex.PatternSyntaxException e) {
                // ignore invalid regex while user is still typing
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void styleTable(JTable t) {
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setRowHeight(28);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.getTableHeader().setBackground(new Color(44, 62, 80));
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setPreferredSize(new Dimension(0, 35));
        t.setSelectionBackground(new Color(213, 232, 252));
        t.setGridColor(new Color(222, 226, 230));
        t.setShowGrid(true);
        t.setIntercellSpacing(new Dimension(1, 1));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
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
        btn.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
