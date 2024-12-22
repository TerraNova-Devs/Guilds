package de.mcterranova.guilds.gui;

import de.mcterranova.guilds.Guilds; // Your main plugin class
import de.mcterranova.guilds.model.DailyTask;
import de.mcterranova.guilds.model.Guild;
import de.mcterranova.guilds.model.MonthlyTask;
import de.mcterranova.guilds.service.MonthlyTaskManager;
import de.mcterranova.guilds.service.TaskManager;
import de.mcterranova.terranovaLib.roseGUI.RoseGUI;
import de.mcterranova.terranovaLib.roseGUI.RoseItem;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemFlag;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.bukkit.Bukkit.getPlayer;

public class TasksGui extends RoseGUI {

    private final Guilds plugin;
    private final Guild guild;

    // Tägliche Tasks pro Spieler
    private final List<DailyTask> dailyTasks;

    // Monatliche Gilden-Task (falls vorhanden)
    private MonthlyTask monthlyTask;

    // Manager für tägliche und monatliche Logik
    private final TaskManager taskManager;
    private final MonthlyTaskManager monthlyTaskManager;

    /**
     * @param plugin     Haupt-Plugin-Klasse
     * @param player     Der Spieler, der die GUI öffnet
     * @param guild      Die Gilde, zu der der Spieler gehört
     * @param dailyTasks Liste an täglichen (per-player) Tasks
     */
    public TasksGui(Guilds plugin, Player player, Guild guild, List<DailyTask> dailyTasks, MonthlyTask monthlyTask) {
        super(player, "tasks_gui", Component.text("Gildenaufgaben"), 4);
        this.plugin = plugin;
        this.guild = guild;
        this.dailyTasks = dailyTasks;

        // Zugriff auf vorhandene Manager
        this.taskManager = plugin.getTaskManager();
        this.monthlyTaskManager = plugin.getMonthlyTaskManager();

        // Die monatliche Task für diese Gilde laden
        this.monthlyTask = monthlyTask;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) throws SQLException {
        Player viewer = (Player) event.getPlayer();
        UUID playerId = viewer.getUniqueId();

        // 1) Inventar mit schwarzem Glas auffüllen
        RoseItem filler = new RoseItem.Builder()
                .material(Material.BLACK_STAINED_GLASS_PANE)
                .displayName("")
                .build();
        fillGui(filler);

        // 2) Überschriften für Tägliche und Monatliche Aufgaben
        RoseItem dailyHeading = new RoseItem.Builder()
                .material(Material.PAINTING)
                .displayName(Component.text("§eTägliche Aufgaben"))
                .build();
        addItem(11, dailyHeading);

        RoseItem monthlyHeading = new RoseItem.Builder()
                .material(Material.PAINTING)
                .displayName(Component.text("§eMonatliche Aufgabe"))
                .build();
        addItem(14, monthlyHeading);

        // 3) Tägliche Tasks-Items generieren
        RoseItem[] dailyItems = new RoseItem[dailyTasks.size()];
        for (int i = 0; i < dailyTasks.size(); i++) {
            DailyTask dt = dailyTasks.get(i);

            int progress = taskManager.getPlayerProgress(guild.getName(), dt, playerId);
            int required = dt.getRequiredAmount();
            boolean completed = taskManager.isTaskCompleted(guild.getName(), dt, playerId);
            boolean claimed = taskManager.isTaskClaimed(guild.getName(), dt, playerId);

            dailyItems[i] = buildDailyTaskItem(dt, progress, required, completed, claimed);
        }

        // 4) Tägliche Items z. B. in Slots 19,20,21... platzieren
        for (int i = 0; i < dailyItems.length; i++) {
            addItem(19 + i, dailyItems[i]);
        }

        // 5) Monatliche Task
        if (monthlyTask != null) {
            int guildProgress = monthlyTaskManager.getGuildProgress(guild, monthlyTask.getDescription());
            int required = monthlyTask.getRequiredAmount();
            boolean completed = monthlyTaskManager.isTaskCompleted(guild, monthlyTask.getDescription());
            boolean claimedByThisPlayer = monthlyTaskManager.isRewardClaimed(guild, monthlyTask.getDescription(), playerId);

            RoseItem monthlyItem = buildMonthlyTaskItem(monthlyTask, guildProgress, required, completed, claimedByThisPlayer);
            // Slot 23 als Beispiel
            addItem(23, monthlyItem);
        }

        // Optional extra items
        RoseItem playerHead = new RoseItem.Builder()
                .material(Material.PLAYER_HEAD)
                .displayName(Component.text("§aGildenbeiträge ansehen"))
                .build()
                .onClick((InventoryClickEvent e) -> {
                    Player p = (Player) e.getWhoClicked();
                    // new PlayerContributionsGui(p).open();
                });
        addItem(16, playerHead);

        RoseItem totem = new RoseItem.Builder()
                .material(Material.TOTEM_OF_UNDYING)
                .displayName(Component.text("§6Monatliche Rangliste"))
                .build()
                .onClick((InventoryClickEvent e) -> {
                    Player p = (Player) e.getWhoClicked();
                    // new MonthlyRaceGui(p).open();
                });
        addItem(25, totem);
    }

