package com.ubivismedia.ubiregions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
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

        int dungeonDepth = determineDungeonSize(biomeName);
        generateEntrance(entrance);
        generateDungeon(entrance.clone().add(0, -5, 0), dungeonDepth, biomeName);
    }

    private int determineDungeonSize(String biomeName) {
        switch (biomeName.toUpperCase()) {
            case "MOUNTAINS":
            case "HILLS":
                return 15 + random.nextInt(10);
            case "DESERT":
            case "PLAINS":
                return 8 + random.nextInt(5);
            case "JUNGLE":
            case "TAIGA":
                return 12 + random.nextInt(7);
            default:
                return 10;
        }
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
        
        placeLootChest(startLocation.clone().add(0, -depth, 0));
        spawnEnemies(startLocation, depth);
    }

    private void placeLootChest(Location location) {
        location.getBlock().setType(Material.CHEST);
        Inventory chestInventory = ((org.bukkit.block.Chest) location.getBlock().getState()).getInventory();
        chestInventory.addItem(new ItemStack(Material.DIAMOND, 1));
        chestInventory.addItem(new ItemStack(Material.GOLD_INGOT, 5));
        chestInventory.addItem(new ItemStack(Material.IRON_SWORD, 1));
        Bukkit.getLogger().info("Placed loot chest at: " + location);
    }

    private void spawnEnemies(Location location, int depth, String biomeName) {
        int numEnemies;
        EntityType[] enemies;
        
        switch (biomeName.toUpperCase()) {
            case "MOUNTAINS":
            case "HILLS":
                numEnemies = random.nextInt(5) + 6;
                enemies = depth > 10 ? new EntityType[]{EntityType.VINDICATOR, EntityType.EVOKER} : new EntityType[]{EntityType.PILLAGER, EntityType.ZOMBIE};
                break;
            case "JUNGLE":
            case "TAIGA":
                numEnemies = random.nextInt(4) + 5;
                enemies = depth > 8 ? new EntityType[]{EntityType.PILLAGER, EntityType.SKELETON} : new EntityType[]{EntityType.ZOMBIE, EntityType.SPIDER};
                break;
            case "DESERT":
            case "PLAINS":
                numEnemies = random.nextInt(3) + 3;
                enemies = depth > 6 ? new EntityType[]{EntityType.HUSK, EntityType.SKELETON} : new EntityType[]{EntityType.ZOMBIE, EntityType.SPIDER};
                break;
            default:
                numEnemies = random.nextInt(3) + 2;
                enemies = new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON};
        }
        
        for (int i = 0; i < numEnemies; i++) {
            EntityType enemyType = enemies[random.nextInt(enemies.length)];
            location.getWorld().spawnEntity(location, enemyType).setCustomName("Dungeon Guard");
        }
        Bukkit.getLogger().info("Spawned " + numEnemies + " enemies at: " + location);
    }

        private void placeTraps(Location location) {
        World world = location.getWorld();
        if (random.nextBoolean()) {
            world.getBlockAt(location).setType(Material.TRIPWIRE_HOOK);
            world.getBlockAt(location.clone().add(1, 0, 0)).setType(Material.STRING);
            world.getBlockAt(location.clone().add(-1, 0, 0)).setType(Material.STRING);
            world.getBlockAt(location.clone().add(0, 0, 1)).setType(Material.STRING);
            world.getBlockAt(location.clone().add(0, 0, -1)).setType(Material.STRING);
            Bukkit.getLogger().info("Placed a tripwire trap at: " + location);
        }
        
        if (random.nextBoolean()) {
            world.getBlockAt(location).setType(Material.DISPENSER);
            org.bukkit.block.Dispenser dispenser = (org.bukkit.block.Dispenser) world.getBlockAt(location).getState();
            dispenser.getInventory().addItem(new ItemStack(Material.ARROW, 10));
            Bukkit.getLogger().info("Placed an arrow trap at: " + location);
        }
    }

    private void placeLootChest(Location location, boolean rareLoot) {
        location.getBlock().setType(Material.CHEST);
        Inventory chestInventory = ((org.bukkit.block.Chest) location.getBlock().getState()).getInventory();
        
        if (rareLoot) {
            chestInventory.addItem(new ItemStack(Material.DIAMOND, 2));
            chestInventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
            chestInventory.addItem(new ItemStack(Material.ENCHANTED_BOOK, 1));
        } else {
            chestInventory.addItem(new ItemStack(Material.IRON_INGOT, 5));
            chestInventory.addItem(new ItemStack(Material.BREAD, 3));
            chestInventory.addItem(new ItemStack(Material.ARROW, 10));
        }
        Bukkit.getLogger().info("Placed " + (rareLoot ? "RARE" : "COMMON") + " loot chest at: " + location);
    }    

    private void placeHiddenRoom(Location location) {
        World world = location.getWorld();
        if (random.nextInt(10) > 7) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        world.getBlockAt(location.clone().add(dx, dy, dz)).setType(Material.STONE);
                    }
                }
            }
            world.getBlockAt(location.clone().add(0, 0, -3)).setType(Material.PISTON);
            placeLootChest(location.clone().add(1, 0, 0), true);
            Bukkit.getLogger().info("Hidden room created at: " + location);
        }
    }
    
    private void generateDungeon(Location startLocation, int depth, String biomeName) {
        if (depth <= 0) return;
        World world = startLocation.getWorld();
        
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    world.getBlockAt(startLocation.clone().add(dx, dy, dz)).setType(Material.STONE);
                }
            }
        }
        
        placeTraps(startLocation.clone().add(0, -1, 0));
        placeHiddenRoom(startLocation.clone().add(4,0,0));
        
        if (random.nextBoolean()) {
            generateDungeon(startLocation.clone().add(0, -5, 0), depth - 1, biomeName);
        }
        if (random.nextBoolean()) {
            generateDungeon(startLocation.clone().add(5, 0, 0), depth - 1, biomeName);
        }
        if (random.nextBoolean()) {
            generateDungeon(startLocation.clone().add(-5, 0, 0), depth - 1, biomeName);
        }
        if (random.nextBoolean()) {
            generateDungeon(startLocation.clone().add(0, 0, 5), depth - 1, biomeName);
        }
        if (random.nextBoolean()) {
            generateDungeon(startLocation.clone().add(0, 0, -5), depth - 1, biomeName);
        }
    }
}
