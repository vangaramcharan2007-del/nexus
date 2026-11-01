package nexus.media;

import java.util.Date;
import java.util.List;
import nexus.common.IdGenerator;

public class CinemaBooking {
    private String id;
    private String movieTitle;
    private String theatre;
    private String showTime;
    private List<String> seatNumbers;
    private double basePrice;
    private double discount;
    private String promoCode;
    private double finalTotal;
    private Date bookingDate;
    private String status;

    public CinemaBooking(String movieTitle, String theatre, String showTime, List<String> seatNumbers,
                         double basePrice, double discount, String promoCode, double finalTotal) {
        this.id = "BMS-" + IdGenerator.generateId().substring(0, 8).toUpperCase();
        this.movieTitle = movieTitle;
        this.theatre = theatre;
        this.showTime = showTime;
        this.seatNumbers = seatNumbers;
        this.basePrice = basePrice;
        this.discount = discount;
        this.promoCode = promoCode != null && !promoCode.trim().isEmpty() ? promoCode : "NONE";
        this.finalTotal = finalTotal;
        this.bookingDate = new Date();
        this.status = "CONFIRMED";
    }

    public CinemaBooking(String id, String movieTitle, String theatre, String showTime, List<String> seatNumbers,
                         double basePrice, double discount, String promoCode, double finalTotal, Date bookingDate, String status) {
        this.id = id;
        this.movieTitle = movieTitle;
        this.theatre = theatre;
        this.showTime = showTime;
        this.seatNumbers = seatNumbers;
        this.basePrice = basePrice;
        this.discount = discount;
        this.promoCode = promoCode;
        this.finalTotal = finalTotal;
        this.bookingDate = bookingDate;
        this.status = status;
    }

    public String getId() { return id; }
    public String getMovieTitle() { return movieTitle; }
    public String getTheatre() { return theatre; }
    public String getShowTime() { return showTime; }
    public List<String> getSeatNumbers() { return seatNumbers; }
    public String getSeatsFormatted() { return String.join(", ", seatNumbers); }
    public double getBasePrice() { return basePrice; }
    public double getDiscount() { return discount; }
    public String getPromoCode() { return promoCode; }
    public double getFinalTotal() { return finalTotal; }
    public Date getBookingDate() { return bookingDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("[%s] %s @ %s (%s) - Seats: %s | Total: $%.2f",
                id, movieTitle, theatre, showTime, getSeatsFormatted(), finalTotal);
    }
}
