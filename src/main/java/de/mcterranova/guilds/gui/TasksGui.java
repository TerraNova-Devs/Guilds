package de.mcterranova.guilds.gui;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.GuildTask;
import de.mcterranova.guilds.service.GuildManager;
import de.mcterranova.guilds.service.TaskManager;
import de.mcterranova.terranovaLib.roseGUI.RoseGUI;
import de.mcterranova.terranovaLib.roseGUI.RoseItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TasksGui extends RoseGUI {

    private final Guilds plugin;
    private final Guild guild;

    private final List<GuildTask> dailyTasks;
    private final List<GuildTask> monthlyTasks;

    private final TaskManager taskManager;

    public TasksGui(Guilds plugin, Player player, Guild guild,
                    List<GuildTask> dailyTasks, List<GuildTask> monthlyTasks) {
        super(player, "tasks_gui", Component.text("Gildenaufgaben"), 4);
        this.plugin = plugin;
        this.guild = guild;
        this.dailyTasks = dailyTasks;
        this.monthlyTasks = monthlyTasks;
        this.taskManager = plugin.getTaskManager();
    }

    @Override
    public void onOpen(InventoryOpenEvent event) throws SQLException {
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
            int personalProgress = progress;  // For daily tasks, "personal" = total anyway
            boolean completed = taskManager.isTaskCompleted(task.getTaskId(), playerId);
            boolean claimed = taskManager.isTaskClaimed(task.getTaskId(), playerId);

            RoseItem item = buildTaskItem(
                    task,
                    progress,
                    personalProgress,
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
                .onClick((InventoryClickEvent e) -> {
                    new GuildMemberGui((Player) e.getWhoClicked(), guild).open();
                });

        addItem(16, memberList);

        RoseItem leaderboard = new RoseItem.Builder()
                .material(Material.TOTEM_OF_UNDYING)
                .displayName(Component.text("§eGilden-Rangliste"))
                .addLore(
                        Component.text("§7Klicke, um die Rangliste aller Gilden zu sehen."),
                        Component.text("§7Sortiert nach Gildenpunkten.")
                )
                .build()
                .onClick((InventoryClickEvent e) -> {
                    new GuildLeaderboardGui((Player) e.getWhoClicked(), plugin.getGuildManager()).open();
                });

        addItem(25, leaderboard);
    }

    private RoseItem buildTaskItem(GuildTask task,
                                   int totalProgress,
                                   int personalProgress,
                                   boolean completed,
                                   boolean claimed,
                                   boolean isMonthly) {

        String displayName = "§a" + task.getDescription();
        List<Component> lore = new ArrayList<>();

        String bar = createProgressBar(totalProgress, task.getRequiredAmount(), 10);
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

    /**
     * Creates a progress bar using partial block characters.
     *
     * Each bar segment can contain:
     * - A full filled block.
     * - A partial filled block followed by a partial empty block.
     *
     * This ensures that each segment occupies consistent space in the GUI.
     *
     * @param current the current progress (e.g., 55)
     * @param max     the required total (e.g., 100)
     * @param length  the total number of segments in the bar (e.g., 20)
     * @return a String representing the progress bar, e.g., "§a█████▌▏████▉▎████"
     */
    private String createProgressBar(int current, int max, int length) {
        if (max <= 0) {
            return "§a" + "█".repeat(length);
        }

        current = Math.min(current, max);

        double progressFraction = (double) current / max;
        double totalPartials = length * 8;
        double progressPartial = progressFraction * totalPartials;

        int fullSegments = (int) (progressPartial / 8);
        int partialValue = (int) (progressPartial % 8);

        StringBuilder bar = new StringBuilder("§a");

        for (int i = 0; i < fullSegments; i++) {
            bar.append("█");
        }

        if (partialValue > 0 && fullSegments < length) {
            bar.append(getPartialBlock(partialValue));
            bar.append("§7");
            bar.append(getPartialBlock(8 - partialValue + 1));
        }

        int filledLength = fullSegments + (partialValue > 0 ? 1 : 0);
        int remaining = length - filledLength;
        bar.append("§7");
        for (int i = 0; i < remaining; i++) {
            bar.append("█");
        }

        return bar.toString();
    }

    /**
     * Returns a partial block character based on the fractional value.
     *
     * @param partialValue an integer from 1 to 7 representing the fraction (1/8 to 7/8)
     * @return a String containing the partial block character
     */
    private String getPartialBlock(int partialValue) {
        switch (partialValue) {
            case 1: return "▏"; // 1/8
            case 2: return "▎"; // 2/8
            case 3: return "▍"; // 3/8
            case 4: return "▌"; // 4/8
            case 5: return "▋"; // 5/8
            case 6: return "▊"; // 6/8
            case 7: return "▉"; // 7/8
            default: return "";   // No partial block
        }
    }

}
