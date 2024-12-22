package de.mcterranova.guilds.database.repository;

import de.mcterranova.guilds.database.ConnectionPool;
import de.mcterranova.guilds.database.dao.TaskDao;
import de.mcterranova.guilds.model.DailyTask;
import de.mcterranova.guilds.model.GuildType;
import de.mcterranova.guilds.model.TaskEventType;
import org.bukkit.Material;

import java.sql.*;
import java.sql.Date;
import java.time.Instant;
import java.util.*;

public class TaskRepository implements TaskDao {

    private final ConnectionPool pool;

    public TaskRepository(ConnectionPool pool) {
        this.pool = pool;
    }

    @Override
    public void assignDailyTasksForGuild(String guildName, List<DailyTask> tasks) {
        try (Connection conn = pool.getDataSource().getConnection()) {
            // Insert tasks for this guild
            for (DailyTask dt : tasks) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO guild_tasks (guild_name, description, material_or_mob, required_amount, points_reward, money_reward, assigned_date, event_type) VALUES (?,?,?,?,?,?,?,?)"
                )) {
                    ps.setString(1, guildName);
                    ps.setString(2, dt.getDescription());
                    ps.setString(3, dt.getMaterialOrMob());
                    ps.setInt(4, dt.getRequiredAmount());
                    ps.setInt(5, dt.getPointsReward());
                    ps.setDouble(6, dt.getMoneyReward());
                    ps.setDate(7, Date.valueOf(Instant.now().atZone(TimeZone.getDefault().toZoneId()).toLocalDate()));
                    ps.setString(8, dt.getEventType().name());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<DailyTask> loadDailyTasksForGuild(String guildName) {
        List<DailyTask> result = new ArrayList<>();
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT description, material_or_mob, required_amount, points_reward, money_reward, event_type FROM guild_tasks WHERE guild_name=?"
             )) {
            ps.setString(1, guildName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String desc = rs.getString("description");
                    String matOrMob = rs.getString("material_or_mob");
                    int req = rs.getInt("required_amount");
                    int pr = rs.getInt("points_reward");
                    double mr = rs.getDouble("money_reward");
                    String typeStr = rs.getString("event_type");
                    TaskEventType type = TaskEventType.valueOf(typeStr.toUpperCase());
                    DailyTask dt = new DailyTask(desc, matOrMob, req, pr, mr, type);
                    result.add(dt);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void updateTaskProgress(String guildName, DailyTask task, UUID playerId, int progress) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO guild_task_progress (guild_name, description, player_uuid, progress) VALUES (?,?,?,?) " +
                             "ON DUPLICATE KEY UPDATE progress=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, task.getDescription());
            ps.setString(3, playerId.toString());
            ps.setInt(4, progress);
            ps.setInt(5, progress);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getProgress(String guildName, DailyTask task, UUID playerId) {
        int prog = 0;
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT progress FROM guild_task_progress WHERE guild_name=? AND description=? AND player_uuid=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, task.getDescription());
            ps.setString(3, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    prog = rs.getInt("progress");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return prog;
    }

    @Override
    public boolean isTaskCompleted(String guildName, DailyTask task, UUID playerId) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT completed_at FROM guild_task_progress WHERE guild_name=? AND description=? AND player_uuid=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, task.getDescription());
            ps.setString(3, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp completedAt = rs.getTimestamp("completed_at");
                    return completedAt != null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void markTaskCompleted(String guildName, DailyTask task, UUID playerId) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE guild_task_progress SET completed_at=NOW() WHERE guild_name=? AND description=? AND player_uuid=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, task.getDescription());
            ps.setString(3, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resetTasks() {
        // Delete all tasks and their progress
        try (Connection conn = pool.getDataSource().getConnection()) {
            conn.prepareStatement("DELETE FROM guild_task_progress").executeUpdate();
            conn.prepareStatement("DELETE FROM guild_tasks").executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Instant getTaskCompletionTime(String guildName, DailyTask task, UUID playerId) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT completed_at FROM guild_task_progress WHERE guild_name=? AND description=? AND player_uuid=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, task.getDescription());
            ps.setString(3, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp completedAt = rs.getTimestamp("completed_at");
                    if (completedAt != null) {
                        return completedAt.toInstant();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isTaskClaimed(String guildName, DailyTask task, UUID playerId) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT claimed_at FROM guild_task_progress " +
                             "WHERE guild_name=? AND description=? AND player_uuid=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, task.getDescription());
            ps.setString(3, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp claimedAt = rs.getTimestamp("claimed_at");
                    return (claimedAt != null);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void markTaskClaimed(String guildName, DailyTask task, UUID playerId) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE guild_task_progress " +
                             "SET claimed_at=NOW() " +
                             "WHERE guild_name=? AND description=? AND player_uuid=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, task.getDescription());
            ps.setString(3, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean dailyTasksExistForToday() {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) as cnt FROM guild_tasks WHERE assigned_date=CURDATE()"
             )) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int cnt = rs.getInt("cnt");
                    return cnt > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Instant getLastReset(String resetType) {
        try (Connection conn = pool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT last_reset FROM task_resets WHERE reset_type=?"
             )) {
            ps.setString(1, resetType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("last_reset");
                    if (ts != null) {
                        return ts.toInstant();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setLastReset(String resetType, Instant timestamp) {
        // If row exists -> update, else insert
        try (Connection conn = pool.getDataSource().getConnection()) {
            // Upsert pattern
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO task_resets (reset_type, last_reset) VALUES (?,?) " +
                            "ON DUPLICATE KEY UPDATE last_reset=VALUES(last_reset)"
            )) {
                ps.setString(1, resetType);
                ps.setTimestamp(2, Timestamp.from(timestamp));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
