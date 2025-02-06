package de.mcterranova.guilds.common.model;

public class UnclaimedReward {
    private int rewardId;
    private String playerUuid;
    private double rewardMoney;

    public UnclaimedReward(int rewardId, String playerUuid, double rewardMoney) {
        this.rewardId = rewardId;
        this.playerUuid = playerUuid;
        this.rewardMoney = rewardMoney;
    }

    public UnclaimedReward(String playerUuid, double rewardMoney) {
        this.playerUuid = playerUuid;
        this.rewardMoney = rewardMoney;
    }

    public int getRewardId() {
        return rewardId;
    }

    public void setRewardId(int rewardId) {
        this.rewardId = rewardId;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(String playerUuid) {
        this.playerUuid = playerUuid;
    }

    public double getRewardMoney() {
        return rewardMoney;
    }

    public void setRewardMoney(double rewardMoney) {
        this.rewardMoney = rewardMoney;
    }
}
