package nexus.media;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import java.util.ArrayList;
import java.util.List;

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
