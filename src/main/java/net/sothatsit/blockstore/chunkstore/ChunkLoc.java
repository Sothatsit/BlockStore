package net.sothatsit.blockstore.chunkstore;

import org.bukkit.Location;
import org.bukkit.World;

public final class ChunkLoc {

    public final int x;
    public final int y;
    public final int z;

    public ChunkLoc(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getBlockX() {
        return x * 16;
    }

    public int getBlockY() {
        return y * 64;
    }

    public int getBlockZ() {
        return z * 16;
    }

    public boolean exists(World world) {
        return y >= 0 && y * 64 < world.getMaxHeight();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(x) ^ Integer.hashCode(y) ^ Integer.hashCode(z);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ChunkLoc))
            return false;

        ChunkLoc other = (ChunkLoc) obj;

        return other.x == x && other.y == y && other.z == z;
    }

    @Override
    public String toString() {
        return "{x: " + x + ", y: " + y + ", z: " + z + "}";
    }

    public static ChunkLoc fromLocation(Location location) {
        return fromLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static ChunkLoc fromLocation(int x, int y, int z) {
        return new ChunkLoc(x / 16, y / 64, z / 16);
    }

}
