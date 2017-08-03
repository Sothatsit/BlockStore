package net.sothatsit.blockstore.chunkstore;

import net.sothatsit.blockstore.BlockStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.*;

public class ChunkManager {

    public static final long CHUNK_UNLOAD_TIMER = 60000;
    
    private final World world;
    private final Map<ChunkLoc, ChunkStore> storeMap;
    private final NameStore nameStore;
    
    public ChunkManager(World world) {
        if (world == null)
            throw new IllegalArgumentException("world cannot be null");
        
        this.world = world;
        this.storeMap = new ConcurrentHashMap<>();

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
            storeMap.values().stream()
                    .filter(store -> !store.isChunkLoaded())
                    .filter(store -> store.getTimeSinceUse() >= CHUNK_UNLOAD_TIMER)
                    .forEach(store -> Bukkit.getScheduler().runTaskAsynchronously(BlockStore.getInstance(), () -> {
                        unloadStore(store);
                    }));
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
        return new File(getChunkXFolder(chunkLoc.x), "z" + chunkLoc.z + "_y" + chunkLoc.y);
    }
    
    public File getStoreFile(ChunkLoc chunkLoc) {
        return new File(getChunkXFolder(chunkLoc.x), "z" + chunkLoc.z + "_y" + chunkLoc.y + ".data");
    }

    public boolean isChunkStoreLoaded(ChunkLoc chunkLoc) {
        return storeMap.containsKey(chunkLoc);
    }

    public ChunkStore getChunkStoreByChunk(ChunkLoc chunkLoc, boolean load) {
        return storeMap.computeIfAbsent(chunkLoc, (key) -> {
            if(!load)
                return null;

            ChunkStore store = loadStore(chunkLoc);

            if (store != null)
                return store;

            return new ChunkStore(world, chunkLoc);
        });
    }
    
    public ChunkStore getChunkStore(Location location) {
        return getChunkStore(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
    
    public ChunkStore getChunkStore(int x, int y, int z) {
        return getChunkStoreByChunk(ChunkLoc.fromLocation(x, y, z), true);
    }
    
    public ChunkStore loadStore(ChunkLoc chunkLoc) {
        ChunkStore loadedStore = getChunkStoreByChunk(chunkLoc, false);

        if(loadedStore != null)
            return loadedStore;

        File legacyFile = getLegacyStoreFile(chunkLoc);
        File file = getStoreFile(chunkLoc);
        
        if (!legacyFile.exists() && !file.exists())
            return null;
        
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
            
            ChunkStore store = ChunkStore.read(stream, version);
            
            if (store == null)
                return null;

            Bukkit.getScheduler().runTask(BlockStore.getInstance(), () -> storeMap.put(chunkLoc, store));

            stream.close();
            
            return store;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();

            BlockStore.getInstance().getLogger().severe("Possibly corrupted BlockStore file " + file);

            return null;
        }
    }
    
    public void unloadStore(ChunkStore store) {
        if (store == null)
            return;

        ChunkLoc chunkLoc = store.getChunkLoc();
        
        storeMap.remove(chunkLoc);
        
        if (!store.isDirty())
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
        storeMap.values().forEach(this::unloadStore);
    }
    
}
