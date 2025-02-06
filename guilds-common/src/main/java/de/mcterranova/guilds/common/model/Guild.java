package de.mcterranova.guilds.common.model;

import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

public class Guild {
    private final String name;
    private int points;
    private final List<GuildMember> members;
    private final GuildType type;
    private final Location hq;

    public Guild(String name, int points, List<GuildMember> members, GuildType type, Location hq) {
        this.name = name;
        this.points = points;
        this.members = members;
        this.type = type;
        this.hq = hq;
    }

    public String getName() { return name; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public List<GuildMember> getMembers() { return members; }
    public GuildType getType() { return type; }
    public Location getHq() { return hq; }

    public void addMember(GuildMember member) {
        if(!members.contains(member)) members.add(member);
    }

    public int getActiveMembersCount() {
        int activeCount = 0;
        for (GuildMember member : members) {
            if (isPlayerActive(member)) {
                activeCount++;
            }
        }
        return activeCount;
    }

    public boolean isPlayerActive(GuildMember player) {
        int scoreContribution =  player.getContributedPoints();

        return scoreContribution >= 20;
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }
}
