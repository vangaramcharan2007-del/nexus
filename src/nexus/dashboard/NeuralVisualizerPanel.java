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

public class NeuralVisualizerPanel extends JPanel implements NexusListener {
    private String[] nodeNames = {"Finance", "Market", "Tasks", "Goals", "Media", "Analytics"};
    private Map<String, Point> nodeCenters = new HashMap<>();
    private List<Particle> particles = new ArrayList<>();
    private Timer timer;

    public NeuralVisualizerPanel() {
        setBackground(new Color(20, 20, 20));
        EventBus.getInstance().subscribeToAll(this); // Assuming subscribeToAll exists, wait...
        
        timer = new Timer(16, e -> {
            updateParticles();
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int radius = Math.min(cx, cy) - 40;

        // Draw connections
        g2.setColor(new Color(0, 255, 255, 30));
        for (int i = 0; i < nodeNames.length; i++) {
            for (int j = i + 1; j < nodeNames.length; j++) {
                Point p1 = getPoint(i, cx, cy, radius);
                Point p2 = getPoint(j, cx, cy, radius);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            Point p = getPoint(i, cx, cy, radius);
            g2.drawLine(cx, cy, p.x, p.y);
        }

        // Draw center node
        g2.setColor(Color.WHITE);
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
