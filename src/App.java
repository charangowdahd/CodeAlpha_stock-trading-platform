import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TradingFrame().setVisible(true));
    }
}

class TradingFrame extends JFrame {
    private static final Color NAVY = new Color(11, 38, 74);
    private static final Color BLUE = new Color(24, 96, 168);
    private static final Color LIGHT_BLUE = new Color(232, 242, 252);
    private static final Color WHITE = Color.WHITE;
    private static final Color GREEN = new Color(20, 130, 85);
    private static final Color RED = new Color(190, 60, 65);
    private final Market market = new Market();
    private final User user = new User("User", 10_000.00);
    private final PortfolioRepository repository = new PortfolioRepository(Path.of("portfolio_history.csv"));
    private final DefaultTableModel marketModel = new DefaultTableModel(
            new Object[] { "Ticker", "Company", "Price" }, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final DefaultTableModel holdingsModel = new DefaultTableModel(
            new Object[] { "Ticker", "Shares", "Price", "Value" }, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JComboBox<String> tickerBox = new JComboBox<>();
    private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
    private final JLabel cashLabel = new JLabel();
    private final JLabel totalLabel = new JLabel();
    private final JLabel returnLabel = new JLabel();
    private final JList<String> historyList = new JList<>();
    private final ProfitLossChart chart = new ProfitLossChart();

    TradingFrame() {
        super("Stock Trading Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setMinimumSize(new Dimension(760, 520));
        setLocationRelativeTo(null);
        repository.load(user.getPortfolio());
        buildUi();
        refresh();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(NAVY);
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(createSummaryPanel(), BorderLayout.NORTH);

        JTable marketTable = new JTable(marketModel);
        JTable holdingsTable = new JTable(holdingsModel);
        styleTable(marketTable);
        styleTable(holdingsTable);
        marketTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        holdingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel tables = new JPanel(new GridLayout(1, 2, 12, 0));
        tables.add(withTitle("Market data", new JScrollPane(marketTable)));
        tables.add(withTitle("Your portfolio", new JScrollPane(holdingsTable)));
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setBackground(NAVY);
        center.add(tables, BorderLayout.CENTER);
        center.add(withTitle("Profit / loss over time", chart), BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setBackground(NAVY);
        bottom.add(createTradePanel(), BorderLayout.CENTER);
        bottom.add(withTitle("Performance history", new JScrollPane(historyList)), BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 4));
        panel.setBackground(BLUE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(88, 157, 218)),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        JLabel title = new JLabel(user.getName() + " portfolio");
        title.setForeground(WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(title);
        panel.add(cashLabel);
        panel.add(totalLabel);
        panel.add(returnLabel);
        return panel;
    }

    private JPanel createTradePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(NAVY);
        for (Stock stock : market.stocks()) tickerBox.addItem(stock.getSymbol());
        tickerBox.setPreferredSize(new Dimension(90, 30));
        quantitySpinner.setPreferredSize(new Dimension(80, 30));
        styleControlLabel(panel, "Ticker:");
        panel.add(tickerBox);
        styleControlLabel(panel, "Quantity:");
        panel.add(quantitySpinner);
        JButton buyButton = new JButton("Buy");
        JButton sellButton = new JButton("Sell");
        JButton priceButton = new JButton("Update price");
        JButton refreshButton = new JButton("Refresh");
        styleButton(buyButton, GREEN);
        styleButton(sellButton, RED);
        styleButton(priceButton, BLUE);
        styleButton(refreshButton, BLUE);
        buyButton.addActionListener(event -> executeTrade(true));
        sellButton.addActionListener(event -> executeTrade(false));
        priceButton.addActionListener(event -> updatePrice());
        refreshButton.addActionListener(event -> refresh());
        panel.add(buyButton);
        panel.add(sellButton);
        panel.add(priceButton);
        panel.add(refreshButton);
        return panel;
    }

    private JPanel withTitle(String title, java.awt.Component content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(LIGHT_BLUE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(132, 170, 207)), title));
        if (content instanceof JScrollPane scrollPane) scrollPane.getViewport().setBackground(WHITE);
        panel.add(content);
        return panel;
    }

    private void styleControlLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setForeground(WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(label);
    }

    private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
    }

