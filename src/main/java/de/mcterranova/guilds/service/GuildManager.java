package de.mcterranova.guilds.service;

import de.mcterranova.guilds.database.dao.GuildDao;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.GuildMember;
import de.mcterranova.guilds.model.GuildType;

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

    public void addMemberToGuild(String guildName, UUID playerId) {
        guildDao.addMemberToGuild(guildName, playerId);
    }

    public void createGuild(String name, GuildType type) {
        Guild existing = getGuildByName(name);
        if (existing != null) return;
        guildDao.createGuild(name, type);
    }

    public void setGuildHQ(String guildName, String worldName, double x, double y, double z) {
        guildDao.updateGuildHQ(guildName, worldName, x, y, z);
    }
}
