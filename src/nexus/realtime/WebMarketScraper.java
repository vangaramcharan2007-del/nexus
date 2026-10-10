package nexus.realtime;

import org.jsoup.Jsoup;
import nexus.market.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebMarketScraper {
    public static List<Product> scrapeMarket(String query) {
        List<Product> products = new ArrayList<>();
        try {
            // Fetch live data from internet
            String json = Jsoup.connect("https://fakestoreapi.com/products")
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .get()
                    .body()
                    .text();

            // Quick regex to parse JSON manually without GSON library
            Pattern titlePattern = Pattern.compile("\"title\":\"(.*?)\"");
            Pattern pricePattern = Pattern.compile("\"price\":([0-9.]+)");
            Pattern categoryPattern = Pattern.compile("\"category\":\"(.*?)\"");
            Pattern imagePattern = Pattern.compile("\"image\":\"(.*?)\"");

            Matcher titleMatcher = titlePattern.matcher(json);
            Matcher priceMatcher = pricePattern.matcher(json);
            Matcher categoryMatcher = categoryPattern.matcher(json);
            Matcher imageMatcher = imagePattern.matcher(json);

            int count = 0;
            while (titleMatcher.find() && priceMatcher.find() && categoryMatcher.find() && imageMatcher.find()) {
                String title = titleMatcher.group(1);
                double price = Double.parseDouble(priceMatcher.group(1));
                String category = categoryMatcher.group(1);
                String imageUrl = imageMatcher.group(1);
                
                java.awt.Image img = null;
                try {
                    img = javax.imageio.ImageIO.read(new java.net.URL(imageUrl));
                    if (img != null) {
                        img = img.getScaledInstance(150, 150, java.awt.Image.SCALE_SMOOTH);
                    }
                } catch (Exception ex) {
                    // ignore image load error
                }
                
                products.add(new Product("[LIVE DATA] " + title, price, category, img));
                count++;
            }
        } catch (Exception e) {
            products.clear();
            products.add(new Product("Fallback Laptop", 1200.00));
        }
        return products;
    }
}

