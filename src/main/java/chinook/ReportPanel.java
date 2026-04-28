package chinook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ReportPanel — Tab 3
 * -------------------
 * Genre Revenue Report — auto-refreshes every time this tab is opened.
 * MainGUI calls loadData() via a ChangeListener on the JTabbedPane.
 *
 * CONCEPT — Aggregation across multiple JOINs:
 *
 *   We want: how much money has each Genre earned?
 *
 *   The money lives in InvoiceLine (UnitPrice * Quantity per track).
 *   The genre lives in Genre, linked through Track.
 *
 *   JOIN chain:
 *     Genre → Track       (via Track.GenreId = Genre.GenreId)
 *     Track → InvoiceLine (via InvoiceLine.TrackId = Track.TrackId)
 *
 *   SUM(il.UnitPrice * il.Quantity) totals the revenue per genre.
 *   GROUP BY g.GenreId groups all rows with the same genre together.
 *   ORDER BY total_revenue DESC puts the highest earner first.
 *
 * WHY NOT use Invoice.Total?
 *   Invoice.Total is the grand total of a purchase (multiple tracks).
 *   If we summed that per genre we'd count the same invoice multiple
 *   times. InvoiceLine has one row per track, so it's accurate.
 */
public class ReportPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JLabel generatedLabel;

    private static final String[] COLUMNS = { "#", "Genre", "Total Revenue (USD)" };

    public ReportPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        loadData();
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setOpaque(false);

        JLabel title = new JLabel("Genre Revenue Report");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(26, 32, 44));
        panel.add(title, BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);

        JLabel sub = new JLabel("Total revenue earned per music genre — ordered highest to lowest");
        sub.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        sub.setForeground(new Color(108, 117, 125));
        row.add(sub);

        row.add(Box.createHorizontalStrut(20));

        JButton refreshBtn = makeButton("↻  Refresh", new Color(52, 152, 219));
        refreshBtn.addActionListener(e -> loadData());
        row.add(refreshBtn);

        panel.add(row, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230)));

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel) {
            @Override public Component prepareRenderer(
                    javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 252));
                // Highlight rank column
                if (col == 0) {
                    c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    c.setForeground(new Color(52, 152, 219));
                }
                // Colour revenue column green
                if (col == 2) {
                    c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    c.setForeground(new Color(39, 174, 96));
                }
                return c;
            }
        };

        styleTable(table);
        // Fix column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setForeground(new Color(39, 174, 96));

        generatedLabel = new JLabel(" ");
        generatedLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        generatedLabel.setForeground(new Color(108, 117, 125));

        panel.add(statusLabel,   BorderLayout.WEST);
        panel.add(generatedLabel, BorderLayout.EAST);
        return panel;
    }

    /**
     * Runs the Genre Revenue SQL and populates the table.
     * Called automatically when the Report tab is opened (via MainGUI).
     *
     * Full SQL:
     *   SELECT g.Name AS Genre,
     *          SUM(il.UnitPrice * il.Quantity) AS total_revenue
     *   FROM Genre g
     *   JOIN Track t        ON g.GenreId = t.GenreId
     *   JOIN InvoiceLine il  ON t.TrackId = il.TrackId
     *   GROUP BY g.GenreId, g.Name
     *   ORDER BY total_revenue DESC
     */
    public void loadData() {
        tableModel.setRowCount(0);

        String sql = """
            SELECT
                g.Name                              AS Genre,
                SUM(il.UnitPrice * il.Quantity)     AS total_revenue
            FROM Genre g
            JOIN Track t        ON g.GenreId = t.GenreId
            JOIN InvoiceLine il  ON t.TrackId = il.TrackId
            GROUP BY g.GenreId, g.Name
            ORDER BY total_revenue DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int rank = 1;
            double grandTotal = 0;

            while (rs.next()) {
                double rev = rs.getDouble("total_revenue");
                grandTotal += rev;
                tableModel.addRow(new Object[]{
                    rank++,
                    rs.getString("Genre"),
                    String.format("$ %.2f", rev)
                });
            }

            statusLabel.setText(String.format(
                "  %d genres | Grand Total: $ %.2f", rank - 1, grandTotal));
            generatedLabel.setText("Generated: "
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "  ");

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Error generating report:\n" + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        t.setRowHeight(30);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        t.getTableHeader().setBackground(new Color(44, 62, 80));
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setPreferredSize(new Dimension(0, 38));
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
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
