package nexus.tasks;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

public class TaskPanel extends JPanel implements NexusListener {
    private TaskService service;
    private DefaultListModel<String> listModel;
    private JList<String> taskList;
    
    public TaskPanel(TaskService service) {
        this.service = service;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        listModel = new DefaultListModel<>();
        updateList(service.getTasks());
        
        taskList = new JList<>(listModel);
        add(new JScrollPane(taskList), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        JTextField taskField = new JTextField(15);
        JComboBox<TaskPriority> priorityCombo = new JComboBox<>(TaskPriority.values());
        JButton addBtn = new JButton("Add Task");
        JButton completeBtn = new JButton("Complete Selected");
        
        bottomPanel.add(taskField);
        bottomPanel.add(priorityCombo);
        bottomPanel.add(addBtn);
        bottomPanel.add(completeBtn);
        add(bottomPanel, BorderLayout.SOUTH);
        
        addBtn.addActionListener(e -> {
            String title = taskField.getText().trim();
            if(!title.isEmpty()) {
                service.addTask(title, (TaskPriority) priorityCombo.getSelectedItem());
                taskField.setText("");
            }
        });
        
        completeBtn.addActionListener(e -> {
            int idx = taskList.getSelectedIndex();
            if(idx >= 0) {
                Task t = service.getTasks().get(idx);
                service.completeTask(t.getId());
            }
        });
        
        EventBus.getInstance().subscribe("TASKS_UPDATED", this);
    }
    
    private void updateList(List<Task> tasks) {
        listModel.clear();
        for(Task t : tasks) {
            String status = t.isCompleted() ? "[X]" : "[ ]";
            listModel.addElement(status + " " + t.getTitle() + " (" + t.getPriority() + ")");
        }
    }
    
    @Override
    public void onEvent(NexusEvent event) {
        if("TASKS_UPDATED".equals(event.getType())) {
            SwingUtilities.invokeLater(() -> {
                @SuppressWarnings("unchecked")
                List<Task> updated = (List<Task>) event.getPayload();
                updateList(updated);
            });
        }
    }
}
