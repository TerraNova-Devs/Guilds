package de.mcterranova.guilds;

import de.mcterranova.guilds.commands.GuildAdminCommand;
import de.mcterranova.guilds.commands.GuildCommand;
import de.mcterranova.guilds.config.DatabaseConfig;
import de.mcterranova.guilds.config.PluginConfig;
import de.mcterranova.guilds.database.ConnectionPool;
import de.mcterranova.guilds.database.repository.GuildRepository;
import de.mcterranova.guilds.database.repository.MonthlyTaskRepository;
import de.mcterranova.guilds.database.repository.TaskRepository;
import de.mcterranova.guilds.listeners.NPCClickListener;
import de.mcterranova.guilds.listeners.PlayerProgressListener;
import de.mcterranova.guilds.service.*;
import de.mcterranova.guilds.util.TimeUtil;
import de.mcterranova.terranovaLib.roseGUI.RoseGUIListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;

public class Guilds extends JavaPlugin {

    private PluginConfig pluginConfig;
    private ConnectionPool connectionPool;

    private GuildRepository guildRepository;
    private TaskRepository taskRepository;

    private GuildManager guildManager;
    private TaskManager taskManager;
    private MonthlyTaskManager monthlyTaskManager;
    private RewardManager rewardManager;
    private NPCManager npcManager;
    private MonthlyTaskRepository monthlyTaskRepository;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginConfig = new PluginConfig(getConfig());

        DatabaseConfig dbConfig = pluginConfig.getDatabaseConfig();
        this.connectionPool = new ConnectionPool(dbConfig);

        runDatabaseSetup();

        this.guildRepository = new GuildRepository(connectionPool);
        this.taskRepository = new TaskRepository(connectionPool);
        this.monthlyTaskRepository = new MonthlyTaskRepository(connectionPool.getDataSource());

        this.guildManager = new GuildManager(this, guildRepository);
        this.taskManager = new TaskManager(this, guildManager, taskRepository);
        this.monthlyTaskManager = new MonthlyTaskManager(this, monthlyTaskRepository, guildManager);
        this.rewardManager = new RewardManager(this, guildManager, taskRepository);
        this.npcManager = new NPCManager(this, guildManager);

        getServer().getPluginManager().registerEvents(new PlayerProgressListener(this, guildManager, taskManager, monthlyTaskManager), this);
        getServer().getPluginManager().registerEvents(new NPCClickListener(this, guildManager, taskManager, monthlyTaskManager, npcManager), this);
        getServer().getPluginManager().registerEvents(new RoseGUIListener(), this);

        getCommand("guild").setExecutor(new GuildCommand(this, guildManager, taskManager));
        getCommand("guildadmin").setExecutor(new GuildAdminCommand(this, guildManager));

        Bukkit.getScheduler().runTaskLater(this, () -> {
            npcManager.init();
        }, 20L);

        taskManager.tryDailyResetOnStartup();
        monthlyTaskManager.tryMonthlyResetOnStartup();

        long ticksUntilMidnight = TimeUtil.getTicksUntilMidnight();
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            taskManager.dailyReset();
        }, ticksUntilMidnight, 24L * 60L * 60L * 20L);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            rewardManager.evaluateMonthlyWinner();
            monthlyTaskManager.resetMonthlyTasks();
        }, TimeUtil.getTicksUntilMonthEnd(), 30L * 24L * 60L * 60L * 20L);

        getLogger().info("GuildPlugin wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        connectionPool.close();
        getLogger().info("GuildPlugin wurde deaktiviert.");
    }

    private void runDatabaseSetup() {
        try (InputStream in = getResource("schema.sql")) {
            if (in == null) {
                getLogger().warning("No schema.sql found in plugin resources!");
                return;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            String sqlCommands = sb.toString();
            String[] statements = sqlCommands.split(";");

            try (Connection conn = connectionPool.getDataSource().getConnection();
                 Statement stmt = conn.createStatement()) {

                for (String s : statements) {
                    s = s.trim();
                    if (!s.isEmpty()) {
                        stmt.execute(s);
                    }
                }
                getLogger().info("Database setup completed successfully.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            getLogger().severe("Error running database setup: " + e.getMessage());
        }
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public GuildManager getGuildManager() {
        return guildManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }

    public MonthlyTaskManager getMonthlyTaskManager() {
        return monthlyTaskManager;
    }
}
