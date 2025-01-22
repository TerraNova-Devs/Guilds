package de.mcterranova.guilds.model;

import java.util.UUID;

public class GuildMember {
    private final UUID uuid;
    private int contributed_points;

    public GuildMember(UUID uuid, int contributed_points) {
        this.uuid = uuid;
        this.contributed_points = contributed_points;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getContributedPoints() {
        return contributed_points;
    }

    public void setContributedPoints(int contributed_points) {
        this.contributed_points = contributed_points;
    }

    public void addPoints(int points) {
        contributed_points += points;
    }

    public void removePoints(int points) {
        contributed_points -= points;
    }
}
