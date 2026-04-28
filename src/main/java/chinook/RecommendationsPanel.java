package chinook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * RecommendationsPanel — Tab 5
 * ----------------------------
 * "Customer Insights & Recommendations"
 *
 * Three sections that all update when a customer is selected:
 *   1. Spending Summary  — total spent, purchases, last purchase date
 *   2. Favourite Genre   — most purchased genre via SQL aggregation
 *   3. Recommendations   — tracks NOT yet purchased in favourite genre
 *
 * ══ SQL CONCEPTS FOR YOUR DEMO ══════════════════════════════════════════
 *
 * SPENDING SUMMARY:
 *   SELECT SUM(Total), COUNT(*), MAX(InvoiceDate)
 *   FROM Invoice WHERE CustomerId = ?
 *   → SUM = total money spent
 *   → COUNT = number of separate purchases (invoices)
 *   → MAX = date of most recent purchase
 *
 * FAVOURITE GENRE (4-table JOIN + GROUP BY):
 *   Invoice → InvoiceLine → Track → Genre
 *   COUNT(*) per genre tells us how many tracks they bought per genre.
 *   GROUP BY genre, ORDER BY count DESC, LIMIT 1 = top genre only.
 *
 * RECOMMENDATIONS (NOT IN subquery — ADVANCED SQL):
 *   Outer query: tracks in favourite genre
 *   Inner subquery: ALL tracks this customer has ever purchased
 *   NOT IN excludes already-purchased tracks from recommendations.
 *
 *   This is the most complex SQL in the project and worth bonus marks.
 *   Document it in your PDF for Task 6.2.
 *
 * ORDER BY RAND():
 *   Shuffles results randomly so recommendations vary each time.
 *   This is non-standard — standard ORDER BY uses column names.
 */
public class RecommendationsPanel extends JPanel {

    private JComboBox<String[]> customerCombo;
    private boolean isLoading = false; // prevents double-load on init

    // Spending summary
    private JLabel totalSpentLbl;
    private JLabel totalPurchasesLbl;
    private JLabel lastPurchaseLbl;

    // Favourite genre
    private JLabel genreNameLbl;
    private JLabel genreCountLbl;

    // Recommendations table
    private DefaultTableModel recModel;
    private JLabel recCountLbl;

