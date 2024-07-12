package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;

@Environment(EnvType.CLIENT)
public class BufferAllocatorCache implements AutoCloseable
{
    protected static final List<RenderLayer> LAYERS = ChunkRenderLayers.LAYERS;
    protected static final List<ChunkRendererSchematicVbo.OverlayRenderType> TYPES = ChunkRenderLayers.TYPES;
    protected static final int EXPECTED_TOTAL_SIZE;
    private final ConcurrentHashMap<RenderLayer, BufferAllocator> layerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ChunkRendererSchematicVbo.OverlayRenderType, BufferAllocator> overlayCache = new ConcurrentHashMap<>();

    protected BufferAllocatorCache() { }

    protected boolean hasBufferByLayer(RenderLayer layer)
    {
        return this.layerCache.containsKey(layer);
    }

    protected boolean hasBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayCache.containsKey(type);
    }

    protected BufferAllocator getBufferByLayer(RenderLayer layer)
    {
        return this.layerCache.computeIfAbsent(layer, l -> new BufferAllocator(l.getExpectedBufferSize()));
    }

    protected BufferAllocator getBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayCache.computeIfAbsent(type, t -> new BufferAllocator(t.getExpectedBufferSize()));
    }

    protected void closeByLayer(RenderLayer layer)
    {
        try
        {
            this.layerCache.remove(layer).close();
        }
        catch (Exception ignored) { }
    }

    protected void closeByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        try
        {
            this.overlayCache.remove(type).close();
        }
        catch (Exception ignored) { }
    }

    protected void resetAll()
    {
        try
        {
            this.layerCache.values().forEach(BufferAllocator::reset);
            this.overlayCache.values().forEach(BufferAllocator::reset);
        }
        catch (Exception ignored) { }
    }

    protected void clearAll()
    {
        try
        {
            this.layerCache.values().forEach(BufferAllocator::clear);
            this.overlayCache.values().forEach(BufferAllocator::clear);
        }
        catch (Exception ignored) { }
    }

    protected void closeAll()
    {
        ArrayList<BufferAllocator> allocators;

        synchronized (this.layerCache)
        {
            allocators = new ArrayList<>(this.layerCache.values());
            this.layerCache.clear();
        }
        synchronized (this.overlayCache)
        {
            allocators.addAll(this.overlayCache.values());
            this.overlayCache.clear();
        }

        allocators.forEach(BufferAllocator::close);
    }

    @Override
    public void close()
    {
        this.closeAll();
    }

    static
    {
        EXPECTED_TOTAL_SIZE = LAYERS.stream().mapToInt(RenderLayer::getExpectedBufferSize).sum() + TYPES.stream().mapToInt(ChunkRendererSchematicVbo.OverlayRenderType::getExpectedBufferSize).sum();
    }
}