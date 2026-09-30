package nexus.finance;

import java.util.Date;
import nexus.common.IdGenerator;

public class Transaction {
    private String id;
    private String description;
    private double amount;
    private Date date;
    
    public Transaction(String description, double amount) {
        this.id = IdGenerator.generateId();
        this.description = description;
        this.amount = amount;
        this.date = new Date();
    }
    
    public String getId() { return id; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public Date getDate() { return date; }
}
