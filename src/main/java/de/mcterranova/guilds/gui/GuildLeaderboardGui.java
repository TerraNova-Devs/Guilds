package de.mcterranova.guilds.gui;

import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.service.GuildManager;
import de.mcterranova.terranovaLib.roseGUI.RoseGUI;
import de.mcterranova.terranovaLib.roseGUI.RoseItem;
import de.mcterranova.terranovaLib.roseGUI.RosePagination;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GuildLeaderboardGui extends RoseGUI {

    private final GuildManager guildManager;
    private final RosePagination pagination;

    public GuildLeaderboardGui(Player player, GuildManager guildManager) {
        super(player, "guild_leaderboard_gui", Component.text("§eGilden-Rangliste"), 6);
        this.guildManager = guildManager;
        this.pagination = new RosePagination(this);
    }

    @Override
    public void onOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        // Get all guilds sorted by points in descending order
        List<Guild> sortedGuilds = guildManager.getAllGuilds().stream()
                .sorted(Comparator.comparingInt(Guild::getPoints).reversed())
                .collect(Collectors.toList());

        pagination.registerPageSlotsBetween(10, 16);
        pagination.registerPageSlotsBetween(19, 25);
        pagination.registerPageSlotsBetween(28, 34);
        pagination.registerPageSlotsBetween(37, 43);

        // Add guilds to pagination
        int rank = 1;
        for (Guild guild : sortedGuilds) {
            RoseItem guildItem = new RoseItem.Builder()
                    .material(Material.EMERALD_BLOCK) // Top guilds get Emerald Blocks
                    .displayName(Component.text("§6#" + rank + " §a" + guild.getName()))
                    .addLore(
                            Component.text("§7Gildenpunkte: §e" + guild.getPoints()),
                            Component.text("§7Mitglieder: §e" + guild.getMembers().size())
                    )
                    .build();
            pagination.addItem(guildItem);
            rank++;
        }

        // Update the GUI with the current page
        pagination.update();

        // Add navigation items
        addNavigationItems();
    }

    /**
     * Adds navigation items for pagination.
     */
    private void addNavigationItems() {
        // Previous Page Button
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

        // Next Page Button
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

        // Fill other slots in the navigation bar
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

        for (int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 49, 51, 52, 53}) {
            addItem(i, filler);
        }
    }
}
