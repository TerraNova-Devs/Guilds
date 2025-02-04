package de.mcterranova.guilds.gui;

import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.GuildMember;
import de.mcterranova.terranovaLib.roseGUI.RoseGUI;
import de.mcterranova.terranovaLib.roseGUI.RoseItem;
import de.mcterranova.terranovaLib.roseGUI.RosePagination;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GuildMemberGui extends RoseGUI {

    private final Guild guild;
    private final RosePagination pagination;

    public GuildMemberGui(Player player, Guild guild) {
        super(player, "guild_member_gui", Component.text("§eGildenmitglieder: " + guild.getName().substring(0, 1).toUpperCase() + guild.getName().substring(1)), 6);
        this.guild = guild;
        this.pagination = new RosePagination(this);
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        // Sort members by contributed points in descending order
        List<GuildMember> sortedMembers = guild.getMembers().stream()
                .sorted(Comparator.comparingInt(GuildMember::getContributedPoints).reversed())
                .collect(Collectors.toList());

        pagination.registerPageSlotsBetween(10, 16);
        pagination.registerPageSlotsBetween(19, 25);
        pagination.registerPageSlotsBetween(28, 34);
        pagination.registerPageSlotsBetween(37, 43);



        // Add members to pagination
        sortedMembers.forEach(member -> {
            try {
                RoseItem memberItem = new RoseItem.Builder()
                        .material(Material.PLAYER_HEAD)
                        .displayName(Component.text("§a" + Bukkit.getOfflinePlayer(member.getUuid()).getName()))
                        .addLore(
                                Component.text("§7Gildenpunkte: §e" + member.getContributedPoints()),
                                Component.text("§7Beigetreten: §e" + member.getJoinedAt().toString())
                        )
                        .build();
                SkullMeta skullMeta = (SkullMeta) memberItem.stack.getItemMeta();
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(member.getUuid()));
                memberItem.stack.setItemMeta(skullMeta);

                pagination.addItem(memberItem);
            } catch (Exception e) {
                RoseItem memberItem = new RoseItem.Builder()
                        .material(Material.PLAYER_HEAD)
                        .displayName(Component.text("§a" + member.getUuid()))
                        .addLore(
                                Component.text("§7Gildenpunkte: §e" + member.getContributedPoints()),
                                Component.text("§7Beigetreten: §e" + member.getJoinedAt().toString())
                        )
                        .build();
                pagination.addItem(memberItem);
            }
        });

        // Update the GUI with the current page
        pagination.update();

        // Add navigation items
        addNavigationItems();
    }

    /**
     * Adds navigation items for pagination.
     */
    private void addNavigationItems() {
        // Previous Page
        RoseItem previousPage = new RoseItem.Builder()
                .material(Material.ARROW)
                .displayName(Component.text("§eVorherige Seite"))
                .build()
                .onClick((InventoryClickEvent e) -> {
                    if (!pagination.isFirstPage()) {
                        pagination.goPreviousPage();
                        pagination.update();
                    }
                });
        addItem(48, previousPage);

        // Next Page
        RoseItem nextPage = new RoseItem.Builder()
                .material(Material.ARROW)
                .displayName(Component.text("§eNächste Seite"))
                .build()
                .onClick((InventoryClickEvent e) -> {
                    if (!pagination.isLastPage()) {
                        pagination.goNextPage();
                        pagination.update();
                    }
                });
        addItem(50, nextPage);

        // Fill the other slots in the navigation bar
        fillBorder();
    }

    /**
     * Fills the navigation bar with decorative filler items.
     */
    private void fillBorder() {
        RoseItem filler = new RoseItem.Builder()
                .material(Material.BLACK_STAINED_GLASS_PANE)
                .displayName(Component.text(""))
                .build();

        // Fill slots except for navigation buttons
        for (int i : new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45, 46, 47, 49, 51, 52, 53}) {
            addItem(i, filler);
        }
    }
}
