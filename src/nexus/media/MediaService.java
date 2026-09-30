package nexus.media;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The MediaService acts as the 'Controller' or 'Business Logic' layer for its respective module.
 * 
 * In a professional software architecture (like MVC or MVVM), Services are responsible for:
 * 1. Managing data state (holding the lists of items, scores, or configurations).
 * 2. Processing business rules (e.g., verifying if an action is valid).
 * 3. Communicating with other parts of the application (publishing events to the EventBus).
 * 
 * By keeping this logic out of the GUI Panels, the code remains easy to test, maintain, and extend.
 */
public class MediaService {
    private List<MediaItem> mediaItems;
    
    public MediaService() {
        mediaItems = new ArrayList<>();
    }
    
    public void addMedia(String title, MediaType type) {
        MediaItem item = new MediaItem(title, type);
        mediaItems.add(item);
        EventBus.getInstance().publish(new NexusEvent("MEDIA_UPDATED", mediaItems, "Media"));
    }
    
    public void updateStatus(String id, MediaStatus newStatus) {
        for(MediaItem item : mediaItems) {
            if(item.getId().equals(id)) {
                item.setStatus(newStatus);
                EventBus.getInstance().publish(new NexusEvent("MEDIA_UPDATED", mediaItems, "Media"));
                if (newStatus == MediaStatus.COMPLETED) {
                    EventBus.getInstance().publish(new NexusEvent("MEDIA_COMPLETED", item, "Media"));
                }
                break;
            }
        }
    }
    
    public void rateMedia(String id, int rating) {
        for(MediaItem item : mediaItems) {
            if(item.getId().equals(id)) {
                item.setRating(rating);
                EventBus.getInstance().publish(new NexusEvent("MEDIA_UPDATED", mediaItems, "Media"));
                break;
            }
        }
    }
    
    public List<MediaItem> getMediaItems() { return mediaItems; }
}
