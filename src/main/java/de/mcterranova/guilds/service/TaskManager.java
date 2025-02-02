package de.mcterranova.guilds.service;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.database.dao.GuildTaskDao;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.GuildTask;
import de.mcterranova.guilds.model.TaskEventType;
import de.mcterranova.guilds.model.TaskPeriodicity;
import de.mcterranova.guilds.util.TimeUtil;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TaskManager {

    private final Guilds plugin;
    private final GuildManager guildManager;
    private final GuildTaskDao taskDao;

    public TaskManager(Guilds plugin, GuildManager guildManager, GuildTaskDao taskDao) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.taskDao = taskDao;
    }

    /**
     * Called during plugin startup to see if we need a daily or monthly reset,
     * then schedule any future resets.
     */
    public void onStartup() {
        checkIfNewDay();
        checkIfNewMonth();
        scheduleDailyReset();
        scheduleMonthlyReset();
    }

    // ----------------------------------------------------------------
    //             DAILY RESET LOGIC
    // ----------------------------------------------------------------

    public void checkIfNewDay() {
        Instant lastDailyReset = taskDao.getLastReset("DAILY");
        LocalDate today = LocalDate.now();

        // If we never stored a daily reset, do one now.
        if (lastDailyReset == null) {
            dailyResetCore();
            taskDao.setLastReset("DAILY", Instant.now());
            return;
        }

        // Compare the date of the last reset to today's date
        LocalDate lastDate = lastDailyReset.atZone(ZoneId.systemDefault()).toLocalDate();
        if (!today.isEqual(lastDate)) {
            dailyResetCore();
            taskDao.setLastReset("DAILY", Instant.now());
        }
    }

    public void dailyResetCore() {
        // Remove any daily tasks from *today* or from yesterday—your choice:
        // This example deletes tasks assigned *today*, so we can reassign fresh tasks:
        taskDao.deleteTasksByDateAndPeriodicity(LocalDate.now(), "DAILY");

        // For each guild, pick e.g. 3 daily tasks from config
        for (Guild g : guildManager.getAllGuilds()) {
            // 1) load from config
            List<GuildTask> possibleTasks = loadTaskPoolFromConfig(g.getType().name(), "DAILY");
            // 2) pick 3 random
            List<GuildTask> randomThree = pickRandomTasks(possibleTasks, 3);
            // 3) create them in DB
            for (GuildTask t : randomThree) {
                t.setGuildName(g.getName());
                t.setPeriodicity("DAILY");
                t.setAssignedDate(LocalDate.now());
                taskDao.createTask(t);
            }
        }

        plugin.getLogger().info("Assigned new daily tasks for all guilds.");
    }

    public void scheduleDailyReset() {
        long ticksUntilMidnight = TimeUtil.getTicksUntilMidnight();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            dailyResetCore();
            taskDao.setLastReset("DAILY", Instant.now());
            // re‐schedule next daily
            scheduleDailyReset();
        }, ticksUntilMidnight);
    }

    // ----------------------------------------------------------------
    //             MONTHLY RESET LOGIC
    // ----------------------------------------------------------------

    public void checkIfNewMonth() {
        Instant lastMonthlyReset = taskDao.getLastReset("MONTHLY");
        LocalDate now = LocalDate.now();

        // If never stored a monthly reset, do one now.
        if (lastMonthlyReset == null) {
            monthlyResetCore();
            taskDao.setLastReset("MONTHLY", Instant.now());
            return;
        }

        // Compare year & month
        LocalDate lastResetDate = lastMonthlyReset.atZone(ZoneId.systemDefault()).toLocalDate();
        boolean sameYear = lastResetDate.getYear() == now.getYear();
        boolean sameMonth = lastResetDate.getMonthValue() == now.getMonthValue();
        if (!sameYear || !sameMonth) {
            monthlyResetCore();
            taskDao.setLastReset("MONTHLY", Instant.now());
        }
    }

    public void monthlyResetCore() {
        // Delete tasks with periodicity=MONTHLY from *today*
        taskDao.deleteTasksByDateAndPeriodicity(LocalDate.now(), "MONTHLY");

        // For each guild, pick 1 monthly task from config (example)
        plugin.getLogger().info("Assigning new monthly tasks for all guilds...");
        for (Guild g : guildManager.getAllGuilds()) {
            plugin.getLogger().info("Assigning monthly tasks for guild: " + g.getName());
            List<GuildTask> possibleTasks = loadTaskPoolFromConfig(g.getType().name(), "MONTHLY");
            if (possibleTasks.isEmpty()) continue;

            // pick 1 random
            GuildTask chosen = pickRandomTasks(possibleTasks, 1).getFirst();
            plugin.getLogger().info("Chose task: " + chosen.getDescription());
            chosen.setGuildName(g.getName());
            chosen.setPeriodicity("MONTHLY");
            chosen.setAssignedDate(LocalDate.now());
            taskDao.createTask(chosen);
        }

        plugin.getLogger().info("Assigned new monthly tasks for all guilds.");
    }

    /**
     * Schedules the monthly reset to occur ~on the 1st of next month, for instance.
     */
    public void scheduleMonthlyReset() {
        long ticksUntilNextMonth = TimeUtil.getTicksUntilNextMonth();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            monthlyResetCore();
            taskDao.setLastReset("MONTHLY", Instant.now());
            // re‐schedule
            scheduleMonthlyReset();
        }, ticksUntilNextMonth);
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

                // If not already completed
                if (task.getPeriodicity().equalsIgnoreCase(TaskPeriodicity.DAILY.name()) && !taskDao.isTaskCompleted(task.getTaskId(), player.getUniqueId())) {
                    int oldProgress = taskDao.getPlayerProgress(task.getTaskId(), player.getUniqueId());
                    int newProgress = oldProgress + amount;

                    // Update partial progress
                    taskDao.updatePlayerProgress(task.getTaskId(), player.getUniqueId(), amount);

                    if (newProgress >= task.getRequiredAmount()) {
                        taskDao.markTaskCompleted(task.getTaskId(), player.getUniqueId());
                        player.sendMessage("§aAufgabe abgeschlossen: " + task.getDescription());
                    }
                } else if (!taskDao.isTaskCompleted(task.getTaskId(), guild.getName())) {
                    int oldProgress = taskDao.getGuildProgress(task.getTaskId(), guild.getName());
                    int newProgress = oldProgress + amount;

                    // Update partial progress
                    taskDao.updatePlayerProgress(task.getTaskId(), player.getUniqueId(), amount);

                    if (newProgress >= task.getRequiredAmount()) {
                        taskDao.markGuildTaskCompleted(task.getTaskId(), guild.getName());
                        guildManager.updateGuildPoints(guild.getName(), guild.getPoints() + task.getPointsReward());
                        plugin.getServer().broadcastMessage("§aGildenaufgabe abgeschlossen: " + task.getDescription());
                    }

                }
            }
        }
    }

    // ----------------------------------------------------------------
    //             CLAIM REWARD
    // ----------------------------------------------------------------

    /**
     * Allows a player to claim the reward for a task that they have completed
     * but not yet claimed. (Used in a GUI or command.)
     *
     * @param task   the GuildTask in question
     * @param player the player claiming the reward
     */
    public void claimReward(GuildTask task, Player player) {
        UUID playerId = player.getUniqueId();

        if (Objects.equals(task.getPeriodicity(), TaskPeriodicity.DAILY.name()) && !taskDao.isTaskCompleted(task.getTaskId(), playerId)) {
            player.sendMessage("§cDu hast diese Aufgabe noch nicht beendet!");
            return;
        }
        if(Objects.equals(task.getPeriodicity(), TaskPeriodicity.MONTHLY.name()) && !taskDao.isTaskCompleted(task.getTaskId(), task.getGuildName())) {
            player.sendMessage("§cDeine Gilde hat diese Aufgabe noch nicht beendet!");
            return;
        }
        if (taskDao.isTaskClaimed(task.getTaskId(), playerId)) {
            player.sendMessage("§cDu hast diese Belohnung bereits abgeholt!");
            return;
        }

        // Mark claimed
        taskDao.markTaskClaimed(task.getTaskId(), playerId);

        // Give item or money, update guild points, etc.
        double money = task.getMoneyReward();
        player.sendMessage("§aDu hast " + money + " Silber für die Aufgabe " + task.getDescription() + " erhalten.");

        Guild guild = guildManager.getGuildByName(task.getGuildName());
        if (guild != null) {
            int newTotal = guild.getPoints() + task.getPointsReward();

            if(Objects.equals(task.getPeriodicity(), TaskPeriodicity.DAILY.name())) {
                guildManager.updateGuildPoints(guild.getName(), newTotal);
                guildManager.addPointsToPlayerContribution(guild.getName(), playerId, task.getPointsReward());
            }

            // Give the player the money reward
            ItemStack moneyStack = OraxenItems.getItemById("terranova_silver").build();
            moneyStack.setAmount((int) money);
            var remaining = player.getInventory().addItem(moneyStack);
            if (!remaining.isEmpty()) {
                player.sendMessage("§cDein Inventar ist voll! Die verbleibenden " + money + " Silber wurden auf den Boden gedroppt.");
                for (ItemStack item : remaining.values()) {
                    Bukkit.getWorld(player.getWorld().getName()).dropItem(player.getLocation(), item);
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

    /**
     * Loads a single task by its primary key (taskId).
     */
    public GuildTask loadTaskById(int taskId) {
        return taskDao.loadTaskById(taskId);
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

    // ----------------------------------------------------------------
    //             UTILITY: LOADING TASKS FROM CONFIG, ETC.
    // ----------------------------------------------------------------

    private List<GuildTask> loadTaskPoolFromConfig(String guildType, String periodicity) {
        String path = "tasks." + guildType + "." + periodicity;
        if (!plugin.getConfig().contains(path)) {
            return Collections.emptyList();
        }

        List<Map<?, ?>> configList = plugin.getConfig().getMapList(path);
        List<GuildTask> tasks = new ArrayList<>();
        for (Map<?, ?> map : configList) {
            String desc       = (String) map.get("description");
            String matOrMob   = (String) map.get("material_or_mob");
            int required      = (int) map.get("required_amount");
            int points        = (int) map.get("points_reward");
            double money      = (double) map.get("money_reward");
            String ev         = (String) map.get("event");

            GuildTask gt = new GuildTask();
            gt.setPeriodicity(periodicity);
            gt.setDescription(desc);
            gt.setMaterialOrMob(matOrMob);
            gt.setRequiredAmount(required);
            gt.setPointsReward(points);
            gt.setMoneyReward(money);
            gt.setEventType(TaskEventType.valueOf(ev.toUpperCase()));
            // We'll set guildName & assignedDate later when we actually assign them
            tasks.add(gt);
        }
        plugin.getLogger().info("Loaded " + tasks.size() + " tasks for " + guildType + " " + periodicity);
        return tasks;
    }

    /**
     * Pick 'count' random tasks from the provided list.
     */
    private List<GuildTask> pickRandomTasks(List<GuildTask> pool, int count) {
        if (pool.isEmpty()) return Collections.emptyList();
        Collections.shuffle(pool, ThreadLocalRandom.current());
        int end = Math.min(count, pool.size());
        return pool.subList(0, end);
    }
}
