package de.mcterranova.guilds;

import de.mcterranova.guilds.commands.GuildAdminCommand;
import de.mcterranova.guilds.commands.GuildCommand;
import de.mcterranova.guilds.config.DatabaseConfig;
import de.mcterranova.guilds.config.PluginConfig;
import de.mcterranova.guilds.database.ConnectionPool;
import de.mcterranova.guilds.database.repository.GuildRepository;
import de.mcterranova.guilds.database.repository.GuildTaskRepository;
import de.mcterranova.guilds.database.repository.UnclaimedRewardRepository;
import de.mcterranova.guilds.listeners.NPCClickListener;
import de.mcterranova.guilds.listeners.PlayerProgressListener;
import de.mcterranova.guilds.service.*;
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
    private GuildTaskRepository guildTaskRepository;
    private UnclaimedRewardRepository unclaimedRewardRepository;

    private GuildManager guildManager;
    private TaskManager taskManager;
    private RewardManager rewardManager;
    private NPCManager npcManager;
    private UnclaimedRewardManager unclaimedRewardManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginConfig = new PluginConfig(getConfig());

        DatabaseConfig dbConfig = pluginConfig.getDatabaseConfig();
        this.connectionPool = new ConnectionPool(dbConfig);

        runDatabaseSetup();

        this.guildRepository = new GuildRepository(connectionPool);
        this.guildTaskRepository = new GuildTaskRepository(connectionPool.getDataSource());
        this.unclaimedRewardRepository = new UnclaimedRewardRepository(connectionPool.getDataSource());

        this.guildManager = new GuildManager(guildRepository);
        this.unclaimedRewardManager = new UnclaimedRewardManager(this, unclaimedRewardRepository);
        this.taskManager = new TaskManager(this, guildManager, guildTaskRepository, unclaimedRewardManager);
        this.rewardManager = new RewardManager(this, guildManager, unclaimedRewardRepository);
        this.npcManager = new NPCManager(this, guildManager);

        getServer().getPluginManager().registerEvents(new PlayerProgressListener(this, guildManager, taskManager), this);
        getServer().getPluginManager().registerEvents(new NPCClickListener(this, guildManager, taskManager, npcManager), this);
        getServer().getPluginManager().registerEvents(new RoseGUIListener(), this);

        getCommand("guild").setExecutor(new GuildCommand(this, guildManager, taskManager));
        getCommand("guildadmin").setExecutor(new GuildAdminCommand(this, guildManager));

        Bukkit.getScheduler().runTaskLater(this, () -> {
            npcManager.init();
        }, 20L);

        taskManager.onStartup();

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

    public UnclaimedRewardManager getUnclaimedRewardManager() {
        return unclaimedRewardManager;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }

}
