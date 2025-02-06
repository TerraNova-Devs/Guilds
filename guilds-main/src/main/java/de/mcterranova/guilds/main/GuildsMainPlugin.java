package de.mcterranova.guilds.main;

import de.mcterranova.guilds.common.Guilds;
import de.mcterranova.guilds.common.commands.GuildCommand;
import de.mcterranova.guilds.common.config.DatabaseConfig;
import de.mcterranova.guilds.common.config.PluginConfig;
import de.mcterranova.guilds.common.database.ConnectionPool;
import de.mcterranova.guilds.common.database.repository.GuildRepository;
import de.mcterranova.guilds.common.database.repository.GuildTaskRepository;
import de.mcterranova.guilds.main.database.repository.UnclaimedRewardRepository;
import de.mcterranova.guilds.common.listeners.PlayerProgressListener;
import de.mcterranova.guilds.common.service.*;
import de.mcterranova.guilds.main.commands.GuildAdminCommand;
import de.mcterranova.guilds.main.listeners.NPCClickListener;
import de.mcterranova.guilds.main.services.MainTaskManager;
import de.mcterranova.guilds.main.services.NPCManager;
import de.mcterranova.guilds.main.services.RewardManager;
import de.mcterranova.guilds.main.services.UnclaimedRewardManager;
import de.mcterranova.terranovaLib.roseGUI.RoseGUIListener;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

public class GuildsMainPlugin extends Guilds {

    private PluginConfig pluginConfig;
    private ConnectionPool connectionPool;

    private GuildManager guildManager;
    private MainTaskManager taskManager;
    private RewardManager rewardManager;
    private UnclaimedRewardManager unclaimedRewardManager;
    private NPCManager npcManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginConfig = new PluginConfig(getConfig());

        // Database setup
        DatabaseConfig dbConfig = pluginConfig.getDatabaseConfig();
        this.connectionPool = new ConnectionPool(dbConfig);

        runDatabaseSetup();

        // Repositories
        GuildRepository guildRepo = new GuildRepository(this, connectionPool);
        GuildTaskRepository taskRepo = new GuildTaskRepository(this, connectionPool.getDataSource());
        UnclaimedRewardRepository unclaimedRepo = new UnclaimedRewardRepository(this, connectionPool.getDataSource());

        // Services from guilds-common
        this.guildManager = new GuildManager(guildRepo);

        // Services
        this.rewardManager = new RewardManager(this, guildManager, unclaimedRepo);
        this.unclaimedRewardManager = new UnclaimedRewardManager(unclaimedRepo);
        this.taskManager = new MainTaskManager(this, guildManager, taskRepo, unclaimedRewardManager);

        // NPC Manager
        npcManager = new NPCManager(this, guildManager);
        Bukkit.getScheduler().runTaskLater(this, () -> npcManager.init(), 20L);

        // Register events
        getServer().getPluginManager().registerEvents(new PlayerProgressListener(guildManager, taskManager), this);
        getServer().getPluginManager().registerEvents(new NPCClickListener(this, guildManager, taskManager, npcManager), this);
        getServer().getPluginManager().registerEvents(new RoseGUIListener(), this);

        // Commands
        Objects.requireNonNull(getCommand("guild")).setExecutor(new GuildCommand(guildManager, taskManager));
        Objects.requireNonNull(getCommand("guildadmin")).setExecutor(new GuildAdminCommand(guildManager));

        getLogger().info("GuildsMain Plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (connectionPool != null) {
            connectionPool.close();
        }
        getLogger().info("GuildsMain Plugin disabled.");
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

            String[] statements = sb.toString().split(";");
            try (Connection conn = connectionPool.getDataSource().getConnection();
                 Statement stmt = conn.createStatement()) {
                for (String s : statements) {
                    s = s.trim();
                    if (!s.isEmpty()) {
                        stmt.execute(s);
                    }
                }
            }
            getLogger().info("Database schema setup complete.");
        } catch (Exception e) {
            this.getLogger().severe("Error setting up database schema: " + e.getMessage());
        }
    }

    @Override
    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    @Override
    public GuildManager getGuildManager() {
        return guildManager;
    }

    @Override
    public MainTaskManager getTaskManager() {
        return taskManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public UnclaimedRewardManager getUnclaimedRewardManager() {
        return unclaimedRewardManager;
    }
}
