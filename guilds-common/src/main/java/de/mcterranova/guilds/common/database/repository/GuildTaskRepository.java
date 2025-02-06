package de.mcterranova.guilds.common.database.repository;

import de.mcterranova.guilds.common.Guilds;
import de.mcterranova.guilds.common.database.dao.GuildTaskDao;
import de.mcterranova.guilds.common.model.GuildTask;
import de.mcterranova.guilds.common.model.TaskEventType;
import de.mcterranova.guilds.common.model.UnclaimedReward;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuildTaskRepository implements GuildTaskDao {

    private final Guilds guilds;
    private final DataSource dataSource;

    public GuildTaskRepository(Guilds guilds, DataSource dataSource) {
        this.guilds = guilds;
        this.dataSource = dataSource;
    }

    @Override
    public void createTask(GuildTask task) {
        String sql = "INSERT INTO guild_tasks (" +
                "guild_name, periodicity, description, material_or_mob, required_amount, " +
                "points_reward, money_reward, assigned_date, event_type" +
                ") VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, task.getGuildName());
            ps.setString(2, task.getPeriodicity());
            ps.setString(3, task.getDescription());
            ps.setString(4, task.getMaterialOrMob());
            ps.setInt(5, task.getRequiredAmount());
            ps.setInt(6, task.getPointsReward());
            ps.setDouble(7, task.getMoneyReward());
            ps.setDate(8, Date.valueOf(task.getAssignedDate()));
            ps.setString(9, task.getEventType().name());

            ps.executeUpdate();

            // Optionally retrieve generated task_id
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    task.setTaskId(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to create task for guild " + task.getGuildName() + " exception: " + e.getMessage());
        }
    }

    @Override
    public List<GuildTask> loadTasksForGuild(String guildName, String periodicity) {
        List<GuildTask> tasks = new ArrayList<>();
        String sql = "SELECT task_id, description, material_or_mob, required_amount, points_reward, " +
                "money_reward, assigned_date, event_type " +
                "FROM guild_tasks " +
                "WHERE guild_name=? AND periodicity=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, guildName);
            ps.setString(2, periodicity);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GuildTask task = new GuildTask();
                    task.setTaskId(rs.getInt("task_id"));
                    task.setGuildName(guildName);
                    task.setPeriodicity(periodicity);
                    task.setDescription(rs.getString("description"));
                    task.setMaterialOrMob(rs.getString("material_or_mob"));
                    task.setRequiredAmount(rs.getInt("required_amount"));
                    task.setPointsReward(rs.getInt("points_reward"));
                    task.setMoneyReward(rs.getDouble("money_reward"));
                    task.setAssignedDate(rs.getDate("assigned_date").toLocalDate());
                    task.setEventType(TaskEventType.valueOf(rs.getString("event_type").toUpperCase()));
                    tasks.add(task);
                }
            }

        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to load tasks for guild " + guildName + " exception: " + e.getMessage());
        }
        return tasks;
    }

    @Override
    public GuildTask loadTaskById(int taskId) {
        String sql = "SELECT guild_name, periodicity, description, material_or_mob, required_amount, " +
                "points_reward, money_reward, assigned_date, event_type " +
                "FROM guild_tasks WHERE task_id=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    GuildTask task = new GuildTask();
                    task.setTaskId(taskId);
                    task.setGuildName(rs.getString("guild_name"));
                    task.setPeriodicity(rs.getString("periodicity"));
                    task.setDescription(rs.getString("description"));
                    task.setMaterialOrMob(rs.getString("material_or_mob"));
                    task.setRequiredAmount(rs.getInt("required_amount"));
                    task.setPointsReward(rs.getInt("points_reward"));
                    task.setMoneyReward(rs.getDouble("money_reward"));
                    task.setAssignedDate(rs.getDate("assigned_date").toLocalDate());
                    task.setEventType(TaskEventType.valueOf(rs.getString("event_type").toUpperCase()));
                    return task;
                }
            }
        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to load task with ID " + taskId + " exception: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void deleteTasksByDateAndPeriodicity(LocalDate date, String periodicity) {
        final String dailySql =
                "DELETE FROM guild_tasks "
                        + "WHERE periodicity = ? "
                        + "  AND assigned_date < ?";

        final String monthlySql =
                "DELETE FROM guild_tasks "
                        + "WHERE periodicity = ? "
                        + "  AND (YEAR(assigned_date)  < YEAR(?) "
                        + "       OR MONTH(assigned_date) < MONTH(?))";

        String sql;

        if ("DAILY".equalsIgnoreCase(periodicity)) {
            sql = dailySql;
        } else if ("MONTHLY".equalsIgnoreCase(periodicity)) {
            sql = monthlySql;
        } else {
            return;
        }

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, periodicity);
            ps.setDate(2, Date.valueOf(date));

            if ("MONTHLY".equalsIgnoreCase(periodicity)) {
                ps.setDate(3, Date.valueOf(date));
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to delete tasks for periodicity " + periodicity + " exception: " + e.getMessage());
        }
    }


    // ------------------
    // Guild progress
    // ------------------

    @Override
    public int getGuildProgress(int taskId, String guildName) {
        String sql = "SELECT SUM(progress) AS total_progress FROM guild_task_progress WHERE task_id=? AND player_uuid IN " +
                "(SELECT player_uuid FROM guild_members WHERE guild_name=?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setString(2, guildName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_progress");
                }
            }
        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to get guild progress for task " + taskId + " and guild " + guildName + " exception: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean isTaskCompleted(int taskId, String guildName) {
        String sql = "SELECT completed_at FROM guild_task_progress WHERE task_id=? AND player_uuid IN " +
                "(SELECT player_uuid FROM guild_members WHERE guild_name=?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setString(2, guildName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp completedAt = rs.getTimestamp("completed_at");
                    return (completedAt != null);
                }
            }
        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to check if task " + taskId + " is completed for guild " + guildName + " exception: " + e.getMessage());
        }
        return false;
    }

    @Override
    public void markGuildTaskCompleted(int taskId, String guildName) {
        String sql = "UPDATE guild_task_progress SET completed_at=NOW() WHERE task_id=? AND player_uuid IN " +
                "(SELECT player_uuid FROM guild_members WHERE guild_name=?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setString(2, guildName);
            ps.executeUpdate();

        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to mark task " + taskId + " as completed for guild " + guildName + " exception: " + e.getMessage());
        }
    }

    // ------------------
    // Player progress
    // ------------------

    @Override
    public void updatePlayerProgress(int taskId, UUID playerId, int progressDelta) {
        // We add the progressDelta to the existing value
        String sql = "INSERT INTO guild_task_progress (task_id, player_uuid, progress) " +
                "VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE progress=progress + VALUES(progress)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setString(2, playerId.toString());
            ps.setInt(3, progressDelta);
            ps.executeUpdate();

        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to update progress for player " + playerId + " and task " + taskId + " exception: " + e.getMessage());
        }
    }

    @Override
    public int getPlayerProgress(int taskId, UUID playerId) {
        String sql = "SELECT progress FROM guild_task_progress WHERE task_id=? AND player_uuid=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setString(2, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("progress");
                }
            }
        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to get progress for player " + playerId + " and task " + taskId + " exception: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean isTaskCompleted(int taskId, UUID playerId) {
        String sql = "SELECT completed_at FROM guild_task_progress WHERE task_id=? AND player_uuid=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setString(2, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp completedAt = rs.getTimestamp("completed_at");
                    return (completedAt != null);
                }
            }
        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to check if task " + taskId + " is completed for player " + playerId + " exception: " + e.getMessage());
        }
        return false;
    }

    @Override
    public void markTaskCompleted(int taskId, UUID playerId) {
        String sql = "UPDATE guild_task_progress SET completed_at=NOW() WHERE task_id=? AND player_uuid=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setString(2, playerId.toString());
            ps.executeUpdate();

        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to mark task " + taskId + " as completed for player " + playerId + " exception: " + e.getMessage());
        }
    }

    @Override
    public boolean isTaskClaimed(int taskId, UUID playerId) {
        String sql = "SELECT claimed_at FROM guild_task_progress WHERE task_id=? AND player_uuid=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setString(2, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp claimedAt = rs.getTimestamp("claimed_at");
                    return (claimedAt != null);
                }
            }
        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to check if task " + taskId + " is claimed for player " + playerId + " exception: " + e.getMessage());
        }
        return false;
    }

    @Override
    public void markTaskClaimed(int taskId, UUID playerId) {
        String sql = "UPDATE guild_task_progress SET claimed_at=NOW() WHERE task_id=? AND player_uuid=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setString(2, playerId.toString());
            ps.executeUpdate();

        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to mark task " + taskId + " as claimed for player " + playerId + " exception: " + e.getMessage());
        }
    }

    // ------------------
    // Resets
    // ------------------

    @Override
    public Instant getLastReset(String resetType) {
        String sql = "SELECT last_reset FROM task_resets WHERE reset_type=?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

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
            guilds.getLogger().warning("Failed to get last reset for type " + resetType + " exception: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void setLastReset(String resetType, Instant timestamp) {
        String sql = "INSERT INTO task_resets (reset_type, last_reset) VALUES (?,?) " +
                "ON DUPLICATE KEY UPDATE last_reset=VALUES(last_reset)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, resetType);
            ps.setTimestamp(2, Timestamp.from(timestamp));
            ps.executeUpdate();

        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to set last reset for type " + resetType + " exception: " + e.getMessage());
        }
    }

    @Override
    public List<UnclaimedReward> getUnclaimedRewards() {
        List<UnclaimedReward> rewards = new ArrayList<>();
        String sql = "SELECT p.player_uuid, t.money_reward " +
                "FROM guild_task_progress p " +
                "JOIN guild_tasks t ON p.task_id = t.task_id " +
                "WHERE p.completed_at IS NOT NULL AND p.claimed_at IS NULL";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String playerId = rs.getString("player_uuid");
                int moneyReward = rs.getInt("money_reward");
                UnclaimedReward reward = new UnclaimedReward(playerId, moneyReward);
                rewards.add(reward);
            }
            return rewards;

        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to get unclaimed rewards exception: " + e.getMessage());
        }
        return rewards;
    }
}
