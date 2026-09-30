package nexus.dashboard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

/**
 * The NeuralVisualizerPanel is a custom graphical component that provides a futuristic, 
 * node-based representation of the application's internal state.
 * 
 * It visually displays the core modules (Dashboard, Finance, Tasks, etc.) as connected nodes.
 * By implementing NexusListener, it can react to global events (like budget changes) 
 * and animate particles flowing between nodes to represent data movement.
 */
public class NeuralVisualizerPanel extends JPanel implements NexusListener {
    private String[] nodeNames = {"Finance", "Market", "Tasks", "Goals", "Media", "Analytics"};
    private Map<String, Point> nodeCenters = new HashMap<>();
    private List<Particle> particles = new ArrayList<>();
    private Timer timer;
    private Color centerColor = Color.CYAN;
    private float pulsePhase = 0.0f;
    private boolean budgetExceeded = false;
    private int currentScore = 100;

    public NeuralVisualizerPanel() {
        setBackground(new Color(20, 20, 20));
        EventBus.getInstance().subscribeToAll(this); // Assuming subscribeToAll exists, wait...
        
                timer = new Timer(16, e -> {
            updateParticles();
            repaint();
        });
        timer.start();

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();
                for (String node : nodeNames) {
                    Point p = nodeCenters.get(node);
                    if (p != null) {
                        double dist = Math.hypot(mx - p.x, my - p.y);
                        if (dist <= 25) {
                            EventBus.getInstance().publish(new NexusEvent("NAVIGATION_REQUEST", node, "Dashboard"));
                            break;
                        }
                    }
                }
            }
        });
        
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();
                boolean overNode = false;
                for (String node : nodeNames) {
                    Point p = nodeCenters.get(node);
                    if (p != null && Math.hypot(mx - p.x, my - p.y) <= 25) {
                        overNode = true;
                        break;
                    }
                }
                setCursor(new Cursor(overNode ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });
    }

    /**
     * This method is the heart of the custom graphics. It is called repeatedly by the Swing framework
     * (triggered by our Timer) to redraw the screen frame-by-frame, creating a smooth animation.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int radius = Math.min(cx, cy) - 40;

        // Step 1: Draw lines connecting all nodes to each other, creating a 'web' effect.
        // We use a semi-transparent cyan color (alpha = 30) for a subtle tech look.
        g2.setColor(new Color(0, 255, 255, 30));
        for (int i = 0; i < nodeNames.length; i++) {
            for (int j = i + 1; j < nodeNames.length; j++) {
                Point p1 = getPoint(i, cx, cy, radius);
                Point p2 = getPoint(j, cx, cy, radius);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y); // Draw line between two peripheral nodes
            }
            Point p = getPoint(i, cx, cy, radius);
            g2.drawLine(cx, cy, p.x, p.y); // Draw line from center to the peripheral node
        }

        // Step 2: Calculate the glowing effect of the central core.
        // Math.sin creates a smooth oscillation between -1 and 1.
        // By mapping this to our alpha (transparency) channel, we get a breathing/pulsing effect.
        pulsePhase += 0.05f;
        int pulseAlpha = (int) (155 + 100 * Math.sin(pulsePhase));
        if(pulseAlpha < 0) pulseAlpha = 0;
        if(pulseAlpha > 255) pulseAlpha = 255;

        // Draw ambient glow
        g2.setColor(new Color(centerColor.getRed(), centerColor.getGreen(), centerColor.getBlue(), pulseAlpha / 3));
        g2.fillOval(cx - 30, cy - 30, 60, 60);

        // Draw center core
        g2.setColor(new Color(centerColor.getRed(), centerColor.getGreen(), centerColor.getBlue(), pulseAlpha));
        g2.fillOval(cx - 15, cy - 15, 30, 30);
        
        // Draw nodes
        for (int i = 0; i < nodeNames.length; i++) {
            Point p = getPoint(i, cx, cy, radius);
            nodeCenters.put(nodeNames[i], p);

            g2.setColor(Color.CYAN);
            g2.fillOval(p.x - 20, p.y - 20, 40, 40);
            
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int sw = fm.stringWidth(nodeNames[i]);
            g2.drawString(nodeNames[i], p.x - sw / 2, p.y + 35);
        }

        // Draw particles
        g2.setColor(new Color(0, 255, 255, 200));
        for (Particle p : particles) {
            g2.fillOval((int)p.x - 4, (int)p.y - 4, 8, 8);
        }
    }

    private Point getPoint(int index, int cx, int cy, int r) {
        double angle = 2 * Math.PI * index / nodeNames.length - Math.PI / 2;
        int x = cx + (int) (r * Math.cos(angle));
        int y = cy + (int) (r * Math.sin(angle));
        return new Point(x, y);
    }

    private void updateParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.move();
            if (p.isDone()) {
                particles.remove(i);
            }
        }
    }

    @Override
    public void onEvent(NexusEvent event) {
        if ("BUDGET_UPDATED".equals(event.getType())) {
            nexus.finance.Budget budget = (nexus.finance.Budget) event.getPayload();
            budgetExceeded = budget.getRemaining() < 0;
            updateCenterColor();
        } else if ("SCORE_UPDATED".equals(event.getType())) {
            currentScore = (Integer) event.getPayload();
            updateCenterColor();
        }

        String sourceStr = event.getSource();
        if (sourceStr == null) return;
        Point startNode = nodeCenters.get(sourceStr);
        if (startNode == null) return;

        // Animate to all other nodes
        for (String node : nodeNames) {
            if (!node.equals(sourceStr)) {
                Point targetNode = nodeCenters.get(node);
                if (targetNode != null) {
                    particles.add(new Particle(startNode.x, startNode.y, targetNode.x, targetNode.y));
                }
            }
        }
    }

    private void updateCenterColor() {
        if (budgetExceeded) {
            centerColor = Color.RED;
        } else if (currentScore >= 150) {
            centerColor = Color.GREEN;
        } else {
            centerColor = Color.CYAN;
        }
    }

    private static class Particle {
        double x, y;
        double tx, ty;
        double dx, dy;
        double dist;
        double speed = 5.0;

        public Particle(double sx, double sy, double tx, double ty) {
            this.x = sx;
            this.y = sy;
            this.tx = tx;
            this.ty = ty;
            double angle = Math.atan2(ty - sy, tx - sx);
            this.dx = Math.cos(angle) * speed;
            this.dy = Math.sin(angle) * speed;
            this.dist = Math.hypot(tx - sx, ty - sy);
        }

        public void move() {
            x += dx;
            y += dy;
            dist -= speed;
        }

        public boolean isDone() {
            return dist <= 0;
        }
    }
}

