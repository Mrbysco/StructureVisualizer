package com.mrbysco.structurevisualizer.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mrbysco.structurevisualizer.StructureVisualizer;
import com.mrbysco.structurevisualizer.render.vbo.MultiVBORenderer;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Block.RenderSideCacheKey;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RenderHandler {
	public static MultiVBORenderer renderBuffer;

	private int tickTrack = 0;

	public static String cachedTemplateName = "";
	public static Template cachedTemplate = null;
	public static boolean renderStructure = false;
	public static Map<BlockPos, BlockState> blocksToRender = new HashMap<>();

	@SubscribeEvent
	public void onRenderWorldLastEvent(RenderWorldLastEvent event) {
		PlayerEntity player = Minecraft.getInstance().player;
		if( player == null )
			return;

		if(renderStructure) {
			if(blocksToRender.isEmpty()) {
				renderStructure = false;
			} else {
				final Minecraft minecraft = Minecraft.getInstance();

				final Vector3d cameraView = minecraft.gameRenderer.getMainCamera().getPosition();
				MatrixStack poseStack = event.getMatrixStack(); //Get current matrix position from the evt call
				poseStack.pushPose();
				poseStack.translate(-cameraView.x, -cameraView.y, -cameraView.z);

				renderTemplate(poseStack, cameraView, player);

				poseStack.popPose();
			}
		}
	}

	public void renderTemplate(MatrixStack poseStack, Vector3d cameraView, PlayerEntity player) {
		tickTrack++;
		BlockPos startPos = new BlockPos(0, 0, 0); //todo: Change pos accordingly
		if (renderBuffer != null && tickTrack < 300) {
			if (tickTrack % 30 == 0) {
				try {
					Vector3d projectedView2 = cameraView;
					Vector3d startPosView = new Vector3d(startPos.getX(), startPos.getY(), startPos.getZ());
					projectedView2 = projectedView2.subtract(startPosView);
					renderBuffer.sort((float) projectedView2.x(), (float) projectedView2.y(), (float) projectedView2.z());
				} catch (Exception ignored) {
				}
			}

			poseStack.translate(startPos.getX(), startPos.getY(), startPos.getZ());
			renderBuffer.render(poseStack.last().pose()); //Actually draw whats in the buffer
			return;
		}

		tickTrack = 0;
		if (renderBuffer != null) //Reset Render Buffer before rebuilding
			renderBuffer.close();

		Minecraft minecraft = Minecraft.getInstance();
		renderBuffer = MultiVBORenderer.of((buffer) -> {
			World level = player.level;
			IVertexBuilder builder = buffer.getBuffer(CustomRenderType.VISUAL_BLOCK);
			IVertexBuilder noDepthbuilder = buffer.getBuffer(CustomRenderType.VISUAL_BLOCK_NO_DEPTH);

			BlockRendererDispatcher dispatcher = minecraft.getBlockRenderer();

			MatrixStack stack = new MatrixStack(); //Create a new matrix stack for use in the buffer building process
			stack.pushPose(); //Save position

			for (Map.Entry<BlockPos, BlockState> entry : blocksToRender.entrySet()) {
				BlockPos targetPos = entry.getKey();
				BlockState state = entry.getValue();

				stack.pushPose(); //Save position again
				//matrix.translate(-startPos.getX(), -startPos.getY(), -startPos.getZ());
				stack.translate(targetPos.getX(), targetPos.getY(), targetPos.getZ());

				IBakedModel ibakedmodel = dispatcher.getBlockModel(state);
				BlockColors blockColors = minecraft.getBlockColors();
				int color = blockColors.getColor(state, level, targetPos, 0);

				float f = (float) (color >> 16 & 255) / 255.0F;
				float f1 = (float) (color >> 8 & 255) / 255.0F;
				float f2 = (float) (color & 255) / 255.0F;
				if(level.isEmptyBlock(targetPos)) {
					try {
						if (state.getRenderShape() == BlockRenderType.MODEL) {
							for (Direction direction : Direction.values()) {
								if (shouldRenderFace(state, level, targetPos, direction) && !isSameBlock(state, targetPos, direction)) {
									if (state.getMaterial().isSolidBlocking()) {
										renderModelBrightnessColorQuads(stack.last(), builder, f, f1, f2, 0.8F, ibakedmodel.getQuads(state, direction, new Random(MathHelper.getSeed(targetPos)), EmptyModelData.INSTANCE), 15728640, 655360);
									} else {
										renderModelBrightnessColorQuads(stack.last(), noDepthbuilder, f, f1, f2, 0.8F, ibakedmodel.getQuads(state, direction, new Random(MathHelper.getSeed(targetPos)), EmptyModelData.INSTANCE), 15728640, 655360);
									}
								}
							}
							if (state.getMaterial().isSolidBlocking())
								renderModelBrightnessColorQuads(stack.last(), builder, f, f1, f2, 0.8F, ibakedmodel.getQuads(state, null, new Random(MathHelper.getSeed(targetPos)), EmptyModelData.INSTANCE), 15728640, 655360);
							else
								renderModelBrightnessColorQuads(stack.last(), noDepthbuilder, f, f1, f2, 0.8F, ibakedmodel.getQuads(state, null, new Random(MathHelper.getSeed(targetPos)), EmptyModelData.INSTANCE), 15728640, 655360);
						}
					} catch (Exception e) {
						StructureVisualizer.LOGGER.trace("Caught exception whilst rendering {}.", state, e);
					}
				}
				stack.popPose(); // Load the position we saved earlier
			}
			stack.popPose(); //Load after loop
		});
//        try {
		Vector3d projectedView2 = minecraft.gameRenderer.getMainCamera().getPosition();
		Vector3d startPosView = new Vector3d(startPos.getX(), startPos.getY(), startPos.getZ());
		projectedView2 = projectedView2.subtract(startPosView);
		renderBuffer.sort((float) projectedView2.x(), (float) projectedView2.y(), (float) projectedView2.z());
//        } catch (Exception ignored) {
//        }
		poseStack.translate(startPos.getX(), startPos.getY(), startPos.getZ());
		renderBuffer.render(poseStack.last().pose()); //Actually draw whats in the buffer
	}

	public static void renderModelBrightnessColorQuads(MatrixStack.Entry matrixEntry, IVertexBuilder builder, float red, float green, float blue, float alpha, List<BakedQuad> listQuads, int combinedLightsIn, int combinedOverlayIn) {
		for(BakedQuad bakedquad : listQuads) {
			float f;
			float f1;
			float f2;

			if (bakedquad.isTinted()) {
				f = red * 1f;
				f1 = green * 1f;
				f2 = blue * 1f;
			} else {
				f = 1f;
				f1 = 1f;
				f2 = 1f;
			}

			builder.addVertexData(matrixEntry, bakedquad, f, f1, f2, alpha, combinedLightsIn, combinedOverlayIn);
		}
	}

	public static boolean shouldRenderFace(BlockState state, IWorldReader realLevel, BlockPos pos, Direction direction) {
		BlockPos blockpos = pos.relative(direction);
		if(!blocksToRender.isEmpty() && blocksToRender.containsKey(blockpos)) {
			BlockState blockstate = blocksToRender.get(blockpos);
			if (state.skipRendering(blockstate, direction)) {
				return false;
			} else if (blockstate.canOcclude()) {
				Block.RenderSideCacheKey block$rendersidecachekey = new Block.RenderSideCacheKey(state, blockstate, direction);
				Object2ByteLinkedOpenHashMap<RenderSideCacheKey> object2bytelinkedopenhashmap = Block.OCCLUSION_CACHE.get();
				byte b0 = object2bytelinkedopenhashmap.getAndMoveToFirst(block$rendersidecachekey);
				if (b0 != 127) {
					return b0 != 0;
				} else {
					VoxelShape voxelshape = state.getFaceOcclusionShape(realLevel, pos, direction);
					VoxelShape voxelshape1 = blockstate.getFaceOcclusionShape(realLevel, blockpos, direction.getOpposite());
					boolean flag = VoxelShapes.joinIsNotEmpty(voxelshape, voxelshape1, IBooleanFunction.ONLY_FIRST);
					if (object2bytelinkedopenhashmap.size() == 2048) {
						object2bytelinkedopenhashmap.removeLastByte();
					}

					object2bytelinkedopenhashmap.putAndMoveToFirst(block$rendersidecachekey, (byte)(flag ? 1 : 0));
					return flag;
				}
			}
		}
		return true;
	}

	public static boolean isSameBlock(BlockState state, BlockPos pos, Direction direction) {
		BlockPos blockpos = pos.relative(direction);
		if(!blocksToRender.isEmpty() && blocksToRender.containsKey(blockpos)) {
			BlockState relativeState = blocksToRender.get(blockpos);
			return relativeState.getBlock().equals(state.getBlock());
		}
		return false;
	}
}
