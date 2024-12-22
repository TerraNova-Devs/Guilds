package de.mcterranova.guilds.service;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.database.dao.TaskDao;
import de.mcterranova.guilds.model.Guild;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.th0rgal.oraxen.api.OraxenItems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class RewardManager {
    private Guilds plugin;
    private GuildManager guildManager;
    private TaskDao taskDao;

    public RewardManager(Guilds plugin, GuildManager guildManager, TaskDao taskDao) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.taskDao = taskDao;
    }

    public void evaluateMonthlyWinner() {
        List<Guild> guilds = new ArrayList<>(guildManager.getAllGuilds());
        guilds.sort(Comparator.comparingInt(Guild::getPoints).reversed());
        if (!guilds.isEmpty()) {
            Guild winner = guilds.get(0);
            giveGuildMonthlyReward(winner);
            Bukkit.broadcastMessage("§eDie Gilde §b" + winner.getName() + "§e hat diesen Monat gewonnen!");
            guildManager.resetAllGuildPoints();
        }
        taskDao.resetTasks();
    }

    public void giveGuildMonthlyReward(Guild guild) {
        double money = plugin.getPluginConfig().getMonthlyWinnerMoney();
        int amount = (int) money;
        ItemStack currencyItem = OraxenItems.getItemById("terranova_silver").build();
        currencyItem.setAmount(amount);

        for (UUID memberUUID : guild.getMembers()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(memberUUID);
            if (op.isOnline() && op.getPlayer() != null) {
                Player p = op.getPlayer();
                p.getInventory().addItem(currencyItem.clone());
                p.sendMessage("§aDeine Gilde hat gewonnen! Du erhältst " + amount + " Währungs-Items.");
            } else {
                // Spieler offline - Hier muss ich das Item zwischenspeichern und bei Login vergeben.

            }
        }
    }
}
