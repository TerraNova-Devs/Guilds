package de.mcterranova.guilds.database.dao;

import de.mcterranova.guilds.model.DailyTask;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.GuildType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface GuildDao {
    List<Guild> getAllGuilds();
    Guild getGuildByName(String name);
    Guild getGuildByMember(UUID playerId);
    void updateGuildPoints(String guildName, int points);
    void resetAllGuildPoints();
    void addMemberToGuild(String guildName, UUID playerId);
    void createGuild(String guildName, GuildType type);
    void updateGuildHQ(String guildName, String worldName, double x, double y, double z);
    boolean isPlayerInAnyGuild(UUID playerId);
}
