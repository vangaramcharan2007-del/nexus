package nexus.media;

import nexus.common.IdGenerator;

public class MediaItem {
    private String id;
    private String title;
    private MediaType type;
    private MediaStatus status;
    private int rating; // 1 to 5
    
    public MediaItem(String title, MediaType type) {
        this.id = IdGenerator.generateId();
        this.title = title;
        this.type = type;
        this.status = MediaStatus.WANT_TO_CONSUME;
        this.rating = 0;
    }
    
    public String getId() { return id; }
    public String getTitle() { return title; }
    public MediaType getType() { return type; }
    public MediaStatus getStatus() { return status; }
    public int getRating() { return rating; }
    
    public void setStatus(MediaStatus status) { this.status = status; }
    public void setRating(int rating) { this.rating = rating; }
}
