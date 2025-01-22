package de.mcterranova.guilds.service;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.database.dao.TaskDao;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.GuildMember;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RewardManager {
    private final Guilds plugin;
    private final GuildManager guildManager;
    private final TaskDao taskDao;

    public RewardManager(Guilds plugin, GuildManager guildManager, TaskDao taskDao) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.taskDao = taskDao;
    }

    public void evaluateMonthlyWinner() {
        List<Guild> guilds = new ArrayList<>(guildManager.getAllGuilds());
        guilds = guilds.stream()
                .sorted(Comparator.comparingDouble(this::calculateFairScore).reversed())
                .collect(Collectors.toList());

        if (!guilds.isEmpty()) {
            Guild winner = guilds.get(0);
            giveGuildMonthlyReward(winner);
            Bukkit.broadcastMessage("§eDie Gilde §b" + winner.getName() + "§e hat diesen Monat gewonnen!");
            guildManager.resetAllGuildPoints();
        }
        taskDao.resetTasks();
    }

    private double calculateFairScore(Guild guild) {
        int totalScore = guild.getPoints();
        int activeMembers = guild.getActiveMembersCount(); // Assuming this method exists
        return (double) totalScore / activeMembers * Math.log(activeMembers + 1);
    }

    public void giveGuildMonthlyReward(Guild guild) {
        double money = plugin.getPluginConfig().getMonthlyWinnerMoney();
        int amount = (int) money;
        ItemStack currencyItem = OraxenItems.getItemById("terranova_silver").build();
        currencyItem.setAmount(amount);

        for (GuildMember member : guild.getMembers()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(member.getUuid());
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