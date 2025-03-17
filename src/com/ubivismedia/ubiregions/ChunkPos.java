package com.ubivismedia.ubiregions;

import java.util.Objects;

public class ChunkPos {
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
