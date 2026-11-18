package nexus.media;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * ISO/IEC 18004 Standard QR Code Generator for NEXUS Mobile Passes and Tickets.
 * Encodes URLs and payloads into standard 2D barcodes with Reed-Solomon error correction,
 * fully decodable by iOS Camera, Android Camera, Google Lens, and barcode scanners.
 */
public class TicketQrGenerator {

    public static BufferedImage generateQrImage(String payload, int targetSize) {
        if (payload == null || payload.trim().isEmpty()) {
            payload = "https://github.com/vangaramcharan2007-del/nexus";
        }
        try {
            QrCode qr = QrCode.encodeText(payload, QrCode.Ecc.MEDIUM);
            int scale = Math.max(3, targetSize / (qr.getSize() + 4));
            BufferedImage raw = qr.toImage(scale, 2);
            
            // Resize smoothly to requested targetSize if needed
            if (raw.getWidth() == targetSize) {
                return raw;
            }
            BufferedImage scaled = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(raw, 0, 0, targetSize, targetSize, null);
            g.dispose();
            return scaled;
        } catch (Exception e) {
            // Fallback safety
            BufferedImage fallback = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = fallback.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, targetSize, targetSize);
            g.setColor(Color.BLACK);
            g.drawString("QR Error", 10, targetSize / 2);
            g.dispose();
            return fallback;
        }
    }
}
