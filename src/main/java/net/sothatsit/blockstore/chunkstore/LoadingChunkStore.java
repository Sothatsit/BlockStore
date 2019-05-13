package net.sothatsit.blockstore.chunkstore;

import com.google.common.base.Preconditions;
import org.bukkit.World;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class LoadingChunkStore extends ChunkStore {

    private final Object lock = new Object();
    private final CountDownLatch latch = new CountDownLatch(1);

    private final AtomicReference<ChunkStore> delegate = new AtomicReference<>();
    private List<Action> pendingActions = new ArrayList<>();
    private List<Consumer<ChunkStore>> onLoad = new ArrayList<>();

    public LoadingChunkStore(World world, ChunkLoc chunkLoc) {
        super(world, chunkLoc);
    }

    public void await() {
        try {
            boolean success = latch.await(1, TimeUnit.SECONDS);

            if(!success)
                throw new RuntimeException("Over one second elapsed waiting for the store to load");
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread interrupted waiting for store to load", e);
        }
    }

    public boolean hasLoaded() {
        return delegate.get() != null;
    }

    public void onLoad(Consumer<ChunkStore> consumer) {
        Preconditions.checkNotNull(consumer, "consumer cannot be null");

        boolean run;

        synchronized (lock) {
            run = hasLoaded();

            if(!run) {
                onLoad.add(consumer);
            }
        }

        if(run) {
            consumer.accept(getDelegate());
        }
    }

    public ChunkStore getDelegate() {
        return delegate.get();
    }

    protected void setDelegate(ChunkStore delegate) {
        Preconditions.checkNotNull(delegate, "delegate cannot be null");
        Preconditions.checkArgument(!hasLoaded(), "Already has a delegate");
        Preconditions.checkArgument(getWorld() == delegate.getWorld(), "Must be in the same world");
        Preconditions.checkArgument(getChunkLoc().equals(delegate.getChunkLoc()), "Must be the same chunk");

        List<Action> pendingActions;
        List<Consumer<ChunkStore>> onLoad;

        synchronized (lock) {
            this.delegate.set(delegate);

            pendingActions = this.pendingActions;
            this.pendingActions = null;

            onLoad = this.onLoad;
            this.onLoad = null;
        }

        for(Action action : pendingActions) {
            action.apply(delegate);
        }

        latch.countDown();

        for(Consumer<ChunkStore> consumer : onLoad) {
            try {
                consumer.accept(delegate);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void queueAction(Action action) {
        synchronized (lock) {
            if(hasLoaded()) {
                action.apply(getDelegate());
                return;
            }

            pendingActions.add(action);
        }
    }

    @Override
    protected void setLastUse() {
        synchronized (lock) {
            if(hasLoaded()) {
                getDelegate().setLastUse();
                return;
            }

            super.setLastUse();
        }
    }

    @Override
    public long getTimeSinceUse() {
        synchronized (lock) {
            return (hasLoaded() ? getDelegate().getTimeSinceUse() : super.getTimeSinceUse());
        }
    }

    @Override
    public boolean isDirty() {
        synchronized (lock) {
            return (hasLoaded() ? getDelegate().isDirty() : pendingActions.size() > 0);
        }
    }

    @Override
    public boolean isPlaced(BlockLoc location) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        await();

        return getDelegate().isPlaced(location);
    }

    @Override
    public void setPlaced(BlockLoc location, boolean value) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        setLastUse();

        queueAction(new SetPlacedAction(location, value));
    }

    @Override
    public Object getMetaValue(BlockLoc location, int plugin, int key) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        await();

        return getDelegate().getMetaValue(location, plugin, key);
    }

    @Override
    public Map<Integer, Object> getMetaValues(BlockLoc location, int plugin) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        await();

        return getDelegate().getMetaValues(location, plugin);
    }

    @Override
    public Map<Integer, Map<Integer, Object>> getMetaValues(BlockLoc location) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        await();

        return getDelegate().getMetaValues(location);
    }

    @Override
    public void setMetaValue(BlockLoc location, int plugin, int key, Object value) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        setLastUse();

        queueAction(new SetMetaValueAction(location, plugin, key, value));
    }

    @Override
    public void removeMetaValue(BlockLoc location, int plugin, int key) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        setLastUse();

        queueAction(new RemoveMetaValueAction(location, plugin, key));
    }

    @Override
    protected BlockMeta getBlockState(BlockLoc location) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");

        await();

        return getDelegate().getBlockState(location);
    }

    @Override
    protected void setBlockState(BlockLoc location, BlockMeta meta) {
        Preconditions.checkArgument(isInChunk(location), "location is not in this chunk");
        Preconditions.checkNotNull(meta, "meta cannot be null");

        setLastUse();

        queueAction(new SetBlockStateAction(location, meta));
    }

    @Override
    public boolean isEmpty() {
        await();

        return getDelegate().isEmpty();
    }

    @Override
    public void write(ObjectOutputStream stream) throws IOException {
        await();

        getDelegate().write(stream);
    }

    private interface Action {

        public void apply(ChunkStore store);

    }

    private class SetPlacedAction implements Action {

        private final BlockLoc location;
        private final boolean value;

        public SetPlacedAction(BlockLoc location, boolean value) {
            this.location = location;
            this.value = value;
        }

        @Override
        public void apply(ChunkStore store) {
            store.setPlaced(location, value);
        }

    }

    private class SetMetaValueAction implements Action {

        private final BlockLoc location;
        private final int plugin;
        private final int key;
        private final Object value;

        public SetMetaValueAction(BlockLoc location, int plugin, int key, Object value) {
            this.location = location;
            this.plugin = plugin;
            this.key = key;
            this.value = value;
        }

        @Override
        public void apply(ChunkStore store) {
            store.setMetaValue(location, plugin, key, value);
        }

    }

    private class RemoveMetaValueAction implements Action {

        private final BlockLoc location;
        private final int plugin;
        private final int key;

        public RemoveMetaValueAction(BlockLoc location, int plugin, int key) {
            this.location = location;
            this.plugin = plugin;
            this.key = key;
        }

        @Override
        public void apply(ChunkStore store) {
            store.removeMetaValue(location, plugin, key);
        }

    }

    private class SetBlockStateAction implements Action {

        private final BlockLoc location;
        private final BlockMeta state;

        public SetBlockStateAction(BlockLoc location, BlockMeta state) {
            this.location = location;
            this.state = state;
        }

        @Override
        public void apply(ChunkStore store) {
            store.setBlockState(location, state);
        }

    }

}
