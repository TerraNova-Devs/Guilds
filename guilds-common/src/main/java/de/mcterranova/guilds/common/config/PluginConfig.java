package de.mcterranova.guilds.common.config;

import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {
    private final FileConfiguration config;

    public PluginConfig(FileConfiguration config) {
        this.config = config;
    }

    public DatabaseConfig getDatabaseConfig() {
        return new DatabaseConfig(
                config.getString("database.host"),
                config.getInt("database.port"),
                config.getString("database.database"),
                config.getString("database.user"),
                config.getString("database.password")
        );
    }

    public double getDailyTaskMoneyBase() {
        return config.getDouble("economy.daily_task_money_base", 50.0);
    }

    public double getMonthlyWinnerMoney() {
        return config.getDouble("economy.monthly_winner_money", 1000.0);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
