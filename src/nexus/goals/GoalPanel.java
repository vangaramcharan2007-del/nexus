package nexus.goals;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

public class GoalPanel extends JPanel implements NexusListener {
    private GoalService service;
    private JPanel goalsContainer;
    
    public GoalPanel(GoalService service) {
        this.service = service;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        goalsContainer = new JPanel();
        goalsContainer.setLayout(new BoxLayout(goalsContainer, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JScrollPane(goalsContainer), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        JTextField goalField = new JTextField(15);
        JButton addGoalBtn = new JButton("Add Goal");
        
        bottomPanel.add(goalField);
        bottomPanel.add(addGoalBtn);
        add(bottomPanel, BorderLayout.SOUTH);
        
        addGoalBtn.addActionListener(e -> {
            String title = goalField.getText().trim();
            if(!title.isEmpty()) {
                service.addGoal(title);
                goalField.setText("");
            }
        });
        
        EventBus.getInstance().subscribe("GOALS_UPDATED", this);
        updateView();
    }
    
    private void updateView() {
        goalsContainer.removeAll();
        for(Goal g : service.getGoals()) {
            JPanel p = new JPanel(new BorderLayout());
            p.setBorder(BorderFactory.createTitledBorder(g.getTitle() + String.format(" (%.0f%%)", g.getProgress() * 100)));
            
            DefaultListModel<String> lm = new DefaultListModel<>();
            for(Milestone m : g.getMilestones()) {
                lm.addElement((m.isCompleted() ? "[X] " : "[ ] ") + m.getDescription());
            }
            JList<String> list = new JList<>(lm);
            p.add(list, BorderLayout.CENTER);
            
            JPanel btnPanel = new JPanel();
            JTextField msField = new JTextField(10);
            JButton addMsBtn = new JButton("Add Milestone");
            JButton compMsBtn = new JButton("Complete Selected");
            
            btnPanel.add(msField);
            btnPanel.add(addMsBtn);
            btnPanel.add(compMsBtn);
            p.add(btnPanel, BorderLayout.SOUTH);
            
            addMsBtn.addActionListener(e -> {
                if(!msField.getText().trim().isEmpty()) {
                    service.addMilestoneToGoal(g.getId(), msField.getText().trim());
                }
            });
            
            compMsBtn.addActionListener(e -> {
                int idx = list.getSelectedIndex();
                if(idx >= 0) {
                    service.completeMilestone(g.getId(), idx);
                }
            });
            
            goalsContainer.add(p);
        }
        goalsContainer.revalidate();
        goalsContainer.repaint();
    }

    @Override
    public void onEvent(NexusEvent event) {
        if("GOALS_UPDATED".equals(event.getType())) {
            SwingUtilities.invokeLater(() -> updateView());
        }
    }
}
