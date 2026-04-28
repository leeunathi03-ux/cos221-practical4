package chinook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * TracksPanel — Tab 2
 * -------------------
 * Shows ALL tracks from the database and provides an
 * "Add New Track" button that opens a popup dialog.
 *
 * CONCEPT 1 — Multi-table JOIN for display:
 *   Track alone only has AlbumId, GenreId, MediaTypeId (foreign keys).
 *   To show human-readable names we JOIN to the related tables:
 *     Track → Album      (LEFT JOIN so tracks without album still show)
 *     Track → Genre      (LEFT JOIN so tracks without genre still show)
 *     Track → MediaType  (INNER JOIN — every track must have a media type)
 *
 * CONCEPT 2 — Populating dropdowns from database:
 *   When adding a track, the Album/Genre/MediaType dropdowns are
 *   populated by querying those tables. We store [id, name] pairs
 *   so the display shows the name but we INSERT the id.
 *
 * CONCEPT 3 — INSERT with PreparedStatement:
 *   User input goes through ? placeholders, never concatenated
 *   into SQL strings. This prevents SQL injection.
 *
 * CONCEPT 4 — MAX(TrackId)+1 for new ID:
 *   Chinook doesn't use AUTO_INCREMENT so we manually calculate
 *   the next available ID before inserting.
 */
