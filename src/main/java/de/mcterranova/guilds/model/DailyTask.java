package de.mcterranova.guilds.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DailyTask {
    private final String description;
    private final String materialOrMob;
    private final int requiredAmount;
    private final int pointsReward;
    private final double moneyReward;
    private final Map<UUID, Integer> progress = new HashMap<>();

    private final TaskEventType eventType;

    public DailyTask(String description, String matOrMob, int requiredAmount, int pointsReward, double moneyReward, TaskEventType eventType) {
        this.description = description;
        this.materialOrMob = matOrMob;
        this.requiredAmount = requiredAmount;
        this.pointsReward = pointsReward;
        this.moneyReward = moneyReward;
        this.eventType = eventType;
    }

    public String getDescription() { return description; }
    public String getMaterialOrMob() { return materialOrMob; }
    public int getRequiredAmount() { return requiredAmount; }
    public int getPointsReward() { return pointsReward; }
    public double getMoneyReward() { return moneyReward; }
    public TaskEventType getEventType() { return eventType; }

    public int getProgress(UUID player) {
        return progress.getOrDefault(player, 0);
    }

    public void addProgress(UUID player, int amount) {
        progress.put(player, getProgress(player) + amount);
    }

    public boolean isComplete(UUID player) {
        return getProgress(player) >= requiredAmount;
    }
}