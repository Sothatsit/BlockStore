package net.sothatsit.blockstore.chunkstore;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkStore {
    
    private World world;
    private ChunkLoc chunkLoc;

    private BitSet store;
    private Map<Integer, BlockMeta> metadata = new ConcurrentHashMap<>();

    private long lastUse;
    private boolean dirty;
    
    public ChunkStore(World world, ChunkLoc loc) {
        this(world, loc, new BitSet(16 * 64 * 16));

        this.dirty = true;
    }
    
    public ChunkStore(World world, ChunkLoc chunkLoc, BitSet store) {
        if (world == null)
            throw new IllegalArgumentException("world cannot be null");
        
        this.world = world;
        this.store = store;
        this.chunkLoc = chunkLoc;
        this.lastUse = System.currentTimeMillis();
        this.dirty = false;
    }
    
    public World getWorld() {
        this.lastUse = System.currentTimeMillis();

        return world;
    }
    
    public ChunkLoc getChunkLoc() {
        this.lastUse = System.currentTimeMillis();

        return chunkLoc;
    }
    
    public BitSet getStore() {
        this.lastUse = System.currentTimeMillis();

        return store;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isChunkLoaded() {
        return world.isChunkLoaded(chunkLoc.x, chunkLoc.z);
    }

    public long getTimeSinceUse() {
        return System.currentTimeMillis() - lastUse;
    }
    
    public boolean getValue(Location location) {
        int relx = location.getBlockX() - chunkLoc.getBlockX();
        int rely = location.getBlockY() - chunkLoc.getBlockY();
        int relz = location.getBlockZ() - chunkLoc.getBlockZ();

        return getValue(relx, rely, relz);
    }
    
    public boolean getValue(int x, int y, int z) {
        this.lastUse = System.currentTimeMillis();

        int blockIndex = ChunkLoc.getChunkBlockIndex(x, y, z);

        return store.get(blockIndex);
    }
    
    public void setValue(Location location, boolean value) {
        int relx = location.getBlockX() - chunkLoc.getBlockX();
        int rely = location.getBlockY() - chunkLoc.getBlockY();
        int relz = location.getBlockZ() - chunkLoc.getBlockZ();

        setValue(relx, rely, relz, value);
    }
    
    public void setValue(int x, int y, int z, boolean value) {
        this.lastUse = System.currentTimeMillis();

        int blockIndex = ChunkLoc.getChunkBlockIndex(x, y, z);
        store.set(blockIndex, value);

        dirty = true;
        
        if (!value) {
            metadata.remove(blockIndex);
        }
    }
    
    public BlockMeta getMeta(Location loc) {
        int blockIndex = ChunkLoc.getChunkBlockIndex(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        return getMeta(blockIndex);
    }
    
    public BlockMeta getMeta(int blockIndex) {
        this.lastUse = System.currentTimeMillis();

        return metadata.computeIfAbsent(blockIndex, BlockMeta::new);
    }
    
    public boolean isEmpty() {
        return store.isEmpty();
    }
    
    public void write(ObjectOutputStream stream) throws IOException {
        this.lastUse = System.currentTimeMillis();

        stream.writeUTF(world.getName());
        stream.writeInt(chunkLoc.x);
        stream.writeInt(chunkLoc.z);
        stream.writeInt(chunkLoc.y);
        stream.writeObject(store);
        
        Set<Integer> plugins = new HashSet<>();
        BlockMeta[] blocks = metadata.values().toArray(new BlockMeta[0]);

        for (BlockMeta meta : blocks) {
            plugins.addAll(meta.getKeys());
        }
        
        stream.writeInt(plugins.size());
        
        for (Integer plugin : plugins) {
            stream.writeInt(plugin);
            
            int blockCount = 0;
            
            for (BlockMeta meta : blocks) {
                if (meta.containsPlugin(plugin)) {
                    blockCount++;
                }
            }
            
            stream.writeInt(blockCount);
            
            for (BlockMeta meta : blocks) {
                if (meta.containsPlugin(plugin)) {
                    stream.writeInt(meta.getBlockIndex());
                    meta.write(stream, plugin);
                }
            }
        }
    }

    public static ChunkStore read(ObjectInputStream stream, int version) throws IOException, ClassNotFoundException {
        switch (version) {
            case 1:
                return readVersion1(stream);
            case 2:
                return readVersion2(stream);
            default:
                throw new IllegalArgumentException("Unknown file version " + version);
        }
    }

    public static ChunkStore readVersion2(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        String worldName = stream.readUTF();
        World world = Bukkit.getWorld(worldName);

        int cx = stream.readInt();
        int cz = stream.readInt();
        int cy = stream.readInt();
        ChunkLoc chunkLoc = new ChunkLoc(cx, cy, cz);

        BitSet values = (BitSet) stream.readObject();

        ChunkStore store = new ChunkStore(world, chunkLoc, values);

        int plugins = stream.readInt();

        for (int i = 0; i < plugins; i++) {
            int plugin = stream.readInt();
            int blocks = stream.readInt();

            for (int w = 0; w < blocks; w++) {
                int blockIndex = stream.readInt();
                store.getMeta(blockIndex).read(stream, plugin);
            }
        }

        return store;
    }

    public static ChunkStore readVersion1(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        String worldName = stream.readUTF();
        World world = Bukkit.getWorld(worldName);
        
        int cx = stream.readInt();
        int cz = stream.readInt();
        int cy = stream.readInt();
        ChunkLoc chunkLoc = new ChunkLoc(cx, cy, cz);

        BitSet values = convertToBitSet((boolean[][][]) stream.readObject());
        
        ChunkStore store = new ChunkStore(world, chunkLoc, values);
        
        int plugins = stream.readInt();
        
        for (int i = 0; i < plugins; i++) {
            int plugin = stream.readInt();
            int blocks = stream.readInt();
            
            for (int w = 0; w < blocks; w++) {
                byte[] loc = unpackInt(stream.readInt());
                int blockIndex = ChunkLoc.getChunkBlockIndex(loc[0], loc[1], loc[2]);

                store.getMeta(blockIndex).read(stream, plugin);
            }
        }
        
        return store;
    }

    private static BitSet convertToBitSet(boolean[][][] values) {
        BitSet bitSet = new BitSet(16 * 64 * 16);

        for(int x = 0; x < 16; x++) {
            for(int y = 0; y < 64; y++) {
                for(int z = 0; z < 16; z++) {
                    bitSet.set(ChunkLoc.getChunkBlockIndex(x, y, z), values[x][y][z]);
                }
            }
        }

        return bitSet;
    }

    private static byte[] unpackInt(int num) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(num).array();
    }
    
}
