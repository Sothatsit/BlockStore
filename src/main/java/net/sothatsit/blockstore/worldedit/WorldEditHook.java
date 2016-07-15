package net.sothatsit.blockstore.worldedit;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.logging.AbstractLoggingExtent;
import com.sk89q.worldedit.util.eventbus.Subscribe;

import net.sothatsit.blockstore.BlockStore;
import net.sothatsit.blockstore.chunkstore.ChunkManager;

public class WorldEditHook {
    
    public WorldEditHook() {
        WorldEdit.getInstance().getEventBus().register(this);
    }
    
    @Subscribe
    public void wrapForLogging(EditSessionEvent event) {
        final ChunkManager manager = BlockStore.getInstance().getManager(event.getWorld().getName());
        
        event.setExtent(new AbstractLoggingExtent(event.getExtent()) {
            @Override
            protected void onBlockChange(Vector pos, BaseBlock newBlock) {
                manager.getChunkStore(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()).setTrue(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), false);
            }
        });
    }
    
}
