package com.mrbysco.structurevisualizer.render.vbo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.math.vector.Matrix4f;

import java.io.Closeable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Vertex Buffer Object for caching the render. Pretty similar to how the chunk caching works
 * Credits to Direwolf20's (and the team of) Building Gadgets which is under the MIT license as of writing 16/06/2021 (d/m/y)
 * https://github.com/Direwolf20-MC/BuildingGadgets/blob/1.16/src/main/java/com/direwolf20/buildinggadgets/client/renders/CopyPasteRender.java
 */
public class MultiVBORenderer implements Closeable {
	private static final int BUFFER_SIZE = 2 * 1024 * 1024 * 3;

	public static MultiVBORenderer of(Consumer<IRenderTypeBuffer> vertexProducer) {
		final Map<RenderType, CustomBufferBuilder> builders = Maps.newHashMap();

		vertexProducer.accept(rt -> builders.computeIfAbsent(rt, (_rt) -> {
			CustomBufferBuilder builder = new CustomBufferBuilder(BUFFER_SIZE);
			builder.begin(_rt.mode(), _rt.format());

			return builder;
		}));

		Map<RenderType, CustomBufferBuilder.State> sortCaches = Maps.newHashMap();
		Map<RenderType, CustomVertexBuffer> buffers = Maps.transformEntries(builders, (rt, builder) -> {
			Objects.requireNonNull(rt);
			Objects.requireNonNull(builder);
			sortCaches.put(rt, builder.getVertexState());

			builder.finishDrawing();
			VertexFormat fmt = rt.format();
			CustomVertexBuffer vbo = new CustomVertexBuffer(fmt);

			vbo.upload(builder);
			return vbo;
		});

		return new MultiVBORenderer(buffers, sortCaches);
	}

	private final ImmutableMap<RenderType, CustomVertexBuffer> buffers;
	private final ImmutableMap<RenderType, CustomBufferBuilder.State> sortCaches;

	protected MultiVBORenderer(Map<RenderType, CustomVertexBuffer> buffers, Map<RenderType, CustomBufferBuilder.State> sortCaches) {
		this.buffers = ImmutableMap.copyOf(buffers);
		this.sortCaches = ImmutableMap.copyOf(sortCaches);
	}

	public void sort(float x, float y, float z) {
		for (Map.Entry<RenderType, CustomBufferBuilder.State> kv : sortCaches.entrySet()) {
			RenderType rt = kv.getKey();
			CustomBufferBuilder.State state = kv.getValue();
			CustomBufferBuilder builder = new CustomBufferBuilder(BUFFER_SIZE);
			builder.begin(rt.mode(), rt.format());
			builder.setVertexState(state);
			builder.sortVertexData(x, y, z);
			builder.finishDrawing();

			CustomVertexBuffer vbo = buffers.get(rt);
			vbo.upload(builder);
		}
	}

	public void render(Matrix4f matrix) {
		buffers.forEach((rt, vbo) -> {
			VertexFormat fmt = rt.format();

			rt.setupRenderState();
			vbo.bindBuffer();
			fmt.setupBufferState(0L);
			vbo.draw(matrix, rt.mode());
			CustomVertexBuffer.unbindBuffer();
			fmt.clearBufferState();
			rt.clearRenderState();
		});
	}

	public void close() {
		buffers.values().forEach(CustomVertexBuffer::close);
	}
}
