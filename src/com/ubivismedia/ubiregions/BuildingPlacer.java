package com.ubivismedia.ubiregions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pillager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class BuildingPlacer {
    private final DatabaseManager databaseManager;
    private final Random random = new Random();

    public BuildingPlacer(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void placeBuilding(int regionId, World world, String biomeName, int x, int z) {
        File schematicDir = new File("plugins/UbiRegions/resources/buildings/" + biomeName);
        if (!schematicDir.exists() || !schematicDir.isDirectory()) {
            Bukkit.getLogger().warning("No schematics found for biome: " + biomeName);
            return;
        }

        File[] files = schematicDir.listFiles((dir, name) -> name.endsWith(".schematic"));
        if (files == null || files.length == 0) {
            Bukkit.getLogger().warning("No schematic files available for biome: " + biomeName);
            return;
        }

        File chosenSchematic = files[random.nextInt(files.length)];
        Location location = findSuitableLocation(world, x, z);
        if (location == null) {
            Bukkit.getLogger().warning("No valid location found in region " + regionId);
            return;
        }

        loadSchematic(chosenSchematic, location);
        saveCastleToDatabase(regionId, location);
        spawnEnemiesAndLoot(location);
    }

    private Location findSuitableLocation(World world, int x, int z) {
        for (int y = world.getMaxHeight(); y > 50; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isSolid()) {
                return new Location(world, x, y + 1, z);
            }
        }
        return null;
    }

    private void loadSchematic(File schematic, Location location) {
        // Placeholder for schematic loading logic
        Bukkit.getLogger().info("Loaded schematic " + schematic.getName() + " at " + location);
    }

    private void saveCastleToDatabase(int regionId, Location location) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO castles (region_id, x, y, z) VALUES (?, ?, ?, ?)");) {
            stmt.setInt(1, regionId);
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockY());
            stmt.setInt(4, location.getBlockZ());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void spawnEnemiesAndLoot(Location location) {
        for (int i = 0; i < 3; i++) {
            Pillager pillager = (Pillager) location.getWorld().spawnEntity(location, EntityType.PILLAGER);
            pillager.setCustomName("Raider");
        }

        Location chestLocation = location.clone().add(2, 0, 2);
        Block chestBlock = chestLocation.getBlock();
        chestBlock.setType(Material.CHEST);
        
        Inventory chestInventory = ((org.bukkit.block.Chest) chestBlock.getState()).getInventory();
        chestInventory.addItem(new ItemStack(Material.DIAMOND, 2));
        chestInventory.addItem(new ItemStack(Material.GOLD_INGOT, 4));
        chestInventory.addItem(new ItemStack(Material.IRON_SWORD, 1));
    }
}
