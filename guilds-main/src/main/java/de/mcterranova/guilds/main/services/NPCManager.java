package de.mcterranova.guilds.main.services;

import de.mcterranova.guilds.common.model.Guild;
import de.mcterranova.guilds.common.service.GuildManager;
import de.mcterranova.guilds.main.GuildsMainPlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public class NPCManager {
    private final GuildsMainPlugin plugin;
    private final GuildManager guildManager;
    private final Map<String, Integer> guildNameToNPCId = new HashMap<>();

    public NPCManager(GuildsMainPlugin plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    public void init() {
        plugin.getLogger().info("[NPCManager] Scanning for existing NPCs...");

        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            plugin.getLogger().info(" - Found NPC: ID=" + npc.getId() + ", name=" + npc.getName());
            if (npc.data().get("guildName") != null) {
                String gName = npc.data().get("guildName");
                plugin.getLogger().info("    -> NPC is associated with guild: " + gName);
                registerExistingNPC(gName, npc.getId());
            }
        }

        for (Guild g : guildManager.getAllGuilds()) {
            spawnNPCIfNotExists(g);
        }

        plugin.getLogger().info("[NPCManager] Initialization complete. All guild NPCs accounted for.");
    }

    public void registerExistingNPC(String guildName, int npcId) {
        guildNameToNPCId.put(guildName.toLowerCase(), npcId);
    }

    public void spawnNPCIfNotExists(Guild guild) {
        String lower = guild.getName().toLowerCase();

        if (guildNameToNPCId.containsKey(lower)) {
            return;
        }

        if (guild.getHq() == null) {
            plugin.getLogger().warning("Guild " + guild.getName() + " has no HQ; NPC not spawned.");
            return;
        }

        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "§e" + guild.getName() + " NPC");
        npc.setName("§e" + guild.getName() + " NPC");
        npc.setProtected(true);
        npc.data().setPersistent("guildName", guild.getName());

        if (!npc.isSpawned()) {
            npc.spawn(guild.getHq());
        }

        guildNameToNPCId.put(lower, npc.getId());
        plugin.getLogger().info("Spawned a new NPC for guild: " + guild.getName() + ", ID=" + npc.getId());
    }

    public String getGuildNameByNPCId(int npcId) {
        for (Map.Entry<String, Integer> e : guildNameToNPCId.entrySet()) {
            if (e.getValue() == npcId) {
                return e.getKey();
            }
        }
        return null;
    }

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
