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
    private final Guilds plugin;
    private final GuildManager guildManager;
    private final TaskManager taskManager;

    public GuildAdminCommand(Guilds plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.taskManager = plugin.getTaskManager(); // single unified TaskManager
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guild.admin")) {
            sender.sendMessage("§cDu hast nicht genügend Rechte um diesen Befehl zu nutzen.");
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
                sender.sendMessage("§Gilde " + guildName + " mit Typ " + type + " wurde erstellt.");
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cUngültiger Gildentyp!");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("sethq")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Nur Spieler können diesen Befehl nutzen.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /guildadmin sethq <name>");
                return true;
            }
            String guildName = args[1];
            var guild = guildManager.getGuildByName(guildName);
            if (guild == null) {
                sender.sendMessage("§cGilde nicht gefunden.");
                return true;
            }

            var loc = player.getLocation();
            guildManager.setGuildHQ(guildName, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
            sender.sendMessage("§aHQ der Gilde " + guildName + " wurde zu deiner Position gesetzt.");
            return true;
        }

        return true;
    }
}
