package de.mcterranova.guilds.model;

public class MonthlyTask {
    private String description;
    private String materialOrMob;
    private int requiredAmount;
    private int pointsReward;
    private double moneyReward;
    private TaskEventType eventType;

    public MonthlyTask(String description, String materialOrMob, int requiredAmount,
                       int pointsReward, double moneyReward, TaskEventType eventType) {
        this.description = description;
        this.materialOrMob = materialOrMob;
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
}
