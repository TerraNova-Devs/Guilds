package de.mcterranova.guilds.service;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.database.dao.GuildTaskDao;  // <-- Use the unified DAO
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

public class RewardManager {

    private final Guilds plugin;
    private final GuildManager guildManager;
    private final GuildTaskDao taskDao;  // replaced old "TaskDao" with "GuildTaskDao"

    public RewardManager(Guilds plugin, GuildManager guildManager, GuildTaskDao taskDao) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.taskDao = taskDao;
    }

    /**
     * Called at the end of the month to determine which guild has the highest
     * “fair score,” grant them a reward, and reset all guild points and tasks.
     */
    public void evaluateMonthlyWinner() {
        // 1) Sort all guilds by "fair score"
        List<Guild> guilds = new ArrayList<>(guildManager.getAllGuilds());
        guilds.sort(Comparator.comparingDouble(this::calculateFairScore).reversed());

        // 2) If there's at least one guild, declare the top as winner
        if (!guilds.isEmpty()) {
            Guild winner = guilds.get(0);
            giveGuildMonthlyReward(winner);

            Bukkit.broadcastMessage("§eDie Gilde §b" + winner.getName() + "§e hat diesen Monat gewonnen!");

            // 3) Reset all guild points
            guildManager.resetAllGuildPoints();

            // 4) (Optional) Reset or remove tasks
            // If you truly want to wipe *every* existing task (both daily & monthly):
            resetAllTasksCompletely();
            // or, if you only want to remove the old monthly tasks, do something like:
            // taskDao.deleteTasksByDateAndPeriodicity(LocalDate.now(), "MONTHLY");
        }
    }

    /**
     * Example of a "fair score" calculation that normalizes by
     * guild size and uses a log factor to reward bigger rosters.
     */
    private double calculateFairScore(Guild guild) {
        int totalScore = guild.getPoints();
        int activeMembers = guild.getActiveMembersCount(); // you need to implement or adapt
        // Avoid division by zero: if guild has no members, treat as 0 or 1
        if (activeMembers < 1) {
            return 0.0;
        }
        return (double) totalScore / activeMembers * Math.log(activeMembers + 1);
    }

    /**
     * Award the monthly winner’s guild members a special item or currency.
     */
    public void giveGuildMonthlyReward(Guild guild) {
        // If you store the reward config in plugin.getConfig(),
        // adapt as needed. For example:
        double money = plugin.getPluginConfig().getMonthlyWinnerMoney();
        int amount = (int) money;

        // Get your custom currency item from Oraxen
        ItemStack currencyItem = OraxenItems.getItemById("terranova_silver").build();
        currencyItem.setAmount(amount);

        // Distribute to every guild member
        for (GuildMember member : guild.getMembers()) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(member.getUuid());
            if (offline.isOnline() && offline.getPlayer() != null) {
                Player p = offline.getPlayer();
                p.getInventory().addItem(currencyItem.clone());
                p.sendMessage("§aDeine Gilde hat gewonnen! Du erhältst " + amount + " Währungs-Items.");
            } else {
                // Spieler ist offline => implement some queue or storage
                // so that you can give the item later when they log in.
            }
        }
    }

    /**
     * Completely removes *all* tasks and all progress from the database.
     * This is optional; call it only if your design requires a full reset.
     */
    private void resetAllTasksCompletely() {
        // There's no built-in method in the new DAO interface,
        // but you can add one if you truly want to remove all tasks:
        // Example:
        try {
            taskDao.deleteTasksByDateAndPeriodicity(null, null); // or create a new method
        } catch (UnsupportedOperationException ex) {
            // Or call your own method in the repository that does:
            // "DELETE FROM guild_task_progress; DELETE FROM guild_tasks;"
            plugin.getLogger().info("All tasks were fully wiped from DB!");
        }
    }

}
