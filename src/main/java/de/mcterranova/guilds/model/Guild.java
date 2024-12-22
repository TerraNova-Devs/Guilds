package de.mcterranova.guilds.model;

import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

public class Guild {
    private String name;
    private int points;
    private List<UUID> members;
    private GuildType type;
    private Location hq;

    public Guild(String name, int points, List<UUID> members, GuildType type, Location hq) {
        this.name = name;
        this.points = points;
        this.members = members;
        this.type = type;
        this.hq = hq;
    }

    public String getName() { return name; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public List<UUID> getMembers() { return members; }
    public GuildType getType() { return type; }
    public Location getHq() { return hq; }

    public void addMember(UUID uuid) {
        if(!members.contains(uuid)) members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }
}
