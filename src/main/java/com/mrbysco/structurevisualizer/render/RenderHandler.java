package com.mrbysco.structurevisualizer.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mrbysco.structurevisualizer.StructureVisualizer;
import com.mrbysco.structurevisualizer.render.vbo.MultiVBORenderer;
import com.mrbysco.structurevisualizer.util.StructureRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class RenderHandler {
	public static MultiVBORenderer renderBuffer;

	private int tickTrack = 0;

	public static String cachedTemplateName = "";
	public static StructureTemplate cachedTemplate = null;
	public static boolean renderStructure = false;
	public static FakeWorld templateWorld = null;
	public static BlockPos position = BlockPos.ZERO;
	public static StructurePlaceSettings placementSettings = StructureRenderHelper.PLACEMENT_SETTINGS;
	public static int templateHeight = 0;
	public static int layer = 0;

	@SubscribeEvent
	public void onRenderWorldLastEvent(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			Player player = Minecraft.getInstance().player;
			if (player == null)
				return;

			if (renderStructure) {
				if (templateWorld == null) {
					cachedTemplateName = "";
					cachedTemplate = null;
					renderStructure = false;
				} else {
					final Minecraft minecraft = Minecraft.getInstance();

					final Vec3 cameraView = minecraft.gameRenderer.getMainCamera().getPosition();
					PoseStack poseStack = event.getPoseStack(); //Get current matrix position from the evt call
					poseStack.pushPose();
					poseStack.translate(-cameraView.x, -cameraView.y, -cameraView.z);

					renderTemplate(poseStack, cameraView, player);

					poseStack.popPose();
				}
			}
		}
	}

	public void renderTemplate(PoseStack poseStack, Vec3 cameraView, Player player) {
		tickTrack++;
		BlockPos startPos = BlockPos.ZERO;
		if (renderBuffer != null && tickTrack < 300) {
			if (tickTrack % 30 == 0) {
				try {
					Vec3 projectedView2 = cameraView;
					Vec3 startPosView = new Vec3(startPos.getX(), startPos.getY(), startPos.getZ());
					projectedView2 = projectedView2.subtract(startPosView);
					renderBuffer.sort((float) projectedView2.x(), (float) projectedView2.y(), (float) projectedView2.z());
				} catch (Exception ignored) {
				}
			}

			poseStack.translate(startPos.getX(), startPos.getY(), startPos.getZ());
			renderBuffer.render(poseStack.last().pose()); //Actually draw whats in the buffer
			System.out.println("Hey");
			return;
		}

		tickTrack = 0;
		if (renderBuffer != null) //Reset Render Buffer before rebuilding
			renderBuffer.close();

		Minecraft minecraft = Minecraft.getInstance();
		renderBuffer = MultiVBORenderer.of((renderTypeBuffer) -> {
			Level level = player.level;
			VertexConsumer builder = renderTypeBuffer.getBuffer(CustomRenderType.VISUAL_BLOCK);
			VertexConsumer noDepthbuilder = renderTypeBuffer.getBuffer(CustomRenderType.VISUAL_BLOCK_NO_DEPTH);

			BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();

			PoseStack stack = new PoseStack(); //Create a new matrix stack for use in the buffer building process
			stack.pushPose(); //Save position

			for (Map.Entry<BlockPos, BlockState> entry : templateWorld.entrySet()) {
				BlockPos targetPos = entry.getKey();
				BlockState state = entry.getValue();
				BlockPos.MutableBlockPos mutablePos = targetPos.mutable();

				stack.pushPose(); //Save position again
				stack.translate(targetPos.getX(), targetPos.getY(), targetPos.getZ());

				BakedModel blockModel = dispatcher.getBlockModel(state);
				BlockColors blockColors = minecraft.getBlockColors();
				int color = blockColors.getColor(state, templateWorld, targetPos, 0);

				float f = (float) (color >> 16 & 255) / 255.0F;
				float f1 = (float) (color >> 8 & 255) / 255.0F;
				float f2 = (float) (color & 255) / 255.0F;
				if (level.isEmptyBlock(targetPos)) {
					try {
						if (state.getRenderShape() == RenderShape.MODEL) {
							for (Direction direction : Direction.values()) {
								mutablePos.setWithOffset(targetPos, direction);
								if (Block.shouldRenderFace(state, templateWorld, targetPos, direction, mutablePos) && !(templateWorld.getBlockState(targetPos.relative(direction)).getBlock().equals(state.getBlock()))) {
									if (state.getMaterial().isSolidBlocking()) {
										renderModelBrightnessColorQuads(stack.last(), builder, f, f1, f2, 0.8F, blockModel.getQuads(state, direction, new Random(Mth.getSeed(targetPos)), EmptyModelData.INSTANCE), 15728640, 655360);
									} else {
										renderModelBrightnessColorQuads(stack.last(), noDepthbuilder, f, f1, f2, 0.8F, blockModel.getQuads(state, direction, new Random(Mth.getSeed(targetPos)), EmptyModelData.INSTANCE), 15728640, 655360);
									}
								}
							}
							if (state.getMaterial().isSolidBlocking()) {
								renderModelBrightnessColorQuads(stack.last(), builder, f, f1, f2, 0.8F, blockModel.getQuads(state, null, new Random(Mth.getSeed(targetPos)), EmptyModelData.INSTANCE), 15728640, 655360);
							} else {
								renderModelBrightnessColorQuads(stack.last(), noDepthbuilder, f, f1, f2, 0.8F, blockModel.getQuads(state, null, new Random(Mth.getSeed(targetPos)), EmptyModelData.INSTANCE), 15728640, 655360);
							}
						}
					} catch (Exception e) {
						StructureVisualizer.LOGGER.trace("Caught exception whilst rendering {}.", state, e);
					}
				}
				stack.popPose(); // Load the position we saved earlier
			}
			stack.popPose(); //Load after loop
		});

		Vec3 projectedView2 = minecraft.gameRenderer.getMainCamera().getPosition();
		Vec3 startPosView = new Vec3(startPos.getX(), startPos.getY(), startPos.getZ());
		projectedView2 = projectedView2.subtract(startPosView);
		renderBuffer.sort((float) projectedView2.x(), (float) projectedView2.y(), (float) projectedView2.z());
		poseStack.translate(startPos.getX(), startPos.getY(), startPos.getZ());
		renderBuffer.render(poseStack.last().pose()); //Actually draw whats in the buffer
	}

	public static void renderModelBrightnessColorQuads(PoseStack.Pose matrixEntry, VertexConsumer builder, float red, float green, float blue, float alpha, List<BakedQuad> listQuads, int combinedLightsIn, int combinedOverlayIn) {
		for (BakedQuad bakedquad : listQuads) {
			float f;
			float f1;
			float f2;

			if (bakedquad.isTinted()) {
				f = red;
				f1 = green;
				f2 = blue;
			} else {
				f = 1f;
				f1 = 1f;
				f2 = 1f;
			}

			builder.putBulkData(matrixEntry, bakedquad, f, f1, f2, alpha, combinedLightsIn, combinedOverlayIn);
		}
	}
}
