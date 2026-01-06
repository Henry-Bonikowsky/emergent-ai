import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Swing-based visualizer for the simulation.
 * Shows the decay landscape, agent position, and coherence.
 */
public class Visualizer extends JPanel {
    private static final int CELL_SIZE = 50;
    private static final int PADDING = 20;

    private World world;
    private JFrame frame;
    private JLabel statsLabel;
    private boolean visualMode = true;
    private int delayMs = 50;

    public Visualizer(World world) {
        this.world = world;

        int width = World.SIZE * CELL_SIZE + PADDING * 2;
        int height = World.SIZE * CELL_SIZE + PADDING * 2 + 60;

        setPreferredSize(new Dimension(width, height));
        setBackground(Color.BLACK);

        frame = new JFrame("Emergent AI - Self-Referential Goal Discovery");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(this, BorderLayout.CENTER);

        // Stats panel
        JPanel statsPanel = new JPanel();
        statsPanel.setBackground(Color.DARK_GRAY);
        statsLabel = new JLabel("Episode: 0 | Steps: 0 | Avg: 0.0");
        statsLabel.setForeground(Color.WHITE);
        statsLabel.setFont(new Font("Monospaced", Font.PLAIN, 14));
        statsPanel.add(statsLabel);
        frame.add(statsPanel, BorderLayout.NORTH);

        // Control panel
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(Color.DARK_GRAY);

        JButton toggleButton = new JButton("Toggle Visual (V)");
        toggleButton.addActionListener(e -> toggleVisualMode());
        controlPanel.add(toggleButton);

        JButton speedUpButton = new JButton("Faster (+)");
        speedUpButton.addActionListener(e -> adjustSpeed(-10));
        controlPanel.add(speedUpButton);

        JButton slowDownButton = new JButton("Slower (-)");
        slowDownButton.addActionListener(e -> adjustSpeed(10));
        controlPanel.add(slowDownButton);

        frame.add(controlPanel, BorderLayout.SOUTH);

        // Keyboard controls
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_V: toggleVisualMode(); break;
                    case KeyEvent.VK_PLUS:
                    case KeyEvent.VK_EQUALS: adjustSpeed(-10); break;
                    case KeyEvent.VK_MINUS: adjustSpeed(10); break;
                    case KeyEvent.VK_ESCAPE: System.exit(0); break;
                }
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void toggleVisualMode() {
        visualMode = !visualMode;
        System.out.println("Visual mode: " + (visualMode ? "ON" : "OFF (fast training)"));
    }

    private void adjustSpeed(int delta) {
        delayMs = Math.max(0, Math.min(500, delayMs + delta));
        System.out.println("Delay: " + delayMs + "ms");
    }

    public void updateStats(long episode, int steps, double rollingAvg) {
        if (visualMode) {
            statsLabel.setText(String.format(
                "Episode: %d | Steps: %d | Avg(100): %.1f | Coherence: %.2f",
                episode, steps, rollingAvg, world.getCoherence()
            ));
            repaint();

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void setWorld(World w) {
        this.world = w;
    }

    public boolean isVisualMode() {
        return visualMode;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw decay landscape
        for (int x = 0; x < World.SIZE; x++) {
            for (int y = 0; y < World.SIZE; y++) {
                double decay = world.getDecayAt(x, y);
                // Map decay [0.1, 3.0] to color (green = low decay, red = high decay)
                float normalized = (float) Math.max(0, Math.min(1, (decay - 0.1) / 2.9));
                Color cellColor = new Color(
                    Math.max(0f, Math.min(1f, normalized)),
                    Math.max(0f, Math.min(1f, 1 - normalized)),
                    0.2f
                );

                int px = PADDING + x * CELL_SIZE;
                int py = PADDING + y * CELL_SIZE;

                g2.setColor(cellColor);
                g2.fillRect(px, py, CELL_SIZE - 2, CELL_SIZE - 2);

                // Grid lines
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(px, py, CELL_SIZE - 2, CELL_SIZE - 2);
            }
        }

        // Draw agent
        int agentPx = PADDING + world.getAgentX() * CELL_SIZE + CELL_SIZE / 2;
        int agentPy = PADDING + world.getAgentY() * CELL_SIZE + CELL_SIZE / 2;
        int agentRadius = (int) (CELL_SIZE * 0.3 * (0.5 + world.getCoherence() * 0.5));

        // Agent glow based on coherence
        float alpha = Math.max(0f, Math.min(1f, (float) world.getCoherence() * 0.3f));
        g2.setColor(new Color(1f, 1f, 1f, alpha));
        g2.fillOval(agentPx - agentRadius - 5, agentPy - agentRadius - 5,
                    agentRadius * 2 + 10, agentRadius * 2 + 10);

        // Agent body
        g2.setColor(Color.WHITE);
        g2.fillOval(agentPx - agentRadius, agentPy - agentRadius,
                    agentRadius * 2, agentRadius * 2);

        // Coherence bar
        int barX = PADDING;
        int barY = PADDING + World.SIZE * CELL_SIZE + 10;
        int barWidth = World.SIZE * CELL_SIZE;
        int barHeight = 20;

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(barX, barY, barWidth, barHeight);

        g2.setColor(new Color(0.2f, 0.8f, 0.3f));
        g2.fillRect(barX, barY, (int) (barWidth * world.getCoherence()), barHeight);

        g2.setColor(Color.WHITE);
        g2.drawRect(barX, barY, barWidth, barHeight);
        g2.drawString("Coherence", barX + 5, barY + 15);
    }
}
