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
import org.bukkit.block.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pillager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class BuildingPlacer {
    private final DatabaseManager databaseManager;
    private final Random random = new Random();
    private final DungeonPlacer dungeonPlacer;
    private final ExecutorService executorService;
    private static final Map<String, List<ItemStack>> biomeLoot = new HashMap<>();

    static {
        biomeLoot.put("DESERT", Arrays.asList(new ItemStack(Material.GOLD_INGOT, 3), new ItemStack(Material.SANDSTONE, 10), new ItemStack(Material.EMERALD, 1), new ItemStack(Material.TNT, 1)));
        biomeLoot.put("PLAINS", Arrays.asList(new ItemStack(Material.WHEAT, 10), new ItemStack(Material.IRON_HOE, 1), new ItemStack(Material.APPLE, 5), new ItemStack(Material.HAY_BLOCK, 2)));
        biomeLoot.put("TAIGA", Arrays.asList(new ItemStack(Material.SPRUCE_LOG, 10), new ItemStack(Material.LEATHER_BOOTS, 1), new ItemStack(Material.BONE, 5), new ItemStack(Material.ARROW, 10)));
        biomeLoot.put("JUNGLE", Arrays.asList(new ItemStack(Material.COCOA_BEANS, 5), new ItemStack(Material.IRON_SWORD, 1), new ItemStack(Material.PARROT_SPAWN_EGG, 1), new ItemStack(Material.BAMBOO, 10)));
        biomeLoot.put("SAVANNA", Arrays.asList(new ItemStack(Material.ACACIA_LOG, 10), new ItemStack(Material.GOLDEN_APPLE, 1), new ItemStack(Material.LEATHER, 5), new ItemStack(Material.BOW, 1)));
        biomeLoot.put("MOUNTAINS", Arrays.asList(new ItemStack(Material.IRON_PICKAXE, 1), new ItemStack(Material.COBBLESTONE, 20), new ItemStack(Material.LAPIS_LAZULI, 3), new ItemStack(Material.EMERALD, 2)));
    }
    
    public BuildingPlacer(DatabaseManager databaseManager, DungeonPlacer dungeonPlacer, ExecutorService executorService) {
        this.databaseManager = databaseManager;
        this.dungeonPlacer = dungeonPlacer;
        this.executorService = executorService;
    }

    public void placeBuilding(int regionId, org.bukkit.World world, String biomeName, int x, int z) {
        executorService.execute(() -> {
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
            Location location = new Location(world, x, world.getHighestBlockYAt(x, z) + 1, z);
            
            loadSchematic(chosenSchematic, location);
            saveCastleToDatabase(regionId, location);

            executorService.execute(() -> {
                spawnEnemiesAndLoot(location, biomeName);
                placeSecretRoom(location);
                dungeonPlacer.placeDungeon(world, biomeName, location);
            });
        });
    }

    private Location findAdjustedFlatArea(org.bukkit.World world, int x, int z) {
        int searchRadius = 5;
        int baseY = world.getHighestBlockYAt(x, z);
        
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                int y = world.getHighestBlockYAt(x + dx, z + dz);
                if (Math.abs(y - baseY) > 3) {
                    baseY = Math.min(baseY, y);
                }
            }
        }
        
        return new Location(world, x, baseY + 1, z);
    }
    
    private void placeSecretRoom(Location location) {
        if (random.nextBoolean()) {
            Location secretRoomLoc = location.clone().add(random.nextInt(5) - 2, -1, random.nextInt(5) - 2);
            Block secretBlock = secretRoomLoc.getBlock();
            secretBlock.setType(Material.STONE_BRICKS);
            Bukkit.getLogger().info("Secret room created at: " + secretRoomLoc);

            Location chestLoc = secretRoomLoc.clone().add(1, 0, 0);
            chestLoc.getBlock().setType(Material.CHEST);
            BlockState state = chestLoc.getBlock().getState();
            if (state instanceof Chest) {
                Inventory chestInventory = ((Chest) state).getInventory();
                chestInventory.addItem(new ItemStack(Material.DIAMOND, 1));
                chestInventory.addItem(new ItemStack(Material.REDSTONE_TORCH, 1));
            }

            Location redstoneDoorLoc = secretRoomLoc.clone().add(0, 0, -1);
            redstoneDoorLoc.getBlock().setType(Material.PISTON);
            Bukkit.getLogger().info("Hidden redstone door placed at: " + redstoneDoorLoc);
        }
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

    private void spawnEnemiesAndLoot(Location location, File schematic, String biomeName) {
        int numPillagers = random.nextInt(3) + 2;
        for (int i = 0; i < numPillagers; i++) {
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
                List<ItemStack> loot = biomeLoot.getOrDefault(biomeName.toUpperCase(), Arrays.asList(new ItemStack(Material.IRON_INGOT, 3), new ItemStack(Material.BREAD, 5)));
                
                Collections.shuffle(loot);
                for (int i = 0; i < Math.min(3, loot.size()); i++) {
                    chestInventory.addItem(loot.get(i));
                }
            }
        }
    }

        private Location findAdjustedFlatArea(org.bukkit.World world, int x, int z) {
        int searchRadius = 5;
        int baseY = world.getHighestBlockYAt(x, z);
        
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                int y = world.getHighestBlockYAt(x + dx, z + dz);
                if (Math.abs(y - baseY) > 3) {
                    baseY = Math.min(baseY, y);
                }
            }
        }
        
        return new Location(world, x, baseY + 1, z);
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
