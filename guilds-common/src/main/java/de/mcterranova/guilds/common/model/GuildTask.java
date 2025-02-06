package de.mcterranova.guilds.common.model;

import java.time.LocalDate;

public class GuildTask {
    private int taskId;
    private String guildName;
    private String periodicity;       // "DAILY", "MONTHLY", etc.
    private String description;
    private String materialOrMob;
    private int requiredAmount;
    private int pointsReward;
    private double moneyReward;
    private LocalDate assignedDate;
    private TaskEventType eventType;

    public GuildTask() {}

    public GuildTask(String guildName, String periodicity, String description,
                     String materialOrMob, int requiredAmount, int pointsReward,
                     double moneyReward, LocalDate assignedDate, TaskEventType eventType)
    {
        this.guildName = guildName;
        this.periodicity = periodicity;
        this.description = description;
        this.materialOrMob = materialOrMob;
        this.requiredAmount = requiredAmount;
        this.pointsReward = pointsReward;
        this.moneyReward = moneyReward;
        this.assignedDate = assignedDate;
        this.eventType = eventType;
    }

    // -- Getters & Setters --

    public int getTaskId() {
        return taskId;
    }
    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getGuildName() {
        return guildName;
    }
    public void setGuildName(String guildName) {
        this.guildName = guildName;
    }

    public String getPeriodicity() {
        return periodicity;
    }
    public void setPeriodicity(String periodicity) {
        this.periodicity = periodicity;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getMaterialOrMob() {
        return materialOrMob;
    }
    public void setMaterialOrMob(String materialOrMob) {
        this.materialOrMob = materialOrMob;
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }
    public void setRequiredAmount(int requiredAmount) {
        this.requiredAmount = requiredAmount;
    }

    public int getPointsReward() {
        return pointsReward;
    }
    public void setPointsReward(int pointsReward) {
        this.pointsReward = pointsReward;
    }

    public double getMoneyReward() {
        return moneyReward;
    }
    public void setMoneyReward(double moneyReward) {
        this.moneyReward = moneyReward;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }
    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }

    public TaskEventType getEventType() {
        return eventType;
    }
    public void setEventType(TaskEventType eventType) {
        this.eventType = eventType;
    }
}
