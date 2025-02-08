package de.mcterranova.guilds.listeners;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.gui.JoinGui;
import de.mcterranova.guilds.gui.TasksGui;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.GuildTask;
import de.mcterranova.guilds.service.GuildManager;
import de.mcterranova.guilds.service.NPCManager;
import de.mcterranova.guilds.service.TaskManager;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class NPCClickListener implements Listener {

    private final GuildManager guildManager;
    private final TaskManager taskManager;
    private final NPCManager npcManager;
    private final Guilds plugin;

    public NPCClickListener(Guilds plugin, GuildManager guildManager, TaskManager taskManager, NPCManager npcManager) {
        this.guildManager = guildManager;
        this.taskManager = taskManager;
        this.npcManager = npcManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onNPCClick(NPCRightClickEvent event) {
        Player player = event.getClicker();
        int npcId = event.getNPC().getId();

        String guildName = npcManager.getGuildNameByNPCId(npcId);
        if (guildName == null) return;

        Guild guild = guildManager.getGuildByName(guildName);
        if (guild == null) return;

        Guild playerGuild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (playerGuild == null || !playerGuild.getName().equalsIgnoreCase(guild.getName())) {
            // Player is not in this guild => show JoinGui for the clicked guild
            new JoinGui(player, guild, guildManager).open();
        } else {
            // Player is in a guild
            if (playerGuild.getName().equalsIgnoreCase(guildName)) {
                // The NPC's guild is the player's guild => open tasks GUI

                List<GuildTask> dailyTasks = taskManager.loadTasksForGuild(guild.getName(), "DAILY");
                List<GuildTask> monthlyTasks = taskManager.loadTasksForGuild(guild.getName(), "MONTHLY");

                // If you only ever have 1 monthly task, you might do:
                // GuildTask monthlyTask = monthlyTasks.isEmpty() ? null : monthlyTasks.get(0);

                new TasksGui(plugin, player, guild, dailyTasks, monthlyTasks).open();
            } else {
                player.sendMessage("§cDu bist bereits in einer anderen Gilde und kannst nicht beitreten.");
            }
        }
    }
}
