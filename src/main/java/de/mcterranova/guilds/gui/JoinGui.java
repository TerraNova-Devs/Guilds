package de.mcterranova.guilds.gui;

import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.service.GuildManager;
import de.mcterranova.terranovaLib.roseGUI.RoseGUI;
import de.mcterranova.terranovaLib.roseGUI.RoseItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.sql.SQLException;
import java.util.UUID;

public class JoinGui extends RoseGUI {

    private final Guild guild;
    private final GuildManager guildManager;

    public JoinGui(Player player, Guild guild, GuildManager guildManager) {
        super(player, "join_gui", Component.text("Join " + guild.getName()), 1);
        this.guild = guild;
        this.guildManager = guildManager;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) throws SQLException {
        RoseItem joinItem = new RoseItem.Builder()
                .material(Material.EMERALD)
                .displayName(Component.text("Beitreten"))
                .build()
                .onClick((InventoryClickEvent e) -> {
                    Player p = (Player) e.getWhoClicked();
                    UUID playerId = p.getUniqueId();

                    if (guildManager.getGuildByPlayer(playerId) != null) {
                        p.sendMessage(Component.text("§cDu bist bereits in einer Gilde!"));
                        return;
                    }

                    guildManager.addMemberToGuild(guild.getName(), playerId);
                    p.sendMessage(Component.text("§aDu bist der Gilde " + guild.getName() + " beigetreten!"));
                    p.closeInventory();
                });

        addItem(4, joinItem);
    }
}