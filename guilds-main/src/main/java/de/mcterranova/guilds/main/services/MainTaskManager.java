package de.mcterranova.guilds.main.services;

import de.mcterranova.guilds.common.Guilds;
import de.mcterranova.guilds.common.database.dao.GuildTaskDao;
import de.mcterranova.guilds.common.model.*;
import de.mcterranova.guilds.common.service.GuildManager;
import de.mcterranova.guilds.common.service.TaskManager;
import de.mcterranova.guilds.common.util.TimeUtil;
import de.mcterranova.guilds.main.GuildsMainPlugin;
import de.mcterranova.guilds.common.model.UnclaimedReward;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MainTaskManager extends TaskManager {
    private final GuildsMainPlugin plugin;
    private final GuildManager guildManager;
    private final GuildTaskDao taskDao;
    private final UnclaimedRewardManager unclaimedRewardManager;

    public MainTaskManager(Guilds plugin, GuildManager guildManager, GuildTaskDao taskDao, UnclaimedRewardManager unclaimedRewardManager) {
        super(plugin, guildManager, taskDao);
        this.plugin = (GuildsMainPlugin) plugin;
        this.guildManager = guildManager;
        this.taskDao = taskDao;
        this.unclaimedRewardManager = unclaimedRewardManager;


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
        this.archiveUnclaimedRewards();

        taskDao.deleteTasksByDateAndPeriodicity(LocalDate.now(), "DAILY");

        for (Guild g : guildManager.getAllGuilds()) {
            List<GuildTask> possibleTasks = loadTaskPoolFromConfig(g.getType().name(), "DAILY");
            // pick 3 random
            List<GuildTask> randomThree = pickRandomTasks(possibleTasks, 3);

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
        this.archiveUnclaimedRewards();

        plugin.getRewardManager().evaluateMonthlyWinner();

        taskDao.deleteTasksByDateAndPeriodicity(LocalDate.now(), "MONTHLY");

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
                    player.getWorld().dropItem(player.getLocation(), item);
                }
            }
        }
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

    private void archiveUnclaimedRewards(){
        List<UnclaimedReward> unclaimedRewards = taskDao.getUnclaimedRewards();

        for (UnclaimedReward reward : unclaimedRewards) {
            plugin.getLogger().info("Archiving unclaimed reward for player: " + reward.getPlayerUuid());
            unclaimedRewardManager.insertReward(reward);
        }
    }

}
