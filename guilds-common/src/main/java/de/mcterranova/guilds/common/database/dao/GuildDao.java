package de.mcterranova.guilds.common.database.dao;

import de.mcterranova.guilds.common.model.Guild;
import de.mcterranova.guilds.common.model.GuildMember;
import de.mcterranova.guilds.common.model.GuildType;

import java.util.List;
import java.util.UUID;

public interface GuildDao {
    List<Guild> getAllGuilds();
    GuildMember getGuildMember(UUID playerId);
    Guild getGuildByName(String name);
    Guild getGuildByMember(UUID playerId);
    void updateGuildPoints(String guildName, int points);
    void resetAllGuildPoints();
    void resetAllPlayerPoints();
    void removeMemberFromGuild(UUID playerId);
    void addMemberToGuild(String guildName, UUID playerId);
    void createGuild(String guildName, GuildType type);
    void updateGuildHQ(String guildName, String worldName, double x, double y, double z);
    boolean isPlayerInAnyGuild(UUID playerId);
    void updatePlayerContribution(String guildName, GuildMember member);
}