    private void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setGridColor(new Color(218, 229, 240));
        table.setSelectionBackground(new Color(207, 228, 247));
        table.setSelectionForeground(new Color(20, 45, 72));
        table.getTableHeader().setBackground(NAVY);
        table.getTableHeader().setForeground(WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
    }

    private void executeTrade(boolean buying) {
        Stock stock = market.find((String) tickerBox.getSelectedItem());
        int quantity = (Integer) quantitySpinner.getValue();
        try {
            Transaction transaction = buying ? user.buy(stock, quantity) : user.sell(stock, quantity);
                repository.save(user.getPortfolio(), user.getPortfolio().totalValue(user.getCash(), market),
                    transaction.getType().toString());
            refresh();
            JOptionPane.showMessageDialog(this, String.format("%s %d %s for $%.2f total.",
                    transaction.getType(), quantity, transaction.getStock().getSymbol(), transaction.getTotal()));
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Trade rejected", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updatePrice() {
        String symbol = (String) tickerBox.getSelectedItem();
        JTextField input = new JTextField(String.format("%.2f", market.find(symbol).getPrice()));
        int result = JOptionPane.showConfirmDialog(this, input, "New price for " + symbol,
                JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                market.updatePrice(symbol, Double.parseDouble(input.getText().trim()));
                refresh();
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(this, exception.getMessage(), "Invalid price", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void refresh() {
        repository.reload(user.getPortfolio());
        marketModel.setRowCount(0);
        for (Stock stock : market.stocks()) {
            marketModel.addRow(new Object[] { stock.getSymbol(), stock.getCompanyName(), money(stock.getPrice()) });
        }
        holdingsModel.setRowCount(0);
        for (Map.Entry<String, Integer> holding : user.getPortfolio().getHoldings().entrySet()) {
            Stock stock = market.find(holding.getKey());
            holdingsModel.addRow(new Object[] { stock.getSymbol(), holding.getValue(), money(stock.getPrice()),
                    money(stock.getPrice() * holding.getValue()) });
        }
        double total = user.getPortfolio().totalValue(user.getCash(), market);
        cashLabel.setText("Cash: " + money(user.getCash()));
        totalLabel.setText("Total: " + money(total));
        returnLabel.setText("Return: " + money(total - user.getPortfolio().getStartingCash()));
        List<String> history = new ArrayList<>();
        for (PortfolioSnapshot snapshot : user.getPortfolio().getPerformanceHistory()) {
                history.add(snapshot.timestamp().format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
                    + "  " + snapshot.action() + "  " + money(snapshot.totalValue()));
        }
        historyList.setModel(new javax.swing.AbstractListModel<>() {
            @Override
            public int getSize() { return history.size(); }
            @Override
            public String getElementAt(int index) { return history.get(index); }
        });
        chart.setData(user.getPortfolio().getPerformanceHistory(), user.getPortfolio().getStartingCash());
        historyList.setBackground(WHITE);
        historyList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cashLabel.setForeground(WHITE);
        totalLabel.setForeground(WHITE);
        returnLabel.setForeground(WHITE);
        cashLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        returnLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    private String money(double value) { return String.format("$%.2f", value); }
}

class ProfitLossChart extends JPanel {
    private static final Color NAVY = new Color(11, 38, 74);
    private static final Color BLUE = new Color(24, 96, 168);
    private static final Color GREEN = new Color(20, 130, 85);
    private static final Color RED = new Color(190, 60, 65);
    private List<PortfolioSnapshot> snapshots = List.of();
    private double startingValue;

    ProfitLossChart() {
        setPreferredSize(new Dimension(100, 170));
        setBackground(Color.WHITE);
    }

    void setData(List<PortfolioSnapshot> snapshots, double startingValue) {
        this.snapshots = List.copyOf(snapshots);
        this.startingValue = startingValue;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int left = 45;
        int right = getWidth() - 18;
        int top = 18;
        int bottom = getHeight() - 28;
        int zero = (top + bottom) / 2;
        canvas.setColor(new Color(220, 230, 240));
        canvas.drawLine(left, zero, right, zero);
        canvas.setColor(NAVY);
        canvas.setFont(new Font("Segoe UI", Font.BOLD, 11));
        canvas.drawString("$0", 12, zero + 4);
        if (snapshots.isEmpty()) {
            canvas.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            canvas.drawString("Complete a trade to plot portfolio profit and loss.", left + 12, zero - 5);
            canvas.dispose();
            return;
        }
        double maximum = 1;
        for (PortfolioSnapshot snapshot : snapshots) {
            maximum = Math.max(maximum, Math.abs(snapshot.totalValue() - startingValue));
            maximum = Math.max(maximum, snapshot.activityValue());
        }
        int slotWidth = Math.max(8, (right - left) / snapshots.size());
        int barWidth = Math.max(5, slotWidth - 8);
        for (int index = 0; index < snapshots.size(); index++) {
            PortfolioSnapshot snapshot = snapshots.get(index);
            double activity = snapshot.activityValue();
            double chartValue = "BUY".equals(snapshot.action()) ? -activity : activity;
            int barHeight = (int) (Math.abs(chartValue) / maximum * ((bottom - top) / 2.0 - 8));
            int x = left + index * slotWidth + 4;
            int y = chartValue >= 0 ? zero - barHeight : zero;
            canvas.setColor("BUY".equals(snapshot.action()) ? RED : GREEN);
            canvas.fillRoundRect(x, y, barWidth, Math.max(2, barHeight), 4, 4);
        }
        canvas.setColor(BLUE);
        canvas.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        canvas.drawString("SELL", right - 42, top + 5);
        canvas.setColor(RED);
        canvas.drawString("BUY", right - 35, bottom + 18);
        canvas.dispose();
    }
}

class Stock {
    private final String symbol;
    private final String companyName;
    private double price;

    Stock(String symbol, String companyName, double price) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.price = price;
    }

    String getSymbol() { return symbol; }
    String getCompanyName() { return companyName; }
    double getPrice() { return price; }
    void setPrice(double price) {
        if (price <= 0) throw new IllegalArgumentException("Price must be greater than zero.");
        this.price = price;
    }
}

class Market {
    private final Map<String, Stock> stocks = new LinkedHashMap<>();

    Market() {
        add(new Stock("AAPL", "Apple Inc.", 189.84));
        add(new Stock("MSFT", "Microsoft Corp.", 417.23));
        add(new Stock("TSLA", "Tesla Inc.", 177.48));
        add(new Stock("NVDA", "NVIDIA Corp.", 121.79));
    }

    private void add(Stock stock) { stocks.put(stock.getSymbol(), stock); }
    List<Stock> stocks() { return List.copyOf(stocks.values()); }
    Stock find(String symbol) { return stocks.get(symbol); }
    void updatePrice(String symbol, double price) { find(symbol).setPrice(price); }

}

class User {
    private final String name;
    private double cash;
    private final Portfolio portfolio;

    User(String name, double startingCash) {
        this.name = name;
        this.cash = startingCash;
        this.portfolio = new Portfolio(startingCash);
    }

    String getName() { return name; }
    double getCash() { return cash; }
    Portfolio getPortfolio() { return portfolio; }

    Transaction buy(Stock stock, int quantity) {
        validateQuantity(quantity);
        double total = stock.getPrice() * quantity;
        if (total > cash) throw new IllegalArgumentException("Insufficient cash.");
        cash -= total;
        portfolio.addShares(stock, quantity);
        return portfolio.record(new Transaction(Transaction.Type.BUY, stock, quantity, stock.getPrice()));
    }

    Transaction sell(Stock stock, int quantity) {
        validateQuantity(quantity);
        if (portfolio.sharesOf(stock) < quantity) throw new IllegalArgumentException("Not enough shares.");
        cash += stock.getPrice() * quantity;
        portfolio.removeShares(stock, quantity);
        return portfolio.record(new Transaction(Transaction.Type.SELL, stock, quantity, stock.getPrice()));
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");
    }
}

class Transaction {
    enum Type { BUY, SELL }
    private final Type type;
    private final Stock stock;
    private final int quantity;
    private final double price;
    Transaction(Type type, Stock stock, int quantity, double price) {
        this.type = type;
        this.stock = stock;
        this.quantity = quantity;
        this.price = price;
    }

    Type getType() { return type; }
    Stock getStock() { return stock; }
    double getTotal() { return price * quantity; }
}

class Portfolio {
    private final double startingCash;
    private final Map<String, Integer> holdings = new LinkedHashMap<>();
    private final List<PortfolioSnapshot> performanceHistory = new ArrayList<>();
    private double lastTransactionValue;

    Portfolio(double startingCash) { this.startingCash = startingCash; }
    double getStartingCash() { return startingCash; }
    void addShares(Stock stock, int quantity) { holdings.merge(stock.getSymbol(), quantity, Integer::sum); }
    void removeShares(Stock stock, int quantity) { holdings.computeIfPresent(stock.getSymbol(), (key, value) -> value - quantity); }
    int sharesOf(Stock stock) { return holdings.getOrDefault(stock.getSymbol(), 0); }
    Transaction record(Transaction transaction) {
        lastTransactionValue = transaction.getTotal();
        return transaction;
    }

    double marketValue(Market market) {
        double value = 0;
        for (Map.Entry<String, Integer> holding : holdings.entrySet()) {
            Stock stock = market.find(holding.getKey());
            if (stock != null) value += stock.getPrice() * holding.getValue();
        }
        return value;
    }

    double totalValue(double cash, Market market) {
        return cash + marketValue(market);
    }

    void addSnapshot(PortfolioSnapshot snapshot) { performanceHistory.add(snapshot); }
    void clearPerformanceHistory() { performanceHistory.clear(); }
    double getLastTransactionValue() { return lastTransactionValue; }
    Map<String, Integer> getHoldings() { return Collections.unmodifiableMap(holdings); }
    List<PortfolioSnapshot> getPerformanceHistory() { return performanceHistory; }
}

record PortfolioSnapshot(LocalDateTime timestamp, double totalValue, String action, double activityValue) {}

class PortfolioRepository {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final Path file;

    PortfolioRepository(Path file) { this.file = file; }

    void save(Portfolio portfolio, double totalValue, String action) {
        double activityValue = portfolio.getLastTransactionValue();
        PortfolioSnapshot snapshot = new PortfolioSnapshot(LocalDateTime.now(), totalValue, action, activityValue);
        portfolio.addSnapshot(snapshot);
        try {
            String line = snapshot.timestamp().format(FORMATTER) + "," + snapshot.totalValue() + ","
                    + snapshot.action() + "," + snapshot.activityValue() + System.lineSeparator();
            Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            System.out.println("Could not save history: " + exception.getMessage());
        }
    }

    void load(Portfolio portfolio) {
        if (!Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file)) {
                String[] values = line.split(",");
                if (values.length >= 2) {
                    String action = values.length >= 3 ? values[2] : "HISTORY";
                    double activity = values.length >= 4 ? Double.parseDouble(values[3]) : 0;
                    portfolio.addSnapshot(new PortfolioSnapshot(
                        LocalDateTime.parse(values[0], FORMATTER), Double.parseDouble(values[1]), action, activity));
                }
            }
        } catch (IOException | RuntimeException exception) {
            System.out.println("Could not load history: " + exception.getMessage());
        }
    }

    void reload(Portfolio portfolio) {
        portfolio.clearPerformanceHistory();
        load(portfolio);
    }
}
