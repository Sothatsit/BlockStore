package net.sothatsit.blockstore.chunkstore;

import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public final class BlockLoc {

    public final ChunkLoc chunkLoc;
    public final int relx;
    public final int rely;
    public final int relz;
    public final int blockIndex;

    public BlockLoc(ChunkLoc chunkLoc, int relx, int rely, int relz) {
        Preconditions.checkNotNull(chunkLoc, "chunkLoc cannot be null");
        Preconditions.checkArgument(relx >= 0 && relx < 16, "relx out of bounds 0-15 inclusive");
        Preconditions.checkArgument(rely >= 0 && rely < 64, "rely out of bounds 0-63 inclusive");
        Preconditions.checkArgument(relz >= 0 && relz < 16, "relz out of bounds 0-15 inclusive");

        this.chunkLoc = chunkLoc;
        this.relx = relx;
        this.rely = rely;
        this.relz = relz;
        this.blockIndex = calcBlockIndex(relx, rely, relz);
    }

    public int getBlockX() {
        return chunkLoc.getBlockX() + relx;
    }

    public int getBlockY() {
        return chunkLoc.getBlockY() + rely;
    }

    public int getBlockZ() {
        return chunkLoc.getBlockZ() + relz;
    }

    public BlockLoc getRelative(BlockFace direction) {
        Preconditions.checkNotNull(direction, "direction cannot be null");

        int x = getBlockX() + direction.getModX();
        int y = getBlockY() + direction.getModY();
        int z = getBlockZ() + direction.getModZ();

        return BlockLoc.fromLocation(x, y, z);
    }

    @Override
    public int hashCode() {
        return chunkLoc.hashCode() ^ Integer.hashCode(relx) ^ Integer.hashCode(rely) ^ Integer.hashCode(relz);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof BlockLoc))
            return false;

        BlockLoc other = (BlockLoc) obj;

        return other.chunkLoc.equals(chunkLoc) && other.relx == relx && other.rely == rely && other.relz == relz;
    }

    @Override
    public String toString() {
        return "{chunkLoc: " + chunkLoc
                + ", relx: " + relx
                + ", rely: " + rely
                + ", relz: " + relz
                + ", blockIndex: " + blockIndex + "}";
    }

    public static int calcBlockIndex(int relx, int rely, int relz) {
        Preconditions.checkArgument(relx >= 0 && relx < 16, "relx out of bounds 0-15 inclusive");
        Preconditions.checkArgument(rely >= 0 && rely < 64, "rely out of bounds 0-63 inclusive");
        Preconditions.checkArgument(relz >= 0 && relz < 16, "relz out of bounds 0-15 inclusive");

        return relx + (16 * relz) + (16 * 16 * rely);
    }

    public static BlockLoc fromBlock(Block block) {
        return fromLocation(block.getX(), block.getY(), block.getZ());
    }

    public static BlockLoc fromLocation(Location location) {
        return fromLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static BlockLoc fromLocation(int x, int y, int z) {
        ChunkLoc chunkLoc = ChunkLoc.fromLocation(x, y, z);

        int relx = x - chunkLoc.getBlockX();
        int rely = y - chunkLoc.getBlockY();
        int relz = z - chunkLoc.getBlockZ();

        return new BlockLoc(chunkLoc, relx, rely, relz);
    }


    public static BlockLoc fromBlockIndex(ChunkLoc chunkLoc, int blockIndex) {
        int relx = blockIndex % 16;
        int relz = (blockIndex % (16 * 16)) / 16;
        int rely = blockIndex / (16 * 16);

        return new BlockLoc(chunkLoc, relx, rely, relz);
    }

}