    public RecommendationsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);

        populateCustomerCombo();
    }

    // ── Header: title + customer dropdown ─────────────────────────────────

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setOpaque(false);

        JLabel title = new JLabel("Customer Insights & Recommendations");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(26, 32, 44));
        panel.add(title, BorderLayout.NORTH);

        JPanel selectorRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        selectorRow.setOpaque(false);
        JLabel lbl = new JLabel("Select Customer:  ");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        selectorRow.add(lbl);

        customerCombo = new JComboBox<>();
        customerCombo.setPreferredSize(new Dimension(300, 34));
        customerCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        selectorRow.add(customerCombo);

        selectorRow.add(Box.createHorizontalStrut(12));
        JButton loadBtn = makeButton("Load Insights", new Color(142, 68, 173));
        loadBtn.addActionListener(e -> loadInsights());
        selectorRow.add(loadBtn);

        panel.add(selectorRow, BorderLayout.CENTER);
        return panel;
    }

    // ── Body: stats left, recommendations right ────────────────────────────

    private JSplitPane buildBody() {
        JSplitPane split = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, buildStatsPanel(), buildRecsPanel());
        split.setDividerLocation(340);
        split.setResizeWeight(0.32);
        split.setBorder(null);
        split.setDividerSize(8);
        return split;
    }

    // ── Left panel: Spending Summary + Favourite Genre ─────────────────────

    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(142, 68, 173), 2),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        // ── Spending Summary ──────────────────────────────────────────────
        JLabel s1 = sectionTitle("Spending Summary", new Color(142, 68, 173));
        panel.add(s1);
        panel.add(Box.createVerticalStrut(12));

        totalSpentLbl     = infoLabel("Total Spent");
        totalPurchasesLbl = infoLabel("Total Purchases");
        lastPurchaseLbl   = infoLabel("Last Purchase");

        panel.add(makeStatRow("💰  Total Spent:",     totalSpentLbl));
        panel.add(Box.createVerticalStrut(8));
        panel.add(makeStatRow("🛒  Purchases:",        totalPurchasesLbl));
        panel.add(Box.createVerticalStrut(8));
        panel.add(makeStatRow("📅  Last Purchase:",   lastPurchaseLbl));
        panel.add(Box.createVerticalStrut(20));

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(222, 226, 230));
        panel.add(sep);
        panel.add(Box.createVerticalStrut(18));

        // ── Favourite Genre ───────────────────────────────────────────────
        JLabel s2 = sectionTitle("Favourite Genre", new Color(39, 174, 96));
        panel.add(s2);
        panel.add(Box.createVerticalStrut(12));

        genreNameLbl  = infoLabel("—");
        genreCountLbl = infoLabel("—");

        panel.add(makeStatRow("🎵  Genre:",             genreNameLbl));
        panel.add(Box.createVerticalStrut(8));
        panel.add(makeStatRow("🔢  Tracks Purchased:", genreCountLbl));

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    // ── Right panel: Recommended Tracks table ─────────────────────────────

    private JPanel buildRecsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = sectionTitle("Recommended Tracks", new Color(52, 152, 219));
        header.add(title, BorderLayout.WEST);
        recCountLbl = new JLabel("");
        recCountLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        recCountLbl.setForeground(new Color(108, 117, 125));
        header.add(recCountLbl, BorderLayout.EAST);

        JLabel sub = new JLabel("Tracks from favourite genre not yet purchased");
        sub.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        sub.setForeground(new Color(108, 117, 125));

        JPanel headerStack = new JPanel(new BorderLayout(0, 4));
        headerStack.setOpaque(false);
        headerStack.add(header, BorderLayout.NORTH);
        headerStack.add(sub,    BorderLayout.CENTER);
        panel.add(headerStack, BorderLayout.NORTH);

        recModel = new DefaultTableModel(
            new String[]{ "Track Name", "Album", "Artist", "Genre" }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable recTable = new JTable(recModel) {
            @Override public Component prepareRenderer(
                    javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 252));
                return c;
            }
        };
        styleTable(recTable);
        panel.add(new JScrollPane(recTable), BorderLayout.CENTER);

        return panel;
    }

    // ── Data loading ───────────────────────────────────────────────────────

    /** Populates the customer dropdown from the database */
    private void populateCustomerCombo() {
        isLoading = true;
        customerCombo.removeAllItems();

        String sql = """
            SELECT CustomerId, FirstName, LastName
            FROM Customer
            ORDER BY LastName, FirstName
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                customerCombo.addItem(new String[]{
                    String.valueOf(rs.getInt("CustomerId")),
                    rs.getString("FirstName") + " " + rs.getString("LastName")
                });
            }

            // Custom renderer — shows "FirstName LastName" not array toString
            customerCombo.setRenderer((list, value, index, isSelected, hasFocus) -> {
                JLabel lbl = new JLabel(value != null ? value[1] : "");
                lbl.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                if (isSelected) { lbl.setBackground(list.getSelectionBackground()); lbl.setOpaque(true); }
                return lbl;
            });

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading customers:\n" + ex.getMessage());
        }

        isLoading = false;

        // Auto-load first customer immediately
        if (customerCombo.getItemCount() > 0) {
            customerCombo.setSelectedIndex(0);
            loadInsights();
        }

        // Wire combo change AFTER init so it doesn't fire twice during setup
        customerCombo.addActionListener(e -> {
            if (!isLoading) loadInsights();
        });
    }

    /** Called when customer is selected — loads all three sections */
    private void loadInsights() {
        String[] selected = (String[]) customerCombo.getSelectedItem();
        if (selected == null) return;
        int customerId = Integer.parseInt(selected[0]);

        loadSpendingSummary(customerId);
        String genre = loadFavouriteGenre(customerId);
        loadRecommendations(customerId, genre);
    }

    /**
     * SPENDING SUMMARY
     *
     * SQL:
     *   SELECT COALESCE(SUM(Total),0), COUNT(*), MAX(InvoiceDate)
     *   FROM Invoice
     *   WHERE CustomerId = ?
     *
     *   SUM(Total)     = total money this customer has ever spent
     *   COUNT(*)       = number of invoices = number of purchases
     *   MAX(InvoiceDate) = date of their most recent purchase
     *   COALESCE(SUM,0) = returns 0 if customer has no invoices (avoids NULL)
     */
    private void loadSpendingSummary(int customerId) {
        String sql = """
            SELECT
                COALESCE(SUM(Total), 0) AS TotalSpent,
                COUNT(*)                AS TotalPurchases,
                MAX(InvoiceDate)        AS LastPurchase
            FROM Invoice
            WHERE CustomerId = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalSpentLbl.setText(     String.format("$ %.2f", rs.getDouble("TotalSpent")));
                    totalPurchasesLbl.setText(  String.valueOf(rs.getInt("TotalPurchases")));
                    String last = rs.getString("LastPurchase");
                    lastPurchaseLbl.setText(last != null ? last.substring(0, 10) : "Never");
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading spending summary:\n" + ex.getMessage());
        }
    }

    /**
     * FAVOURITE GENRE — 4-table JOIN + GROUP BY + ORDER BY + LIMIT
     *
     * SQL JOIN chain:
     *   Invoice i → InvoiceLine il → Track t → Genre g
     *
     *   WHERE i.CustomerId = ?  → only this customer's purchases
     *   GROUP BY g.GenreId, g.Name → one row per genre
     *   COUNT(*) → how many tracks they bought in each genre
     *   ORDER BY TrackCount DESC → highest count first
     *   LIMIT 1 → only the top genre
     *
     * Returns the genre name (or null if customer has no purchases).
     */
    private String loadFavouriteGenre(int customerId) {
        String sql = """
            SELECT
                g.Name   AS Genre,
                COUNT(*) AS TrackCount
            FROM Invoice i
            JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
            JOIN Track t        ON il.TrackId  = t.TrackId
            JOIN Genre g        ON t.GenreId   = g.GenreId
            WHERE i.CustomerId = ?
            GROUP BY g.GenreId, g.Name
            ORDER BY TrackCount DESC
            LIMIT 1
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String genre = rs.getString("Genre");
                    int count    = rs.getInt("TrackCount");
                    genreNameLbl.setText(genre);
                    genreCountLbl.setText(count + " tracks");
                    return genre;
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading favourite genre:\n" + ex.getMessage());
        }

        genreNameLbl.setText("No purchase history");
        genreCountLbl.setText("—");
        return null;
    }

    /**
     * RECOMMENDATION ENGINE — NOT IN subquery (Advanced SQL)
     *
     * GOAL: Show tracks in the customer's favourite genre
     *       that they have NOT purchased yet.
     *
     * OUTER QUERY:
     *   SELECT t.Name, al.Title, ar.Name, g.Name
     *   FROM Track t
     *   JOIN Genre g   ON t.GenreId  = g.GenreId   ← filter by genre
     *   JOIN Album al  ON t.AlbumId  = al.AlbumId  ← get album name
     *   JOIN Artist ar ON al.ArtistId = ar.ArtistId ← get artist name
     *   WHERE g.Name = ?                             ← favourite genre
     *
     * SUBQUERY (NOT IN):
     *   SELECT il.TrackId
     *   FROM InvoiceLine il
     *   JOIN Invoice i ON il.InvoiceId = i.InvoiceId
     *   WHERE i.CustomerId = ?
     *   → This gives ALL TrackIds this customer has ever bought
     *
     *   AND t.TrackId NOT IN (subquery)
     *   → Excludes already-purchased tracks from results
     *
     * ORDER BY RAND() → random shuffle so recommendations vary
     * LIMIT 10        → cap at 10 suggestions
     *
     * WHY THIS IS ADVANCED:
     *   Standard SQL uses simple WHERE conditions.
     *   This uses a nested subquery inside NOT IN — a subquery
     *   that itself joins two tables. This is documented in PDF
     *   for Task 6.2 bonus marks.
     */
    private void loadRecommendations(int customerId, String genre) {
        recModel.setRowCount(0);

        if (genre == null) {
            recModel.addRow(new Object[]{
                "No purchase history found — cannot generate recommendations.", "", "", ""
            });
            recCountLbl.setText("");
            return;
        }

        String sql = """
            SELECT
                t.Name   AS TrackName,
                al.Title AS Album,
                ar.Name  AS Artist,
                g.Name   AS Genre
            FROM Track t
            JOIN Genre g    ON t.GenreId   = g.GenreId
            JOIN Album al   ON t.AlbumId   = al.AlbumId
            JOIN Artist ar  ON al.ArtistId = ar.ArtistId
            WHERE g.Name = ?
              AND t.TrackId NOT IN (
                  SELECT il.TrackId
                  FROM InvoiceLine il
                  JOIN Invoice i ON il.InvoiceId = i.InvoiceId
                  WHERE i.CustomerId = ?
              )
            ORDER BY RAND()
            LIMIT 10
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, genre);
            ps.setInt(2, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    recModel.addRow(new Object[]{
                        rs.getString("TrackName"),
                        rs.getString("Album"),
                        rs.getString("Artist"),
                        rs.getString("Genre")
                    });
                    count++;
                }
                if (count == 0) {
                    recModel.addRow(new Object[]{
                        "This customer has purchased all available tracks in their favourite genre!",
                        "", "", ""
                    });
                    recCountLbl.setText("");
                } else {
                    recCountLbl.setText(count + " suggestions");
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Error loading recommendations:\n" + ex.getMessage());
        }
    }

    // ── UI helpers ─────────────────────────────────────────────────────────

    private JLabel sectionTitle(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lbl.setForeground(color);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel infoLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(new Color(33, 37, 41));
        return lbl;
    }

    private JPanel makeStatRow(String labelText, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel key = new JLabel(labelText);
        key.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        key.setForeground(new Color(108, 117, 125));
        key.setPreferredSize(new Dimension(160, 24));

        row.add(key,        BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setRowHeight(28);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.getTableHeader().setBackground(new Color(44, 62, 80));
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setPreferredSize(new Dimension(0, 35));
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