public class TracksPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel countLabel;

    private static final String[] COLUMNS = {
        "ID", "Track Name", "Album", "Genre", "Media Type", "Duration (ms)", "Price ($)"
    };

    public TracksPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);

        loadData();
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setOpaque(false);

        JLabel title = new JLabel("Track Catalogue");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(26, 32, 44));
        panel.add(title, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.setOpaque(false);

        JButton addBtn = makeButton("＋  Add New Track", new Color(39, 174, 96));
        addBtn.addActionListener(e -> openAddTrackDialog());
        btnRow.add(addBtn);

        btnRow.add(Box.createHorizontalStrut(15));
        countLabel = new JLabel("");
        countLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        countLabel.setForeground(new Color(108, 117, 125));
        btnRow.add(countLabel);

        panel.add(btnRow, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230)));

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel) {
            @Override public Component prepareRenderer(
                    javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 252));
                return c;
            }
        };

        styleTable(table);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Loads ALL tracks — no LIMIT.
     *
     * SQL JOIN chain explained:
     *   FROM Track t
     *   LEFT JOIN Album al    ON t.AlbumId     = al.AlbumId
     *     → LEFT so tracks with no album (AlbumId is nullable) still appear
     *   LEFT JOIN Genre g     ON t.GenreId     = g.GenreId
     *     → LEFT so tracks with no genre still appear
     *   JOIN MediaType mt     ON t.MediaTypeId = mt.MediaTypeId
     *     → INNER because every track must have a valid media type (NOT NULL in schema)
     *
     *   COALESCE(al.Title, 'Unknown') returns 'Unknown' if album is NULL
     */
    private void loadData() {
        tableModel.setRowCount(0);

        String sql = """
            SELECT
                t.TrackId,
                t.Name,
                COALESCE(al.Title, 'Unknown') AS Album,
                COALESCE(g.Name,  'Unknown')  AS Genre,
                mt.Name                        AS MediaType,
                t.Milliseconds,
                t.UnitPrice
            FROM Track t
            LEFT JOIN Album al    ON t.AlbumId     = al.AlbumId
            LEFT JOIN Genre g     ON t.GenreId     = g.GenreId
            JOIN  MediaType mt    ON t.MediaTypeId = mt.MediaTypeId
            ORDER BY t.TrackId DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("TrackId"),
                    rs.getString("Name"),
                    rs.getString("Album"),
                    rs.getString("Genre"),
                    rs.getString("MediaType"),
                    rs.getInt("Milliseconds"),
                    String.format("%.2f", rs.getDouble("UnitPrice"))
                });
                count++;
            }
            countLabel.setText(count + " tracks loaded");

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Error loading tracks:\n" + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Opens the Add New Track dialog.
     * Dropdowns (Album, Genre, MediaType) are populated from the DB.
     * Uses GridBagLayout for a clean, aligned form.
     */
    private void openAddTrackDialog() {
        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), "Add New Track", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(470, 450);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Color.WHITE);

        // ── Form ──────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        JTextField nameField     = new JTextField(22);
        JTextField composerField = new JTextField(22);
        JTextField msField       = new JTextField("300000");
        JTextField bytesField    = new JTextField("0");
        JTextField priceField    = new JTextField("0.99");

        // Dropdowns populated from database
        JComboBox<String[]> albumCombo     = new JComboBox<>();
        JComboBox<String[]> genreCombo     = new JComboBox<>();
        JComboBox<String[]> mediaTypeCombo = new JComboBox<>();

        populateCombo(albumCombo,     "SELECT AlbumId,     Title FROM Album     ORDER BY Title");
        populateCombo(genreCombo,     "SELECT GenreId,     Name  FROM Genre     ORDER BY Name");
        populateCombo(mediaTypeCombo, "SELECT MediaTypeId, Name  FROM MediaType ORDER BY Name");

        addRow(form, gbc, 0, "Track Name *:",  nameField);
        addRow(form, gbc, 1, "Composer:",       composerField);
        addRow(form, gbc, 2, "Album *:",         albumCombo);
        addRow(form, gbc, 3, "Genre:",           genreCombo);
        addRow(form, gbc, 4, "Media Type *:",    mediaTypeCombo);
        addRow(form, gbc, 5, "Milliseconds *:",  msField);
        addRow(form, gbc, 6, "Bytes:",           bytesField);
        addRow(form, gbc, 7, "Unit Price *:",    priceField);

        dialog.add(form, BorderLayout.CENTER);

        // ── Buttons ───────────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setBackground(new Color(248, 249, 252));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(222, 226, 230)));

        JButton saveBtn   = makeButton("Save Track", new Color(39, 174, 96));
        JButton cancelBtn = makeButton("Cancel",     new Color(108, 117, 125));

        saveBtn.addActionListener(e -> {
            // Validate required fields
            if (nameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Track Name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                int ms      = Integer.parseInt(msField.getText().trim());
                int bytes   = Integer.parseInt(bytesField.getText().trim());
                double price = Double.parseDouble(priceField.getText().trim());
                insertTrack(nameField, composerField, albumCombo, genreCombo, mediaTypeCombo, ms, bytes, price);
                dialog.dispose();
                loadData(); // reload table to show new entry
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Milliseconds, Bytes and Price must be numbers.", "Validation", JOptionPane.WARNING_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    /**
     * Inserts a new track into the database.
     *
     * Steps:
     * 1. Get MAX(TrackId)+1 to determine the new ID
     *    (Chinook doesn't use AUTO_INCREMENT)
     * 2. INSERT with all required fields using PreparedStatement
     *    — ? placeholders prevent SQL injection
     *
     * SQL:
     *   INSERT INTO Track
     *     (TrackId, Name, AlbumId, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice)
     *   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
     */
    private void insertTrack(JTextField nameF, JTextField composerF,
                             JComboBox<String[]> albumC, JComboBox<String[]> genreC,
                             JComboBox<String[]> mediaC,
                             int ms, int bytes, double price) {

        int albumId     = Integer.parseInt(((String[]) albumC.getSelectedItem())[0]);
        int genreId     = Integer.parseInt(((String[]) genreC.getSelectedItem())[0]);
        int mediaTypeId = Integer.parseInt(((String[]) mediaC.getSelectedItem())[0]);

        String getMax = "SELECT COALESCE(MAX(TrackId), 0) + 1 AS NextId FROM Track";
        String insert = """
            INSERT INTO Track
              (TrackId, Name, AlbumId, MediaTypeId, GenreId, Composer, Milliseconds, Bytes, UnitPrice)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Step 1: get next ID
            int nextId;
            try (PreparedStatement ps = conn.prepareStatement(getMax);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                nextId = rs.getInt("NextId");
            }
            // Step 2: insert
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setInt(1, nextId);
                ps.setString(2, nameF.getText().trim());
                ps.setInt(3, albumId);
                ps.setInt(4, mediaTypeId);
                ps.setInt(5, genreId);
                ps.setString(6, composerF.getText().trim());
                ps.setInt(7, ms);
                ps.setInt(8, bytes);
                ps.setDouble(9, price);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this,
                "Track added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Error saving track:\n" + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Populates a JComboBox from a SQL query.
     * Each item is stored as String[]{id, displayName}.
     * A custom renderer shows only the name in the dropdown.
     */
    @SuppressWarnings("unchecked")
    private void populateCombo(JComboBox<String[]> combo, String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                combo.addItem(new String[]{ rs.getString(1), rs.getString(2) });
            }
            combo.setRenderer((list, value, index, isSelected, hasFocus) -> {
                JLabel lbl = new JLabel(value != null ? value[1] : "");
                lbl.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                if (isSelected) {
                    lbl.setBackground(list.getSelectionBackground());
                    lbl.setOpaque(true);
                }
                return lbl;
            });

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Error loading dropdown data:\n" + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;
        p.add(field, gbc);
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
