package net.sothatsit.blockstore.chunkstore;

import com.google.common.base.Preconditions;
import net.sothatsit.blockstore.BlockStore;
import net.sothatsit.blockstore.BlockStoreConfig;
import net.sothatsit.blockstore.PreloadStrategy;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.*;

public class ChunkManager {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private final World world;
    private final NameStore nameStore;
    private final Map<ChunkLoc, ChunkStore> storeMap = new ConcurrentHashMap<>();

    public ChunkManager(World world) {
        if (world == null)
            throw new IllegalArgumentException("world cannot be null");
        
        this.world = world;
        this.nameStore = new NameStore();
        {
            File namesFile = getNamesFile();

            if (namesFile.exists()) {
                try {
                    FileInputStream fileStream = new FileInputStream(namesFile);
                    ObjectInputStream stream = new ObjectInputStream(fileStream);

                    nameStore.read(stream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Bukkit.getScheduler().runTaskTimer(BlockStore.getInstance(), () -> {
            BlockStoreConfig config = BlockStore.getInstance().getBlockStoreConfig();

            long unloadTime = config.getUnloadTimeMS();
            PreloadStrategy preloadStrategy = config.getPreloadStrategy();

            // Take a snapshot of the values at this point in time to loop over
            new HashSet<>(storeMap.values()).forEach(store -> {
                if(store.getTimeSinceUse() < unloadTime || preloadStrategy.shouldRemainLoaded(store))
                    return;

                Bukkit.getScheduler().runTaskAsynchronously(BlockStore.getInstance(), () -> {
                    unloadStore(store);
                });
            });
        }, 100, 100);
    }

    public File getNamesFile() {
        return new File(getStoreFolder(), "names.dat");
    }

    public void saveNames() {
        File namesFile = getNamesFile();
        
        if (!namesFile.exists()) {
            try {
                namesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        try {
            FileOutputStream fileStream = new FileOutputStream(namesFile);
            ObjectOutputStream stream = new ObjectOutputStream(fileStream);
            
            nameStore.write(stream);
            
            stream.flush();
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public NameStore getNameStore() {
        return nameStore;
    }
    
    public World getWorld() {
        return world;
    }
    
    public Map<ChunkLoc, ChunkStore> getChunkStores() {
        return storeMap;
    }
    
    public File getStoreFolder() {
        File f = new File(world.getWorldFolder(), "block_place_store");
        
        if (!f.exists()) {
            f.mkdir();
        }
        
        return f;
    }
    
    public File getChunkXFolder(int cx) {
        File f = new File(getStoreFolder(), "x" + cx);
        
        if (!f.exists()) {
            f.mkdir();
        }
        
        return f;
    }

    public File getLegacyStoreFile(ChunkLoc chunkLoc) {
        if(!chunkLoc.exists(world))
            return null;

        return new File(getChunkXFolder(chunkLoc.x), "z" + chunkLoc.z + "_y" + chunkLoc.y);
    }
    
    public File getStoreFile(ChunkLoc chunkLoc) {
        if(!chunkLoc.exists(world))
            return null;

        return new File(getChunkXFolder(chunkLoc.x), "z" + chunkLoc.z + "_y" + chunkLoc.y + ".data");
    }

    public ChunkStore getChunkStore(Location location) {
        return getChunkStore(ChunkLoc.fromLocation(location), true);
    }

    public ChunkStore getChunkStore(ChunkLoc chunkLoc, boolean load) {
        if(!chunkLoc.exists(world))
            throw new IllegalArgumentException("chunkLoc " + chunkLoc + " does not exist in this world");

        if(!load)
            return storeMap.get(chunkLoc);

        return storeMap.computeIfAbsent(chunkLoc, key -> {
            LoadingChunkStore chunkStore = loadStore(chunkLoc);

            chunkStore.onLoad(loadedChunkStore -> Bukkit.getScheduler().runTask(BlockStore.getInstance(), () -> {
                storeMap.replace(chunkLoc, chunkStore, loadedChunkStore);
            }));

            return chunkStore;
        });
    }

    public void retrieveChunkStore(Location location, Consumer<ChunkStore> consumer) {
        retrieveChunkStore(ChunkLoc.fromLocation(location), consumer);
    }

    public void retrieveChunkStore(ChunkLoc chunkLoc, Consumer<ChunkStore> consumer) {
        ChunkStore store = getChunkStore(chunkLoc, true);

        if(store instanceof LoadingChunkStore) {
            ((LoadingChunkStore) store).onLoad(consumer);
        } else {
            consumer.accept(store);
        }
    }

    public void moveBlocksAsync(Collection<Block> blocks, BlockFace direction) {
        Set<BlockLoc> blockLocs = blocks.stream()
                .map(BlockLoc::fromBlock)
                .collect(Collectors.toSet());

        executor.execute(() -> {
            moveBlocks(blockLocs, direction);
        });
    }

    public void moveBlocks(Collection<BlockLoc> blocks, BlockFace direction) {
        Map<BlockLoc, BlockMeta> newStates = new HashMap<>();

        for(BlockLoc location : blocks) {
            BlockMeta state = getChunkStore(location.chunkLoc, true).getBlockState(location);

            newStates.put(location.getRelative(direction), state);
        }

        for(BlockLoc location : blocks) {
            if(newStates.containsKey(location))
                continue;

            getChunkStore(location.chunkLoc, true).setPlaced(location, false);
        }

        newStates.forEach((location, state) -> {
            if(state != null) {
                getChunkStore(location.chunkLoc, true).setBlockState(location, state);
            } else {
                getChunkStore(location.chunkLoc, true).setPlaced(location, false);
            }
        });
    }

    public void preloadChunks() {
        for(Chunk chunk : world.getLoadedChunks()) {
            preloadChunk(chunk);
        }
    }

    public void preloadChunk(Chunk chunk) {
        for(int y = 0; y < (world.getMaxHeight() + 63) / 64; ++y) {
            preloadChunkStore(new ChunkLoc(chunk.getX(), y, chunk.getZ()));
        }
    }

    public void preloadStoresAround(ChunkLoc chunkLoc) {
        for(int dx = -1; dx <= 1; ++dx) {
            for(int dy = -1; dy <= 1; ++dy) {
                for(int dz = -1; dz <= 1; ++dz) {
                    ChunkLoc relative = new ChunkLoc(chunkLoc.x + dx, chunkLoc.y + dy, chunkLoc.z + dz);

                    preloadChunkStore(relative);
                }
            }
        }
    }

    public void preloadChunkStore(ChunkLoc chunkLoc) {
        if(!chunkLoc.exists(world))
            return;

        ChunkStore store = getChunkStore(chunkLoc, true);

        store.setLastUse();
    }

    public LoadingChunkStore loadStore(ChunkLoc chunkLoc) {
        Preconditions.checkArgument(chunkLoc.exists(world), "chunkLoc does not exist in this world");

        LoadingChunkStore loadingChunkStore = new LoadingChunkStore(world, chunkLoc);

        executor.execute(() -> {
            loadingChunkStore.setDelegate(loadStoreSync(chunkLoc));
        });

        return loadingChunkStore;
    }

    public LoadedChunkStore loadStoreSync(ChunkLoc chunkLoc) {
        Preconditions.checkArgument(chunkLoc.exists(world), "chunkLoc does not exist in this world");

        File legacyFile = getLegacyStoreFile(chunkLoc);
        File file = getStoreFile(chunkLoc);

        if (!legacyFile.exists() && !file.exists())
            return new LoadedChunkStore(world, chunkLoc);

        try {
            ObjectInputStream stream;
            int version;

            if(file.exists()) {
                FileInputStream fileStream = new FileInputStream(file);
                GZIPInputStream zipStream = new GZIPInputStream(fileStream);
                stream = new ObjectInputStream(zipStream);
                version = stream.readInt();
            } else {
                FileInputStream fileStream = new FileInputStream(legacyFile);
                stream = new ObjectInputStream(fileStream);
                version = 1;
            }

            LoadedChunkStore store = LoadedChunkStore.read(stream, version);

            if (store == null)
                return new LoadedChunkStore(world, chunkLoc);

            stream.close();

            return store;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();

            BlockStore.getInstance().getLogger().severe("Possibly corrupted BlockStore file " + file);

            return new LoadedChunkStore(world, chunkLoc);
        }
    }
    
    public void unloadStore(ChunkStore store) {
        if(store == null)
            return;

        ChunkLoc chunkLoc = store.getChunkLoc();

        storeMap.remove(store.getChunkLoc(), store);

        if(!store.isDirty())
            return;

        boolean empty = store.isEmpty();

        File file = getStoreFile(chunkLoc);
        File legacyFile = getLegacyStoreFile(chunkLoc);
        
        if (!file.exists() && !empty) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (file.exists() && empty) {
            file.delete();
        }
        
        try {
            FileOutputStream fileStream = new FileOutputStream(file);
            GZIPOutputStream zipStream = new GZIPOutputStream(fileStream);
            ObjectOutputStream stream = new ObjectOutputStream(zipStream);

            stream.writeInt(2);
            store.write(stream);
            
            stream.flush();
            stream.close();

            if(legacyFile.exists()) {
                legacyFile.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void saveAll() {
        // Take a snapshot of the values at this point in time to loop over
        new HashSet<>(storeMap.values()).forEach(this::unloadStore);
    }
    
}
