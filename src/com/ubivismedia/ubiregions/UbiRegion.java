package com.ubivismedia.ubiregions;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UbiRegions extends JavaPlugin {
    private static UbiRegions instance;
    private ExecutorService executorService;
    private DatabaseManager databaseManager;
    private BuildingPlacer buildingPlacer;
    private DungeonPlacer dungeonPlacer;

    @Override
    public void onEnable() {
        instance = this;

        // Create a thread pool with a limited number of threads
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // Initialize database and game components
        databaseManager = new DatabaseManager();
        dungeonPlacer = new DungeonPlacer(databaseManager, this);
        buildingPlacer = new BuildingPlacer(databaseManager, dungeonPlacer, executorService);

        // Register events (if needed)
        Bukkit.getPluginManager().registerEvents(new ChunkListener(buildingPlacer), this);

        getLogger().info("UbiRegions has been enabled!");
    }

    @Override
    public void onDisable() {
        // Shutdown the thread pool to prevent memory leaks
        if (executorService != null) {
            executorService.shutdown();
        }
        getLogger().info("UbiRegions has been disabled!");
    }

    public static UbiRegions getInstance() {
        return instance;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
