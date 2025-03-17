package com.ubivismedia.ubiregions;

import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkListener implements Listener {

    private final WorldManager worldManager;

    public ChunkListener(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        worldManager.scanChunk(chunk);
    }
}
