package com.mrbysco.structurevisualizer.render.vbo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

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

	public static MultiVBORenderer of(Consumer<MultiBufferSource> vertexProducer) {
		final Map<RenderType, BufferBuilder> builders = Maps.newHashMap();

		vertexProducer.accept(rt -> builders.computeIfAbsent(rt, (_rt) -> {
			BufferBuilder builder = new BufferBuilder(BUFFER_SIZE);
			builder.begin(_rt.mode(), _rt.format());

			return builder;
		}));

		Map<RenderType, BufferBuilder.SortState> sortCaches = Maps.newHashMap();
		Map<RenderType, VertexBuffer> buffers = Maps.transformEntries(builders, (rt, builder) -> {
			Objects.requireNonNull(rt);
			Objects.requireNonNull(builder);
//			sortCaches.put(rt, builder.getSortState());

			builder.end();
			VertexBuffer vbo = new VertexBuffer();

			vbo.upload(builder);
			return vbo;
		});

		return new MultiVBORenderer(buffers, sortCaches);
	}

	private final ImmutableMap<RenderType, VertexBuffer> buffers;
	private final ImmutableMap<RenderType, BufferBuilder.SortState> sortCaches;

	protected MultiVBORenderer(Map<RenderType, VertexBuffer> buffers, Map<RenderType, BufferBuilder.SortState> sortCaches) {
		this.buffers = ImmutableMap.copyOf(buffers);
		this.sortCaches = ImmutableMap.copyOf(sortCaches);
	}

	//Maybe one day
	public void sort(float x, float y, float z) {
//		for (Map.Entry<RenderType, BufferBuilder.SortState> renderTypeSortStateEntry : sortCaches.entrySet()) {
//			RenderType renderType = renderTypeSortStateEntry.getKey();
//			BufferBuilder.SortState state = renderTypeSortStateEntry.getValue();
//			BufferBuilder builder = new BufferBuilder(BUFFER_SIZE);
//			builder.begin(renderType.mode(), renderType.format());
//
//
//			builder.restoreSortState(new SortState(state.mode, state.vertices, state.sortingPoints, x, y, z));
//			builder.end();
//
//			VertexBuffer vbo = buffers.get(renderType);
//			vbo.upload(builder);
//		}
	}

	public void render(Matrix4f matrix) {
		buffers.forEach((rt, vbo) -> {
			RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);

			rt.setupRenderState();
			vbo.bind();
			vbo.drawWithShader(matrix, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());

//			VertexBuffer.unbind();
//			VertexBuffer.unbindVertexArray();

			rt.clearRenderState();
		});
	}

	public void close() {
		buffers.values().forEach(VertexBuffer::close);
	}
}
