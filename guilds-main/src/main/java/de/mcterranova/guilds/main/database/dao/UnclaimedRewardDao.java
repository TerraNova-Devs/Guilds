package de.mcterranova.guilds.main.database.dao;

import de.mcterranova.guilds.common.model.UnclaimedReward;

import java.util.List;

public interface UnclaimedRewardDao {
    void insertReward(UnclaimedReward reward);
    List<UnclaimedReward> getRewardsForPlayer(String playerUuid);
    void markRewardClaimed(int rewardId);
    void markRewardsClaimed(String playerUuid);
}
