package com.mrbysco.structurevisualizer.render;

import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.opengl.GL11;

public class CustomRenderType extends RenderType {
	public CustomRenderType(String nameIn, VertexFormat formatIn, Mode drawModeIn, int bufferSizeIn, boolean useDelegateIn, boolean needsSortingIn, Runnable setupTaskIn, Runnable clearTaskIn) {
		super(nameIn, formatIn, drawModeIn, bufferSizeIn, useDelegateIn, needsSortingIn, setupTaskIn, clearTaskIn);
	}

	public static final RenderType VISUAL_BLOCK = create("structurevisualizer:block",
			DefaultVertexFormat.BLOCK, Mode.QUADS, 256, false, false,
			RenderType.CompositeState.builder()
//					.setShadeModelState(SMOOTH_SHADE)
					.setShaderState(RenderStateShard.BLOCK_SHADER)
					.setLightmapState(LIGHTMAP)
					.setTextureState(BLOCK_SHEET_MIPPED)
					.setLayeringState(VIEW_OFFSET_Z_LAYERING)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setDepthTestState(LEQUAL_DEPTH_TEST)
					.setCullState(NO_CULL)
					.setWriteMaskState(COLOR_DEPTH_WRITE)
					.createCompositeState(false)
	);

	public static final RenderType VISUAL_BLOCK_NO_DEPTH = create("structurevisualizer:block_no_depth",
			DefaultVertexFormat.BLOCK, Mode.QUADS, 256, false, false,
			RenderType.CompositeState.builder()
//					.setShadeModelState(SMOOTH_SHADE)
					.setShaderState(RenderStateShard.BLOCK_SHADER)
					.setLightmapState(LIGHTMAP)
					.setTextureState(BLOCK_SHEET_MIPPED)
					.setLayeringState(VIEW_OFFSET_Z_LAYERING)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setDepthTestState(LEQUAL_DEPTH_TEST)
					.setCullState(NO_CULL)
					.setWriteMaskState(COLOR_WRITE)
//					.setAlphaState(MIDWAY_ALPHA)
					.createCompositeState(false)
	);
}
