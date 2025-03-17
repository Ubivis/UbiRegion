package com.ubivismedia.ubiregions;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Biome;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class WorldManager {
    private final DatabaseManager databaseManager;
    private final Map<ChunkPos, Integer> regionMap = new HashMap<>();
    private int regionCounter = 0;

    public WorldManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void scanChunk(Chunk chunk) {
        ChunkPos pos = new ChunkPos(chunk.getX(), chunk.getZ());
        Biome biome = chunk.getBlock(0, 0, 0).getBiome();

        int existingRegionId = findAdjacentRegion(pos, biome);
        if (existingRegionId != -1) {
            updateRegion(existingRegionId, pos);
        } else {
            int regionId = ++regionCounter;
            BoundingBox boundingBox = floodFill(pos, biome, regionId);
            saveRegion(regionId, biome, boundingBox);
        }
    }

    private int findAdjacentRegion(ChunkPos pos, Biome biome) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT region_id FROM region_chunks rc JOIN regions r ON rc.region_id = r.id " +
                             "WHERE (chunk_x = ? AND chunk_z = ?) AND r.biome = ?")) {
            stmt.setInt(1, pos.x);
            stmt.setInt(2, pos.z);
            stmt.setString(3, biome.name());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("region_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void updateRegion(int regionId, ChunkPos pos) {
        saveChunk(regionId, pos);
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
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO regions (id, biome, min_x, min_z, max_x, max_z) VALUES (?, ?, ?, ?, ?, ?)");) {
            stmt.setInt(1, regionId);
            stmt.setString(2, biome.name());
            stmt.setInt(3, boundingBox.minX);
            stmt.setInt(4, boundingBox.minZ);
            stmt.setInt(5, boundingBox.maxX);
            stmt.setInt(6, boundingBox.maxZ);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveChunk(int regionId, ChunkPos pos) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO region_chunks (region_id, chunk_x, chunk_z) VALUES (?, ?, ?)");) {
            stmt.setInt(1, regionId);
            stmt.setInt(2, pos.x);
            stmt.setInt(3, pos.z);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
