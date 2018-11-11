package net.sothatsit.blockstore;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import net.sothatsit.blockstore.chunkstore.BlockLoc;
import net.sothatsit.blockstore.chunkstore.ChunkManager;
import net.sothatsit.blockstore.chunkstore.ChunkStore;

public class WorldEditHook {

    void register() {
        WorldEdit.getInstance().getEventBus().register(this);
    }

    @Subscribe
    public void wrapForLogging(EditSessionEvent event) {
        World world = event.getWorld();
        if (world == null) {
            return;
        }

        final ChunkManager manager = BlockStore.getInstance().getManager(world.getName());
        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
            @Override
            public boolean setBlock(BlockVector3 pos, BlockStateHolder block) throws WorldEditException {
                BlockLoc blockLoc = BlockLoc.fromLocation(pos.getX(), pos.getY(), pos.getZ());
                ChunkStore store = manager.getChunkStore(blockLoc.chunkLoc, true);
                store.setPlaced(blockLoc, false);
                return getExtent().setBlock(pos, block);
            }
        });
    }

}