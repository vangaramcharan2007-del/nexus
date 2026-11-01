package nexus.media;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;
import java.util.ArrayList;
import java.util.List;

public class MediaService implements NexusListener {
    private List<MediaItem> mediaItems;
    private List<MediaItem> searchResults;
    
    public MediaService() {
        mediaItems = new ArrayList<>();
        searchResults = new ArrayList<>();
        
        EventBus.getInstance().subscribe("OS_MEDIA_PLAYING", this);
        
        // Seed default popular items if empty
        seedDefaults();

        // Perform initial live search for popular shows in background
        searchOnline("Arcane");
    }

    private void seedDefaults() {
        MediaItem m1 = new MediaItem("Interstellar", MediaType.MOVIE, "Sci-Fi, Adventure", 8.7, 
                "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.", 
                "https://static.tvmaze.com/uploads/images/medium_portrait/6/16463.jpg");
        m1.setStatus(MediaStatus.IN_PROGRESS);
        m1.setRating(5);
        mediaItems.add(m1);

        MediaItem m2 = new MediaItem("Dune: Part Two", MediaType.MOVIE, "Sci-Fi, Action", 8.6, 
                "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.", 
                "");
        m2.setStatus(MediaStatus.WANT_TO_CONSUME);
        mediaItems.add(m2);
    }

    public void searchOnline(String query) {
        new Thread(() -> {
            List<MediaItem> results = RealtimeMediaService.searchShowsOnline(query);
            this.searchResults = results;
            EventBus.getInstance().publish(new NexusEvent("MEDIA_SEARCH_RESULTS", results, "Media"));
        }).start();
    }
    
    public void addMedia(String title, MediaType type) {
        MediaItem item = new MediaItem(title, type);
        addMediaItem(item);
    }

    public void addMediaItem(MediaItem item) {
        for (MediaItem existing : mediaItems) {
            if (existing.getTitle().equalsIgnoreCase(item.getTitle())) {
                return; // avoid duplicates
            }
        }
        mediaItems.add(0, item);
        EventBus.getInstance().publish(new NexusEvent("MEDIA_UPDATED", mediaItems, "Media"));
        EventBus.getInstance().publish(new NexusEvent("MEDIA_ADDED", item, "Media"));
    }
    
    public void updateStatus(String id, MediaStatus newStatus) {
        for (MediaItem item : mediaItems) {
            if (item.getId().equals(id)) {
                item.setStatus(newStatus);
                EventBus.getInstance().publish(new NexusEvent("MEDIA_STATUS_CHANGED", item, "Media"));
                EventBus.getInstance().publish(new NexusEvent("MEDIA_UPDATED", mediaItems, "Media"));
                if (newStatus == MediaStatus.COMPLETED) {
                    EventBus.getInstance().publish(new NexusEvent("MEDIA_COMPLETED", item, "Media"));
                }
                break;
            }
        }
    }
    
    public void rateMedia(String id, int rating) {
        for (MediaItem item : mediaItems) {
            if (item.getId().equals(id)) {
                item.setRating(rating);
                EventBus.getInstance().publish(new NexusEvent("MEDIA_RATED", item, "Media"));
                EventBus.getInstance().publish(new NexusEvent("MEDIA_UPDATED", mediaItems, "Media"));
                break;
            }
        }
    }

    public void deleteMedia(String id) {
        MediaItem removed = null;
        for (int i = 0; i < mediaItems.size(); i++) {
            if (mediaItems.get(i).getId().equals(id)) {
                removed = mediaItems.remove(i);
                break;
            }
        }
        if (removed != null) {
            EventBus.getInstance().publish(new NexusEvent("MEDIA_DELETED", removed, "Media"));
            EventBus.getInstance().publish(new NexusEvent("MEDIA_UPDATED", mediaItems, "Media"));
        }
    }
    
    public List<MediaItem> getMediaItems() { return mediaItems; }
    public List<MediaItem> getSearchResults() { return searchResults; }
    
    public void setMediaItems(List<MediaItem> items) {
        if (items != null && !items.isEmpty()) {
            this.mediaItems = items;
            EventBus.getInstance().publish(new NexusEvent("MEDIA_UPDATED", mediaItems, "Media"));
        }
    }

    @Override
    public void onEvent(NexusEvent event) {
        if ("OS_MEDIA_PLAYING".equals(event.getType())) {
            String payload = (String) event.getPayload();
            addMedia(payload, MediaType.MUSIC);
        }
    }
}
