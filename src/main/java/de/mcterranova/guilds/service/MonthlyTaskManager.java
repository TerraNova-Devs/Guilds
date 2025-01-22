package de.mcterranova.guilds.service;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.database.dao.MonthlyTaskDao;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.MonthlyTask;
import de.mcterranova.guilds.model.TaskEventType;
import de.mcterranova.guilds.util.TimeUtil;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class MonthlyTaskManager {

    private final MonthlyTaskDao monthlyTaskDao;
    private final GuildManager guildManager;
    private final RewardManager rewardManager;
    private final Guilds plugin;

    private final Map<String, List<MonthlyTask>> monthlyTaskPools = new HashMap<>();

    private final Map<String, MonthlyTask> assignedTasks = new HashMap<>();

    public MonthlyTaskManager(Guilds plugin, MonthlyTaskDao monthlyTaskDao, GuildManager guildManager) {
        this.plugin = plugin;
        this.monthlyTaskDao = monthlyTaskDao;
        this.guildManager = guildManager;
        this.rewardManager = plugin.getRewardManager();
        loadMonthlyTaskPoolsFromConfig();
    }

    public void onStartup() {
        // If the server was offline at month boundary, check and do a reset
        checkIfNewMonth();

        // Then schedule an event for the next month boundary
        scheduleMonthlyReset();
    }

    public void loadMonthlyTaskPoolsFromConfig() {
        if (!plugin.getConfig().contains("monthly_task_pools")) {
            plugin.getLogger().info("No monthly_task_pools defined in config.yml.");
            return;
        }
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("monthly_task_pools");
        if (section == null) return;

        for (String guildType : section.getKeys(false)) {
            List<MonthlyTask> pool = new ArrayList<>();
            List<Map<?,?>> list = section.getMapList(guildType);
            for (Map<?,?> map : list) {
                String desc = (String) map.get("description");
                String eventStr = (String) map.get("event");
                String material = (String) map.get("material_or_mob");
                int req = (int) map.get("required_amount");
                int pr = (int) map.get("points_reward");
                double mr = (double) map.get("money_reward");

                TaskEventType eventType = TaskEventType.valueOf(eventStr.toUpperCase());

                MonthlyTask mt = new MonthlyTask(desc, material, req, pr, mr, eventType);
                pool.add(mt);
            }
            monthlyTaskPools.put(guildType.toUpperCase(), pool);
            plugin.getLogger().info("Loaded pool of " + pool.size()
                    + " monthly tasks for guild type: " + guildType);
        }
    }

    public void assignRandomMonthlyTask(Guild guild) {
        List<MonthlyTask> pool = monthlyTaskPools.get(guild.getType().toString().toUpperCase());
        if (pool == null || pool.isEmpty()) return;

        Collections.shuffle(pool);
        MonthlyTask chosen = pool.get(0);

        assignedTasks.put(guild.getName(), chosen);

        monthlyTaskDao.assignMonthlyTask(guild.getName(), chosen);
    }

    public void monthlyResetCore() {
        assignedTasks.clear();
        monthlyTaskPools.clear();
        monthlyTaskDao.resetMonthlyTasks();
        loadMonthlyTaskPoolsFromConfig();


        for (Guild guild : guildManager.getAllGuilds()) {
            assignRandomMonthlyTask(guild);
        }
        monthlyTaskDao.setLastReset("MONTHLY", Instant.now());
    }

    public void scheduleMonthlyReset() {
        long ticksUntilNextMonth = TimeUtil.getTicksUntilNextMonth();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 1) Evaluate the monthly winner
            rewardManager.evaluateMonthlyWinner();

            // 2) Actually do the reset
            monthlyResetCore();
            monthlyTaskDao.setLastReset("MONTHLY", Instant.now());

            // 3) Schedule the next month boundary
            scheduleMonthlyReset();
        }, ticksUntilNextMonth);
    }

    public void checkIfNewMonth() {
        Instant lastResetInstant = monthlyTaskDao.getLastReset("MONTHLY");
        if (lastResetInstant == null) {
            monthlyResetCore();
            monthlyTaskDao.setLastReset("MONTHLY", Instant.now());
            return;
        }

        LocalDate lastResetDate = lastResetInstant.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate now = LocalDate.now();

        boolean sameYear  = now.getYear() == lastResetDate.getYear();
        boolean sameMonth = now.getMonthValue() == lastResetDate.getMonthValue();
        if (!sameYear || !sameMonth) {
            // We are in a new month
            monthlyResetCore();
            monthlyTaskDao.setLastReset("MONTHLY", Instant.now());
        }
    }


    /**
     * Handles events that contribute to a guild's monthly task.
     * Updates individual player progress and checks for task completion.
     *
     * @param guild       The guild involved
     * @param eventType   The type of event contributing to the task
     * @param materialOrMob The material or mob associated with the task
     * @param amount      The amount contributed by the event
     * @param playerId    The UUID of the player contributing
     */
    public void handleMonthlyEvent(Guild guild, TaskEventType eventType, String materialOrMob, int amount, UUID playerId) {
        MonthlyTask mt = monthlyTaskDao.loadMonthlyTask(guild.getName());
        if (mt == null) {
            plugin.getLogger().warning("No monthly task assigned to guild: " + guild.getName());
            return;
        }
        if (mt.getEventType() != eventType) return;
        if (!mt.getMaterialOrMob().equalsIgnoreCase(materialOrMob)) return;

        if (monthlyTaskDao.isTaskCompleted(guild.getName(), mt.getDescription())) {
            // Task already completed; no further action needed
            return;
        }

        // Update the player's individual progress
        monthlyTaskDao.updatePlayerMonthlyProgress(guild.getName(), mt.getDescription(), playerId, amount);
        plugin.getLogger().info("Updated progress for player " + playerId + " on task '" + mt.getDescription() + "' for guild: " + guild.getName());

        // Check if the entire guild has completed the monthly task
        int guildProgress = monthlyTaskDao.getGuildProgress(guild.getName(), mt.getDescription());
        if (guildProgress >= mt.getRequiredAmount()) {
            monthlyTaskDao.markPlayerMonthlyTaskCompleted(guild.getName(), mt.getDescription());
            guildManager.updateGuildPoints(guild.getName(), mt.getPointsReward());
            Bukkit.broadcastMessage("§aThe guild " + guild.getName() +
                    " has completed their monthly task: " + mt.getDescription());
            plugin.getLogger().info("Guild " + guild.getName() + " has completed their monthly task: " + mt.getDescription());
        }
    }

    /**
     * Allows a player to claim their monthly reward upon task completion.
     *
     * @param guild   The guild
     * @param description The description of the monthly task
     * @param playerId    The UUID of the player claiming the reward
     * @return true if the reward was successfully claimed; false otherwise
     */
    public boolean claimMonthlyReward(Guild guild, String description, UUID playerId) {
        // Check if the guild has completed the task
        if (!monthlyTaskDao.isTaskCompleted(guild.getName(), description)) {
            return false;
        }

        // Check if the player has already claimed the reward
        if (monthlyTaskDao.isRewardClaimed(guild.getName(), description, playerId)) {
            return false;
        }

        // Mark the reward as claimed for the player
        monthlyTaskDao.markRewardClaimed(guild.getName(), description, playerId);
        plugin.getLogger().info("Player " + playerId + " has claimed the reward for task '" + description + "' in guild: " + guild.getName());

        // Grant the reward to the player
        Player p = Bukkit.getPlayer(playerId);
        MonthlyTask mt = monthlyTaskDao.loadMonthlyTask(guild.getName());
        if (mt != null && mt.getDescription().equals(description) && p != null && p.isOnline()) {
            double money = mt.getMoneyReward();
            ItemStack silver = OraxenItems.getItemById("terranova_silver").build();
            silver.setAmount((int) money); // Assuming money is represented as an item quantity
            p.getInventory().addItem(silver);
            p.sendMessage("§aYou have received the reward for '" + mt.getDescription() + "'!");
            plugin.getLogger().info("Reward granted to player " + playerId + " for task '" + description + "'.");
            return true;
        }

        // If the player is offline or task details are missing
        plugin.getLogger().warning("Failed to grant reward to player " + playerId + " for task '" + description + "'. Player may be offline.");
        return false;
    }

    public MonthlyTask getMonthlyTask(Guild guild) {
        return monthlyTaskDao.loadMonthlyTask(guild.getName());
    }

    public int getGuildProgress(Guild guild, String description) {
        return monthlyTaskDao.getGuildProgress(guild.getName(), description);
    }

    public int getPlayerProgress(Guild guild, String description, UUID uuid) {
        return monthlyTaskDao.getPlayerMonthlyProgress(guild.getName(), description, uuid);
    }

    public boolean isTaskCompleted(Guild guild, String description) {
        return monthlyTaskDao.isTaskCompleted(guild.getName(), description);
    }

    public boolean isRewardClaimed(Guild guild, String description, UUID playerId) {
        return monthlyTaskDao.isRewardClaimed(guild.getName(), description, playerId);
    }

    public MonthlyTask loadMonthlyTask(String guildName) {
        return monthlyTaskDao.loadMonthlyTask(guildName);
    }

}
