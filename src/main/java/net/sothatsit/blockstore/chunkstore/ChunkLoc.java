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

    @Override
    public int hashCode() {
        return Integer.hashCode(x) ^ Integer.hashCode(y) ^ Integer.hashCode(z);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof ChunkLoc))
            return false;

        ChunkLoc other = (ChunkLoc) obj;

        return other.x == x && other.y == y && other.z == z;
    }

    @Override
    public String toString() {
        return "{x: " + x + ", y: " + y + ", z: " + z + "}";
    }

    public static ChunkLoc fromLocation(Location location) {
        return fromLocation(location.getX(), location.getY(), location.getZ());
    }

    public static ChunkLoc fromLocation(double x, double y, double z) {
        int cx = (int) Math.floor(x / 16d);
        int cy = (int) Math.floor(y / 64d);
        int cz = (int) Math.floor(z / 16d);

        return new ChunkLoc(cx, cy, cz);
    }

    private static int positiveRemainder(int num, int divisor) {
        int remainder = num % divisor;
        return (remainder < 0 ? remainder + divisor : remainder);
    }

    public static int[] getChunkRelCoords(int x, int y, int z) {
        return new int[] {
                positiveRemainder(x, 16),
                positiveRemainder(y, 64),
                positiveRemainder(z, 16)
        };
    }

    public static int getChunkBlockIndex(Location location) {
        return getChunkBlockIndex(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static int getChunkBlockIndex(int x, int y, int z) {
        x = positiveRemainder(x, 16);
        y = positiveRemainder(y, 64);
        z = positiveRemainder(z, 16);

        return x + (16 * z) + (16 * 16 * y);
    }

    public static int[] getLocFromChunkBlockIndex(int blockIndex) {
        int x = blockIndex % 16;
        int z = (blockIndex % (16 * 16)) / 16;
        int y = blockIndex / (16 * 16);

        return new int[] {x, y, z};
    }

    public static Location getLocFromChunkBlockIndex(World world, ChunkLoc chunkLoc, int blockIndex) {
        int[] loc = getLocFromChunkBlockIndex(blockIndex);

        int x = chunkLoc.getBlockX() + loc[0];
        int y = chunkLoc.getBlockY() + loc[1];
        int z = chunkLoc.getBlockZ() + loc[2];

        return new Location(world, x, y, z);
    }

}
