package net.sothatsit.blockstore;

import net.sothatsit.blockstore.chunkstore.ChunkLoc;
import net.sothatsit.blockstore.chunkstore.ChunkManager;
import net.sothatsit.blockstore.worldedit.WorldEditHook;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockStore extends JavaPlugin implements Listener {
    
    private static BlockStore instance;
    private Map<String, ChunkManager> managers;
    
    @Override
    public void onEnable() {
        instance = this;
        managers = new ConcurrentHashMap<>();
        
        Bukkit.getPluginManager().registerEvents(this, this);
        
        getCommand("blockstore").setExecutor(new BlockStoreCommand());
        
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null) {
            getLogger().info("Attempting to hook WorldEdit...");
            
            try {
                WorldEditHook hook = new WorldEditHook();

                hook.register();

                getLogger().info("Hooked WorldEdit.");
            } catch (Throwable e) {
                getLogger().severe("");
                getLogger().severe("     [!] Hooking WorldEdit has Failed [!] ");
                getLogger().severe("An error has been thrown hooking WorldEdit. This is");
                getLogger().severe("likely due to an outdated WorldEdit plugin. If you");
                getLogger().severe("wish for WorldEdit to clear the placed block stores");
                getLogger().severe("then please update WorldEdit to the latest version.");
                getLogger().severe("");
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void onDisable() {
        for (ChunkManager manager : managers.values()) {
            manager.saveAll();
            manager.saveNames();
        }
    }
    
    public ChunkManager getManager(String world) {
        return managers.get(world);
    }
    
    public ChunkManager getManager(World world) {
        return managers.computeIfAbsent(world.getName(), key -> new ChunkManager(world));
    }
    
    public Map<String, ChunkManager> getChunkManagers() {
        return managers;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent e) {
        setPlaced(e.getBlock().getLocation(), false);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        setPlaced(e.getBlock().getLocation(), false);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        setPlaced(e.getBlock().getLocation(), true);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent e) {
        List<Block> setTrue = new ArrayList<>();
        List<Block> setFalse = new ArrayList<>();
        
        for (Block b : e.getBlocks()) {
            setFalse.add(b);
            setTrue.add(b.getRelative(e.getDirection()));
        }
        
        for (Block b : setFalse) {
            if (setTrue.contains(b))
                continue;
            
            setPlaced(b.getLocation(), false);
        }
        
        for (Block b : setTrue) {
            setPlaced(b.getLocation(), true);
        }
        
        setPlaced(e.getBlock().getRelative(e.getDirection()).getLocation(), true);
    }
    
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent e) {
        if (e.getRetractLocation() == null || e.getBlock().getType() != Material.PISTON_STICKY_BASE)
            return;
        
        setPlaced(e.getRetractLocation(), false);
    }
    
    @EventHandler
    public void onChunkLoad(final ChunkLoadEvent e) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Chunk chunk = e.getChunk();
            ChunkManager manager = getManager(chunk.getWorld());

            for (int i = 0; i < e.getWorld().getMaxHeight(); i += 64) {
                ChunkLoc chunkLoc = new ChunkLoc(chunk.getX(), i / 64, chunk.getZ());

                if(manager.isChunkStoreLoaded(chunkLoc))
                    continue;

                Bukkit.getScheduler().runTaskAsynchronously(this, () -> manager.loadStore(chunkLoc));
            }
        }, 1);
    }
    
    public static boolean isPlaced(Location loc) {
        ChunkManager manager = getChunkManager(loc);

        return manager.getChunkStore(loc).getValue(loc);
    }
    
    public static void setPlaced(Location loc, boolean value) {
        ChunkManager manager = getChunkManager(loc);

        manager.getChunkStore(loc).setValue(loc, value);
    }
    
    public static ChunkManager getChunkManager(Location loc) {
        return instance.getManager(loc.getWorld());
    }
    
    public static ChunkManager getChunkManager(World world) {
        return instance.getManager(world);
    }
    
    public static BlockStore getInstance() {
        return instance;
    }
    
}
