package de.mcterranova.guilds.common.database.repository;

import de.mcterranova.guilds.common.Guilds;
import de.mcterranova.guilds.common.database.ConnectionPool;
import de.mcterranova.guilds.common.database.dao.GuildDao;
import de.mcterranova.guilds.common.model.Guild;
import de.mcterranova.guilds.common.model.GuildMember;
import de.mcterranova.guilds.common.model.GuildType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuildRepository implements GuildDao {
    private final Guilds guilds;
    private final ConnectionPool pool;

    public GuildRepository(Guilds guilds, ConnectionPool pool) {
        this.guilds = guilds;
        this.pool = pool;
    }

    @Override
    public List<Guild> getAllGuilds() {
        List<Guild> guilds = new ArrayList<>();
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT guild_name, points, guild_type, hq_world, hq_x, hq_y, hq_z FROM guilds");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("guild_name");
                int points = rs.getInt("points");
                String typeStr = rs.getString("guild_type");
                GuildType type = GuildType.valueOf(typeStr.toUpperCase());

                String worldName = rs.getString("hq_world");
                double x = rs.getDouble("hq_x");
                double y = rs.getDouble("hq_y");
                double z = rs.getDouble("hq_z");
                Location hq = null;
                if (worldName != null) {
                    World w = Bukkit.getWorld(worldName);
                    if (w != null) {
                        hq = new Location(w, x, y, z);
                    }
                }

                List<GuildMember> members = getGuildMembers(name, conn);
                guilds.add(new Guild(name, points, members, type, hq));
            }
        } catch (Exception e) {
            this.guilds.getLogger().warning("Failed to get all guilds: " + e.getMessage());
        }
        return guilds;
    }

    private List<GuildMember> getGuildMembers(String guildName, Connection conn) {
        List<GuildMember> members = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT player_uuid, contributed_points, joined_at FROM guild_members WHERE guild_name=?")) {
            ps.setString(1, guildName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(new GuildMember(UUID.fromString(rs.getString("player_uuid")), rs.getInt("contributed_points"), rs.getDate("joined_at").toLocalDate()));
                }
            }
        } catch (Exception e) {
            guilds.getLogger().warning("Failed to get guild members for guild " + guildName + ": " + e.getMessage());
        }
        return members;
    }

    @Override
    public GuildMember getGuildMember(UUID playerId) {
        try (Connection conn = pool.getDataSource().getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT contributed_points, joined_at FROM guild_members WHERE player_uuid=?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new GuildMember(playerId, rs.getInt("contributed_points"),rs.getDate("joined_at").toLocalDate());
                }
            }
        } catch (Exception e) {
            guilds.getLogger().warning("Failed to get guild member for player " + playerId + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public void updatePlayerContribution(String guildName, GuildMember member) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE guild_members SET contributed_points=? WHERE guild_name=? AND player_uuid=?")) {
            ps.setInt(1, member.getContributedPoints());
            ps.setString(2, guildName);
            ps.setString(3, member.getUuid().toString());
            ps.executeUpdate();
        } catch (Exception e) {
            guilds.getLogger().warning("Failed to update player contribution for guild " + guildName + ": " + e.getMessage());
        }
    }

    @Override
    public Guild getGuildByName(String name) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT guild_name, points, guild_type, hq_world, hq_x, hq_y, hq_z FROM guilds WHERE guild_name=?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int points = rs.getInt("points");
                    String typeStr = rs.getString("guild_type");
                    GuildType type = GuildType.valueOf(typeStr.toUpperCase());

                    String worldName = rs.getString("hq_world");
                    double x = rs.getDouble("hq_x");
                    double y = rs.getDouble("hq_y");
                    double z = rs.getDouble("hq_z");
                    Location hq = null;
                    if (worldName != null) {
                        World w = Bukkit.getWorld(worldName);
                        if (w != null) {
                            hq = new Location(w, x, y, z);
                        }
                    }

                    List<GuildMember> members = getGuildMembers(name, conn);
                    return new Guild(name, points, members, type, hq);
                }
            }
        } catch (Exception e) {
            guilds.getLogger().warning("Failed to get guild by name " + name + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public Guild getGuildByMember(UUID playerId) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT guild_name FROM guild_members WHERE player_uuid=?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String guildName = rs.getString("guild_name");
                    return getGuildByName(guildName);
                }
            }
        } catch (Exception e) {
            guilds.getLogger().warning("Failed to get guild by member " + playerId + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public void updateGuildPoints(String guildName, int points) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE guilds SET points=? WHERE guild_name=?")) {
            ps.setInt(1, points);
            ps.setString(2, guildName);
            ps.executeUpdate();
        } catch (Exception e) {
            guilds.getLogger().warning("Failed to update guild points for guild " + guildName + ": " + e.getMessage());
        }
    }

    @Override
    public void resetAllGuildPoints() {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE guilds SET points=0")) {
            ps.executeUpdate();
        } catch (Exception e) {
            guilds.getLogger().warning("Failed to reset all guild points: " + e.getMessage());
        }
    }

    @Override
    public void resetAllPlayerPoints() {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE guild_members SET contributed_points=0")) {
            ps.executeUpdate();
        } catch (Exception e) {
            guilds.getLogger().warning("Failed to reset all player points: " + e.getMessage());
        }
    }

    @Override
    public void removeMemberFromGuild(UUID playerId) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM guild_members WHERE player_uuid=?")) {
            ps.setString(1, playerId.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            guilds.getLogger().warning("Failed to remove member from guild: " + e.getMessage());
        }
    }

    @Override
    public void addMemberToGuild(String guildName, UUID playerId) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO guild_members (guild_name, player_uuid, joined_at) VALUES (?,?,?)")) {
            ps.setString(1, guildName);
            ps.setString(2, playerId.toString());
            ps.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
            ps.executeUpdate();
        } catch (Exception e) {
            guilds.getLogger().warning("Failed to add member to guild: " + e.getMessage());
        }
    }

    @Override
    public boolean isPlayerInAnyGuild(UUID playerId) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT guild_name FROM guild_members WHERE player_uuid=?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            guilds.getLogger().warning("Failed to check if player is in any guild: " + e.getMessage());
        }
        return false;
    }

    public void createGuild(String guildName, GuildType type) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO guilds (guild_name, points, guild_type) VALUES (?, 0, ?)"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, type.name());
            ps.executeUpdate();
        } catch (Exception e) {
            guilds.getLogger().warning("Failed to create guild: " + e.getMessage());
        }
    }

    public void updateGuildHQ(String guildName, String worldName, double x, double y, double z) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE guilds SET hq_world=?, hq_x=?, hq_y=?, hq_z=? WHERE guild_name=?"
             )) {
            ps.setString(1, worldName);
            ps.setDouble(2, x);
            ps.setDouble(3, y);
            ps.setDouble(4, z);
            ps.setString(5, guildName);
            ps.executeUpdate();
        } catch (Exception e) {
            guilds.getLogger().warning("Failed to update guild HQ: " + e.getMessage());
        }
    }

}
