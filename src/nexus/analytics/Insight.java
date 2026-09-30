package nexus.analytics;

import java.util.Date;

public class Insight {
    private String message;
    private Date timestamp;
    
    public Insight(String message) {
        this.message = message;
        this.timestamp = new Date();
    }
    
    public String getMessage() { return message; }
    public Date getTimestamp() { return timestamp; }
}
