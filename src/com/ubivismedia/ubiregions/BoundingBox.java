package com.ubivismedia.ubiregions;

public class BoundingBox {
    final int minX, minZ, maxX, maxZ;
    
    public BoundingBox(int minX, int minZ, int maxX, int maxZ) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }
}
