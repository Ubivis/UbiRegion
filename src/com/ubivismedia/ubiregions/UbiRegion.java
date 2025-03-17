package com.ubivismedia.ubiregions;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UbiRegions extends JavaPlugin {

    private static UbiRegions instance;
    private ExecutorService executor;
    private DatabaseManager databaseManager;
    private WorldManager worldManager;

    @Override
    public void onEnable() {
        instance = this;
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        databaseManager = new DatabaseManager();
        worldManager = new WorldManager(databaseManager);
        
        Bukkit.getPluginManager().registerEvents(new ChunkListener(worldManager), this);
        getLogger().info("UbiRegions has been enabled!");
    }

    @Override
    public void onDisable() {
        databaseManager.shutdown();
        executor.shutdown();
        getLogger().info("UbiRegions has been disabled!");
    }

    public static UbiRegions getInstance() {
        return instance;
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}
