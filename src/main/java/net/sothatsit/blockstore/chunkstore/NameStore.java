package net.sothatsit.blockstore.chunkstore;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NameStore {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    private final List<String> names = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> ids = new ConcurrentHashMap<>();

    public int toId(String name, boolean create) {
        Preconditions.checkNotNull(name, "name cannot be null");

        try {
            readLock.lock();

            int id = ids.getOrDefault(name, -1);

            if (!create || id >= 0)
                return id;
        } finally {
            readLock.unlock();
        }

        return addName(name);
    }

    private int addName(String name) {
        try {
            writeLock.lock();

            int id = names.size();

            names.add(name);
            ids.put(name, id);

            return id;
        } finally {
            writeLock.unlock();
        }
    }
    
    public String fromId(int id) {
        Preconditions.checkArgument(id >= 0, "Invalid id " + id + ", valid ids are >= 0");
        Preconditions.checkArgument(id < names.size(), "Invalid id " + id + ", outside of the range of known ids");

        try {
            readLock.lock();

            return names.get(id);
        } finally {
            readLock.unlock();
        }
    }

    public Map<String, Object> keysFromId(Map<Integer, Object> values) {
        Preconditions.checkNotNull(values, "values cannot be null");

        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

        values.forEach((keyId, value) -> {
            String key = fromId(keyId);

            builder.put(key, value);
        });

        return builder.build();
    }

    public Map<String, Map<String, Object>> deepKeysFromId(Map<Integer, Map<Integer, Object>> values) {
        Preconditions.checkNotNull(values, "values cannot be null");

        ImmutableMap.Builder<String, Map<String, Object>> builder = ImmutableMap.builder();

        values.forEach((keyId, subValuesByIds) -> {
            String key = fromId(keyId);
            Map<String, Object> subValues = keysFromId(subValuesByIds);

            builder.put(key, subValues);
        });

        return builder.build();
    }

    public void write(ObjectOutputStream stream) throws IOException {
        Preconditions.checkNotNull(stream, "stream cannot be null");

        try {
            readLock.lock();

            stream.writeInt(names.size());

            for(String name : names) {
                stream.writeUTF(name);
            }
        } finally {
            readLock.unlock();
        }
    }
    
    public void read(ObjectInputStream stream) throws IOException {
        Preconditions.checkNotNull(stream, "stream cannot be null");

        try {
            writeLock.lock();

            int amount = stream.readInt();

            for(int i = 0; i < amount; ++i) {
                addName(stream.readUTF());
            }
        } finally {
            writeLock.unlock();
        }
    }
}
