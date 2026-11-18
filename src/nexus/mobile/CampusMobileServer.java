package nexus.mobile;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

import nexus.database.DatabaseService;
import nexus.media.CinemaBooking;
import nexus.media.TicketQrGenerator;
import nexus.tasks.Task;

/**
 * Local Wi-Fi Mobile Companion & Live Receipt Server for NEXUS Platform.
 * Powered by pure JDK com.sun.net.httpserver with zero external dependencies.
 * Allows students & examiners to open live receipts and companion passes on their mobile browser
 * over the local campus Wi-Fi network (http://<laptop-ip>:8080).
 */
public class CampusMobileServer {

    private static CampusMobileServer instance;
    private HttpServer server;
    private int port = 8080;
    private String localIp = "127.0.0.1";
    private boolean running = false;

    private CampusMobileServer() {
        this.localIp = discoverLocalIp();
    }

    public static synchronized CampusMobileServer getInstance() {
        if (instance == null) {
            instance = new CampusMobileServer();
        }
        return instance;
    }

    public synchronized boolean start() {
        if (running) return true;

        this.localIp = discoverLocalIp();
        int[] candidatePorts = { 8080, 8081, 8082, 8888, 9090 };
        for (int p : candidatePorts) {
            try {
                server = HttpServer.create(new InetSocketAddress(p), 0);
                this.port = p;
                break;
            } catch (IOException ignored) {}
        }

        if (server == null) {
            System.err.println("[CampusMobileServer] Could not bind to any candidate port.");
            return false;
        }

        // Register HTTP Endpoints
        server.createContext("/", new DashboardHandler());
        server.createContext("/ticket", new TicketReceiptHandler());
        server.createContext("/api/status", new StatusApiHandler());
        server.createContext("/api/qr", new QrApiHandler());

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        running = true;

        System.out.println("[CampusMobileServer] Active on " + getAccessUrl() + " (Local Wi-Fi Companion)");
        return true;
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            running = false;
            System.out.println("[CampusMobileServer] Stopped.");
        }
    }

    public boolean isRunning() { return running; }
    public int getPort() { return port; }
    public String getLocalIp() { return localIp; }
    public String getAccessUrl() { return "http://" + localIp + ":" + port; }
    public String getTicketUrl(String bookingId) {
        return getAccessUrl() + "/ticket?id=" + (bookingId != null ? bookingId : "");
    }

    private String discoverLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    // --- HTTP HANDLERS ---

    private class TicketReceiptHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String bookingId = "";
            if (query != null && query.contains("id=")) {
                for (String param : query.split("&")) {
                    if (param.startsWith("id=")) {
                        bookingId = param.substring(3).trim();
                        break;
                    }
                }
            }

            DatabaseService db = DatabaseService.getInstance();
            List<CinemaBooking> bookings = db.loadBookings();
            CinemaBooking matched = null;

            if (!bookingId.isEmpty()) {
                for (CinemaBooking b : bookings) {
                    if (b.getId().equalsIgnoreCase(bookingId)) {
                        matched = b;
                        break;
                    }
                }
            }

            // Fallback to most recent booking if specific ID not found or query empty
            if (matched == null && !bookings.isEmpty()) {
                matched = bookings.get(bookings.size() - 1);
            }

            String html;
            if (matched != null) {
                html = buildTicketReceiptHtml(matched);
            } else {
                html = "<!DOCTYPE html><html><body style='font-family:sans-serif;background:#0b1329;color:#fff;text-align:center;padding:40px;'>" +
                        "<h2>No Active Ticket Found</h2><p>Please book a cinema seat in the NEXUS desktop app first.</p>" +
                        "<a href='/' style='color:#38bdf8;'>Go to Companion Dashboard</a></body></html>";
            }

            sendResponse(exchange, 200, html, "text/html; charset=UTF-8");
        }

        private String buildTicketReceiptHtml(CinemaBooking b) {
            String qrUrl = "/api/qr?data=" + java.net.URLEncoder.encode(
                    "NEXUS TICKET " + b.getId() + " | " + b.getMovieTitle() + " | Seats: " + b.getSeatsFormatted(),
                    StandardCharsets.UTF_8
            );

            return "<!DOCTYPE html>\n" +
                "<html lang='en'>\n" +
                "<head>\n" +
                "<meta charset='UTF-8'>\n" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "<title>NEXUS Cinema Ticket Receipt</title>\n" +
                "<style>\n" +
                "  * { box-sizing: border-box; margin: 0; padding: 0; }\n" +
                "  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0b1329; color: #f1f5f9; padding: 20px; display: flex; justify-content: center; align-items: center; min-height: 100vh; }\n" +
                "  .ticket-card { width: 100%; max-width: 400px; background: #1e293b; border-radius: 20px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.5); border: 1px solid #334155; }\n" +
                "  .ticket-top { background: linear-gradient(135deg, #0284c7, #2563eb); padding: 24px 20px; text-align: center; color: white; }\n" +
                "  .cinema-brand { font-size: 13px; text-transform: uppercase; letter-spacing: 2px; font-weight: 700; opacity: 0.9; }\n" +
                "  .movie-title { font-size: 24px; font-weight: 800; margin: 8px 0 4px; }\n" +
                "  .theater-info { font-size: 13px; opacity: 0.85; }\n" +
                "  .status-badge { display: inline-block; background: #10b981; color: white; font-size: 11px; font-weight: 800; padding: 4px 12px; border-radius: 99px; margin-top: 10px; text-transform: uppercase; }\n" +
                "  .ticket-body { padding: 20px; }\n" +
                "  .info-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px dashed #334155; font-size: 14px; }\n" +
                "  .info-label { color: #94a3b8; }\n" +
                "  .info-val { font-weight: 700; color: #fff; }\n" +
                "  .seats-highlight { color: #38bdf8; font-size: 16px; font-weight: 800; }\n" +
                "  .qr-section { text-align: center; margin: 20px 0 10px; padding: 14px; background: white; border-radius: 12px; display: inline-block; width: 100%; }\n" +
                "  .qr-section img { display: block; margin: 0 auto; }\n" +
                "  .qr-caption { color: #0f172a; font-size: 11px; font-weight: 700; margin-top: 6px; letter-spacing: 1px; text-transform: uppercase; }\n" +
                "  .ticket-footer { text-align: center; padding: 14px 20px; background: #0f172a; font-size: 12px; color: #64748b; border-top: 1px solid #1e293b; }\n" +
                "  .btn-dashboard { display: block; width: 100%; margin-top: 14px; padding: 12px; background: #334155; color: #fff; text-align: center; text-decoration: none; border-radius: 10px; font-weight: 700; font-size: 13px; }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class='ticket-card'>\n" +
                "  <div class='ticket-top'>\n" +
                "    <div class='cinema-brand'>NEXUS CINEMA ENTERTAINMENT</div>\n" +
                "    <div class='movie-title'>" + escape(b.getMovieTitle()) + "</div>\n" +
                "    <div class='theater-info'>📍 " + escape(b.getTheatre()) + " • Screen 1 (4K Atmos)</div>\n" +
                "    <div class='status-badge'>✓ " + escape(b.getStatus()) + " PASS</div>\n" +
                "  </div>\n" +
                "  <div class='ticket-body'>\n" +
                "    <div class='info-row'><span class='info-label'>Booking ID:</span><span class='info-val'>" + escape(b.getId()) + "</span></div>\n" +
                "    <div class='info-row'><span class='info-label'>Seats:</span><span class='seats-highlight'>" + escape(b.getSeatsFormatted()) + "</span></div>\n" +
                "    <div class='info-row'><span class='info-label'>Showtime:</span><span class='info-val'>" + escape(b.getShowTime()) + "</span></div>\n" +
                "    <div class='info-row'><span class='info-label'>Amount Paid:</span><span class='info-val' style='color:#34d399;'>₹" + String.format("%.2f", b.getFinalTotal()) + "</span></div>\n" +
                "    <div class='info-row'><span class='info-label'>Payment:</span><span class='info-val'>" + escape(b.getPaymentMethod()) + "</span></div>\n" +
                "    <div class='info-row'><span class='info-label'>Date:</span><span class='info-val'>" + (b.getBookingDate() != null ? b.getBookingDate().toString() : "Recent") + "</span></div>\n" +
                "    <div class='qr-section'>\n" +
                "      <img src='" + qrUrl + "' width='160' height='160' alt='Gate Barcode'>\n" +
                "      <div class='qr-caption'>Security Gate Pass • Admit All</div>\n" +
                "    </div>\n" +
                "    <a href='/' class='btn-dashboard'>Open Companion App</a>\n" +
                "  </div>\n" +
                "  <div class='ticket-footer'>NEXUS Verified Electronic Ticket • Present at gate on phone</div>\n" +
                "</div>\n" +
                "</body>\n" +
                "</html>";
        }
    }

    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            DatabaseService db = DatabaseService.getInstance();
            List<Task> tasks = db.loadTasks();
            List<CinemaBooking> bookings = db.loadBookings();

            StringBuilder tasksHtml = new StringBuilder();
            for (Task t : tasks) {
                tasksHtml.append(String.format(
                    "<li class='task-item'><span>%s <b>%s</b></span><span class='badge %s'>%s</span></li>",
                    t.isCompleted() ? "✅" : "⏳", escape(t.getTitle()), t.getPriority().name().toLowerCase(), t.getPriority()
                ));
            }
            if (tasks.isEmpty()) {
                tasksHtml.append("<li class='empty'>No active tasks recorded.</li>");
            }

            StringBuilder ticketsHtml = new StringBuilder();
            for (CinemaBooking b : bookings) {
                String passUrl = "/ticket?id=" + b.getId();
                ticketsHtml.append(String.format(
                    "<div class='ticket-card'>" +
                    "  <div class='t-title'>🎬 %s</div>" +
                    "  <div class='t-meta'>📍 %s | 🕒 %s</div>" +
                    "  <div class='t-seats'>💺 Seats: <b>%s</b> | Paid: <b>₹%.2f</b></div>" +
                    "  <a href='%s' style='display:inline-block;padding:8px 14px;background:#0284c7;color:#fff;border-radius:8px;text-decoration:none;font-weight:700;font-size:12px;margin-top:6px;'>📱 Open Verified Pass</a>" +
                    "</div>",
                    escape(b.getMovieTitle()), escape(b.getTheatre()), escape(b.getShowTime()),
                    escape(b.getSeatsFormatted()), b.getFinalTotal(), passUrl
                ));
            }
            if (bookings.isEmpty()) {
                ticketsHtml.append("<div class='empty'>No cinema tickets booked yet. Book passes in the Desktop Box Office!</div>");
            }

            String html = "<!DOCTYPE html>\n" +
                "<html lang='en'>\n" +
                "<head>\n" +
                "<meta charset='UTF-8'>\n" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "<title>NEXUS Mobile Companion</title>\n" +
                "<style>\n" +
                "  body { font-family: -apple-system, system-ui, sans-serif; background: #0b1329; color: #f1f5f9; margin: 0; padding: 16px; }\n" +
                "  .container { max-width: 480px; margin: 0 auto; }\n" +
                "  .header { display: flex; align-items: center; justify-content: space-between; padding-bottom: 16px; border-bottom: 1px solid #1e293b; }\n" +
                "  .logo { font-size: 20px; font-weight: 800; color: #38bdf8; letter-spacing: 1px; }\n" +
                "  .badge-online { background: #10b981; color: white; padding: 4px 10px; border-radius: 99px; font-size: 11px; font-weight: 700; }\n" +
                "  .card { background: #1e293b; border: 1px solid #334155; border-radius: 16px; padding: 18px; margin-top: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.3); }\n" +
                "  .card-title { font-size: 14px; text-transform: uppercase; letter-spacing: 1px; color: #94a3b8; font-weight: 700; margin-bottom: 12px; }\n" +
                "  .student-name { font-size: 18px; font-weight: 700; color: #fff; }\n" +
                "  .student-meta { font-size: 13px; color: #64748b; margin-top: 2px; }\n" +
                "  .task-list { list-style: none; padding: 0; margin: 0; }\n" +
                "  .task-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #334155; font-size: 14px; }\n" +
                "  .badge { padding: 3px 8px; border-radius: 6px; font-size: 11px; font-weight: 700; }\n" +
                "  .badge.high, .badge.urgent { background: #ef4444; color: white; }\n" +
                "  .badge.medium { background: #f59e0b; color: white; }\n" +
                "  .badge.low { background: #10b981; color: white; }\n" +
                "  .ticket-card { background: #0f172a; border: 1px solid #334155; border-radius: 12px; padding: 14px; margin-bottom: 12px; }\n" +
                "  .t-title { font-size: 16px; font-weight: 800; color: #f8fafc; }\n" +
                "  .t-meta { font-size: 12px; color: #94a3b8; margin: 4px 0; }\n" +
                "  .t-seats { font-size: 13px; color: #38bdf8; margin-bottom: 8px; }\n" +
                "  .empty { color: #64748b; font-size: 13px; font-style: italic; text-align: center; padding: 15px 0; }\n" +
                "  .footer { text-align: center; font-size: 11px; color: #475569; margin-top: 24px; }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class='container'>\n" +
                "  <div class='header'>\n" +
                "    <div class='logo'>NEXUS COMPANION</div>\n" +
                "    <div class='badge-online'>WI-FI LIVE</div>\n" +
                "  </div>\n" +
                "  <div class='card'>\n" +
                "    <div class='student-name'>NEXUS Production User</div>\n" +
                "    <div class='student-meta'>Department of Computational Intelligence</div>\n" +
                "    <div class='student-meta'>SRMIST Kattankulathur Campus</div>\n" +
                "  </div>\n" +
                "  <div class='card'>\n" +
                "    <div class='card-title'>Cinema Tickets & Digital Passes</div>\n" +
                "    " + ticketsHtml + "\n" +
                "  </div>\n" +
                "  <div class='card'>\n" +
                "    <div class='card-title'>Active Tasks (" + tasks.size() + ")</div>\n" +
                "    <ul class='task-list'>" + tasksHtml + "</ul>\n" +
                "  </div>\n" +
                "  <div class='footer'>NEXUS Platform | Local Wi-Fi Companion</div>\n" +
                "</div>\n" +
                "</body>\n" +
                "</html>";

            sendResponse(exchange, 200, html, "text/html; charset=UTF-8");
        }
    }

    private class StatusApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DatabaseService db = DatabaseService.getInstance();
            int tasks = db.loadTasks().size();
            int bookings = db.loadBookings().size();

            String json = String.format(
                "{\"status\":\"ONLINE\",\"version\":\"Production-Companion\",\"tasks\":%d,\"cinemaBookings\":%d,\"server\":\"NEXUS-Embedded-JDK\"}",
                tasks, bookings
            );
            sendResponse(exchange, 200, json, "application/json");
        }
    }

    private class QrApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String data = "NEXUS SECURE PASS";
            if (query != null && query.startsWith("data=")) {
                data = java.net.URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8);
            }

            BufferedImage img = TicketQrGenerator.generateQrImage(data, 180);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            byte[] bytes = baos.toByteArray();

            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private void sendResponse(HttpExchange exchange, int code, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
