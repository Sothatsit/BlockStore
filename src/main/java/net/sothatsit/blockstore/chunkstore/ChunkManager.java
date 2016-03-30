package net.sothatsit.blockstore.chunkstore;

import net.sothatsit.blockstore.BlockStore;
import net.sothatsit.blockstore.util.NameStore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;

public class ChunkManager {

    public static final long CHUNK_UNLOAD_TIMER = 60000;
    
    private World world;
    private Map<String, ChunkStore> storeMap;
    private NameStore nameStore;
    
    public ChunkManager(World world) {
        if (world == null) {
            throw new IllegalArgumentException("world cannot be null");
        }
        
        this.world = world;
        this.storeMap = new HashMap<>();
        
        loadNames();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                final Collection<ChunkStore> collection = storeMap.values();
                final List<ChunkStore> list = new ArrayList<>();
                
                for (ChunkStore store : collection) {
                    if (store.isChunkLoaded()) {
                        list.add(store);
                    }
                }
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (ChunkStore store : list) {
                            if (System.currentTimeMillis() - store.getLastUse() >= CHUNK_UNLOAD_TIMER) {
                                unloadStore(store);
                            }
                        }
                    }
                }.runTaskAsynchronously(BlockStore.getInstance());
            }
        }.runTaskTimer(BlockStore.getInstance(), 100, 100);
    }
    
    public void loadNames() {
        this.nameStore = new NameStore();
        
        File namesFile = new File(getStoreFolder(), "names.dat");
        
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
    
    public void saveNames() {
        File namesFile = new File(getStoreFolder(), "names.dat");
        
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
    
    public Map<String, ChunkStore> getChunkStores() {
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
    
    public File getStoreFile(int cx, int cz, int cy) {
        return new File(getChunkXFolder(cx), "z" + cz + "_y" + cy);
    }
    
    public ChunkStore getChunkStoreByChunk(int cx, int cz, int cy, boolean load) {
        String id = cx + "_" + cz + "_" + cy;
        
        ChunkStore store = storeMap.get(id);
        
        if (store != null || !load) {
            return store;
        }
        
        store = loadStore(cx, cz, cy);
        
        if (store != null) {
            return store;
        }
        
        store = new ChunkStore(world, cx, cz, cy);
        
        storeMap.put(id, store);
        
        return store;
    }
    
    public ChunkStore getChunkStore(Location location) {
        return getChunkStore(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
    
    public ChunkStore getChunkStore(int x, int y, int z) {
        int[] chunk = toChunkCoordinates(x, y, z);
        return getChunkStoreByChunk(chunk[0], chunk[1], chunk[2], true);
    }
    
    public ChunkStore loadStore(int cx, int cz, int cy) {
        File file = getStoreFile(cx, cz, cy);
        
        if (!file.exists()) {
            return null;
        }
        
        try {
            FileInputStream fileStream = new FileInputStream(file);
            ObjectInputStream stream = new ObjectInputStream(fileStream);
            
            final ChunkStore store = ChunkStore.read(stream);
            
            if (store == null) {
                return null;
            }
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    storeMap.put(store.getID(), store);
                }
            }.runTask(BlockStore.getInstance());
            
            stream.close();
            
            return store;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public void unloadStore(ChunkStore store) {
        if (store == null) {
            return;
        }
        
        storeMap.remove(store.getID());
        
        if (!store.isDirty()) {
            return;
        }
        
        boolean empty = store.isEmpty();
        
        int[] coords = store.getChunkCoords();
        File file = getStoreFile(coords[0], coords[1], coords[2]);
        
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
            ObjectOutputStream stream = new ObjectOutputStream(fileStream);
            
            store.write(stream);
            
            stream.flush();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void saveAll() {
        for (ChunkStore store : storeMap.values().toArray(new ChunkStore[0])) {
            unloadStore(store);
        }
    }
    
    public static int[] toChunkCoordinates(int x, int y, int z) {
        int cx = (int) Math.floor(x / 16d);
        int cz = (int) Math.floor(z / 16d);
        int cy = (int) Math.floor(y / 64d);
        
        return new int[] { cx, cz, cy };
    }
    
}
