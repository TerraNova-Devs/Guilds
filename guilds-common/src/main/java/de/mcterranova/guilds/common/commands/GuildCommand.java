package de.mcterranova.guilds.common.commands;

import de.mcterranova.guilds.common.model.Guild;
import de.mcterranova.guilds.common.model.GuildTask;
import de.mcterranova.guilds.common.service.GuildManager;
import de.mcterranova.guilds.common.service.TaskManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class GuildCommand implements CommandExecutor {
    private final GuildManager guildManager;
    private final TaskManager taskManager;

    public GuildCommand(GuildManager guildManager, TaskManager taskManager) {
        this.guildManager = guildManager;
        this.taskManager = taskManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können diesen Befehl nutzen.");
            return true;
        }

        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (args.length == 0) {
            if (guild == null) {
                player.sendMessage("Du bist in keiner Gilde.");
            } else {
                player.sendMessage("§eDeine Gilde: §b" + guild.getName());
                player.sendMessage("§eGildenpunkte: §b" + guild.getPoints());
                player.sendMessage("§eBefehle: /guild tasks");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("tasks")) {
            if (guild == null) {
                player.sendMessage("Du bist in keiner Gilde.");
                return true;
            }

            // Load today's daily tasks for that guild:
            List<GuildTask> dailyTasks = taskManager.loadTasksForGuild(guild.getName(), "DAILY");
            player.sendMessage("§eTägliche Aufgaben für " + guild.getName() + ":");
            for (GuildTask task : dailyTasks) {
                int progress = taskManager.getPlayerProgress(task.getTaskId(), player.getUniqueId());
                // Or if you prefer a ratio: progress + "/" + task.getRequiredAmount()
                player.sendMessage(" - " + task.getDescription() + " (" + progress + "/" + task.getRequiredAmount() + ")");
            }
            return true;
        }

        return true;
    }
}
