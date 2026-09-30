package nexus.media;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

public class MediaPanel extends JPanel implements NexusListener {
    private MediaService service;
    private DefaultListModel<String> listModel;
    private JList<String> mediaList;
    
    public MediaPanel(MediaService service) {
        this.service = service;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        listModel = new DefaultListModel<>();
        mediaList = new JList<>(listModel);
        add(new JScrollPane(mediaList), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        JTextField titleField = new JTextField(10);
        JComboBox<MediaType> typeCombo = new JComboBox<>(MediaType.values());
        JButton addBtn = new JButton("Add Media");
        JButton completeBtn = new JButton("Mark Completed");
        
        bottomPanel.add(titleField);
        bottomPanel.add(typeCombo);
        bottomPanel.add(addBtn);
        bottomPanel.add(completeBtn);
        add(bottomPanel, BorderLayout.SOUTH);
        
        addBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            if(!title.isEmpty()) {
                service.addMedia(title, (MediaType) typeCombo.getSelectedItem());
                titleField.setText("");
            }
        });
        
        completeBtn.addActionListener(e -> {
            int idx = mediaList.getSelectedIndex();
            if(idx >= 0) {
                MediaItem item = service.getMediaItems().get(idx);
                service.updateStatus(item.getId(), MediaStatus.COMPLETED);
            }
        });
        
        EventBus.getInstance().subscribe("MEDIA_UPDATED", this);
        updateList(service.getMediaItems());
    }
    
    private void updateList(List<MediaItem> items) {
        listModel.clear();
        for(MediaItem item : items) {
            listModel.addElement(String.format("[%s] %s - %s", item.getType(), item.getTitle(), item.getStatus()));
        }
    }

    @Override
    public void onEvent(NexusEvent event) {
        if("MEDIA_UPDATED".equals(event.getType())) {
            SwingUtilities.invokeLater(() -> {
                @SuppressWarnings("unchecked")
                List<MediaItem> updated = (List<MediaItem>) event.getPayload();
                updateList(updated);
            });
        }
    }
}
