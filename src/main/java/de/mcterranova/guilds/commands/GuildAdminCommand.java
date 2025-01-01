package de.mcterranova.guilds.commands;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.model.GuildType;
import de.mcterranova.guilds.service.GuildManager;
import de.mcterranova.guilds.service.TaskManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class GuildAdminCommand implements CommandExecutor {
    private Guilds plugin;
    private GuildManager guildManager;
    private TaskManager taskManager;

    public GuildAdminCommand(Guilds plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.taskManager = plugin.getTaskManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guild.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§e/guildadmin create <name> <type>");
            sender.sendMessage("§e/guildadmin sethq <name>");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /guildadmin create <name> <type>");
                return true;
            }
            String guildName = args[1];
            String typeStr = args[2].toUpperCase();
            try {
                GuildType type = GuildType.valueOf(typeStr);
                guildManager.createGuild(guildName, type);
                taskManager.assignDailyTasksForAllGuildsIfMissing();
                sender.sendMessage("§aGuild " + guildName + " created with type " + type + ".");
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cInvalid guild type!");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("sethq")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /guildadmin sethq <name>");
                return true;
            }
            Player player = (Player)sender;
            String guildName = args[1];
            var guild = guildManager.getGuildByName(guildName);
            if (guild == null) {
                sender.sendMessage("§cGuild not found.");
                return true;
            }

            var loc = player.getLocation();
            guildManager.setGuildHQ(guildName, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
            sender.sendMessage("§aHQ of guild " + guildName + " set to your current location.");
            return true;
        }

        return true;
    }
}