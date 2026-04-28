package chinook;

import javax.swing.*;
import java.awt.*;

/**
 * MainGUI — Application Entry Point
 * ----------------------------------
 * Creates the main JFrame with 5 tabs.
 * Each tab is a separate panel class for clean code organisation.
 *
 * CONCEPT — Event Dispatch Thread (EDT):
 *   All Swing GUI updates MUST happen on the EDT.
 *   SwingUtilities.invokeLater() schedules our GUI creation
 *   on the EDT — this is the correct way to start a Swing app.
 *
 * CONCEPT — ChangeListener on JTabbedPane:
 *   We want the Report tab to refresh its data every time it is
 *   opened. We attach a ChangeListener to the tabbed pane — it
 *   fires whenever the selected tab changes. We check if the new
 *   tab index is 2 (Report tab) and call loadData() if so.
 */
public class MainGUI extends JFrame {

    private ReportPanel reportPanel;

    public MainGUI() {
        setTitle("Chinook Music Store — COS221 Practical 4");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 720);
        setMinimumSize(new Dimension(950, 620));
        setLocationRelativeTo(null); // centre on screen

        // Test DB connection on startup — show helpful error if it fails
        try {
            DatabaseConnection.getConnection().close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "⚠  Cannot connect to the database!\n\n"
                + "Error: " + ex.getMessage() + "\n\n"
                + "Please check:\n"
                + "  1. MySQL/MariaDB is running\n"
                + "  2. Environment variables are set correctly\n"
                + "  3. Database name matches CHINOOK_DB_NAME\n"
                + "  4. Password is correct in CHINOOK_DB_PASSWORD",
                "Connection Error", JOptionPane.ERROR_MESSAGE);
        }

        buildUI();
        setVisible(true);
    }

    private void buildUI() {
        // ── Tab pane ───────────────────────────────────────────────────────
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(new Color(245, 247, 250));

        tabs.addTab("👥  Employees",       new EmployeesPanel());
        tabs.addTab("🎵  Tracks",           new TracksPanel());

        reportPanel = new ReportPanel();
        tabs.addTab("📊  Report",           reportPanel);

        tabs.addTab("🔔  Notifications",    new NotificationsPanel());
        tabs.addTab("💡  Recommendations",  new RecommendationsPanel());

        // Reload report data every time the Report tab (index 2) is selected
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 2) {
                reportPanel.loadData();
            }
        });

        add(tabs, BorderLayout.CENTER);

        // ── Footer bar ─────────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(26, 32, 44));
        footer.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        JLabel left = new JLabel("COS221 Practical 4  |  Chinook Music Store");
        left.setForeground(new Color(173, 181, 189));
        left.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JLabel right = new JLabel("MySQL/MariaDB via JDBC  |  Java Swing  ");
        right.setForeground(new Color(173, 181, 189));
        right.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        footer.add(left,  BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    public static void main(String[] args) {
        // Apply system look and feel for native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Always start Swing apps on the Event Dispatch Thread
        SwingUtilities.invokeLater(MainGUI::new);
    }
}
