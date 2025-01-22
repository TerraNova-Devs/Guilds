package de.mcterranova.guilds.service;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.database.dao.TaskDao;
import de.mcterranova.guilds.model.*;
import de.mcterranova.guilds.util.TimeUtil;
import io.th0rgal.oraxen.api.OraxenItems;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class TaskManager {
    private final Guilds plugin;
    private final GuildManager guildManager;
    private final TaskDao taskDao;

    public TaskManager(Guilds plugin, GuildManager guildManager, TaskDao taskDao) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.taskDao = taskDao;
    }

    public void onStartup() {
        // 1) Check if new day => do the reset if needed
        checkIfNewDay();

        // 2) Schedule the next midnight for daily reset
        scheduleDailyReset();
    }

    public void tryDailyResetOnStartup() {
        if (!taskDao.dailyTasksExistForToday()) {
            dailyResetCore();
            taskDao.setLastReset("DAILY", Instant.now());
        } else {
            plugin.getLogger().info("Daily tasks already assigned for today. Skipping reset on startup...");
        }
    }

    /**
     * Called by the midnight scheduler to do a daily reset if needed.
     */
    public void dailyReset() {
        if (!taskDao.dailyTasksExistForToday()) {
            dailyResetCore();
            taskDao.setLastReset("DAILY", Instant.now());
        } else {
            plugin.getLogger().info("Midnight run: tasks for today already exist. Skipping daily reset...");
        }
    }

    /**
     * The actual logic that picks 3 tasks for each guild etc.
     * We do not forcibly nuke the tasks with resetTasks() => we only remove old tasks
     * or rely on assigned_date=CURDATE() to keep them unique. Up to you.
     */
    private void dailyResetCore() {
        taskDao.resetTasks();
        plugin.getLogger().info("Reset old tasks...");

        for (Guild g : guildManager.getAllGuilds()) {
            GuildType type = g.getType();
            List<DailyTask> pool = loadTaskPoolForType(type);
            List<DailyTask> dailySelection = pickRandomTasks(pool, 3);
            taskDao.assignDailyTasksForGuild(g.getName(), dailySelection);
        }
        plugin.getLogger().info("Assigned new daily tasks to all guilds.");
    }

    public void assignDailyTasksForAllGuildsIfMissing() {
        for (Guild g : guildManager.getAllGuilds()) {
            List<DailyTask> existing = taskDao.loadDailyTasksForGuild(g.getName());
            if (existing.isEmpty()) {
                List<DailyTask> pool = loadTaskPoolForType(g.getType());
                List<DailyTask> dailySelection = pickRandomTasks(pool, 3);
                taskDao.assignDailyTasksForGuild(g.getName(), dailySelection);
                plugin.getLogger().info("Assigned daily tasks to " + g.getName() + " mid-cycle.");
            }
        }
    }


    public void handleEvent(TaskEventType eventType, Player player, String matOrMob, int amount) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) return;

        List<DailyTask> tasks = taskDao.loadDailyTasksForGuild(guild.getName());
        for (DailyTask dt : tasks) {
            if (dt.getEventType() == eventType && dt.getMaterialOrMob().equals(matOrMob)) {
                if (taskDao.isTaskCompleted(guild.getName(), dt, player.getUniqueId())) {
                    continue;
                }
                int oldProgress = taskDao.getProgress(guild.getName(), dt, player.getUniqueId());
                int newProgress = oldProgress + amount;
                taskDao.updateTaskProgress(guild.getName(), dt, player.getUniqueId(), newProgress);

                if (newProgress >= dt.getRequiredAmount()) {
                    completeTask(guild, dt, player);
                }
            }
        }
    }


    public void completeTask(Guild guild, DailyTask task, Player player) {
        taskDao.markTaskCompleted(guild.getName(), task, player.getUniqueId());
        player.sendMessage("§aAufgabe abgeschlossen! " + task.getDescription());
    }

    public void claimReward(Guild guild, DailyTask task, UUID playerId) {
        if (isTaskClaimed(guild.getName(), task, playerId)) {
            return;
        }

        Player p = Bukkit.getPlayer(playerId);
        if (p != null && p.isOnline()) {
            int amount = (int) task.getMoneyReward();
            ItemStack itemStack = OraxenItems.getItemById("terranova_silver").build();
            itemStack.setAmount(amount);
            p.getInventory().addItem(itemStack);
            p.sendMessage("§aDu hast " + amount + " " + PlainTextComponentSerializer.plainText().serialize(itemStack.displayName()) + " als Belohnung erhalten!");
        }

        int currentPoints = guild.getPoints();
        GuildMember guildMember = guild.getMembers().stream().filter(member -> playerId.equals(member.getUuid())).findFirst().orElse(null);
        guildMember.setContributedPoints(task.getPointsReward());
        guild.setPoints(currentPoints + task.getPointsReward());
        guildManager.updateGuildPoints(guild.getName(), currentPoints + task.getPointsReward());
        guildManager.updatePlayerPoints(guild.getName(), guildMember);

        taskDao.markTaskClaimed(guild.getName(), task, playerId);
    }

    public boolean isTaskCompleted(String guildName, DailyTask task, UUID playerId) {
        return taskDao.isTaskCompleted(guildName, task, playerId);
    }

    public boolean isTaskClaimed(String guildName, DailyTask task, UUID playerId) {
        return taskDao.isTaskClaimed(guildName, task, playerId);
    }

    public int getPlayerProgress(String guildName, DailyTask task, UUID playerId) {
        return taskDao.getProgress(guildName, task, playerId);
    }

    public List<DailyTask> getDailyTasksForGuild(String guildName){
        return taskDao.loadDailyTasksForGuild(guildName);
    }

    public Instant getTaskCompletionTime(String guildName, DailyTask task, UUID playerId) {
        return taskDao.getTaskCompletionTime(guildName, task, playerId);
    }

    // The existing pool loading code
    private List<DailyTask> loadTaskPoolForType(GuildType type) {
        String path = "tasks." + type.name();
        if (!plugin.getConfig().contains(path)) {
            plugin.getLogger().warning("No tasks defined for guild type: " + type);
            return Collections.emptyList();
        }

        List<Map<?,?>> taskList = plugin.getConfig().getMapList(path);
        List<DailyTask> pool = new ArrayList<>();
        for (Map<?,?> gt : taskList) {
            String desc = (String) gt.get("description");
            String eventStr = (String) gt.get("event");
            TaskEventType eventType = TaskEventType.valueOf(eventStr.toUpperCase());
            String matOrMob = ((String)gt.get("material_or_mob")).toUpperCase();
            int req = (Integer) gt.get("required_amount");
            int pr = (Integer) gt.get("points_reward");
            double mr = (Double) gt.get("money_reward");
            pool.add(new DailyTask(desc, matOrMob, req, pr, mr, eventType));
        }
        return pool;
    }

    private List<DailyTask> pickRandomTasks(List<DailyTask> pool, int count) {
        if (pool.isEmpty()) return Collections.emptyList();
        List<DailyTask> copy = new ArrayList<>(pool);
        Collections.shuffle(copy);
        return copy.subList(0, Math.min(count, copy.size()));
    }

    public void checkIfNewDay() {
        Instant lastResetInstant = taskDao.getLastReset("DAILY");
        LocalDate today = LocalDate.now();

        if (lastResetInstant == null) {
            // No record => first time => do a reset
            dailyResetCore();
            taskDao.setLastReset("DAILY", Instant.now());
            return;
        }

        LocalDate lastResetDate = lastResetInstant.atZone(ZoneId.systemDefault()).toLocalDate();

        if (!today.isEqual(lastResetDate)) {
            // Means the day changed
            dailyResetCore();
            taskDao.setLastReset("DAILY", Instant.now());
        }
    }

    public void scheduleDailyReset() {
        long ticksUntilMidnight = TimeUtil.getTicksUntilMidnight();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            dailyResetCore();
            taskDao.setLastReset("DAILY", Instant.now());

            scheduleDailyReset();
        }, ticksUntilMidnight);
    }
}
