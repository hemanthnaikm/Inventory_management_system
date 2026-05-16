package javatosql;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InventoryApp extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(InventoryApp.class.getName());

    // ─── DATABASE CONFIGURATION ───
    private static final String HOST = "localhost";
    private static final int PORT = 3306;
    private static final String DB_NAME = "inventory_management_db"; 
    private static final String USERNAME = "root";
    private static final String PASSWORD = "1122"; 

    // Robust connection parameters to bypass authentication/SSL handshake errors
    private static final String BASE_URL = "jdbc:mysql://" + HOST + ":" + PORT + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useLegacyDatetimeCode=false";
    private static final String DB_URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useLegacyDatetimeCode=false";

    // ─── GUI COMPONENTS ───
    private JTextField txtSku, txtName, txtQty, txtPrice;
    private JComboBox<String> cmbStatus;
    private JButton btnAdd, btnRefresh, btnDeleteInStock, btnToggleInStock, btnDeleteSold, btnToggleSold;
    
    private JTable inStockTable, soldTable;
    private DefaultTableModel inStockModel, soldModel;
    private InventoryGraphPanel graphPanel;

    public InventoryApp() {
        super("Modern Inventory Dashboard & Analytics");
        
        // Setup Database and Tables
        initDatabase();

        // Initialize Table Models
        String[] columns = {"ID", "Item Code", "Product Name", "Quantity", "Price (₹)", "Status", "Timestamp"};
        inStockModel = new DefaultTableModel(columns, 0);
        soldModel = new DefaultTableModel(columns, 0);

        // Build Clean UI Layout
        initUI();

        // Sync local view with SQL data
        loadTableData();
    }

    // ─── STEP 1: DATABASE INITIALIZATION ───
    private void initDatabase() {
        try {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                Class.forName("com.mysql.jdbc.Driver"); 
            }

            // 1. Create schema if missing
            try (Connection conn = DriverManager.getConnection(BASE_URL, USERNAME, PASSWORD);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            }

            // 2. Build tracking table structure
            try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
                 Statement stmt = conn.createStatement()) {
                
                String createTableSQL = """
                    CREATE TABLE IF NOT EXISTS inventory_items (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        sku VARCHAR(50) NOT NULL UNIQUE,
                        product_name VARCHAR(100) NOT NULL,
                        quantity INT NOT NULL,
                        price DECIMAL(10,2) NOT NULL,
                        status ENUM('IN STOCK', 'SOLD') DEFAULT 'IN STOCK',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """;
                stmt.executeUpdate(createTableSQL);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Database initialization failed", ex);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, 
                "MySQL Connection Error!\n\nPlease check:\n1. Is your MySQL Server running on port 3306?\n2. Is the password '1122' correct?\n\nDetails: " + ex.getMessage(), 
                "SQL Connection Failure", JOptionPane.ERROR_MESSAGE));
        }
    }

    // ─── STEP 2: DASHBOARD UI DESIGN ───
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);
        
        JPanel mainContainer = new JPanel(new BorderLayout(20, 20));
        mainContainer.setBackground(new Color(248, 250, 252)); 
        mainContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(mainContainer);

        // Sidebar Panel
        JPanel sidePanel = new JPanel(new GridBagLayout());
        sidePanel.setBackground(Color.WHITE);
        sidePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
            BorderFactory.createEmptyBorder(25, 20, 25, 20)
        ));
        sidePanel.setPreferredSize(new Dimension(320, 600));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel lblTitle = new JLabel("Stock Entry Management");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(15, 23, 42)); 
        sidePanel.add(lblTitle, gbc);

        gbc.gridy++;
        sidePanel.add(Box.createVerticalStrut(10), gbc);

        // Field Entry System
        JLabel lblSku = new JLabel("Item Code / SKU");
        styleFieldLabel(lblSku);
        gbc.gridy++; sidePanel.add(lblSku, gbc);
        txtSku = new JTextField(); styleTextField(txtSku);
        gbc.gridy++; sidePanel.add(txtSku, gbc);

        JLabel lblName = new JLabel("Product Name");
        styleFieldLabel(lblName);
        gbc.gridy++; sidePanel.add(lblName, gbc);
        txtName = new JTextField(); styleTextField(txtName);
        gbc.gridy++; sidePanel.add(txtName, gbc);

        JLabel lblQty = new JLabel("Quantity Available");
        styleFieldLabel(lblQty);
        gbc.gridy++; sidePanel.add(lblQty, gbc);
        txtQty = new JTextField(); styleTextField(txtQty);
        gbc.gridy++; sidePanel.add(txtQty, gbc);

        JLabel lblPrice = new JLabel("Unit Price (₹)");
        styleFieldLabel(lblPrice);
        gbc.gridy++; sidePanel.add(lblPrice, gbc);
        txtPrice = new JTextField(); styleTextField(txtPrice);
        gbc.gridy++; sidePanel.add(txtPrice, gbc);

        JLabel lblStatus = new JLabel("Initial Status");
        styleFieldLabel(lblStatus);
        gbc.gridy++; sidePanel.add(lblStatus, gbc);
        cmbStatus = new JComboBox<>(new String[]{"IN STOCK", "SOLD"});
        cmbStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cmbStatus.setBackground(Color.WHITE);
        gbc.gridy++; sidePanel.add(cmbStatus, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        sidePanel.add(Box.createVerticalGlue(), gbc);
        gbc.weighty = 0.0;

        // Clean Text Buttons (Rectangle Emojis Completely Removed)
        btnAdd = new ModernButton("Add Product Item", new Color(79, 70, 229), Color.WHITE);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 10, 0); sidePanel.add(btnAdd, gbc);

        btnRefresh = new ModernButton("Refresh System Dash", new Color(241, 245, 249), new Color(71, 85, 105));
        gbc.gridy++; sidePanel.add(btnRefresh, gbc);

        mainContainer.add(sidePanel, BorderLayout.WEST);

        // Display Window Panel
        JPanel rightContainer = new JPanel(new BorderLayout(20, 20));
        rightContainer.setOpaque(false);

        // Real-time custom analytics canvas
        graphPanel = new InventoryGraphPanel(inStockModel, soldModel);
        graphPanel.setPreferredSize(new Dimension(650, 230));
        rightContainer.add(graphPanel, BorderLayout.NORTH);

        // Dual Tab Layout setup
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("Available Items (In Stock)", createTableCard(
                inStockModel, 
                inStockTable = new JTable(inStockModel), 
                btnDeleteInStock = new ModernButton("Delete Item", new Color(239, 68, 68), Color.WHITE), 
                btnToggleInStock = new ModernButton("Mark as Sold", new Color(59, 130, 246), Color.WHITE)
        ));

        tabbedPane.addTab("Sold Out Registry", createTableCard(
                soldModel, 
                soldTable = new JTable(soldModel), 
                btnDeleteSold = new ModernButton("Delete Record", new Color(239, 68, 68), Color.WHITE), 
                btnToggleSold = new ModernButton("Return to Stock", new Color(16, 185, 129), Color.WHITE)
        ));

        rightContainer.add(tabbedPane, BorderLayout.CENTER);
        mainContainer.add(rightContainer, BorderLayout.CENTER);

        // Listeners
        btnAdd.addActionListener(this::handleInsert);
        btnRefresh.addActionListener(e -> loadTableData());
        
        btnDeleteInStock.addActionListener(e -> handleDelete(inStockTable));
        btnDeleteSold.addActionListener(e -> handleDelete(soldTable));
        
        btnToggleInStock.addActionListener(e -> handleStatusToggle(inStockTable, "SOLD"));
        btnToggleSold.addActionListener(e -> handleStatusToggle(soldTable, "IN STOCK"));
    }

    private JPanel createTableCard(DefaultTableModel model, JTable table, JButton btnDelete, JButton btnToggle) {
        JPanel cardPanel = new JPanel(new BorderLayout(10, 10));
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        table.setRowHeight(35);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setGridColor(new Color(241, 245, 249));
        table.setSelectionBackground(new Color(238, 242, 255));
        table.setSelectionForeground(new Color(79, 70, 229));
        table.setShowGrid(true);

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(new Color(100, 116, 139));
        table.getTableHeader().setPreferredSize(new Dimension(0, 35));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(241, 245, 249), 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        cardPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionRow.setOpaque(false);
        actionRow.add(btnToggle);
        actionRow.add(btnDelete);
        cardPanel.add(actionRow, BorderLayout.SOUTH);

        return cardPanel;
    }

    private void styleFieldLabel(JLabel label) {
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(new Color(100, 116, 139));
    }

    private void styleTextField(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    // ─── STEP 3: DATA EXECUTIONS (INSERT) ───
    private void handleInsert(ActionEvent e) {
        String sku = txtSku.getText().trim();
        String name = txtName.getText().trim();
        String qtyStr = txtQty.getText().trim();
        String priceStr = txtPrice.getText().trim();
        String status = (String) cmbStatus.getSelectedItem();

        if (sku.isEmpty() || name.isEmpty() || qtyStr.isEmpty() || priceStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields must be completely filled!", "Input Alert", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int quantity = Integer.parseInt(qtyStr);
            double price = Double.parseDouble(priceStr);

            String insertSQL = "INSERT INTO inventory_items (sku, product_name, quantity, price, status) VALUES (?, ?, ?, ?, ?)";

            try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
                 PreparedStatement ps = conn.prepareStatement(insertSQL)) {
                
                ps.setString(1, sku);
                ps.setString(2, name);
                ps.setInt(3, quantity);
                ps.setDouble(4, price);
                ps.setString(5, status);

                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Item added successfully to database!", "Success", JOptionPane.INFORMATION_MESSAGE);
                
                clearInputFields();
                loadTableData();

            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "SQL Write Failure", ex);
                JOptionPane.showMessageDialog(this, "Database Write Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Validation Failed:\n- Quantity must be a whole number\n- Price must be a valid numeric value", "Format Exception", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── STEP 4: DATA EXECUTIONS (DELETE) ───
    private void handleDelete(JTable targetTable) {
        int selectedRow = targetTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item row from the table to delete!", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int targetId = (int) targetTable.getValueAt(selectedRow, 0);
        int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete Item ID " + targetId + "?", "Delete Confirmation", JOptionPane.YES_NO_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            String deleteSQL = "DELETE FROM inventory_items WHERE id = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
                 PreparedStatement ps = conn.prepareStatement(deleteSQL)) {
                
                ps.setInt(1, targetId);
                ps.executeUpdate();
                
                loadTableData(); 
                JOptionPane.showMessageDialog(this, "Item removed completely from SQL registry.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Delete processing failed", ex);
            }
        }
    }

    // ─── STEP 5: DATA EXECUTIONS (UPDATE STATUS) ───
    private void handleStatusToggle(JTable targetTable, String newStatus) {
        int selectedRow = targetTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row first to switch its status!", "Selection Missing", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int targetId = (int) targetTable.getValueAt(selectedRow, 0);
        String updateSQL = "UPDATE inventory_items SET status = ? WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(updateSQL)) {
            
            ps.setString(1, newStatus);
            ps.setInt(2, targetId);
            ps.executeUpdate();
            
            loadTableData(); 
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Status modification failed", ex);
        }
    }

    // ─── STEP 6: RECONCILE AND FRESH LOAD DATA ───
    private void loadTableData() {
        inStockModel.setRowCount(0); 
        soldModel.setRowCount(0);
        
        String querySQL = "SELECT * FROM inventory_items ORDER BY id DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(querySQL)) {

            while (rs.next()) {
                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("sku"), 
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getBigDecimal("price"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at")
                };
                
                if ("SOLD".equalsIgnoreCase(rs.getString("status"))) {
                    soldModel.addRow(row);
                } else {
                    inStockModel.addRow(row);
                }
            }

            if (graphPanel != null) {
                graphPanel.repaint();
            }

        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Failed to load and clear structural datasets", ex);
        }
    }

    private void clearInputFields() {
        txtSku.setText("");
        txtName.setText("");
        txtQty.setText("");
        txtPrice.setText("");
        cmbStatus.setSelectedIndex(0);
    }

    // ─── FLAT CUSTOM UI BUTTON COMPONENT ───
    static class ModernButton extends JButton {
        private final Color bgColor;

        public ModernButton(String text, Color bg, Color fg) {
            super(text);
            this.bgColor = bg;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(fg);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(140, 38));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isPressed()) {
                g2.setColor(bgColor.darker());
            } else if (getModel().isRollover()) {
                g2.setColor(bgColor.brighter());
            } else {
                g2.setColor(bgColor);
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ─── BILATERAL QUANTITIES GRAPH PANEL ───
    static class InventoryGraphPanel extends JPanel {
        private final DefaultTableModel inStock;
        private final DefaultTableModel sold;

        public InventoryGraphPanel(DefaultTableModel inStock, DefaultTableModel sold) {
            this.inStock = inStock;
            this.sold = sold;
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            g2.setColor(new Color(15, 23, 42));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.drawString("Real-time Analytics Metrics (In Stock: Green | Sold: Blue)", 15, 25);

            int totalItems = inStock.getRowCount() + sold.getRowCount();
            if (totalItems == 0) {
                g2.setColor(new Color(148, 163, 184));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g2.drawString("No operational data available to plot.", width / 2 - 110, height / 2 + 10);
                return;
            }

            int padLeft = 45, padRight = 20, padTop = 55, padBottom = 35;
            int graphW = width - padLeft - padRight;
            int graphH = height - padTop - padBottom;

            int maxQty = 0;
            int stockLimit = Math.min(inStock.getRowCount(), 4);
            int soldLimit = Math.min(sold.getRowCount(), 4);
            int renderCount = stockLimit + soldLimit;

            for (int i = 0; i < stockLimit; i++) {
                int qty = Integer.parseInt(inStock.getValueAt(i, 3).toString());
                if (qty > maxQty) maxQty = qty;
            }
            for (int i = 0; i < soldLimit; i++) {
                int qty = Integer.parseInt(sold.getValueAt(i, 3).toString());
                if (qty > maxQty) maxQty = qty;
            }
            if (maxQty == 0) maxQty = 10;
            maxQty = (int) (maxQty * 1.2); 

            for (int k = 1; k <= 3; k++) {
                int lineY = padTop + graphH - (graphH * k / 3);
                g2.setColor(new Color(241, 245, 249));
                g2.drawLine(padLeft, lineY, width - padRight, lineY);
                g2.setColor(new Color(148, 163, 184));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.drawString(String.valueOf(maxQty * k / 3), 12, lineY + 4);
            }

            g2.setColor(new Color(226, 232, 240));
            g2.drawLine(padLeft, height - padBottom, width - padRight, height - padBottom);

            int gap = 20;
            int barW = (graphW - (gap * (renderCount - 1))) / renderCount;
            if (barW < 6) barW = 6;

            int globalIdx = 0;

            for (int i = 0; i < stockLimit; i++) {
                renderBar(g2, inStock, i, globalIdx++, barW, gap, maxQty, padLeft, height, padBottom, graphH, new Color(16, 185, 129));
            }

            for (int i = 0; i < soldLimit; i++) {
                renderBar(g2, sold, i, globalIdx++, barW, gap, maxQty, padLeft, height, padBottom, graphH, new Color(59, 130, 246));
            }
        }

        private void renderBar(Graphics2D g2, DefaultTableModel model, int modelIdx, int globalIdx, 
                               int barW, int gap, int maxQty, int padLeft, int height, int padBottom, int graphH, Color barColor) {
            String pName = model.getValueAt(modelIdx, 2).toString();
            int qty = Integer.parseInt(model.getValueAt(modelIdx, 3).toString());

            if (pName.length() > 9) pName = pName.substring(0, 7) + "..";

            int barH = (int) (((double) qty / maxQty) * graphH);
            int x = padLeft + globalIdx * (barW + gap);
            int y = height - padBottom - barH;

            g2.setColor(barColor);
            g2.fillRoundRect(x, y, barW, barH, 6, 6);
            if (barH > 4) {
                g2.fillRect(x, height - padBottom - 4, barW, 4); 
            }

            g2.setColor(new Color(15, 23, 42));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            String txt = String.valueOf(qty);
            g2.drawString(txt, x + (barW - g2.getFontMetrics().stringWidth(txt)) / 2, y - 5);

            g2.setColor(new Color(100, 116, 139));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString(pName, x + (barW - g2.getFontMetrics().stringWidth(pName)) / 2, height - padBottom + 16);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new InventoryApp().setVisible(true);
        });
    }
}