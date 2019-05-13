package net.sothatsit.blockstore.chunkstore;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BlockMeta {

    private final Map<MetaKey, Object> metadata = new ConcurrentHashMap<>();

    public boolean containsPlugin(int plugin) {
        return metadata.keySet().stream().anyMatch(key -> key.plugin == plugin);
    }

    public boolean containsValue(int plugin, int key) {
        return metadata.containsKey(new MetaKey(plugin, key));
    }

    public Object getValue(int plugin, int key) {
        return metadata.get(new MetaKey(plugin, key));
    }

    public Map<Integer, Object> getAllValues(int plugin) {
        ImmutableMap.Builder<Integer, Object> values = ImmutableMap.builder();

        metadata.entrySet().stream()
                .filter(entry -> entry.getKey().plugin == plugin)
                .forEach(entry -> values.put(entry.getKey().key, entry.getValue()));

        return values.build();
    }

    public Map<Integer, Map<Integer, Object>> getAllValues() {
        Map<Integer, ImmutableMap.Builder<Integer, Object>> builders = new HashMap<>();

        metadata.forEach((jointKey, value) -> {
            int plugin = jointKey.plugin;
            int key = jointKey.key;

            ImmutableMap.Builder<Integer, Object> builder = builders.get(plugin);

            if(builder == null) {
                builder = ImmutableMap.builder();
                builders.put(plugin, builder);
            }

            builder.put(key, value);
        });

        ImmutableMap.Builder<Integer, Map<Integer, Object>> values = ImmutableMap.builder();

        builders.forEach((plugin, valuesBuilder) -> values.put(plugin, valuesBuilder.build()));

        return values.build();
    }

    public Set<Integer> getPlugins() {
        return metadata.keySet().stream().map(key -> key.plugin).collect(Collectors.toSet());
    }

    public void setValue(int plugin, int key, Object value) {
        Preconditions.checkNotNull(value, "value cannot be null");

        metadata.put(new MetaKey(plugin, key), value);
    }
    
    public Object removeValue(int plugin, int key) {
        return metadata.remove(new MetaKey(plugin, key));
    }

    public void read(ObjectInputStream stream, int plugin) throws IOException, ClassNotFoundException {
        int amount = stream.readInt();

        for (int i = 0; i < amount; i++) {
            int key = stream.readInt();
            Object value = stream.readObject();

            setValue(plugin, key, value);
        }
    }

    public void write(ObjectOutputStream stream, int plugin) throws IOException {
        Map<Integer, Object> map = getAllValues(plugin);

        stream.writeInt(map.size());

        for (Entry<Integer, Object> entry : map.entrySet()) {
            stream.writeInt(entry.getKey());
            stream.writeObject(entry.getValue());
        }
    }

    private final static class MetaKey {

        private final int plugin;
        private final int key;

        public MetaKey(int plugin, int key) {
            this.plugin = plugin;
            this.key = key;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(plugin) ^ Integer.hashCode(key);
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof MetaKey))
                return false;

            MetaKey other = (MetaKey) obj;

            return other.plugin == plugin && other.key == key;
        }

    }
    
}
