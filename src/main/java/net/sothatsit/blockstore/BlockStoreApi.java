package net.sothatsit.blockstore;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import net.sothatsit.blockstore.chunkstore.BlockMeta;
import net.sothatsit.blockstore.chunkstore.ChunkManager;
import net.sothatsit.blockstore.chunkstore.NameStore;

public class BlockStoreApi {
    
    public static boolean isPlaced(Block block) {
        return isPlaced(block.getLocation());
    }
    
    public static boolean isPlaced(Location loc) {
        return BlockStore.isPlaced(loc);
    }
    
    public static void setPlaced(Block block, boolean placed) {
        setPlaced(block.getLocation(), placed);
    }
    
    public static void setPlaced(Location loc, boolean placed) {
        BlockStore.setPlaced(loc, placed);
    }
    
    private static final Set<Class<?>> classWhitelist = new HashSet<>();
    
    static {
        classWhitelist.add(String.class);
        
        classWhitelist.add(Byte.class);
        classWhitelist.add(Short.class);
        classWhitelist.add(Integer.class);
        classWhitelist.add(Long.class);
        classWhitelist.add(Float.class);
        classWhitelist.add(Double.class);
        
        classWhitelist.add(byte.class);
        classWhitelist.add(short.class);
        classWhitelist.add(int.class);
        classWhitelist.add(long.class);
        classWhitelist.add(float.class);
        classWhitelist.add(double.class);
    }
    
    private static BlockMeta getBlockMeta(ChunkManager manager, Location loc) {
        return manager.getChunkStore(loc).getMeta(loc);
    }
    
    public static Object getBlockMeta(Block block, Plugin plugin, String key) {
        return getBlockMeta(block.getLocation(), plugin, key);
    }
    
    public static Object getBlockMeta(Location loc, Plugin plugin, String key) {
        ChunkManager manager = BlockStore.getChunkManager(loc);
        
        NameStore names = manager.getNameStore();
        BlockMeta meta = getBlockMeta(manager, loc);
        
        return meta.get(names.toInt(plugin.getName(), false), names.toInt(key, false));
    }
    
    public static boolean containsBlockMeta(Block block, Plugin plugin, String key) {
        return containsBlockMeta(block.getLocation(), plugin, key);
    }
    
    public static boolean containsBlockMeta(Location loc, Plugin plugin, String key) {
        return getBlockMeta(loc, plugin, key) != null;
    }
    
    public static void setBlockMeta(Block block, Plugin plugin, String key, Object value) {
        setBlockMeta(block.getLocation(), plugin, key, value);
    }
    
    public static void setBlockMeta(Location loc, Plugin plugin, String key, Object value) {
        if (value == null)
            throw new IllegalArgumentException("'value' cannot be null");

        Class<?> baseType = value.getClass();
        
        while (baseType.isArray()) {
            baseType = baseType.getComponentType();
        }
        
        if (!classWhitelist.contains(baseType))
            throw new IllegalArgumentException("'value' must be a value or array of type String, byte, short, int, long, float or double");
        
        ChunkManager manager = BlockStore.getChunkManager(loc);
        
        NameStore names = manager.getNameStore();
        BlockMeta meta = getBlockMeta(manager, loc);
        
        meta.set(names.toInt(plugin.getName(), true), names.toInt(key, true), value);
    }
    
    public static void removeBlockMeta(Block block, Plugin plugin, String key) {
        removeBlockMeta(block.getLocation(), plugin, key);
    }
    
    public static void removeBlockMeta(Location loc, Plugin plugin, String key) {
        ChunkManager manager = BlockStore.getChunkManager(loc);
        
        NameStore names = manager.getNameStore();
        BlockMeta meta = getBlockMeta(manager, loc);
        
        meta.remove(names.toInt(plugin.getName(), false), names.toInt(key, false));
    }
    
}
