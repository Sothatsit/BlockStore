package net.sothatsit.blockstore;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import net.sothatsit.blockstore.chunkstore.*;
import net.sothatsit.blockstore.util.Checks;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

public class BlockStoreApi {

    private static final Set<Class<?>> classWhitelist = new HashSet<>();

    static {
        classWhitelist.add(String.class);

        classWhitelist.add(Boolean.class);
        classWhitelist.add(Byte.class);
        classWhitelist.add(Short.class);
        classWhitelist.add(Integer.class);
        classWhitelist.add(Long.class);
        classWhitelist.add(Float.class);
        classWhitelist.add(Double.class);

        classWhitelist.add(boolean.class);
        classWhitelist.add(byte.class);
        classWhitelist.add(short.class);
        classWhitelist.add(int.class);
        classWhitelist.add(long.class);
        classWhitelist.add(float.class);
        classWhitelist.add(double.class);
    }

    private static ChunkManager getChunkManager(Location location) {
        return BlockStore.getInstance().getManager(location);
    }

    private static ChunkStore getChunkStore(Location location) {
        return getChunkManager(location).getChunkStore(location);
    }

    private static NameStore getNameStore(Location location) {
        return getChunkManager(location).getNameStore();
    }

    private static void retrieveChunkStore(Location location, Consumer<ChunkStore> consumer) {
        getChunkManager(location).retrieveChunkStore(location, store -> {
            Bukkit.getScheduler().runTask(BlockStore.getInstance(), () -> {
                consumer.accept(store);
            });
        });
    }

    public static void preloadChunk(Block block) {
        preloadChunk(block.getChunk());
    }

    public static void preloadChunk(Location location) {
        preloadChunk(location.getChunk());
    }

    public static void preloadChunk(Chunk chunk) {
        ChunkManager chunkManager = BlockStore.getInstance().getManager(chunk.getWorld());

        chunkManager.preloadChunk(chunk);
    }

    public static boolean isPlaced(Block block) {
        return isPlaced(block.getLocation());
    }
    
    public static boolean isPlaced(Location location) {
        return getChunkStore(location).isPlaced(location);
    }

    public static void retrieveIsPlaced(Block block, Consumer<Boolean> consumer) {
        retrieveIsPlaced(block.getLocation(), consumer);
    }

    public static void retrieveIsPlaced(Location location, Consumer<Boolean> consumer) {
        retrieveChunkStore(location, chunkStore -> {
            consumer.accept(isPlaced(location));
        });
    }

    public static void setPlaced(Block block, boolean placed) {
        setPlaced(block.getLocation(), placed);
    }
    
    public static void setPlaced(Location location, boolean placed) {
        getChunkStore(location).setPlaced(location, placed);
    }

    public static Object getBlockMeta(Block block, Plugin plugin, String key) {
        return getBlockMeta(block.getLocation(), plugin, key);
    }
    
    public static Object getBlockMeta(Location location, Plugin plugin, String key) {
        NameStore names = getNameStore(location);

        int pluginId = names.toId(plugin.getName(), false);
        int keyId = names.toId(key, false);

        return getChunkStore(location).getMetaValue(location, pluginId, keyId);
    }

    public static void retrieveBlockMeta(Block block, Plugin plugin, String key, Consumer<Object> consumer) {
        retrieveBlockMeta(block.getLocation(), plugin, key, consumer);
    }

    public static void retrieveBlockMeta(Location location, Plugin plugin, String key, Consumer<Object> consumer) {
        retrieveChunkStore(location, store -> {
            consumer.accept(getBlockMeta(location, plugin, key));
        });
    }

    public static Map<String, Object> getAllBlockMeta(Block block, Plugin plugin) {
        return getAllBlockMeta(block.getLocation(), plugin);
    }

    public static Map<String, Object> getAllBlockMeta(Location location, Plugin plugin) {
        NameStore names = getNameStore(location);

        int pluginId = names.toId(plugin.getName(), false);

        Map<Integer, Object> metaValues = getChunkStore(location).getMetaValues(location, pluginId);

        return names.keysFromId(metaValues);
    }

    public static void retrieveAllBlockMeta(Block block, Plugin plugin, Consumer<Map<String, Object>> consumer) {
        retrieveAllBlockMeta(block.getLocation(), plugin, consumer);
    }

    public static void retrieveAllBlockMeta(Location location, Plugin plugin, Consumer<Map<String, Object>> consumer) {
        retrieveChunkStore(location, store -> {
            consumer.accept(getAllBlockMeta(location, plugin));
        });
    }

    public static Map<String, Map<String, Object>> getAllBlockMeta(Block block) {
        return getAllBlockMeta(block.getLocation());
    }

    public static Map<String, Map<String, Object>> getAllBlockMeta(Location location) {
        NameStore names = getNameStore(location);

        Map<Integer, Map<Integer, Object>> metaValues = getChunkStore(location).getMetaValues(location);

        return names.deepKeysFromId(metaValues);
    }

    public static void retrieveAllBlockMeta(Block block, Consumer<Map<String, Map<String, Object>>> consumer) {
        retrieveAllBlockMeta(block.getLocation(), consumer);
    }

    public static void retrieveAllBlockMeta(Location location, Consumer<Map<String, Map<String, Object>>> consumer) {
        retrieveChunkStore(location, store -> {
            consumer.accept(getAllBlockMeta(location));
        });
    }

    public static boolean containsBlockMeta(Block block, Plugin plugin, String key) {
        return containsBlockMeta(block.getLocation(), plugin, key);
    }
    
    public static boolean containsBlockMeta(Location location, Plugin plugin, String key) {
        return getBlockMeta(location, plugin, key) != null;
    }

    public static void retrieveContainsBlockMeta(Block block, Plugin plugin, String key, Consumer<Boolean> consumer) {
        retrieveContainsBlockMeta(block.getLocation(), plugin, key, consumer);
    }

    public static void retrieveContainsBlockMeta(Location location, Plugin plugin, String key, Consumer<Boolean> consumer) {
        retrieveBlockMeta(location, plugin, key, blockMeta -> {
            consumer.accept(containsBlockMeta(location, plugin, key));
        });
    }

    public static void setBlockMeta(Block block, Plugin plugin, String key, Object value) {
        setBlockMeta(block.getLocation(), plugin, key, value);
    }
    
    public static void setBlockMeta(Location location, Plugin plugin, String key, Object value) {
        Checks.ensureNonNull(value, "value");

        Class<?> baseType = value.getClass();
        while (baseType.isArray()) {
            baseType = baseType.getComponentType();
        }

        Checks.ensureTrue(classWhitelist.contains(baseType),
                "value must be a value or array of type String, boolean, byte, short, int, long, float or double");

        NameStore names = getNameStore(location);

        int pluginId = names.toId(plugin.getName(), true);
        int keyId = names.toId(key, true);

        getChunkStore(location).setMetaValue(location, pluginId, keyId, value);
    }

    public static void removeBlockMeta(Block block, Plugin plugin, String key) {
        removeBlockMeta(block.getLocation(), plugin, key);
    }
    
    public static void removeBlockMeta(Location location, Plugin plugin, String key) {
        NameStore names = getNameStore(location);

        int pluginId = names.toId(plugin.getName(), false);
        int keyId = names.toId(key, false);

        getChunkStore(location).removeMetaValue(location, pluginId, keyId);
    }
    
}
