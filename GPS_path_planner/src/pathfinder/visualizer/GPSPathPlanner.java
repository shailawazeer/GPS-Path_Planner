package pathfinder.visualizer;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class GPSPathPlanner extends JFrame {
    private static final int GRID_SIZE = 20;
    private static final int CELL_SIZE = 25;
    private MapPanel mapPanel;
    private JTextField startField, endField;
    private JComboBox<String> algorithmCombo;
    private JButton findPathButton, clearButton;
    private JLabel statusLabel, distanceLabel;



    // Grid representation: 0=road, 1=obstacle, 2=start, 3=end, 4=path
    private int[][] grid;
    private Point startPoint, endPoint;
    private java.util.List<Point> currentPath;
    private Timer animationTimer;
    private int animationStep;

    public GPSPathPlanner() {
        setTitle("GPS Navigation Path Planner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout());

        initializeGrid();
        initializeComponents();
        setupLayout();

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void initializeGrid() {
        grid = new int[GRID_SIZE][GRID_SIZE];
        currentPath = new ArrayList<>();

        // Create a fixed, realistic road network with obstacles
        createRealisticMap();
    }

    private void createRealisticMap() {
        // Clear grid first
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                grid[i][j] = 0; // All roads initially
            }
        }

        // Add realistic obstacle patterns (buildings, construction zones)

        // Building blocks
        addRoadBlock(3, 3, 5, 5);   // Small building
        addRoadBlock(8, 2, 10, 4);  // Office building
        addRoadBlock(15, 6, 17, 8); // Shopping center
        addRoadBlock(2, 12, 4, 15); // Residential block
        addRoadBlock(12, 10, 14, 13); // Hospital

        // Construction zones (temporary blocks)
        addRoadBlock(7, 8, 7, 11);  // Road construction
        addRoadBlock(16, 16, 18, 16); // Bridge repair

        // Natural obstacles
        addRoadBlock(6, 15, 8, 17); // Park/Lake
        addRoadBlock(11, 5, 13, 7); // Hill/Forest

        // Random scattered obstacles (parked cars, minor blocks)
        int[] fixedObstacles = {
                1, 7,   // Parked car
                4, 9,   // Traffic light maintenance
                9, 14,  // Accident scene
                14, 2,  // Police checkpoint
                18, 11, // Delivery truck
                6, 6,   // Street vendor
                13, 18  // Road maintenance
        };

        for (int i = 0; i < fixedObstacles.length; i += 2) {
            int x = fixedObstacles[i];
            int y = fixedObstacles[i + 1];
            if (x < GRID_SIZE && y < GRID_SIZE) {
                grid[x][y] = 1;
            }
        }
    }

    private void addRoadBlock(int x1, int y1, int x2, int y2) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                if (x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE) {
                    grid[x][y] = 1;
                }
            }
        }
    }

    private void initializeComponents() {
        mapPanel = new MapPanel();
        startField = new JTextField("0,0", 8);
        endField = new JTextField("19,19", 8);
        algorithmCombo = new JComboBox<>(new String[]{"BFS (Breadth-First Search)", "DFS (Depth-First Search)"});
        findPathButton = new JButton("Find Path");
        clearButton = new JButton("Clear Path");
        statusLabel = new JLabel("Enter start and end coordinates");
        distanceLabel = new JLabel("Distance: 0");

        findPathButton.addActionListener(e -> findPath());
        clearButton.addActionListener(e -> clearPath());

        mapPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX() / CELL_SIZE;
                int y = e.getY() / CELL_SIZE;
                if (x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        setStartPoint(x, y);
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        setEndPoint(x, y);
                    }
                }
            }
        });
    }

    private void setupLayout() {
        // Control panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Navigation Control"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        controlPanel.add(new JLabel("Start (x,y):"), gbc);
        gbc.gridx = 1;
        controlPanel.add(startField, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        controlPanel.add(new JLabel("End (x,y):"), gbc);
        gbc.gridx = 3;
        controlPanel.add(endField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        controlPanel.add(new JLabel("Algorithm:"), gbc);
        gbc.gridx = 2; gbc.gridwidth = 2;
        controlPanel.add(algorithmCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        controlPanel.add(findPathButton, gbc);
        gbc.gridx = 2; gbc.gridwidth = 2;
        controlPanel.add(clearButton, gbc);

        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout());
        statusPanel.add(statusLabel);
        statusPanel.add(Box.createHorizontalStrut(20));
        statusPanel.add(distanceLabel);

        // Legend panel
        JPanel legendPanel = createLegendPanel();

        // Main layout
        add(controlPanel, BorderLayout.NORTH);
        add(mapPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        add(legendPanel, BorderLayout.EAST);
    }

    private JPanel createLegendPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Legend"));

        panel.add(createLegendItem(Color.WHITE, "Open Road"));
        panel.add(createLegendItem(Color.BLACK, "Blocked Road"));
        panel.add(createLegendItem(Color.GREEN, "Start Point"));
        panel.add(createLegendItem(Color.RED, "End Point"));
        panel.add(createLegendItem(Color.BLUE, "Path Route"));
        panel.add(Box.createVerticalStrut(10));

        return panel;
    }

    private JPanel createLegendItem(Color color, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel colorBox = new JPanel();
        colorBox.setBackground(color);
        colorBox.setPreferredSize(new Dimension(15, 15));
        colorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        item.add(colorBox);
        item.add(new JLabel(text));
        return item;
    }

    private void setStartPoint(int x, int y) {
        if (grid[x][y] != 1) { // Not an obstacle
            startPoint = new Point(x, y);
            startField.setText(x + "," + y);
            mapPanel.repaint();
        }
    }

    private void setEndPoint(int x, int y) {
        if (grid[x][y] != 1) { // Not an obstacle
            endPoint = new Point(x, y);
            endField.setText(x + "," + y);
            mapPanel.repaint();
        }
    }

    private void findPath() {
        try {
            String[] startCoords = startField.getText().split(",");
            String[] endCoords = endField.getText().split(",");

            int startX = Integer.parseInt(startCoords[0].trim());
            int startY = Integer.parseInt(startCoords[1].trim());
            int endX = Integer.parseInt(endCoords[0].trim());
            int endY = Integer.parseInt(endCoords[1].trim());

            if (!isValidPoint(startX, startY) || !isValidPoint(endX, endY)) {
                statusLabel.setText("Invalid coordinates!");
                return;
            }

            if (grid[startX][startY] == 1 || grid[endX][endY] == 1) {
                statusLabel.setText("Start or end point is blocked!");
                return;
            }

            startPoint = new Point(startX, startY);
            endPoint = new Point(endX, endY);

            String algorithm = (String) algorithmCombo.getSelectedItem();
            List<Point> path;

            if (algorithm.startsWith("BFS")) {
                path = findPathBFS(startPoint, endPoint);
            } else {
                path = findPathDFS(startPoint, endPoint);
            }

            if (path != null && !path.isEmpty()) {
                currentPath = path;
                animatePathDrawing();
                distanceLabel.setText("Distance: " + (path.size() - 1) + " units");
                statusLabel.setText("Path found using " + algorithm);
            } else {
                statusLabel.setText("No path found!");
                distanceLabel.setText("Distance: 0");
            }

        } catch (Exception e) {
            statusLabel.setText("Invalid input format! Use x,y");
        }
    }

    private boolean isValidPoint(int x, int y) {
        return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE;
    }

    private List<Point> findPathBFS(Point start, Point end) {
        Queue<Point> queue = new LinkedList<>();
        Map<Point, Point> parent = new HashMap<>();
        Set<Point> visited = new HashSet<>();

        queue.offer(start);
        visited.add(start);
        parent.put(start, null);

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            Point current = queue.poll();

            if (current.equals(end)) {
                return reconstructPath(parent, start, end);
            }

            for (int i = 0; i < 4; i++) {
                int newX = current.x + dx[i];
                int newY = current.y + dy[i];
                Point next = new Point(newX, newY);

                if (isValidPoint(newX, newY) && !visited.contains(next) && grid[newX][newY] != 1) {
                    queue.offer(next);
                    visited.add(next);
                    parent.put(next, current);
                }
            }
        }

        return null; // No path found
    }

    private List<Point> findPathDFS(Point start, Point end) {
        Set<Point> visited = new HashSet<>();
        List<Point> path = new ArrayList<>();

        if (dfsHelper(start, end, visited, path)) {
            return new ArrayList<>(path);
        }

        return null; // No path found
    }

    private boolean dfsHelper(Point current, Point end, Set<Point> visited, List<Point> path) {
        visited.add(current);
        path.add(current);

        if (current.equals(end)) {
            return true;
        }
//        explores all 4 adjacent cells (up, down, left, right) using direction arrays
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        for (int i = 0; i < 4; i++) {
            int newX = current.x + dx[i];
            int newY = current.y + dy[i];
            Point next = new Point(newX, newY);

            if (isValidPoint(newX, newY) && !visited.contains(next) && grid[newX][newY] != 1) {
                if (dfsHelper(next, end, visited, path)) {
                    return true;
                }
            }
        }

        path.remove(path.size() - 1); // Backtrack
        return false;
    }

    private List<Point> reconstructPath(Map<Point, Point> parent, Point start, Point end) {
        List<Point> path = new ArrayList<>();
        Point current = end;

        while (current != null) {
            path.add(0, current);
            current = parent.get(current);
        }

        return path;
    }

    private void animatePathDrawing() {
        if (animationTimer != null) {
            animationTimer.stop();
        }

        animationStep = 0;
        animationTimer = new Timer(100, e -> {
            animationStep++;
            mapPanel.repaint();

            if (animationStep >= currentPath.size()) {
                animationTimer.stop();
            }
        });

        animationTimer.start();
    }

    private void clearPath() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        currentPath.clear();
        animationStep = 0;
        startPoint = null;
        endPoint = null;
        statusLabel.setText("Path cleared");
        distanceLabel.setText("Distance: 0");
        mapPanel.repaint();
    }

    private class MapPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw grid
            for (int x = 0; x < GRID_SIZE; x++) {
                for (int y = 0; y < GRID_SIZE; y++) {
                    Rectangle rect = new Rectangle(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);

                    if (grid[x][y] == 1) {
                        g2d.setColor(Color.BLACK); // Obstacle/blocked road
                        g2d.fill(rect);
                    } else {
                        g2d.setColor(Color.WHITE); // Open road
                        g2d.fill(rect);
                    }

                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.draw(rect);
                }
            }

            // Draw animated path
            g2d.setStroke(new BasicStroke(4));
            for (int i = 0; i < Math.min(animationStep, currentPath.size() - 1); i++) {
                Point p1 = currentPath.get(i);
                Point p2 = currentPath.get(i + 1);

                g2d.setColor(Color.BLUE);
                g2d.drawLine(
                        p1.x * CELL_SIZE + CELL_SIZE / 2,
                        p1.y * CELL_SIZE + CELL_SIZE / 2,
                        p2.x * CELL_SIZE + CELL_SIZE / 2,
                        p2.y * CELL_SIZE + CELL_SIZE / 2
                );
            }

            // Draw start point
            if (startPoint != null) {
                g2d.setColor(Color.GREEN);
                g2d.fillOval(
                        startPoint.x * CELL_SIZE + 5,
                        startPoint.y * CELL_SIZE + 5,
                        CELL_SIZE - 10,
                        CELL_SIZE - 10
                );
                g2d.setColor(Color.BLACK);
                g2d.drawString("START", startPoint.x * CELL_SIZE + 2, startPoint.y * CELL_SIZE + CELL_SIZE - 2);
            }

            // Draw end point
            if (endPoint != null) {
                g2d.setColor(Color.RED);
                g2d.fillOval(
                        endPoint.x * CELL_SIZE + 5,
                        endPoint.y * CELL_SIZE + 5,
                        CELL_SIZE - 10,
                        CELL_SIZE - 10
                );
                g2d.setColor(Color.WHITE);
                g2d.drawString("END", endPoint.x * CELL_SIZE + 5, endPoint.y * CELL_SIZE + CELL_SIZE - 2);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);
        }
    }

    private static void run() {
        try {
            UIManager.setLookAndFeel(UIManager.getLookAndFeel());
        } catch (Exception e) {
            System.out.println("Could not set system look and feel, using default");
        }

        new GPSPathPlanner().setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GPSPathPlanner::run);
    }
}