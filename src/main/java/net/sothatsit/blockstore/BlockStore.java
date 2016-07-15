package net.sothatsit.blockstore;

import net.sothatsit.blockstore.chunkstore.ChunkManager;
import net.sothatsit.blockstore.chunkstore.ChunkStore;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockStore extends JavaPlugin implements Listener {
    
    private static BlockStore instance;
    private Map<String, ChunkManager> managers;
    
    @Override
    public void onEnable() {
        instance = this;
        managers = new HashMap<String, ChunkManager>();
        
        Bukkit.getPluginManager().registerEvents(this, this);
        
        getCommand("blockstore").setExecutor(new BlockStoreCommand());
        
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null) {
            getLogger().info("Attempting to hook WorldEdit.");
            
            try {
                new net.sothatsit.blockstore.worldedit.WorldEditHook();
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
        ChunkManager manager = managers.get(world.getName());
        
        if (manager != null) {
            return manager;
        }
        
        manager = new ChunkManager(world);
        
        managers.put(world.getName(), manager);
        
        return manager;
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
            if (setTrue.contains(b)) {
                continue;
            }
            
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
        if (e.getRetractLocation() == null || e.getBlock().getType() != Material.PISTON_STICKY_BASE) {
            return;
        }
        
        setPlaced(e.getRetractLocation(), false);
    }
    
    @EventHandler
    public void onChunkLoad(final ChunkLoadEvent e) {
        new BukkitRunnable() {
            @Override
            public void run() {
                final Chunk chunk = e.getChunk();
                final ChunkManager manager = getManager(chunk.getWorld());
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < e.getWorld().getMaxHeight(); i += 64) {
                            manager.loadStore(chunk.getX(), chunk.getZ(), i / 64);
                        }
                    }
                }.runTaskAsynchronously(instance);
            }
        }.runTaskLater(this, 1);
    }
    
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        Chunk chunk = e.getChunk();
        ChunkManager manager = getManager(e.getWorld());
        
        for (int i = 0; i < e.getWorld().getMaxHeight(); i += 64) {
            ChunkStore store = manager.getChunkStoreByChunk(chunk.getX(), chunk.getZ(), i / 64, false);
            
            if (store != null) {
                store.setTimeToUnload(5000);
            }
        }
    }
    
    public static boolean isPlaced(Location loc) {
        ChunkManager manager = getChunkManager(loc);
        return manager.getChunkStore(loc).isTrue(loc);
    }
    
    public static void setPlaced(Location loc, boolean value) {
        ChunkManager manager = getChunkManager(loc);
        manager.getChunkStore(loc).setTrue(loc, value);
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
