package net.sothatsit.blockstore.chunkstore;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import net.sothatsit.blockstore.util.NumberUtils;

public class ChunkStore {
    
    private World world;
    private int chunkx;
    private int chunkz;
    private int chunky;
    private boolean[][][] store;
    private Map<Integer, BlockMeta> metadata = new HashMap<>();
    private long lastUse;
    private boolean dirty;
    
    public ChunkStore(World world, int cx, int cz, int cy) {
        this(world, cx, cz, cy, new boolean[16][64][16]);
        this.dirty = true;
    }
    
    public ChunkStore(World world, int cx, int cz, int cy, boolean[][][] store) {
        if (world == null) {
            throw new IllegalArgumentException("world cannot be null");
        }
        
        this.world = world;
        this.store = store;
        this.chunkx = cx;
        this.chunkz = cz;
        this.chunky = cy;
        this.lastUse = System.currentTimeMillis();
        this.dirty = false;
    }
    
    public World getWorld() {
        this.lastUse = System.currentTimeMillis();
        return world;
    }
    
    public int[] getChunkCoords() {
        this.lastUse = System.currentTimeMillis();
        return new int[] { chunkx, chunkz, chunky };
    }
    
    public boolean isChunkLoaded() {
        return world.isChunkLoaded(chunkz, chunkz);
    }
    
    public boolean[][][] getStore() {
        this.lastUse = System.currentTimeMillis();
        return store;
    }
    
    public String getID() {
        this.lastUse = System.currentTimeMillis();
        return chunkx + "_" + chunkz + "_" + chunky;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
    
    public long getLastUse() {
        return lastUse;
    }
    
    public void setTimeToUnload(long time) {
        lastUse = System.currentTimeMillis() - ChunkManager.CHUNK_UNLOAD_TIMER + time;
    }
    
    public boolean isTrue(Location loc) {
        return isTrue(loc.getBlockX() - (chunkx * 16), loc.getBlockY() - (chunky * 64), loc.getBlockZ() - (chunkz * 16));
    }
    
    public boolean isTrue(int x, int y, int z) {
        this.lastUse = System.currentTimeMillis();
        x = x % 16;
        y = y % 64;
        z = z % 16;
        return store[x][y][z];
    }
    
    public void setTrue(Location loc, boolean value) {
        setTrue(loc.getBlockX() - (chunkx * 16), loc.getBlockY() - (chunky * 64), loc.getBlockZ() - (chunkz * 16), value);
    }
    
    public void setTrue(int x, int y, int z, boolean value) {
        this.lastUse = System.currentTimeMillis();
        x = x % 16;
        y = y % 64;
        z = z % 16;
        
        store[x][y][z] = value;
        dirty = true;
        
        if (!value) {
            metadata.remove(NumberUtils.packLoc(x, y, z));
        }
    }
    
    public BlockMeta getMeta(Location loc) {
        return getMeta(NumberUtils.packLoc(loc.getBlockX() - (chunkx * 16), loc.getBlockY() - (chunky * 64), loc.getBlockZ() - (chunkz * 16)));
    }
    
    public BlockMeta getMeta(int loc) {
        BlockMeta meta = metadata.get(loc);
        
        if (meta == null) {
            meta = new BlockMeta(loc);
            metadata.put(loc, meta);
        }
        
        return meta;
    }
    
    public boolean isEmpty() {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 64; y++) {
                for (int z = 0; z < 16; z++) {
                    if (store[x][y][z]) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    public void write(ObjectOutputStream stream) throws IOException {
        this.lastUse = System.currentTimeMillis();
        stream.writeUTF(world.getName());
        stream.writeInt(chunkx);
        stream.writeInt(chunkz);
        stream.writeInt(chunky);
        stream.writeObject(store);
        
        Set<Integer> plugins = new HashSet<>();
        Collection<BlockMeta> blocks = metadata.values();
        
        for (BlockMeta meta : blocks) {
            plugins.addAll(meta.getKeys());
        }
        
        stream.writeInt(plugins.size());
        
        for (Integer plugin : plugins) {
            stream.writeInt(plugin);
            
            int amount = 0;
            
            for (BlockMeta meta : blocks) {
                if (meta.containsPlugin(plugin)) {
                    amount++;
                }
            }
            
            stream.writeInt(amount);
            
            for (BlockMeta meta : blocks) {
                if (meta.containsPlugin(plugin)) {
                    meta.write(stream, plugin);
                }
            }
        }
    }
    
    public static ChunkStore read(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        String worldName = stream.readUTF();
        World world = Bukkit.getWorld(worldName);
        
        int cx = stream.readInt();
        int cz = stream.readInt();
        int cy = stream.readInt();
        
        boolean[][][] values = (boolean[][][]) stream.readObject();
        
        ChunkStore store = new ChunkStore(world, cx, cz, cy, values);
        
        int plugins = stream.readInt();
        
        for (int i = 0; i < plugins; i++) {
            int plugin = stream.readInt();
            int blocks = stream.readInt();
            
            for (int w = 0; w < blocks; w++) {
                int loc = stream.readInt();
                store.getMeta(loc).read(stream, plugin);
            }
        }
        
        return store;
    }
    
}
