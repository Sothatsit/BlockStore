package net.sothatsit.blockstore.chunkstore;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class LoadedChunkStore extends ChunkStore {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    private final Map<Integer, BlockMeta> metadata = new ConcurrentHashMap<>();
    private final BitSet store;
    private boolean dirty = false;

    public LoadedChunkStore(World world, ChunkLoc loc) {
        this(world, loc, new BitSet(16 * 64 * 16));
    }
    
    public LoadedChunkStore(World world, ChunkLoc chunkLoc, BitSet store) {
        super(world, chunkLoc);

        this.store = store;
    }

    @Override
    public boolean isDirty() {
        try {
            readLock.lock();
            return dirty;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isPlaced(BlockLoc location) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        setLastUse();

        try {
            readLock.lock();
            return store.get(location.blockIndex);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void setPlaced(BlockLoc location, boolean value) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        setLastUse();

        try {
            writeLock.lock();

            store.set(location.blockIndex, value);

            if (!value) {
                metadata.remove(location.blockIndex);
            }

            dirty = true;
        } finally {
            writeLock.unlock();
        }
    }

    private BlockMeta getMeta(BlockLoc location) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        setLastUse();

        return metadata.computeIfAbsent(location.blockIndex, blockIndex -> new BlockMeta());
    }

    @Override
    public Object getMetaValue(BlockLoc location, int plugin, int key) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        return getMeta(location).getValue(plugin, key);
    }

    @Override
    public Map<Integer, Object> getMetaValues(BlockLoc location, int plugin) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        return getMeta(location).getAllValues(plugin);
    }

    @Override
    public Map<Integer, Map<Integer, Object>> getMetaValues(BlockLoc location) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        return getMeta(location).getAllValues();
    }

    @Override
    public void setMetaValue(BlockLoc location, int plugin, int key, Object value) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        try {
            writeLock.lock();

            getMeta(location).setValue(plugin, key, value);

            dirty = true;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void removeMetaValue(BlockLoc location, int plugin, int key) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        try {
            writeLock.lock();

            getMeta(location).removeValue(plugin, key);

            dirty = true;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    protected BlockMeta getBlockState(BlockLoc location) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        return (isPlaced(location) ? getMeta(location) : null);
    }

    @Override
    protected void setBlockState(BlockLoc location, BlockMeta meta) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");
        Preconditions.checkNotNull(meta, "meta cannot be null");

        setLastUse();

        try {
            writeLock.lock();

            store.set(location.blockIndex, true);
            metadata.put(location.blockIndex, meta);

            dirty = true;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        try {
            readLock.lock();
            return store.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void write(ObjectOutputStream stream) throws IOException {
        try {
            readLock.lock();

            ChunkLoc chunkLoc = getChunkLoc();

            stream.writeUTF(getWorld().getName());
            stream.writeInt(chunkLoc.x);
            stream.writeInt(chunkLoc.z);
            stream.writeInt(chunkLoc.y);
            stream.writeObject(store);

            Set<Integer> plugins = metadata.values().stream()
                    .flatMap(meta -> meta.getPlugins().stream())
                    .collect(Collectors.toSet());

            stream.writeInt(plugins.size());

            for (Integer plugin : plugins) {
                stream.writeInt(plugin);

                int blockCount = (int) metadata.values().stream()
                        .filter(meta -> meta.containsPlugin(plugin))
                        .count();

                stream.writeInt(blockCount);

                for (Map.Entry<Integer, BlockMeta> entry : metadata.entrySet()) {
                    int blockIndex = entry.getKey();
                    BlockMeta meta = entry.getValue();

                    if(!meta.containsPlugin(plugin))
                        continue;

                    stream.writeInt(blockIndex);
                    meta.write(stream, plugin);
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    public static LoadedChunkStore read(ObjectInputStream stream, int version) throws IOException, ClassNotFoundException {
        switch (version) {
            case 1:
                return readVersion1(stream);
            case 2:
                return readVersion2(stream);
            default:
                throw new IllegalArgumentException("Unknown file version " + version);
        }
    }

    public static LoadedChunkStore readVersion2(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        String worldName = stream.readUTF();
        World world = Bukkit.getWorld(worldName);

        int cx = stream.readInt();
        int cz = stream.readInt();
        int cy = stream.readInt();
        ChunkLoc chunkLoc = new ChunkLoc(cx, cy, cz);

        BitSet values = (BitSet) stream.readObject();

        LoadedChunkStore store = new LoadedChunkStore(world, chunkLoc, values);

        int plugins = stream.readInt();

        for (int i = 0; i < plugins; i++) {
            int plugin = stream.readInt();
            int blocks = stream.readInt();

            for (int w = 0; w < blocks; w++) {
                BlockLoc location = BlockLoc.fromBlockIndex(chunkLoc, stream.readInt());

                store.getMeta(location).read(stream, plugin);
            }
        }

        return store;
    }

    public static LoadedChunkStore readVersion1(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        String worldName = stream.readUTF();
        World world = Bukkit.getWorld(worldName);
        
        int cx = stream.readInt();
        int cz = stream.readInt();
        int cy = stream.readInt();
        ChunkLoc chunkLoc = new ChunkLoc(cx, cy, cz);

        BitSet values = convertToBitSet((boolean[][][]) stream.readObject());
        
        LoadedChunkStore store = new LoadedChunkStore(world, chunkLoc, values);
        
        int plugins = stream.readInt();
        
        for (int i = 0; i < plugins; i++) {
            int plugin = stream.readInt();
            int blocks = stream.readInt();
            
            for (int w = 0; w < blocks; w++) {
                byte[] loc = unpackInt(stream.readInt());
                BlockLoc location = new BlockLoc(chunkLoc, loc[0], loc[1], loc[2]);

                store.getMeta(location).read(stream, plugin);
            }
        }
        
        return store;
    }

    private static BitSet convertToBitSet(boolean[][][] values) {
        BitSet bitSet = new BitSet(16 * 64 * 16);

        for(int x = 0; x < 16; x++) {
            for(int y = 0; y < 64; y++) {
                for(int z = 0; z < 16; z++) {
                    bitSet.set(BlockLoc.calcBlockIndex(x, y, z), values[x][y][z]);
                }
            }
        }

        return bitSet;
    }

    private static byte[] unpackInt(int num) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(num).array();
    }
    
}
