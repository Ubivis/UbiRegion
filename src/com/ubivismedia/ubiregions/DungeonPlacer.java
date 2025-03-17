package com.ubivismedia.ubiregions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.Random;

public class DungeonPlacer {
    private final Random random = new Random();

    public void placeDungeon(World world, String biomeName) {
        Location entrance = findSuitableLocation(world);
        if (entrance == null) {
            Bukkit.getLogger().warning("No suitable dungeon location found in biome: " + biomeName);
            return;
        }

        generateEntrance(entrance);
        generateDungeon(entrance.clone().add(0, -5, 0), 10);
    }

    private Location findSuitableLocation(World world) {
        for (int attempts = 0; attempts < 50; attempts++) {
            int x = random.nextInt(5000) - 2500;
            int z = random.nextInt(5000) - 2500;
            int y = world.getHighestBlockYAt(x, z);
            Block block = world.getBlockAt(x, y, z);
            
            if (block.getType().isSolid() && (isNearMountain(world, x, z) || isFlatArea(world, x, z))) {
                return new Location(world, x, y, z);
            }
        }
        return null;
    }

    private boolean isNearMountain(World world, int x, int z) {
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                int y = world.getHighestBlockYAt(x + dx, z + dz);
                if (y > world.getHighestBlockYAt(x, z) + 5) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isFlatArea(World world, int x, int z) {
        int baseY = world.getHighestBlockYAt(x, z);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                int y = world.getHighestBlockYAt(x + dx, z + dz);
                if (Math.abs(y - baseY) > 2) {
                    return false;
                }
            }
        }
        return true;
    }

    private void generateEntrance(Location location) {
        World world = location.getWorld();
        for (int y = 0; y < 3; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -1; z <= 1; z++) {
                    world.getBlockAt(location.clone().add(x, y, z)).setType(Material.AIR);
                }
            }
        }
        Bukkit.getLogger().info("Dungeon entrance created at: " + location);
    }

    private void generateDungeon(Location startLocation, int depth) {
        if (depth <= 0) return;
        World world = startLocation.getWorld();
        
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    world.getBlockAt(startLocation.clone().add(dx, dy, dz)).setType(Material.STONE);
                }
            }
        }
        
        if (random.nextBoolean()) {
            generateDungeon(startLocation.clone().add(0, -5, 0), depth - 1);
        }
        if (random.nextBoolean()) {
            generateDungeon(startLocation.clone().add(5, 0, 0), depth - 1);
        }
        if (random.nextBoolean()) {
            generateDungeon(startLocation.clone().add(-5, 0, 0), depth - 1);
        }
        if (random.nextBoolean()) {
            generateDungeon(startLocation.clone().add(0, 0, 5), depth - 1);
        }
        if (random.nextBoolean()) {
            generateDungeon(startLocation.clone().add(0, 0, -5), depth - 1);
        }
        if (random.nextBoolean()) {
            generateDungeon(startLocation.clone().add(0, 5, 0), depth - 1);
        }
        
        placeLootChest(startLocation.clone().add(0, -depth, 0));
        spawnEnemies(startLocation);
    }

    private void placeLootChest(Location location) {
        location.getBlock().setType(Material.CHEST);
        Inventory chestInventory = ((org.bukkit.block.Chest) location.getBlock().getState()).getInventory();
        chestInventory.addItem(new ItemStack(Material.DIAMOND, 1));
        chestInventory.addItem(new ItemStack(Material.GOLD_INGOT, 5));
        chestInventory.addItem(new ItemStack(Material.IRON_SWORD, 1));
        Bukkit.getLogger().info("Placed loot chest at: " + location);
    }

    private void spawnEnemies(Location location) {
        int numEnemies = random.nextInt(3) + 2;
        for (int i = 0; i < numEnemies; i++) {
            Zombie zombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
            zombie.setCustomName("Dungeon Guard");
        }
        Bukkit.getLogger().info("Spawned " + numEnemies + " enemies at: " + location);
    }
}
