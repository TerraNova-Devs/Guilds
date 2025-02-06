package de.mcterranova.guilds.common.model;

import java.time.LocalDate;
import java.util.UUID;

public class GuildMember {
    private final UUID uuid;
    private int contributed_points;
    private LocalDate joined_at;

    public GuildMember(UUID uuid, int contributed_points, LocalDate joined_at) {
        this.uuid = uuid;
        this.contributed_points = contributed_points;
        this.joined_at = joined_at;
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

    public LocalDate getJoinedAt() {
        return joined_at;
    }

    public void setJoinedAt(LocalDate joined_at) {
        this.joined_at = joined_at;
    }
}
