// Copyright (c) 2023 Joseph Hale
// 
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package dev.thehale.power_farming;

import java.util.Map;
import static java.util.Map.entry;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class PowerFarmingListener implements Listener {
    /**
     * Map of crop types to their seed types.
     * 
     * Add more crops here if you want them to be auto-replanted.
     */
    private Map<Material, Material> crops = Map.ofEntries(
            entry(Material.WHEAT, Material.WHEAT_SEEDS),
            entry(Material.CARROTS, Material.CARROT),
            entry(Material.POTATOES, Material.POTATO),
            entry(Material.BEETROOTS, Material.BEETROOT_SEEDS),
            entry(Material.NETHER_WART, Material.NETHER_WART),
            entry(Material.COCOA, Material.COCOA_BEANS),
            entry(Material.MELON_STEM, Material.MELON_SEEDS),
            entry(Material.ATTACHED_MELON_STEM, Material.MELON_SEEDS),
            entry(Material.PUMPKIN_STEM, Material.PUMPKIN_SEEDS),
            entry(Material.ATTACHED_PUMPKIN_STEM, Material.PUMPKIN_SEEDS),
            entry(Material.SWEET_BERRY_BUSH, Material.SWEET_BERRIES));

    private Map<Location, Long> lastBreakTime = new HashMap<>();
    private final long MINIMUM_BREAK_INTERVAL = 500;

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event == null || event.isCancelled() || event.getPlayer() == null)
            return;
        autoReplant(event);
    }

    private void autoReplant(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block brokenBlock = event.getBlock();
        Location brokenLocation = brokenBlock.getLocation();
        Material seed = crops.get(brokenBlock.getType());

        boolean isCrop = seed != null;
        long timestamp = Instant.now().toEpochMilli();

        if (isCrop) {
            event.setCancelled(true); // Stop event propagation

            boolean isBrokenRecently = lastBreakTime.containsKey(brokenLocation)
                    && lastBreakTime.get(brokenLocation) > timestamp - MINIMUM_BREAK_INTERVAL;

            if (!isBrokenRecently) {
                dropItems(brokenBlock.getDrops(new ItemStack(Material.IRON_HOE), player), brokenLocation);
                
                boolean playerHasSeed = player.getInventory().containsAtLeast(new ItemStack(seed), 1);
                if (!player.isSneaking() && playerHasSeed) {
                    removeFromInventory(player, new ItemStack(seed, 1));
                    replant(brokenBlock);
                } else {
                    brokenBlock.breakNaturally(new ItemStack(Material.IRON_HOE));
                }
            }
            lastBreakTime.put(brokenLocation, timestamp);
        }
    }

    private void dropItems(Collection<ItemStack> items, Location location) {
        for (ItemStack item : items) {
            if (item.getType() != Material.AIR) {
                location.getWorld().dropItemNaturally(location, item);
            }
        }
    }

    private void replant(Block brokenBlock) {
        Ageable newAge = (Ageable) brokenBlock.getBlockData();
        newAge.setAge(0);
        brokenBlock.setBlockData(newAge);
    }

    private void removeFromInventory(Player player, ItemStack item) {
        int stackLocation = player.getInventory().first(item.getType());
        ItemStack stack = player.getInventory().getItem(stackLocation);
        stack.setAmount(stack.getAmount() - item.getAmount());
        if (stack.getAmount() <= 0) {
            // TODO - handle the case where we need to remove from multiple stacks
            player.getInventory().setItem(stackLocation, null);
        } else {
            player.getInventory().setItem(stackLocation, stack);
        }
    }
}