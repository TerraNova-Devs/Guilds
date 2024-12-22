package de.mcterranova.guilds.database.dao;

import de.mcterranova.guilds.model.MonthlyTask;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

public interface MonthlyTaskDao {

    // Load tasks for a guild (or for all guilds) from guild_monthly_tasks
    // Or if each guild only has 1 monthly task, just load one
    MonthlyTask loadMonthlyTask(String guildName);
    HashMap<String, MonthlyTask> loadMonthlyTasks();

    void resetMonthlyTasks();

    Instant getLastReset(String resetType);
    void setLastReset(String resetType, Instant timestamp);

    // Insert/assign a monthly task to a guild
    void assignMonthlyTask(String guildName, MonthlyTask task);

    // Progress-related
    int getGuildProgress(String guildName, String description);
    void updateGuildProgress(String guildName, String description, int progress);

    boolean isTaskCompleted(String guildName, String description);
    void markTaskCompleted(String guildName, String description);

    // If you want claimed logic
    boolean isRewardClaimed(String guildName, String description, UUID playerId);
    void markRewardClaimed(String guildName, String description, UUID playerId);
}
