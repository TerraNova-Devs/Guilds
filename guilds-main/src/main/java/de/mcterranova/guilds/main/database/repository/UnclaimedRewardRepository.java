package de.mcterranova.guilds.main.database.repository;


import de.mcterranova.guilds.common.Guilds;
import de.mcterranova.guilds.common.model.UnclaimedReward;
import de.mcterranova.guilds.main.database.dao.UnclaimedRewardDao;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UnclaimedRewardRepository implements UnclaimedRewardDao {

    private final Guilds guilds;
    private final DataSource dataSource;

    public UnclaimedRewardRepository(Guilds guilds, DataSource dataSource) {
        this.guilds = guilds;
        this.dataSource = dataSource;
    }

    @Override
    public void insertReward(UnclaimedReward reward) {
        String sql = "INSERT INTO unclaimed_rewards (player_uuid, reward_money) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reward.getPlayerUuid());
            ps.setDouble(2, reward.getRewardMoney());
            ps.executeUpdate();
        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to insert reward for player " + reward.getPlayerUuid() + " exception: " + e.getMessage());
        }
    }

    @Override
    public List<UnclaimedReward> getRewardsForPlayer(String playerUuid) {
        List<UnclaimedReward> rewards = new ArrayList<>();
        String sql = "SELECT reward_id, player_uuid, reward_money FROM unclaimed_rewards WHERE player_uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int rewardId = rs.getInt("reward_id");
                    String pUuid = rs.getString("player_uuid");
                    double rewardMoney = rs.getDouble("reward_money");
                    rewards.add(new UnclaimedReward(rewardId, pUuid, rewardMoney));
                }
            }
        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to get rewards for player " + playerUuid + " exception: " + e.getMessage());
        }
        return rewards;
    }

    @Override
    public void markRewardClaimed(int rewardId) {
        String sql = "DELETE FROM unclaimed_rewards WHERE reward_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rewardId);
            ps.executeUpdate();
        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to mark reward " + rewardId + " as claimed exception: " + e.getMessage());
        }
    }

    @Override
    public void markRewardsClaimed(String playerUuid) {
        String sql = "DELETE FROM unclaimed_rewards WHERE player_uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            guilds.getLogger().warning("Failed to mark rewards for player " + playerUuid + " as claimed exception: " + e.getMessage());
        }
    }
}
