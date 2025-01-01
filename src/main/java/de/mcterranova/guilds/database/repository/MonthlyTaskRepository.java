package de.mcterranova.guilds.database.repository;

import de.mcterranova.guilds.database.dao.MonthlyTaskDao;
import de.mcterranova.guilds.model.MonthlyTask;
import de.mcterranova.guilds.model.TaskEventType;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

public class MonthlyTaskRepository implements MonthlyTaskDao {

    private final DataSource dataSource;

    public MonthlyTaskRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public MonthlyTask loadMonthlyTask(String guildName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT description, material_or_mob, required_amount, points_reward, money_reward, event_type " +
                             "FROM guild_monthly_tasks WHERE guild_name=?"
             )) {
            ps.setString(1, guildName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String desc = rs.getString("description");
                    String mat = rs.getString("material_or_mob");
                    int req = rs.getInt("required_amount");
                    int pr = rs.getInt("points_reward");
                    double mr = rs.getDouble("money_reward");
                    TaskEventType eventType = TaskEventType.valueOf(rs.getString("event_type").toUpperCase());

                    return new MonthlyTask(desc, mat, req, pr, mr, eventType);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void assignMonthlyTask(String guildName, MonthlyTask task) {
        // INSERT INTO guild_monthly_tasks ...
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO guild_monthly_tasks (guild_name, description, material_or_mob, required_amount, points_reward, money_reward, event_type, assigned_date) " +
                             "VALUES (?,?,?,?,?,?,?,CURDATE()) " +
                             "ON DUPLICATE KEY UPDATE description=VALUES(description), material_or_mob=VALUES(material_or_mob), " +
                             "required_amount=VALUES(required_amount), points_reward=VALUES(points_reward), money_reward=VALUES(money_reward), " +
                             "event_type=VALUES(event_type), assigned_date=VALUES(assigned_date)"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getMaterialOrMob());
            ps.setInt(4, task.getRequiredAmount());
            ps.setInt(5, task.getPointsReward());
            ps.setDouble(6, task.getMoneyReward());
            ps.setString(7, task.getEventType().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Progress
    @Override
    public int getGuildProgress(String guildName, String description) {
        // Aggregate progress from all players
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT SUM(progress) AS total_progress FROM guild_monthly_progress " +
                             "WHERE guild_name=? AND description=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, description);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_progress");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void updateGuildProgress(String guildName, String description, int progress) {
        // Not used in per-player progress; consider removing or repurposing
        // Alternatively, update total progress if tracking separately
    }

    @Override
    public boolean isTaskCompleted(String guildName, String description) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT completed_at FROM guild_monthly_progress WHERE guild_name=? AND description=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, description);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("completed_at");
                    return ts != null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void markTaskCompleted(String guildName, String description) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO guild_monthly_progress (guild_name, description, progress, completed_at) " +
                             "VALUES (?, ?, ?, NOW()) " +
                             "ON DUPLICATE KEY UPDATE progress=VALUES(progress), completed_at=VALUES(completed_at)"
             )) {
            // Assuming 'progress' in guild_monthly_progress reflects total progress
            int totalProgress = getGuildProgress(guildName, description);
            ps.setString(1, guildName);
            ps.setString(2, description);
            ps.setInt(3, totalProgress);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isRewardClaimed(String guildName, String description, UUID playerId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT claimed_at FROM guild_monthly_claim WHERE guild_name=? AND description=? AND player_uuid=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, description);
            ps.setString(3, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp claimed = rs.getTimestamp("claimed_at");
                    return claimed != null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void markRewardClaimed(String guildName, String description, UUID playerId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO guild_monthly_claim (guild_name, description, player_uuid, claimed_at) " +
                             "VALUES (?,?,?,NOW()) " +
                             "ON DUPLICATE KEY UPDATE claimed_at=VALUES(claimed_at)"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, description);
            ps.setString(3, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resetMonthlyTasks() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM guild_monthly_progress");
            stmt.executeUpdate("DELETE FROM guild_monthly_claim");
            stmt.executeUpdate("DELETE FROM guild_monthly_tasks");
            stmt.executeUpdate("DELETE FROM guild_monthly_player_progress"); // Also reset player contributions
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, MonthlyTask> loadMonthlyTasks() {
        HashMap<String, MonthlyTask> monthlyTasks = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM guild_monthly_tasks")) {
            while (rs.next()) {
                String guildName = rs.getString("guild_name");
                String desc = rs.getString("description");
                String mat = rs.getString("material_or_mob");
                int req = rs.getInt("required_amount");
                int pr = rs.getInt("points_reward");
                double mr = rs.getDouble("money_reward");
                TaskEventType eventType = TaskEventType.valueOf(rs.getString("event_type").toUpperCase());
                MonthlyTask mt = new MonthlyTask(desc, mat, req, pr, mr, eventType);
                monthlyTasks.put(guildName, mt);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return monthlyTasks;
    }

    @Override
    public Instant getLastReset(String resetType) {
        try (Connection conn = dataSource.getConnection();
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
        try (Connection conn = dataSource.getConnection()) {
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

    // **New Methods for Per-Player Monthly Progress**

    /**
     * Update a player's progress on a monthly task.
     *
     * @param guildName    Name of the guild
     * @param description  Description of the task
     * @param playerId     UUID of the player
     * @param progress     Amount to add to the player's progress
     */
    @Override
    public void updatePlayerMonthlyProgress(String guildName, String description, UUID playerId, int progress) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO guild_monthly_progress (guild_name, description, player_uuid, progress) " +
                             "VALUES (?,?,?,?) " +
                             "ON DUPLICATE KEY UPDATE progress = progress + VALUES(progress)"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, description);
            ps.setString(3, playerId.toString());
            ps.setInt(4, progress);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve a player's progress on a specific monthly task.
     *
     * @param guildName   Name of the guild
     * @param description Description of the task
     * @param playerId    UUID of the player
     * @return Progress amount, or 0 if none exists
     */
    @Override
    public int getPlayerMonthlyProgress(String guildName, String description, UUID playerId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT progress FROM guild_monthly_progress WHERE guild_name=? AND description=? AND player_uuid=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, description);
            ps.setString(3, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("progress");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Mark a player's contribution as completed.
     *
     * @param guildName   Name of the guild
     * @param description Description of the task
     */
    @Override
    public void markPlayerMonthlyTaskCompleted(String guildName, String description) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE guild_monthly_progress SET completed_at=NOW() " +
                             "WHERE guild_name=? AND description=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if a player's contribution to a monthly task is completed.
     *
     * @param guildName   Name of the guild
     * @param description Description of the task
     * @param playerId    UUID of the player
     * @return true if completed, false otherwise
     */
    @Override
    public boolean isPlayerMonthlyTaskCompleted(String guildName, String description, UUID playerId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT completed_at FROM guild_monthly_progress WHERE guild_name=? AND description=? AND player_uuid=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, description);
            ps.setString(3, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("completed_at");
                    return ts != null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
