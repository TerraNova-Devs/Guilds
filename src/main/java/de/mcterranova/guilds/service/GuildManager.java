package de.mcterranova.guilds.service;

import de.mcterranova.guilds.database.dao.GuildDao;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.GuildMember;
import de.mcterranova.guilds.model.GuildType;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public class GuildManager {
    private final GuildDao guildDao;

    public GuildManager(GuildDao guildDao) {
        this.guildDao = guildDao;
    }

    public Guild getGuildByPlayer(UUID playerId) {
        return guildDao.getGuildByMember(playerId);
    }

    public Guild getGuildByName(String name) {
        return guildDao.getGuildByName(name);
    }

    public List<Guild> getAllGuilds() {
        return guildDao.getAllGuilds();
    }

    public void updateGuildPoints(String guildName, int points) {
        guildDao.updateGuildPoints(guildName, points);
    }

    public void updatePlayerPoints(String guildName, GuildMember member) {
        guildDao.updatePlayerContribution(guildName, member);
    }

    public void addPointsToPlayerContribution(String guildName, UUID playerId, int pointsToAdd) {
        // Retrieve the guild, find the member, update contributed points
        Guild guild = getGuildByName(guildName);
        if (guild == null) return;

        GuildMember member = guild.getMembers().stream()
                .filter(m -> m.getUuid().equals(playerId))
                .findFirst().orElse(null);
        if (member == null) return;

        member.setContributedPoints(member.getContributedPoints() + pointsToAdd);
        guildDao.updatePlayerContribution(guildName, member);
    }

    public void resetAllGuildPoints() {
        guildDao.resetAllGuildPoints();
    }

    public boolean isPlayerInAnyGuild(UUID playerId) {
        return guildDao.isPlayerInAnyGuild(playerId);
    }

    public void joinGuild(String guildName, UUID playerId) {
        if(canSwitchGuild(playerId)){
            guildDao.addMemberToGuild(guildName, playerId);
        }
    }

    public void createGuild(String name, GuildType type) {
        Guild existing = getGuildByName(name);
        if (existing != null) return;
        guildDao.createGuild(name, type);
    }

    public void setGuildHQ(String guildName, String worldName, double x, double y, double z) {
        guildDao.updateGuildHQ(guildName, worldName, x, y, z);
    }

    public GuildMember getGuildMember(UUID playerId) {
        return guildDao.getGuildMember(playerId);
    }

    /**
     * Checks if the player can switch guilds.
     * Conditions:
     * - It's the first week of the month.
     * OR
     * - The player hasn't switched guilds in the past 31 days.
     *
     * @param playerId The UUID of the player.
     * @return True if eligible to switch, false otherwise.
     */
    public boolean canSwitchGuild(UUID playerId) {
        Guild guild = getGuildByPlayer(playerId);
        if (guild == null) {
            // Player is not in any guild, can join any guild
            return true;
        }

        GuildMember member = guild.getMembers().stream()
                .filter(m -> m.getUuid().equals(playerId))
                .findFirst()
                .orElse(null);

        if (member == null) {
            return false; // Should not happen
        }

        LocalDate joinedAt = member.getJoinedAt();
        LocalDate today = LocalDate.now();

        boolean isFirstWeek = today.getDayOfMonth() <= 7;

        return isFirstWeek && joinedAt.isBefore(today);
    }

    /**
     * Handles the guild switching process for a player.
     * Removes the player from their current guild and updates the 'joined_at' date.
     *
     * @param player The player who wants to switch guilds.
     */
    public void switchGuild(String guildName, Player player) {
        UUID playerId = player.getUniqueId();
        Guild currentGuild = getGuildByPlayer(playerId);

        if (currentGuild != null) {
            leaveGuild(playerId);
            player.sendMessage(Component.text("§aDu hast deine Gilde verlassen: " + currentGuild.getName()));
        }

        // Update the 'joined_at' to the current date
        joinGuild(guildName, playerId);

        // Optionally, open a GUI to let the player join a new guild
        // For simplicity, we'll just notify them
        player.sendMessage(Component.text("§aDu bist der " + guildName + " beigetreten."));
    }

    public void leaveGuild(UUID playerId) {
        if(canSwitchGuild(playerId)) {
            guildDao.removeMemberFromGuild(playerId);
        }
    }
}
