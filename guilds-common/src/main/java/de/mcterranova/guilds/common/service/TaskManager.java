package de.mcterranova.guilds.common.service;

import de.mcterranova.guilds.common.Guilds;
import de.mcterranova.guilds.common.database.dao.GuildTaskDao;
import de.mcterranova.guilds.common.model.*;
import de.mcterranova.guilds.common.util.ProgressBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.*;

public class TaskManager {

    private final Guilds plugin;
    private final GuildManager guildManager;
    private final GuildTaskDao taskDao;

    public TaskManager(Guilds plugin, GuildManager guildManager, GuildTaskDao taskDao) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.taskDao = taskDao;
    }

    // ----------------------------------------------------------------
    //             HANDLING EVENTS
    // ----------------------------------------------------------------

    /**
     * Called by your listeners when a relevant in‐game event occurs, e.g.
     * block break, mob kill, craft, etc. We only handle tasks of the given
     * 'periodicity' (DAILY or MONTHLY), plus matching eventType & material.
     *
     * @param eventType     e.g. BLOCK_BREAK, ENTITY_KILL, etc.
     * @param player        the Player
     * @param materialOrMob the "material" or "mob" name (e.g. "DIAMOND_ORE")
     * @param amount        how many items or kills
     */
    public void handleEvent(TaskEventType eventType, Player player, String materialOrMob, int amount) {
        // 1) Find the player's guild
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            return; // not in a guild => ignore
        }

        // 2) Load both daily and monthly tasks
        List<GuildTask> dailyTasks = taskDao.loadTasksForGuild(guild.getName(), "DAILY");
        List<GuildTask> monthlyTasks = taskDao.loadTasksForGuild(guild.getName(), "MONTHLY");

        // 3) Combine them into one list (or handle them in two loops—up to you)
        List<GuildTask> allTasks = new ArrayList<>(dailyTasks);
        allTasks.addAll(monthlyTasks);

        // 4) Process each relevant task
        for (GuildTask task : allTasks) {
            // Check if it matches the event type & material
            if (task.getEventType() == eventType
                    && task.getMaterialOrMob().equalsIgnoreCase(materialOrMob)) {

                int required = task.getRequiredAmount();

                // If not already completed
                if (task.getPeriodicity().equalsIgnoreCase(TaskPeriodicity.DAILY.name()) && !taskDao.isTaskCompleted(task.getTaskId(), player.getUniqueId())) {
                    int oldProgress = taskDao.getPlayerProgress(task.getTaskId(), player.getUniqueId());
                    int newProgress = oldProgress + amount;

                    // Update partial progress
                    taskDao.updatePlayerProgress(task.getTaskId(), player.getUniqueId(), amount);

                    // Show an action bar with the new progress
                    int clampedProgress = Math.min(newProgress, required); // just in case it overshoots
                    String bar = ProgressBar.createProgressBar(clampedProgress, required);
                    String msg = "§6 " + task.getDescription() + " " + bar + " §e(" + clampedProgress + "/" + required + ")";

                    player.sendActionBar(Component.text(msg));

                    if (newProgress >= task.getRequiredAmount()) {
                        taskDao.markTaskCompleted(task.getTaskId(), player.getUniqueId());
                        player.sendMessage("§aAufgabe abgeschlossen: " + task.getDescription());
                    }
                } else if (!taskDao.isTaskCompleted(task.getTaskId(), guild.getName())) {
                    int oldProgress = taskDao.getGuildProgress(task.getTaskId(), guild.getName());
                    int newProgress = oldProgress + amount;

                    // Update partial progress
                    taskDao.updatePlayerProgress(task.getTaskId(), player.getUniqueId(), amount);

                    int clampedProgress = Math.min(newProgress, required); // just in case it overshoots
                    String bar = ProgressBar.createProgressBar(clampedProgress, required);
                    String msg = "§6" + task.getDescription() + " " + bar + " §e(" + clampedProgress + "/" + required + ")";

                    player.sendActionBar(Component.text(msg));

                    if (newProgress >= task.getRequiredAmount()) {
                        taskDao.markGuildTaskCompleted(task.getTaskId(), guild.getName());
                        guildManager.updateGuildPoints(guild.getName(), guild.getPoints() + task.getPointsReward());
                        plugin.getServer().broadcast(Component.text("§aGildenaufgabe abgeschlossen: " + task.getDescription()));
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------
    //             SIMPLE WRAPPERS FOR DAO
    // ----------------------------------------------------------------

    /**
     * Loads tasks for the given guild & periodicity.
     * E.g. "DAILY" or "MONTHLY".
     */
    public List<GuildTask> loadTasksForGuild(String guildName, String periodicity) {
        return taskDao.loadTasksForGuild(guildName, periodicity);
    }

    public boolean isTaskCompleted(int taskId, String guildName) {
        return taskDao.isTaskCompleted(taskId, guildName);
    }

    /**
     * Checks if a player has completed a particular task.
     */
    public boolean isTaskCompleted(int taskId, UUID playerId) {
        return taskDao.isTaskCompleted(taskId, playerId);
    }

    /**
     * Checks if a player already claimed the reward for a task.
     */
    public boolean isTaskClaimed(int taskId, UUID playerId) {
        return taskDao.isTaskClaimed(taskId, playerId);
    }

    /**
     * Gets how much progress a player has made on a particular task.
     */
    public int getPlayerProgress(int taskId, UUID playerId) {
        return taskDao.getPlayerProgress(taskId, playerId);
    }

    public int getGuildProgress(int taskId, String guildName) {
        return taskDao.getGuildProgress(taskId, guildName);
    }


}
