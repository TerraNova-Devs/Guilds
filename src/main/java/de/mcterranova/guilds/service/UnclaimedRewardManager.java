package de.mcterranova.guilds.service;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.database.dao.GuildTaskDao;
import de.mcterranova.guilds.database.dao.UnclaimedRewardDao;
import de.mcterranova.guilds.model.UnclaimedReward;

import java.util.List;
import java.util.UUID;

public class UnclaimedRewardManager {

    private final Guilds plugin;
    private final UnclaimedRewardDao unclaimedRewardDao;

    public UnclaimedRewardManager(Guilds plugin, UnclaimedRewardDao unclaimedRewardDao) {
        this.plugin = plugin;
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
