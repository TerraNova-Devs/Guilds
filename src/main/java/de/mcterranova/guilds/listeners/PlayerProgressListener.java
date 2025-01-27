package de.mcterranova.guilds.listeners;

import de.mcterranova.guilds.Guilds;
import de.mcterranova.guilds.model.TaskEventType;
import de.mcterranova.guilds.service.GuildManager;
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

    public PlayerProgressListener(Guilds plugin, GuildManager guildManager, TaskManager taskManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.taskManager = taskManager;
    }

    // ------------------------------------------------------------------------
    // 1) BLOCK BREAK & HARVEST
    // ------------------------------------------------------------------------
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material mat = block.getType();

        // If it's a crop, only count if fully grown
        if (isCrop(mat) && !isFullyGrown(block)) {
            return;
        }

        // Single unified call, handle both daily & monthly tasks
        taskManager.handleEvent(TaskEventType.BLOCK_BREAK, player, mat.name(), 1);

        // If you want to treat HARVEST differently, you can do:
        if (isCrop(mat)) {
            taskManager.handleEvent(TaskEventType.HARVEST, player, mat.name(), 1);
        }
    }

    private boolean isCrop(Material mat) {
        return switch (mat) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART -> true;
            default -> false;
        };
    }

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
        int amountCrafted = event.getRecipe().getResult().getAmount();
        if (event.isShiftClick()) {
            amountCrafted = computeShiftClickAmount(event);
        }
        taskManager.handleEvent(TaskEventType.CRAFT_ITEM, player, mat.name(), amountCrafted);
    }

    private int computeShiftClickAmount(CraftItemEvent event) {
        int perCraft = event.getRecipe().getResult().getAmount();
        ItemStack[] matrix = event.getInventory().getMatrix().clone();
        int maxCrafts = Integer.MAX_VALUE;

        for (ItemStack slot : matrix) {
            if (slot == null || slot.getType().isAir()) continue;
            int slotCount = slot.getAmount();
            // You might need something more advanced if your recipes vary
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
    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        Material mat = event.getItemType();
        int amount = event.getItemAmount();

        taskManager.handleEvent(TaskEventType.SMELT, player, mat.name(), amount);
    }

    // ------------------------------------------------------------------------
    // 4) FISHING
    // ------------------------------------------------------------------------
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        switch (event.getState()) {
            case CAUGHT_FISH, CAUGHT_ENTITY -> {
                Player player = event.getPlayer();
                Entity caught = event.getCaught();
                if (caught instanceof Item itemEntity) {
                    ItemStack caughtStack = itemEntity.getItemStack();
                    Material caughtMat = caughtStack.getType();
                    int amount = caughtStack.getAmount();
                    taskManager.handleEvent(TaskEventType.FISH, player, caughtMat.name(), amount);
                }
            }
            default -> { }
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
    }
}