    /**
     * Baut ein Item für eine TÄGLICHE (per-player) Aufgabe.
     */
    private RoseItem buildDailyTaskItem(DailyTask dt, int progress, int required,
                                        boolean completed, boolean claimed) {
        String displayName = "§a" + dt.getDescription();
        List<Component> lore = new ArrayList<>();

        if (!completed) {
            // Zeige Fortschrittsbalken
            return buildProgressItem(dt, displayName, progress, required, false);
        } else {
            // Aufgabe ist abgeschlossen
            if (claimed) {
                // Belohnung abgeholt
                lore.add(Component.text("§aAbgeschlossen"));
                lore.add(Component.text("§7Belohnung bereits abgeholt"));

                return new RoseItem.Builder()
                        .material(Material.BOOK)
                        .displayName(Component.text(displayName))
                        .addLore(lore.toArray(new Component[0]))
                        .isEnchanted(true)
                        .build();

            } else {
                // Noch nicht abgeholt -> klickbar
                lore.add(Component.text("§aAufgabe abgeschlossen!"));
                lore.add(Component.text("§6Klicke, um deine Belohnung abzuholen."));

                return new RoseItem.Builder()
                        .material(Material.BOOK)
                        .displayName(Component.text(displayName))
                        .addLore(lore.toArray(new Component[0]))
                        .isEnchanted(true)
                        .build()
                        .onClick((InventoryClickEvent e) -> {
                            // Belohnung beanspruchen
                            taskManager.claimReward(guild, dt, e.getWhoClicked().getUniqueId());
                            // GUI aktualisieren
                            new TasksGui(plugin, (Player) e.getWhoClicked(), guild, dailyTasks, monthlyTask).open();
                        });
            }
        }
    }

    /**
     * Baut ein Item für die MONATLICHE (gildenweite) Aufgabe,
     * das aber jeder Spieler EINMAL abholen kann.
     */
    private RoseItem buildMonthlyTaskItem(MonthlyTask mt, int guildProgress, int required,
                                          boolean completed, boolean claimedByThisPlayer) {

        String displayName = "§b(Monatlich) " + mt.getDescription();
        List<Component> lore = new ArrayList<>();

        if (!completed) {
            // Noch nicht abgeschlossen -> gildenweiter Fortschritt
            RoseItem item = buildProgressItem(mt, displayName, guildProgress, required, true);
            // wir fügen noch eine Zeile Lore hinzu, um zu kennzeichnen, dass es Gildenfortschritt ist
            item.stack.lore().add(Component.text("§7Gildenfortschritt"));
            return item;
        } else {
            // Gilde hat das Ziel erreicht
            if (claimedByThisPlayer) {
                // Spieler hat bereits abgeholt
                lore.add(Component.text("§aGilde hat es geschafft!"));
                lore.add(Component.text("§7Belohnung bereits abgeholt"));

                return new RoseItem.Builder()
                        .material(Material.BOOK)
                        .displayName(Component.text(displayName))
                        .addLore(lore.toArray(new Component[0]))
                        .isEnchanted(true)
                        .build();
            } else {
                // Gilde fertig, Spieler noch nicht abgeholt -> klickbar
                lore.add(Component.text("§aGilde hat die Aufgabe erfüllt!"));
                lore.add(Component.text("§6Klicke, um deine Belohnung abzuholen."));

                return new RoseItem.Builder()
                        .material(Material.BOOK)
                        .displayName(Component.text(displayName))
                        .addLore(lore.toArray(new Component[0]))
                        .isEnchanted(true)
                        .build()
                        .onClick((InventoryClickEvent e) -> {
                            // Gildenweiter Task -> jeder Spieler einmal abholen
                            monthlyTaskManager.claimMonthlyReward(guild, mt.getDescription(), e.getWhoClicked().getUniqueId());
                            // GUI neu öffnen
                            new TasksGui(plugin, (Player) e.getWhoClicked(), guild, dailyTasks, monthlyTask).open();
                        });
            }
        }
    }

    private RoseItem buildProgressItem(DailyTask dt, String displayName, int progress, int required, boolean isMonthly) {
        List<Component> lore = new ArrayList<>();
        int totalSegments = 10;
        double ratio = Math.min((double) progress / required, 1.0);
        int filledSegments = (int) (ratio * totalSegments);

        StringBuilder progressBar = new StringBuilder();
        for (int seg = 0; seg < totalSegments; seg++) {
            progressBar.append(seg < filledSegments ? "§a■" : "§7■");
        }
        int percent = (int)(ratio * 100);

        lore.add(Component.text(progressBar.toString()));
        lore.add(Component.text("§7" + progress + "§f/§7" + required + "  (§a" + percent + "%§f)"));
        lore.add(Component.text("§3+ " + (int)dt.getMoneyReward() + " Silber"));

        return new RoseItem.Builder()
                .material(Material.BOOK)
                .displayName(Component.text(displayName))
                .addLore(lore.toArray(new Component[0]))
                .build();
    }

    private RoseItem buildProgressItem(MonthlyTask mt, String displayName, int progress, int required, boolean isMonthly) {
        List<Component> lore = new ArrayList<>();
        int totalSegments = 10;
        double ratio = Math.min((double) progress / required, 1.0);
        int filledSegments = (int) (ratio * totalSegments);

        StringBuilder progressBar = new StringBuilder();
        for (int seg = 0; seg < totalSegments; seg++) {
            progressBar.append(seg < filledSegments ? "§a■" : "§7■");
        }
        int percent = (int)(ratio * 100);

        lore.add(Component.text(progressBar.toString()));
        lore.add(Component.text("§7" + progress + "§f/§7" + required + "  (§a" + percent + "%§f)"));
        lore.add(Component.text("§a+ " + mt.getMoneyReward() + " Silber"));

        return new RoseItem.Builder()
                .material(Material.BOOK)
                .displayName(Component.text(displayName))
                .addLore(lore.toArray(new Component[0]))
                .build();
    }
}
