package net.sothatsit.blockstore.chunkstore;

import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ChunkStore {

    private final World world;
    private final ChunkLoc chunkLoc;

    private AtomicLong lastUse;

    public ChunkStore(World world, ChunkLoc chunkLoc) {
        Preconditions.checkNotNull(world, "world cannot be null");
        Preconditions.checkNotNull(chunkLoc, "chunkLoc cannot be null");

        this.world = world;
        this.chunkLoc = chunkLoc;
        this.lastUse = new AtomicLong(System.currentTimeMillis());
    }

    public World getWorld() {
        return world;
    }

    public ChunkLoc getChunkLoc() {
        return chunkLoc;
    }

    protected boolean isInChunk(BlockLoc location) {
        return location.chunkLoc.equals(chunkLoc);
    }

    public boolean isChunkLoaded() {
        return world.isChunkLoaded(chunkLoc.x, chunkLoc.z);
    }

    protected void setLastUse() {
        this.lastUse.set(System.currentTimeMillis());
    }

    public long getTimeSinceUse() {
        return System.currentTimeMillis() - lastUse.get();
    }

    public final boolean isPlaced(Location location) {
        return isPlaced(BlockLoc.fromLocation(location));
    }

    public final void setPlaced(Location location, boolean value) {
        setPlaced(BlockLoc.fromLocation(location), value);
    }

    public final Object getMetaValue(Location location, int plugin, int key) {
        return getMetaValue(BlockLoc.fromLocation(location), plugin, key);
    }

    public final Map<Integer, Object> getMetaValues(Location location, int plugin) {
        return getMetaValues(BlockLoc.fromLocation(location), plugin);
    }

    public final Map<Integer, Map<Integer, Object>> getMetaValues(Location location) {
        return getMetaValues(BlockLoc.fromLocation(location));
    }

    public final void setMetaValue(Location location, int plugin, int key, Object value) {
        setMetaValue(BlockLoc.fromLocation(location), plugin, key, value);
    }

    public final void removeMetaValue(Location location, int plugin, int key) {
        removeMetaValue(BlockLoc.fromLocation(location), plugin, key);
    }

    public abstract boolean isDirty();

    public abstract boolean isPlaced(BlockLoc location);

    public abstract void setPlaced(BlockLoc location, boolean value);

    public abstract Object getMetaValue(BlockLoc location, int plugin, int key);

    public abstract Map<Integer, Object> getMetaValues(BlockLoc location, int plugin);

    public abstract Map<Integer, Map<Integer, Object>> getMetaValues(BlockLoc location);

    public abstract void setMetaValue(BlockLoc location, int plugin, int key, Object value);

    public abstract void removeMetaValue(BlockLoc location, int plugin, int key);

    protected abstract BlockMeta getBlockState(BlockLoc location);

    protected abstract void setBlockState(BlockLoc location, BlockMeta meta);

    public abstract boolean isEmpty();

    public abstract void write(ObjectOutputStream stream) throws IOException;

}
