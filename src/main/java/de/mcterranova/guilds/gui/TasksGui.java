package de.mcterranova.guilds.gui;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.GuildTask;
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
            boolean completed = taskManager.isTaskCompleted(task.getTaskId(), playerId);
            boolean claimed = taskManager.isTaskClaimed(task.getTaskId(), playerId);

            RoseItem item = buildTaskItem(task, progress, completed, claimed);
            // place them in row 3, columns left (just an example)
            addItem(19 + i, item);
        }

        // 4) Build monthly items
        for (int i = 0; i < monthlyTasks.size(); i++) {
            GuildTask task = monthlyTasks.get(i);

            // For monthly tasks, you might show both guild progress + personal
            // But in the simplest approach, you only track per-player completion
            int progress = taskManager.getGuildProgress(task.getTaskId(), guild.getName());
            boolean completed = taskManager.isTaskCompleted(task.getTaskId(), guild.getName());
            boolean claimed = taskManager.isTaskClaimed(task.getTaskId(), playerId);

            RoseItem item = buildTaskItem(task, progress, completed, claimed);
            // place them e.g. in row 3, columns right
            addItem(23 + i, item);
        }
    }

    private RoseItem buildTaskItem(GuildTask task, int progress, boolean completed, boolean claimed) {
        String displayName = "§a" + task.getDescription();
        List<Component> lore = new ArrayList<>();

        // Show e.g. "progress / required"
        lore.add(Component.text("§7Fortschritt: " + progress + "/" + task.getRequiredAmount()));
        lore.add(Component.text("§7Belohnung: " + task.getPointsReward() + " Pkt & " + task.getMoneyReward() + " Silber"));

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
                            // Claim it
                            Player p = (Player) e.getWhoClicked();
                            taskManager.claimReward(task, p);
                            // Re-open the GUI to refresh items
                            new TasksGui(plugin, p, guild, dailyTasks, monthlyTasks).open();
                        });
            }
        }
    }
}
