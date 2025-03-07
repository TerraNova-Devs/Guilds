package de.mcterranova.guilds.gui;

import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.service.GuildManager;
import de.mcterranova.guilds.service.RewardManager;
import de.mcterranova.terranovaLib.roseGUI.RoseGUI;
import de.mcterranova.terranovaLib.roseGUI.RoseItem;
import de.mcterranova.terranovaLib.roseGUI.RosePagination;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GuildLeaderboardGui extends RoseGUI {

    private final GuildManager guildManager;
    private final RewardManager rewardManager;
    private final RosePagination pagination;

    // Now include the RewardManager (or alternatively pass the plugin instance to get it)
    public GuildLeaderboardGui(Player player, GuildManager guildManager, RewardManager rewardManager) {
        super(player, "guild_leaderboard_gui", Component.text("§eGilden-Rangliste"), 6);
        this.guildManager = guildManager;
        this.rewardManager = rewardManager;
        this.pagination = new RosePagination(this);
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        // Sort guilds by weighted score (highest first)
        List<Guild> sortedGuilds = guildManager.getAllGuilds().stream()
                .sorted(Comparator.comparingDouble(g -> rewardManager.calculateFairScore((Guild) g)).reversed())
                .collect(Collectors.toList());

        pagination.registerPageSlotsBetween(10, 16);
        pagination.registerPageSlotsBetween(19, 25);
        pagination.registerPageSlotsBetween(28, 34);
        pagination.registerPageSlotsBetween(37, 43);

        int rank = 1;
        for (Guild guild : sortedGuilds) {
            // Calculate the weighted score (only active members count)
            double weightedScore = rewardManager.calculateFairScore(guild);
            RoseItem guildItem = new RoseItem.Builder()
                    .material(Material.EMERALD_BLOCK)
                    .displayName(Component.text("§6#" + rank + " §a" + guild.getName()))
                    .addLore(
                            Component.text("§7Gildenpunkte: §e" + guild.getPoints()),
                            Component.text("§7Gewichtete Gildenpunkte: §e" + String.format("%.2f", weightedScore)),
                            Component.text("§7Mitglieder: §e" + guild.getMembers().size())
                    )
                    .build();
            pagination.addItem(guildItem);
            rank++;
        }

        pagination.update();
        addNavigationItems();
    }

    private void addNavigationItems() {
        // Add navigation items as before (previous/next page, filler, etc.)
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

        fillBorder();
    }

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
