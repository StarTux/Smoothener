package com.winthier.smoothener;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class SmoothenerPlugin extends JavaPlugin implements Listener {
    final Random random = new Random(System.currentTimeMillis());
    final Set<Material> blockTypes = EnumSet.of(
        Material.LOG, Material.LOG_2, Material.DOUBLE_STEP, Material.DOUBLE_STONE_SLAB2);
    final int LOG_SMOOTH_BITS = 12;
    final int DOUBLE_STEP_SMOOTH_BITS = 8;
    final ItemStack smoothenerItem = new ItemStack(Material.GOLD_PICKAXE);
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String args[]) {
        return false;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (!blockTypes.contains(block.getType())) return;
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null) return;
        smoothenerItem.setDurability(itemInHand.getDurability());
        if (!smoothenerItem.isSimilar(itemInHand)) return;
        BlockState replacedState = block.getState();
        if (!smoothenBlock(block)) return;
        BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(block, replacedState, null, itemInHand, player, true);
        getServer().getPluginManager().callEvent(blockPlaceEvent);
        if (blockPlaceEvent.isCancelled()) {
            replacedState.update();
            return;
        }
        reduceToolDurability(player);
    }

    boolean smoothenBlock(Block block)
    {
        int data = (int)block.getData();
        switch (block.getType()) {
        case LOG: case LOG_2:
            if ((data & LOG_SMOOTH_BITS) == LOG_SMOOTH_BITS) return false;
            data |= LOG_SMOOTH_BITS;
            block.setData((byte)data);
            block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);
            break;
        case DOUBLE_STONE_SLAB2:
            if (data != 0) return false;
        case DOUBLE_STEP:
            if (data != 0 && data != 1) return false;
            if ((data & DOUBLE_STEP_SMOOTH_BITS) == DOUBLE_STEP_SMOOTH_BITS) return false;
            data |= DOUBLE_STEP_SMOOTH_BITS;
            block.setData((byte)data);
            block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
            break;
        default: return false;
        }
        return true;
    }

    void reduceToolDurability(Player player)
    {
        ItemStack item = player.getItemInHand();
        if (item.getType() != Material.GOLD_PICKAXE) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        int unbr = item.getEnchantmentLevel(Enchantment.DURABILITY);
        if (unbr > 0) {
            double chance = 1.0 / (double)(unbr + 1);
            if (random.nextDouble() < chance) return;
        }
        short dmg = item.getDurability();
        if (dmg >= item.getType().getMaxDurability()) {
            PlayerItemBreakEvent pibe = new PlayerItemBreakEvent(player, item);
            getServer().getPluginManager().callEvent(pibe);
            player.playSound(player.getEyeLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            item.setAmount(item.getAmount() - 1);
            if (item.getAmount() == 0) {
                player.setItemInHand(null);
            } else {
                item.setDurability((short)0);
            }
        } else {
            item.setDurability((short)(dmg + 1));
        }
    }
}
