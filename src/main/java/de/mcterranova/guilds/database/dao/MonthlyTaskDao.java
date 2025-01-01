package de.mcterranova.guilds.database.dao;

import de.mcterranova.guilds.model.MonthlyTask;

import java.time.Instant;
import java.util.UUID;

public interface MonthlyTaskDao {

    MonthlyTask loadMonthlyTask(String guildName);

    void assignMonthlyTask(String guildName, MonthlyTask task);

    int getGuildProgress(String guildName, String description);

    // Remove or repurpose if not needed
    void updateGuildProgress(String guildName, String description, int progress);

    boolean isTaskCompleted(String guildName, String description);

    void markTaskCompleted(String guildName, String description);

    boolean isRewardClaimed(String guildName, String description, UUID playerId);

    void markRewardClaimed(String guildName, String description, UUID playerId);

    void resetMonthlyTasks();

    // **New Methods for Per-Player Monthly Progress**

    void updatePlayerMonthlyProgress(String guildName, String description, UUID playerId, int progress);

    int getPlayerMonthlyProgress(String guildName, String description, UUID playerId);

    void markPlayerMonthlyTaskCompleted(String guildName, String description);

    boolean isPlayerMonthlyTaskCompleted(String guildName, String description, UUID playerId);

    Instant getLastReset(String resetType);

    void setLastReset(String resetType, Instant timestamp);
}
