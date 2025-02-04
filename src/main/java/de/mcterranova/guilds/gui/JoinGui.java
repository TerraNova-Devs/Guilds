package de.mcterranova.guilds.gui;

import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.GuildMember;
import de.mcterranova.guilds.service.GuildManager;
import de.mcterranova.terranovaLib.roseGUI.RoseGUI;
import de.mcterranova.terranovaLib.roseGUI.RoseItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JoinGui extends RoseGUI {

    private final Guild guild;
    private final GuildManager guildManager;

    public JoinGui(Player player, Guild guild, GuildManager guildManager) {
        super(player, "join_gui", Component.text("Join " + guild.getName().substring(0, 1).toUpperCase() + guild.getName().substring(1)), 3);
        this.guild = guild;
        this.guildManager = guildManager;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) throws SQLException {
        Player viewer = (Player) event.getPlayer();
        UUID playerId = viewer.getUniqueId();

        RoseItem filler = new RoseItem.Builder()
                .material(Material.BLACK_STAINED_GLASS_PANE)
                .displayName("")
                .build();
        fillGui(filler);

        addLeaveSwitchGuildOption(viewer, playerId);
    }

    /**
     * Adds the Leave/Switch Guild option to the GUI.
     *
     * @param player   The player viewing the GUI.
     * @param playerId The UUID of the player.
     */
    private void addLeaveSwitchGuildOption(Player player, UUID playerId) {
        boolean canSwitch = guildManager.canSwitchGuild(playerId);
        Material material = canSwitch ? Material.GREEN_BANNER : Material.RED_BANNER;
        String displayName = canSwitch ? "§aGilde wechseln" : "§cGilde wechseln (gesperrt)";
        List<Component> lore = new ArrayList<>();

        if (canSwitch) {
            lore.add(Component.text("§7Du kannst jetzt deine Gilde wechseln."));
            lore.add(Component.text("§7Bedingungen:"));
            lore.add(Component.text("§7Anfang des Monats. (Die ersten 7 Tage)"));
            lore.add(Component.text("§7Heute noch nicht gewechselt."));
        } else {
            // Calculate remaining days
            GuildMember member = guildManager.getGuildMember(playerId);

            lore.add(Component.text("§7Du kannst deine Gilde noch nicht wechseln."));
            if (member != null) {
                LocalDate today = LocalDate.now();
                long daysUntilNextMonth = today.until(today.withDayOfMonth(1).plusMonths(1), ChronoUnit.DAYS);
                if (daysUntilNextMonth > 0 && today.getDayOfMonth() > 7) {
                    lore.add(Component.text("§7Noch §e" + daysUntilNextMonth + " §7Tage."));
                    lore.add(Component.text("§7Warte bis zum Anfang des nächsten Monats."));
                } else {
                    lore.add(Component.text("§7Heute bereits gewechselt."));
                }
            }
        }

        RoseItem switchGuildItem = new RoseItem.Builder()
                .material(material)
                .displayName(Component.text(displayName))
                .addLore(lore.toArray(new Component[0]))
                .build()
                .onClick((InventoryClickEvent e) -> {
                    if (canSwitch) {
                        Player p = (Player) e.getWhoClicked();
                        guildManager.switchGuild(guild.getName(), p);
                        p.sendMessage(Component.text("§aDu hast deine Gilde verlassen!"));
                        p.closeInventory();
                    } else {
                        player.sendMessage(Component.text("§cDu kannst deine Gilde noch nicht wechseln!"));
                    }
                });

        int slot = 13;
        addItem(slot, switchGuildItem);
    }
}