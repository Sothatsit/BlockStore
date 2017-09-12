package net.sothatsit.blockstore;

import net.sothatsit.blockstore.chunkstore.ChunkLoc;
import net.sothatsit.blockstore.chunkstore.ChunkManager;
import net.sothatsit.blockstore.chunkstore.ChunkStore;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public enum PreloadStrategy {

    ALL("All") {
        @Override
        public boolean shouldRemainLoaded(ChunkStore store) {
            return store.isChunkLoaded();
        }

        @Override
        public void initialise() {
            BlockStore plugin = BlockStore.getInstance();

            for(World world : Bukkit.getWorlds()) {
                ChunkManager chunkManager = plugin.getManager(world);

                chunkManager.preloadChunks();
            }
        }
    },

    CLOSE("Close") {
        @Override
        public boolean shouldRemainLoaded(ChunkStore store) {
            ChunkLoc chunkLoc = store.getChunkLoc();

            for(Player player : store.getWorld().getPlayers()) {
                ChunkLoc playerLoc = ChunkLoc.fromLocation(player.getLocation());

                if(Math.abs(playerLoc.x - chunkLoc.x) <= 1
                        || Math.abs(playerLoc.y - chunkLoc.y) <= 1
                        || Math.abs(playerLoc.z - chunkLoc.z) <= 1)
                    return true;
            }

            return false;
        }

        @Override
        public void initialise() {
            BlockStore plugin = BlockStore.getInstance();

            for(World world : Bukkit.getWorlds()) {
                ChunkManager chunkManager = plugin.getManager(world);

                for(Player player : world.getPlayers()) {
                    ChunkLoc playerLocation = ChunkLoc.fromLocation(player.getLocation());

                    chunkManager.preloadStoresAround(playerLocation);
                }
            }
        }
    },

    NONE("None") {
        @Override
        public boolean shouldRemainLoaded(ChunkStore store) {
            return false;
        }

        @Override
        public void initialise() {

        }
    };

    private final String name;

    private PreloadStrategy(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public abstract boolean shouldRemainLoaded(ChunkStore store);

    public abstract void initialise();

    public static PreloadStrategy getStrategy(String name) {
        for(PreloadStrategy strategy : PreloadStrategy.values()) {
            if(!strategy.getName().equalsIgnoreCase(name))
                continue;

            return strategy;
        }

        return null;
    }

}
