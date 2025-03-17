package com.ubivismedia.ubiregions;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UbiRegions extends JavaPlugin implements Listener {

    private static UbiRegions instance;
    private Connection connection;
    private ExecutorService executor;
    private final Map<ChunkPos, Integer> regionMap = new HashMap<>();
    private int regionCounter = 0;

    @Override
    public void onEnable() {
        instance = this;
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        setupDatabase();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("UbiRegions has been enabled!");
    }

    @Override
    public void onDisable() {
        shutdownDatabase();
        executor.shutdown();
        getLogger().info("UbiRegions has been disabled!");
    }

    private void setupDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:plugins/UbiRegions/regions.db");
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS regions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "biome TEXT,"
                    + "min_x INTEGER,"
                    + "min_z INTEGER,"
                    + "max_x INTEGER,"
                    + "max_z INTEGER);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS region_chunks ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "region_id INTEGER NOT NULL,"
                    + "chunk_x INTEGER NOT NULL,"
                    + "chunk_z INTEGER NOT NULL,"
                    + "FOREIGN KEY (region_id) REFERENCES regions(id) ON DELETE CASCADE);");
            stmt.close();
            getLogger().info("Database setup complete.");
        } catch (SQLException e) {
            getLogger().severe("Could not set up database: " + e.getMessage());
        }
    }

    private void shutdownDatabase() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            getLogger().severe("Could not close database connection: " + e.getMessage());
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        executor.execute(() -> scanChunk(event.getChunk()));
    }

    private void scanChunk(Chunk chunk) {
        ChunkPos pos = new ChunkPos(chunk.getX(), chunk.getZ());
        Biome biome = chunk.getBlock(0, 0, 0).getBiome();
        
        if (!regionMap.containsKey(pos)) {
            int regionId = ++regionCounter;
            BoundingBox boundingBox = floodFill(pos, biome, regionId);
            saveRegion(regionId, biome, boundingBox);
        }
    }

    private BoundingBox floodFill(ChunkPos start, Biome biome, int regionId) {
        Queue<ChunkPos> queue = new LinkedList<>();
        queue.add(start);
        int minX = start.x, minZ = start.z, maxX = start.x, maxZ = start.z;

        while (!queue.isEmpty()) {
            ChunkPos pos = queue.poll();
            if (regionMap.containsKey(pos)) continue;

            Chunk chunk = Bukkit.getWorlds().get(0).getChunkAt(pos.x, pos.z);
            if (chunk.getBlock(0, 0, 0).getBiome() != biome) continue;

            regionMap.put(pos, regionId);
            saveChunk(regionId, pos);

            minX = Math.min(minX, pos.x);
            minZ = Math.min(minZ, pos.z);
            maxX = Math.max(maxX, pos.x);
            maxZ = Math.max(maxZ, pos.z);

            queue.add(new ChunkPos(pos.x + 1, pos.z));
            queue.add(new ChunkPos(pos.x - 1, pos.z));
            queue.add(new ChunkPos(pos.x, pos.z + 1));
            queue.add(new ChunkPos(pos.x, pos.z - 1));
        }
        return new BoundingBox(minX, minZ, maxX, maxZ);
    }

    private void saveRegion(int regionId, Biome biome, BoundingBox boundingBox) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO regions (id, biome, min_x, min_z, max_x, max_z) VALUES (?, ?, ?, ?, ?, ?)");) {
            stmt.setInt(1, regionId);
            stmt.setString(2, biome.name());
            stmt.setInt(3, boundingBox.minX);
            stmt.setInt(4, boundingBox.minZ);
            stmt.setInt(5, boundingBox.maxX);
            stmt.setInt(6, boundingBox.maxZ);
            stmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("Could not save region: " + e.getMessage());
        }
    }

    private void saveChunk(int regionId, ChunkPos pos) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO region_chunks (region_id, chunk_x, chunk_z) VALUES (?, ?, ?)");) {
            stmt.setInt(1, regionId);
            stmt.setInt(2, pos.x);
            stmt.setInt(3, pos.z);
            stmt.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("Could not save chunk: " + e.getMessage());
        }
    }

    public static UbiRegions getInstance() {
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}

class ChunkPos {
    final int x, z;
    public ChunkPos(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ChunkPos)) return false;
        ChunkPos other = (ChunkPos) obj;
        return this.x == other.x && this.z == other.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}

class BoundingBox {
    final int minX, minZ, maxX, maxZ;
    public BoundingBox(int minX, int minZ, int maxX, int maxZ) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }
}
