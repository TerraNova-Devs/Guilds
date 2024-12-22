package de.mcterranova.guilds.service;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.database.dao.MonthlyTaskDao;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.MonthlyTask;
import de.mcterranova.guilds.model.TaskEventType;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MonthlyTaskManager {

    private final MonthlyTaskDao monthlyTaskDao;
    private final GuildManager guildManager;
    private final Guilds plugin;

    private Map<String, List<MonthlyTask>> monthlyTaskPools = new HashMap<>();

    private Map<String, MonthlyTask> assignedTasks = new HashMap<>();

    public MonthlyTaskManager(Guilds plugin, MonthlyTaskDao monthlyTaskDao, GuildManager guildManager) {
        this.plugin = plugin;
        this.monthlyTaskDao = monthlyTaskDao;
        this.guildManager = guildManager;
        loadMonthlyTaskPoolsFromConfig();
    }

    public void handleMonthlyEvent(Guild guild, TaskEventType eventType, String materialOrMob, int amount) {
        MonthlyTask mt = monthlyTaskDao.loadMonthlyTask(guild.getName());
        if (mt == null) return;
        if (mt.getEventType() != eventType) return;
        if (!mt.getMaterialOrMob().equalsIgnoreCase(materialOrMob)) return;

        if (monthlyTaskDao.isTaskCompleted(guild.getName(), mt.getDescription())) {
            return;
        }

        int oldProg = monthlyTaskDao.getGuildProgress(guild.getName(), mt.getDescription());
        int newProg = oldProg + amount;

        monthlyTaskDao.updateGuildProgress(guild.getName(), mt.getDescription(), newProg);

        if (newProg >= mt.getRequiredAmount()) {
            monthlyTaskDao.markTaskCompleted(guild.getName(), mt.getDescription());
            Bukkit.broadcastMessage("§aDie Gilde " + guild.getName() +
                    " hat ihre monatliche Aufgabe abgeschlossen: " + mt.getDescription());
        }
    }

    public boolean claimMonthlyReward(Guild guild, String description, UUID playerId) {
        if (!monthlyTaskDao.isTaskCompleted(guild.getName(), description)) {
            return false;
        }

        if (monthlyTaskDao.isRewardClaimed(guild.getName(), description, playerId)) {
            return false;
        }

        monthlyTaskDao.markRewardClaimed(guild.getName(), description, playerId);

        Player p = Bukkit.getPlayer(playerId);
        MonthlyTask mt = monthlyTaskDao.loadMonthlyTask(guild.getName());
        if (mt != null && mt.getDescription().equals(description)) {
            double money = mt.getMoneyReward();
            ItemStack silver = OraxenItems.getItemById("terranova_silver").build();
            silver.setAmount((int) money);
            p.getInventory().addItem(silver);
            p.sendMessage("§aDu hast die Belohnung für '" + mt.getDescription() + "' erhalten!");
        }
        return true;
    }

    public MonthlyTask getMonthlyTask(Guild guild) {
        return monthlyTaskDao.loadMonthlyTask(guild.getName());
    }

    public int getGuildProgress(Guild guild, String description) {
        return monthlyTaskDao.getGuildProgress(guild.getName(), description);
    }

    public boolean isTaskCompleted(Guild guild, String description) {
        return monthlyTaskDao.isTaskCompleted(guild.getName(), description);
    }

    public boolean isRewardClaimed(Guild guild, String description, UUID playerId) {
        return monthlyTaskDao.isRewardClaimed(guild.getName(), description, playerId);
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

    public void assignRandomMonthlyTask(String guildName) {
        List<MonthlyTask> pool = monthlyTaskPools.get(guildName);
        if (pool == null || pool.isEmpty()) return;

        Collections.shuffle(pool);
        MonthlyTask chosen = pool.get(0);

        // store in memory
        assignedTasks.put(guildName, chosen);

        // also store in DB
        monthlyTaskDao.assignMonthlyTask(guildName, chosen);
    }

    public MonthlyTask loadMonthlyTask(String guildName) {
        return monthlyTaskDao.loadMonthlyTask(guildName);
    }

    public void resetMonthlyTasks() {
        assignedTasks.clear();
        monthlyTaskPools.clear();
        monthlyTaskDao.resetMonthlyTasks();
        loadMonthlyTaskPoolsFromConfig();

        for (Guild guild : guildManager.getAllGuilds()) {
            assignRandomMonthlyTask(guild.getName());
        }
    }

    public void tryMonthlyResetOnStartup() {
        assignedTasks = monthlyTaskDao.loadMonthlyTasks();
        if (assignedTasks.isEmpty()) {
            resetMonthlyTasks();
        }
    }
}
