package de.mcterranova.guilds.listeners;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.gui.JoinGui;
import de.mcterranova.guilds.gui.TasksGui;
import de.mcterranova.guilds.model.DailyTask;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.service.*;
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
        if (playerGuild == null) {
            new JoinGui(player, guild, guildManager).open();
        } else {
            if (playerGuild.getName().equalsIgnoreCase(guildName)) {
                List<DailyTask> tasks = taskManager.getDailyTasksForGuild(guild.getName());
                new TasksGui(plugin, player, guild, tasks).open();
            } else {
                player.sendMessage("§cDu bist bereits in einer anderen Gilde und kannst nicht beitreten.");
            }
        }
    }
}
