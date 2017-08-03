package net.sothatsit.blockstore.chunkstore;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class BlockMeta {

    private int blockIndex;
    private Map<Integer, Map<Integer, Object>> metadata;
    
    public BlockMeta(int blockIndex) {
        this.blockIndex = blockIndex;
        this.metadata = new HashMap<>();
    }

    public int getBlockIndex() {
        return blockIndex;
    }
    
    private Map<Integer, Object> getMap(int plugin) {
        Map<Integer, Object> map = metadata.get(plugin);
        
        if (map == null) {
            map = new HashMap<>();
            metadata.put(plugin, map);
        }
        
        return map;
    }
    
    public void read(ObjectInputStream stream, int plugin) throws IOException, ClassNotFoundException {
        int amount = stream.readInt();
        
        for (int i = 0; i < amount; i++) {
            int key = stream.readInt();
            Object value = stream.readObject();
            
            getMap(plugin).put(key, value);
        }
    }
    
    public void write(ObjectOutputStream stream, int plugin) throws IOException {
        Map<Integer, Object> map = getMap(plugin);

        stream.writeInt(map.size());
        
        for (Entry<Integer, Object> entry : map.entrySet()) {
            stream.writeInt(entry.getKey());
            stream.writeObject(entry.getValue());
        }
    }
    
    public boolean containsPlugin(int plugin) {
        return metadata.containsKey(plugin);
    }
    
    public Set<Integer> getKeys() {
        return metadata.keySet();
    }
    
    public Map<Integer, Map<Integer, Object>> getRaw() {
        return metadata;
    }
    
    public void set(int plugin, int key, Object value) {
        if (value == null) {
            remove(plugin, key);
        } else {
            getMap(plugin).put(key, value);
        }
    }
    
    public void remove(int plugin, int key) {
        Map<Integer, Object> map = getMap(plugin);
        
        map.remove(key);
        
        if (map.size() == 0) {
            metadata.remove(plugin);
        }
    }
    
    public Object get(int plugin, int key) {
        return (contains(plugin, key) ? getMap(plugin).get(key) : null);
    }
    
    public boolean contains(int plugin, int key) {
        return metadata.containsKey(plugin) && metadata.get(plugin).containsKey(key);
    }
    
}
