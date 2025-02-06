package de.mcterranova.guilds.farming;

import de.mcterranova.guilds.common.Guilds;
import de.mcterranova.guilds.common.commands.GuildCommand;
import de.mcterranova.guilds.common.config.DatabaseConfig;
import de.mcterranova.guilds.common.database.ConnectionPool;
import de.mcterranova.guilds.common.database.repository.GuildRepository;
import de.mcterranova.guilds.common.database.repository.GuildTaskRepository;
import de.mcterranova.guilds.common.listeners.PlayerProgressListener;
import de.mcterranova.guilds.common.service.GuildManager;
import de.mcterranova.guilds.common.service.TaskManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

public class GuildsFarmingPlugin extends Guilds {

    private ConnectionPool connectionPool;
    private GuildManager guildManager;
    private TaskManager taskManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // minimal reading of DB config from config.yml
        FileConfiguration cfg = getConfig();
        DatabaseConfig dbConfig = new DatabaseConfig(
                cfg.getString("database.host"),
                cfg.getInt("database.port"),
                cfg.getString("database.database"),
                cfg.getString("database.user"),
                cfg.getString("database.password")
        );
        connectionPool = new ConnectionPool(dbConfig);

        // Repositories
        GuildRepository guildRepo = new GuildRepository(this, connectionPool);
        GuildTaskRepository taskRepo = new GuildTaskRepository(this, connectionPool.getDataSource());

        // Services
        this.guildManager = new GuildManager(guildRepo);
        this.taskManager = new TaskManager(this, guildManager, taskRepo);

        // Register only the minimal events
        getServer().getPluginManager().registerEvents(new PlayerProgressListener(guildManager, taskManager), this);

        // minimal /guild command
        Objects.requireNonNull(getCommand("guild")).setExecutor(new GuildCommand(guildManager, taskManager));

        getLogger().info("GuildsFarming Plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (connectionPool != null) {
            connectionPool.close();
        }
        getLogger().info("GuildsFarming Plugin disabled.");
    }

    @Override
    public GuildManager getGuildManager() {
        return guildManager;
    }

    @Override
    public TaskManager getTaskManager() {
        return taskManager;
    }
}
