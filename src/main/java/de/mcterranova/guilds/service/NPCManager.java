package de.mcterranova.guilds.service;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.model.Guild;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public class NPCManager {
    private final Guilds plugin;
    private final GuildManager guildManager;
    private final Map<String, Integer> guildNameToNPCId = new HashMap<>();

    public NPCManager(Guilds plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    /**
     * Initializes NPCs after server and Citizens are fully loaded.
     * This method:
     * - Scans existing NPCs for guild metadata and registers them.
     * - Spawns NPCs for guilds that don't have any NPC yet.
     */
    public void init() {
        // Map existing NPCs from Citizens that have "guildName" metadata
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.data().has("guildName")) {
                String gName = npc.data().get("guildName");
                registerExistingNPC(gName, npc.getId());
            }
        }

        // For guilds that don't have an NPC, spawn them now
        for (Guild g : guildManager.getAllGuilds()) {
            spawnNPCIfNotExists(g);
        }

        plugin.getLogger().info("NPCManager initialization complete. All guild NPCs accounted for.");
    }

    /**
     * Registers an existing NPC (one that Citizens persisted) to the internal mapping.
     * @param guildName the name of the guild this NPC represents
     * @param npcId the NPC's ID
     */
    public void registerExistingNPC(String guildName, int npcId) {
        guildNameToNPCId.put(guildName.toLowerCase(), npcId);
    }

    /**
     * Spawns an NPC for the given guild if none exists yet.
     * If an NPC is already known for this guild, does nothing.
     */
    public void spawnNPCIfNotExists(Guild guild) {
        String lower = guild.getName().toLowerCase();
        if (guildNameToNPCId.containsKey(lower)) {
            // NPC already known, do nothing
            return;
        }
        if (guild.getHq() == null) {
            plugin.getLogger().warning("Guild " + guild.getName() + " has no HQ, NPC not spawned.");
            return;
        }

        // Create new NPC since none exists for this guild
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "§e" + guild.getName() + " NPC");
        npc.setName("§e" + guild.getName() + " NPC");
        npc.setProtected(true);
        npc.data().set("guildName", guild.getName());

        if (!npc.isSpawned()) {
            npc.spawn(guild.getHq());
        }

        guildNameToNPCId.put(lower, npc.getId());
    }

    /**
     * Retrieves the guild name from an NPC's ID, if it exists.
     * @param npcId the NPC's ID
     * @return the guild name associated with this NPC, or null if none
     */
    public String getGuildNameByNPCId(int npcId) {
        for (Map.Entry<String,Integer> e : guildNameToNPCId.entrySet()) {
            if (e.getValue() == npcId) {
                return e.getKey();
            }
        }
        return null;
    }

    /**
     * If you ever need to remove all NPCs (e.g., on plugin disable):
     */
    public void removeAllNPCs() {
        for (Integer id : guildNameToNPCId.values()) {
            NPC npc = CitizensAPI.getNPCRegistry().getById(id);
            if (npc != null) {
                npc.despawn();
                CitizensAPI.getNPCRegistry().deregister(npc);
            }
        }
        guildNameToNPCId.clear();
    }
}
