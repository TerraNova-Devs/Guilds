package de.mcterranova.guilds.main.gui;

import de.mcterranova.guilds.common.model.Guild;
import de.mcterranova.guilds.common.model.GuildTask;
import de.mcterranova.guilds.common.model.UnclaimedReward;
import de.mcterranova.guilds.common.util.ProgressBar;
import de.mcterranova.guilds.main.services.UnclaimedRewardManager;
import de.mcterranova.guilds.main.GuildsMainPlugin;
import de.mcterranova.guilds.main.services.MainTaskManager;
import de.mcterranova.terranovaLib.roseGUI.RoseGUI;
import de.mcterranova.terranovaLib.roseGUI.RoseItem;
import io.th0rgal.oraxen.api.OraxenItems;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TasksGui extends RoseGUI {

    private final GuildsMainPlugin plugin;
    private final Guild guild;

    private final List<GuildTask> dailyTasks;
    private final List<GuildTask> monthlyTasks;

    private final MainTaskManager taskManager;
    private final UnclaimedRewardManager urManager;

    public TasksGui(GuildsMainPlugin plugin, Player player, Guild guild,
                    List<GuildTask> dailyTasks, List<GuildTask> monthlyTasks) {
        super(player, "tasks_gui", Component.text("Gildenaufgaben " + guild.getName().substring(0, 1).toUpperCase() + guild.getName().substring(1)), 5);
        this.plugin = plugin;
        this.guild = guild;
        this.dailyTasks = dailyTasks;
        this.monthlyTasks = monthlyTasks;
        this.taskManager = plugin.getTaskManager();
        this.urManager = plugin.getUnclaimedRewardManager();
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Player viewer = (Player) event.getPlayer();
        UUID playerId = viewer.getUniqueId();

        // 1) Fill with black glass
        RoseItem filler = new RoseItem.Builder()
                .material(Material.BLACK_STAINED_GLASS_PANE)
                .displayName("")
                .build();
        fillGui(filler);

        // 2) Heading items
        RoseItem dailyHeading = new RoseItem.Builder()
                .material(Material.PAINTING)
                .displayName(Component.text("§eTägliche Aufgaben"))
                .build();
        addItem(11, dailyHeading);

        RoseItem monthlyHeading = new RoseItem.Builder()
                .material(Material.PAINTING)
                .displayName(Component.text("§eMonatliche Aufgaben"))
                .build();
        addItem(14, monthlyHeading);

        // 3) Build daily task items
        for (int i = 0; i < dailyTasks.size(); i++) {
            GuildTask task = dailyTasks.get(i);

            int progress = taskManager.getPlayerProgress(task.getTaskId(), playerId);
            boolean completed = taskManager.isTaskCompleted(task.getTaskId(), playerId);
            boolean claimed = taskManager.isTaskClaimed(task.getTaskId(), playerId);

            RoseItem item = buildTaskItem(
                    task,
                    progress,
                    progress,
                    completed,
                    claimed,
                    false
            );
            addItem(19 + i, item);
        }

        // 4) Build monthly items
        for (int i = 0; i < monthlyTasks.size(); i++) {
            GuildTask task = monthlyTasks.get(i);

            int guildProgress = taskManager.getGuildProgress(task.getTaskId(), guild.getName());
            // For monthly tasks, we also want to show the player's personal progress:
            int personalProgress = taskManager.getPlayerProgress(task.getTaskId(), playerId);

            boolean completed = taskManager.isTaskCompleted(task.getTaskId(), guild.getName());
            boolean claimed = taskManager.isTaskClaimed(task.getTaskId(), playerId);

            RoseItem item = buildTaskItem(
                    task,
                    guildProgress,
                    personalProgress,
                    completed,
                    claimed,
                    true
            );
            addItem(23 + i, item);
        }

        RoseItem memberList = new RoseItem.Builder()
                .material(Material.PLAYER_HEAD)
                .displayName(Component.text("§eGildenmitglieder"))
                .addLore(
                        Component.text("§7Klicke, um die Mitgliederliste zu sehen."),
                        Component.text("§7Sortiert nach Gildenpunkten.")
                )
                .build()
                .onClick((InventoryClickEvent e) -> new GuildMemberGui((Player) e.getWhoClicked(), guild).open());

        addItem(16, memberList);

        RoseItem leaderboard = new RoseItem.Builder()
                .material(Material.TOTEM_OF_UNDYING)
                .displayName(Component.text("§eGilden-Rangliste"))
                .addLore(
                        Component.text("§7Klicke, um die Rangliste aller Gilden zu sehen."),
                        Component.text("§7Sortiert nach Gildenpunkten.")
                )
                .build()
                .onClick((InventoryClickEvent e) -> new GuildLeaderboardGui((Player) e.getWhoClicked(), plugin.getGuildManager(), plugin.getRewardManager()).open());

        addItem(25, leaderboard);

        List<UnclaimedReward> rewardsPreview = urManager.getRewards(playerId.toString());

        RoseItem.Builder claimRewardsItemBuilder = new RoseItem.Builder();

        if (!rewardsPreview.isEmpty()) {
            claimRewardsItemBuilder
                    .material(Material.CHEST)
                    .displayName(Component.text("§eUnbeanspruchte Belohnungen"))
                    .addLore(
                            Component.text("§7Du hast " + rewardsPreview.size() + " Belohnungen, die du abholen kannst.")
                    );
        } else {
            claimRewardsItemBuilder
                    .material(Material.CHEST)
                    .displayName(Component.text("§cBelohnungen abholen"))
                    .addLore(
                            Component.text("§7Klicke, um alle unbeanspruchten Belohnungen"),
                            Component.text("§7Du hast 0 Belohnungen, die du abholen kannst.")
                    );
        }
        RoseItem claimRewardsItem = claimRewardsItemBuilder.build()
                .onClick((InventoryClickEvent e) -> {
                    int rewardsClaimed = 0;
                    List<UnclaimedReward> rewards = urManager.getRewards(playerId.toString());
                    urManager.claimRewards(playerId.toString());
                    for (UnclaimedReward reward : rewards) {
                        int amount = (int) reward.getRewardMoney();
                        ItemStack moneyStack = OraxenItems.getItemById("terranova_silver").build();
                        moneyStack.setAmount(amount);
                        var remaining = viewer.getInventory().addItem(moneyStack.clone());
                        if (!remaining.isEmpty()) {
                            viewer.sendMessage("§cDein Inventar ist voll! Überschüssige Items wurden gedroppt.");
                            remaining.values().forEach(item ->
                                    viewer.getWorld().dropItem(viewer.getLocation(), item)
                            );
                        }
                        rewardsClaimed++;
                    }
                    if (rewardsClaimed > 0) {
                        viewer.sendMessage(Component.text("§aDu hast " + rewardsClaimed + " unbeanspruchte Belohnungen abgeholt!"));
                    } else {
                        viewer.sendMessage(Component.text("§eKeine unbeanspruchten Belohnungen gefunden."));
                    }
                    // Refresh the GUI.
                    new TasksGui(plugin, viewer, guild, dailyTasks, monthlyTasks).open();
                });

        addItem(34, claimRewardsItem);
    }

    private RoseItem buildTaskItem(GuildTask task,
                                   int totalProgress,
                                   int personalProgress,
                                   boolean completed,
                                   boolean claimed,
                                   boolean isMonthly) {

        String displayName = "§a" + task.getDescription();
        List<Component> lore = new ArrayList<>();

        String bar = ProgressBar.createProgressBar(totalProgress, task.getRequiredAmount());
        String progressLine = "§7Fortschritt: " + bar + " §e(" + totalProgress + "/" + task.getRequiredAmount() + ")";
        lore.add(Component.text(progressLine));

        if (isMonthly) {
            lore.add(Component.text("§7Dein Beitrag: §e" + personalProgress));
        }

        String rewardLine = "§7Belohnung: §b" + task.getPointsReward() + " §7Gildenpunkte & §b" + task.getMoneyReward() + " §7Silber";
        lore.add(Component.text(rewardLine));

        if (!completed) {
            lore.add(Component.text("§cNoch nicht abgeschlossen."));
            return new RoseItem.Builder()
                    .material(Material.PAPER)
                    .displayName(Component.text(displayName))
                    .addLore(lore.toArray(new Component[0]))
                    .build();
        } else {
            if (claimed) {
                lore.add(Component.text("§aAbgeschlossen & Belohnung abgeholt"));
                return new RoseItem.Builder()
                        .material(Material.BOOK)
                        .displayName(Component.text(displayName))
                        .addLore(lore.toArray(new Component[0]))
                        .isEnchanted(true)
                        .build();
            } else {
                lore.add(Component.text("§eAbgeschlossen! §6Klick, um Belohnung abzuholen"));
                return new RoseItem.Builder()
                        .material(Material.BOOK)
                        .displayName(Component.text(displayName))
                        .addLore(lore.toArray(new Component[0]))
                        .isEnchanted(true)
                        .build()
                        .onClick((InventoryClickEvent e) -> {
                            Player p = (Player) e.getWhoClicked();
                            taskManager.claimReward(task, p);
                            new TasksGui(plugin, p, guild, dailyTasks, monthlyTasks).open();
                        });
            }
        }
    }
}
