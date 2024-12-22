package de.mcterranova.guilds.database.repository;

import de.mcterranova.guilds.database.ConnectionPool;
import de.mcterranova.guilds.database.dao.GuildDao;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.GuildType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuildRepository implements GuildDao {
    private final ConnectionPool pool;

    public GuildRepository(ConnectionPool pool) {
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

                List<UUID> members = getGuildMembers(name, conn);
                guilds.add(new Guild(name, points, members, type, hq));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return guilds;
    }

    private List<UUID> getGuildMembers(String guildName, Connection conn) {
        List<UUID> members = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT player_uuid FROM guild_members WHERE guild_name=?")) {
            ps.setString(1, guildName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(UUID.fromString(rs.getString("player_uuid")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return members;
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

                    List<UUID> members = getGuildMembers(name, conn);
                    return new Guild(name, points, members, type, hq);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    @Override
    public void resetAllGuildPoints() {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE guilds SET points=0")) {
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addMemberToGuild(String guildName, UUID playerId) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO guild_members (guild_name, player_uuid) VALUES (?,?)")) {
            ps.setString(1, guildName);
            ps.setString(2, playerId.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

}
