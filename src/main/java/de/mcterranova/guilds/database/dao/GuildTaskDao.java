package de.mcterranova.guilds.database.dao;

import de.mcterranova.guilds.model.GuildTask;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GuildTaskDao {

    // -- Task creation & retrieval
    void createTask(GuildTask task);
    List<GuildTask> loadTasksForGuild(String guildName, String periodicity);
    GuildTask loadTaskById(int taskId);

    // Delete tasks for a certain date & periodicity (e.g. clearing old daily tasks)
    void deleteTasksByDateAndPeriodicity(LocalDate date, String periodicity);

    // -- Guild Progress
    boolean isTaskCompleted(int taskId, String guildName);
    int getGuildProgress(int taskId, String guildName);
    void markGuildTaskCompleted(int taskId, String guildName);

    // -- Player Progress
    void updatePlayerProgress(int taskId, UUID playerId, int progressDelta);
    int getPlayerProgress(int taskId, UUID playerId);
    boolean isTaskCompleted(int taskId, UUID playerId);
    void markTaskCompleted(int taskId, UUID playerId);

    boolean isTaskClaimed(int taskId, UUID playerId);
    void markTaskClaimed(int taskId, UUID playerId);

    // -- Resets (for daily or monthly or other)
    Instant getLastReset(String resetType);
    void setLastReset(String resetType, Instant timestamp);
}
