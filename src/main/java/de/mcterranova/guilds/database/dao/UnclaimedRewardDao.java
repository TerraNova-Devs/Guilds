package de.mcterranova.guilds.database.dao;

import de.mcterranova.guilds.model.UnclaimedReward;

import java.util.List;
import java.util.UUID;

public interface UnclaimedRewardDao {
    void insertReward(UnclaimedReward reward);
    List<UnclaimedReward> getRewardsForPlayer(String playerUuid);
    void markRewardClaimed(int rewardId);
    void markRewardsClaimed(String playerUuid);
}
