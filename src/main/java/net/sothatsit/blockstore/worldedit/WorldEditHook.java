package net.sothatsit.blockstore.worldedit;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.logging.AbstractLoggingExtent;
import com.sk89q.worldedit.util.eventbus.Subscribe;

import com.sk89q.worldedit.world.World;
import net.sothatsit.blockstore.BlockStore;
import net.sothatsit.blockstore.chunkstore.ChunkManager;
import net.sothatsit.blockstore.chunkstore.ChunkStore;

public class WorldEditHook {

    public void register() {
        WorldEdit.getInstance().getEventBus().register(this);
    }
    
    @Subscribe
    public void wrapForLogging(EditSessionEvent event) {
        World world = event.getWorld();

        if(world == null)
            return;

        final ChunkManager manager = BlockStore.getInstance().getManager(world.getName());
        
        event.setExtent(new AbstractLoggingExtent(event.getExtent()) {
            @Override
            protected void onBlockChange(Vector pos, BaseBlock newBlock) {
                ChunkStore store = manager.getChunkStore(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());

                store.setValue(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(), false);
            }
        });
    }
    
}
