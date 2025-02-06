package de.mcterranova.guilds.main.services;

import de.mcterranova.guilds.main.database.dao.UnclaimedRewardDao;
import de.mcterranova.guilds.common.model.UnclaimedReward;

import java.util.List;

public class UnclaimedRewardManager {

    private final UnclaimedRewardDao unclaimedRewardDao;

    public UnclaimedRewardManager(UnclaimedRewardDao unclaimedRewardDao) {
        this.unclaimedRewardDao = unclaimedRewardDao;
    }

    public void insertReward(UnclaimedReward reward) {
        unclaimedRewardDao.insertReward(reward);
    }

    public void claimRewards(String uuid) {
        unclaimedRewardDao.markRewardsClaimed(uuid);
    }

    public List<UnclaimedReward> getRewards (String uuid) {
        return unclaimedRewardDao.getRewardsForPlayer(uuid);
    }

}
