package com.ubivismedia.ubiregions;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pillager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

public class BuildingPlacer {
    private final DatabaseManager databaseManager;
    private final Random random = new Random();

    public BuildingPlacer(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void placeBuilding(int regionId, org.bukkit.World world, String biomeName, int x, int z) {
        File schematicDir = new File("plugins/UbiRegions/resources/buildings/" + biomeName);
        if (!schematicDir.exists() || !schematicDir.isDirectory()) {
            Bukkit.getLogger().warning("No schematics found for biome: " + biomeName);
            return;
        }

        File[] files = schematicDir.listFiles((dir, name) -> name.endsWith(".schem"));
        if (files == null || files.length == 0) {
            Bukkit.getLogger().warning("No schematic files available for biome: " + biomeName);
            return;
        }

        File chosenSchematic = files[random.nextInt(files.length)];
        Location location = findFlatArea(world, x, z);
        if (location == null) {
            Bukkit.getLogger().warning("No valid flat location found in region " + regionId);
            return;
        }

        loadSchematic(chosenSchematic, location);
        saveCastleToDatabase(regionId, location);
        spawnEnemiesAndLoot(location, chosenSchematic);
    }

    private Location findFlatArea(org.bukkit.World world, int x, int z) {
        int searchRadius = 5;
        int baseY = world.getHighestBlockYAt(x, z);
        
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                int y = world.getHighestBlockYAt(x + dx, z + dz);
                if (Math.abs(y - baseY) > 2) return null;
            }
        }
        
        return new Location(world, x, baseY + 1, z);
    }

    private void loadSchematic(File schematic, Location location) {
        try (FileInputStream fis = new FileInputStream(schematic)) {
            ClipboardFormat format = ClipboardFormat.findByFile(schematic);
            if (format == null) {
                Bukkit.getLogger().warning("Invalid schematic format: " + schematic.getName());
                return;
            }
            
            ClipboardReader reader = format.getReader(fis);
            Clipboard clipboard = reader.read();
            World weWorld = BukkitAdapter.adapt(location.getWorld());
            
            com.sk89q.worldedit.EditSession editSession = FaweAPI.getEditSessionBuilder(weWorld).build();
            
            int rotationAngle = random.nextInt(4) * 90;
            Transform transform = new AffineTransform().rotateY(rotationAngle);
            
            Operations.complete(new ClipboardHolder(clipboard).setTransform(transform).createPaste(editSession)
                    .to(BukkitAdapter.asBlockVector(location))
                    .ignoreAirBlocks(true)
                    .build());
            editSession.flushQueue();

            Bukkit.getLogger().info("Loaded schematic " + schematic.getName() + " at " + location + " with rotation: " + rotationAngle + "°");
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void spawnEnemiesAndLoot(Location location, File schematic) {
        for (int i = 0; i < 3; i++) {
            Pillager pillager = (Pillager) location.getWorld().spawnEntity(location, EntityType.PILLAGER);
            pillager.setCustomName("Raider");
        }

        Location chestLocation = findHighestWalkableBlock(location);
        if (chestLocation != null) {
            Block chestBlock = chestLocation.getBlock();
            chestBlock.setType(Material.CHEST);
            
            BlockState state = chestBlock.getState();
            if (state instanceof Chest) {
                Inventory chestInventory = ((Chest) state).getInventory();
                chestInventory.addItem(new ItemStack(Material.DIAMOND, 2));
                chestInventory.addItem(new ItemStack(Material.GOLD_INGOT, 4));
                chestInventory.addItem(new ItemStack(Material.IRON_SWORD, 1));
            }
        }
    }

    private Location findHighestWalkableBlock(Location location) {
        org.bukkit.World world = location.getWorld();
        int startX = location.getBlockX();
        int startZ = location.getBlockZ();
        
        int highestY = world.getHighestBlockYAt(startX, startZ);
        for (int y = highestY; y > 50; y--) {
            Block block = world.getBlockAt(startX, y, startZ);
            if (block.getType().isSolid() && world.getBlockAt(startX, y + 1, startZ).getType() == Material.AIR) {
                return new Location(world, startX, y + 1, startZ);
            }
        }
        return null;
    }
}
