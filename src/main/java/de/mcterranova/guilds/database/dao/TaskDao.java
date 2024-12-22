package de.mcterranova.guilds.database.dao;

import de.mcterranova.guilds.model.DailyTask;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TaskDao {
    boolean dailyTasksExistForToday();
    Instant getLastReset(String resetType);
    void setLastReset(String resetType, Instant timestamp);
    void assignDailyTasksForGuild(String guildName, List<DailyTask> tasks);
    List<DailyTask> loadDailyTasksForGuild(String guildName);
    void updateTaskProgress(String guildName, DailyTask task, UUID playerId, int progress);
    int getProgress(String guildName, DailyTask task, UUID playerId);
    boolean isTaskCompleted(String guildName, DailyTask task, UUID playerId);
    void markTaskCompleted(String guildName, DailyTask task, UUID playerId);
    void resetTasks(); // deletes all tasks and progress from previous day
    Instant getTaskCompletionTime(String guildName, DailyTask task, UUID playerId);
    boolean isTaskClaimed(String guildName, DailyTask task, UUID playerId);
    void markTaskClaimed(String guildName, DailyTask task, UUID playerId);
}
