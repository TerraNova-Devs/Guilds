package de.mcterranova.guilds.listeners;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.model.MonthlyTask;
import de.mcterranova.guilds.model.TaskEventType;
import de.mcterranova.guilds.service.GuildManager;
import de.mcterranova.guilds.service.MonthlyTaskManager;
import de.mcterranova.guilds.service.TaskManager;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerProgressListener implements Listener {

    private final Guilds plugin;
    private final GuildManager guildManager;
    private final TaskManager taskManager;
    private final MonthlyTaskManager monthlyTaskManager;

    public PlayerProgressListener(Guilds plugin, GuildManager guildManager, TaskManager taskManager, MonthlyTaskManager monthlyTaskManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.taskManager = taskManager;
        this.monthlyTaskManager = monthlyTaskManager;
    }

    // ------------------------------------------------------------------------
    // 1) BLOCK BREAK & HARVEST
    // ------------------------------------------------------------------------
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material mat = block.getType();

        if (isCrop(mat)) {
            if (!isFullyGrown(block)) {
                return;
            }
            taskManager.handleEvent(TaskEventType.HARVEST, player, mat.name(), 1);
            monthlyTaskManager.handleMonthlyEvent(guildManager.getGuildByPlayer(player.getUniqueId()), TaskEventType.HARVEST, mat.name(), 1);
        }

        taskManager.handleEvent(TaskEventType.BLOCK_BREAK, player, mat.name(), 1);
        monthlyTaskManager.handleMonthlyEvent(guildManager.getGuildByPlayer(player.getUniqueId()), TaskEventType.BLOCK_BREAK, mat.name(), 1);

    }

    // Helper: which materials are farmland crops
    private boolean isCrop(Material mat) {
        switch (mat) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case NETHER_WART:
                return true;
            default:
                return false;
        }
    }

    // Helper: check if an ageable block is at max age
    private boolean isFullyGrown(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            return ageable.getAge() == ageable.getMaximumAge();
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // 2) CRAFTING (including SHIFT-click)
    // ------------------------------------------------------------------------
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Material mat = event.getRecipe().getResult().getType();

        // Non-shift craft => usually 1 craft operation
        int amountCrafted = event.getRecipe().getResult().getAmount();

        // If SHIFT-click, compute how many items are actually crafted
        if (event.isShiftClick()) {
            amountCrafted = computeShiftClickAmount(event);
        }

        taskManager.handleEvent(TaskEventType.CRAFT_ITEM, player, mat.name(), amountCrafted);
        monthlyTaskManager.handleMonthlyEvent(guildManager.getGuildByPlayer(player.getUniqueId()), TaskEventType.CRAFT_ITEM, mat.name(), amountCrafted);
    }

    /**
     * Attempts to compute how many items are crafted when shift-clicking.
     * For simple 1:1 recipes, this is usually good enough.
     * For more complex shaped recipes (multiple items per slot),
     * we might parse the ShapedRecipe or ShapelessRecipe more precisely.
     */
    private int computeShiftClickAmount(CraftItemEvent event) {
        int perCraft = event.getRecipe().getResult().getAmount();
        ItemStack[] matrix = event.getInventory().getMatrix().clone();

        int maxCrafts = Integer.MAX_VALUE;

        for (ItemStack slot : matrix) {
            if (slot == null || slot.getType().isAir()) continue;
            int slotCount = slot.getAmount();
            int usesPerCraft = 1;
            int craftsFromSlot = slotCount / usesPerCraft;
            if (craftsFromSlot < maxCrafts) {
                maxCrafts = craftsFromSlot;
            }
        }

        int totalItems = maxCrafts * perCraft;
        return (totalItems > 0) ? totalItems : perCraft;
    }

    // ------------------------------------------------------------------------
    // 3) FURNACE EXTRACTION => SMELT
    // ------------------------------------------------------------------------
    /**
     * This event fires when a player takes items out of a furnace's output slot,
     * thus crediting the player who actually collects the smelted items.
     */
    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        Material mat = event.getItemType();
        int amount = event.getItemAmount();

        taskManager.handleEvent(TaskEventType.SMELT, player, mat.name(), amount);
        monthlyTaskManager.handleMonthlyEvent(guildManager.getGuildByPlayer(player.getUniqueId()), TaskEventType.SMELT, mat.name(), amount);
    }

    // ------------------------------------------------------------------------
    // 4) FISHING
    // ------------------------------------------------------------------------
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        switch (event.getState()) {
            case CAUGHT_FISH:
            case CAUGHT_ENTITY: {
                Player player = event.getPlayer();

                Entity caught = event.getCaught();
                if (caught == null) {
                    return;
                }

                if (caught instanceof Item itemEntity) {
                    ItemStack caughtStack = itemEntity.getItemStack();
                    Material caughtMat = caughtStack.getType();

                    // Differentiate between cod, salmon, tropical fish, pufferfish, or even random junk
                    // This way your config could specify event=FISH, material=SALMON, etc.
                    // The amount is caughtStack.getAmount(), typically 1
                    int amount = caughtStack.getAmount();

                    taskManager.handleEvent(TaskEventType.FISH, player, caughtMat.name(), amount);
                    monthlyTaskManager.handleMonthlyEvent(guildManager.getGuildByPlayer(player.getUniqueId()), TaskEventType.FISH, caughtMat.name(), amount);
                }
                break;
            }
            default:
                // ignore other states like FISHING, BITE, etc.
                break;
        }
    }

    // ------------------------------------------------------------------------
    // 5) ENTITY DEATH => KILL
    // ------------------------------------------------------------------------
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player player = event.getEntity().getKiller();
        EntityType type = event.getEntityType();

        taskManager.handleEvent(TaskEventType.ENTITY_KILL, player, type.name(), 1);
        monthlyTaskManager.handleMonthlyEvent(guildManager.getGuildByPlayer(player.getUniqueId()), TaskEventType.ENTITY_KILL, type.name(), 1);
    }
}
