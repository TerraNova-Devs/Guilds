package de.mcterranova.guilds.commands;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.model.DailyTask;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.service.GuildManager;
import de.mcterranova.guilds.service.TaskManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;

public class GuildCommand implements CommandExecutor {
    private Guilds plugin;
    private GuildManager guildManager;
    private TaskManager taskManager;

    public GuildCommand(Guilds plugin, GuildManager guildManager, TaskManager taskManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.taskManager = taskManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur Spieler können diesen Befehl nutzen.");
            return true;
        }

        Player player = (Player)sender;
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (args.length == 0) {
            if (guild == null) {
                player.sendMessage("Du bist in keiner Gilde.");
            } else {
                player.sendMessage("§eDeine Gilde: §b" + guild.getName());
                player.sendMessage("§ePunkte: §b" + guild.getPoints());
                player.sendMessage("§eBefehle: /guild tasks");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("tasks")) {
            if (guild == null) {
                player.sendMessage("Du bist in keiner Gilde.");
                return true;
            }

            List<DailyTask> tasks = taskManager.getDailyTasksForGuild(guild.getName());
            player.sendMessage("§eTägliche Aufgaben für " + guild.getName() + ":");
            for (DailyTask dt : tasks) {
                player.sendMessage(" - " + dt.getDescription() + " (" + dt.getProgress(player.getUniqueId()) + "/" + dt.getRequiredAmount() + ")");
            }
            return true;
        }

        return true;
    }
}
