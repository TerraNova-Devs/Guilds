package de.mcterranova.guilds.service;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.database.dao.UnclaimedRewardDao;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.GuildMember;
import de.mcterranova.guilds.model.UnclaimedReward;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RewardManager {

    private final Guilds plugin;
    private final GuildManager guildManager;
    private final UnclaimedRewardDao unclaimedRewardDao;

    public RewardManager(Guilds plugin, GuildManager guildManager, UnclaimedRewardDao unclaimedRewardDao) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.unclaimedRewardDao = unclaimedRewardDao;
    }

    /**
     * Called at the end of the month to determine which guild has the highest
     * “fair score,” grant them a reward, and reset all guild points and tasks.
     */
    public void evaluateMonthlyWinner() {
        // Sort all guilds by "fair score"
        List<Guild> guilds = new ArrayList<>(guildManager.getAllGuilds());
        guilds.sort(Comparator.comparingDouble(this::calculateFairScore).reversed());

        // If there's at least one guild, declare the top as winner
        if (!guilds.isEmpty()) {
            Guild winner = guilds.getFirst();
            giveGuildMonthlyReward(winner);

            Bukkit.broadcastMessage("§eDie Gilde §b" + winner.getName() + "§e hat diesen Monat gewonnen!");

            // Reset all guild points
            guildManager.resetAllGuildPoints();
        }
    }

    /**
     * Example of a "fair score" calculation that normalizes by
     * guild size and uses a log factor to reward bigger rosters.
     */
    public double calculateFairScore(Guild guild) {
        int totalScore = guild.getPoints();
        int activeMembers = guild.getActiveMembersCount();
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
        double money = plugin.getPluginConfig().getMonthlyWinnerMoney();
        int amount = (int) money;

        // Distribute to every guild member
        for (GuildMember member : guild.getMembers()) {
            if(guild.isPlayerActive(member)) {
                UnclaimedReward reward = new UnclaimedReward(member.getUuid().toString(), amount);
                unclaimedRewardDao.insertReward(reward);
            }
        }
    }

}
