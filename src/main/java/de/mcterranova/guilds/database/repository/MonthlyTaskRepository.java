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
                     "SELECT description, material_or_mob, required_amount, points_reward, money_reward, 'BLOCK_BREAK' as event_type " +
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
                    TaskEventType eventType = TaskEventType.valueOf(rs.getString("event_type"));

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
                     "INSERT INTO guild_monthly_tasks (guild_name, description, material_or_mob, required_amount, points_reward, money_reward, assigned_date) " +
                             "VALUES (?,?,?,?,?,?,CURDATE()) "
             )) {
            ps.setString(1, guildName);
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getMaterialOrMob());
            ps.setInt(4, task.getRequiredAmount());
            ps.setInt(5, task.getPointsReward());
            ps.setDouble(6, task.getMoneyReward());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Progress
    @Override
    public int getGuildProgress(String guildName, String description) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT progress FROM guild_monthly_progress WHERE guild_name=? AND description=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, description);
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

    @Override
    public void updateGuildProgress(String guildName, String description, int progress) {
        // upsert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO guild_monthly_progress (guild_name, description, progress) " +
                             "VALUES (?,?,?) " +
                             "ON DUPLICATE KEY UPDATE progress=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, description);
            ps.setInt(3, progress);
            ps.setInt(4, progress);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
                     "UPDATE guild_monthly_progress SET completed_at=NOW() WHERE guild_name=? AND description=?"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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

    public void markRewardClaimed(String guildName, String description, UUID playerId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO guild_monthly_claim (guild_name, description, player_uuid, claimed_at) " +
                             "VALUES (?,?,?,NOW()) " +
                             "ON DUPLICATE KEY UPDATE claimed_at=NOW()"
             )) {
            ps.setString(1, guildName);
            ps.setString(2, description);
            ps.setString(3, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void resetMonthlyTasks() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM guild_monthly_progress");
            stmt.executeUpdate("DELETE FROM guild_monthly_claim");
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
                TaskEventType eventType = TaskEventType.valueOf(rs.getString("event_type"));
                MonthlyTask mt = new MonthlyTask(desc, mat, req, pr, mr, eventType);
                monthlyTasks.put(guildName, mt);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return monthlyTasks;
    }
}
