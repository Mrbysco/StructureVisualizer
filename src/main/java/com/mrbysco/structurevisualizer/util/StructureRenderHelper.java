package com.mrbysco.structurevisualizer.util;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mrbysco.structurevisualizer.render.FakeWorld;
import com.mrbysco.structurevisualizer.render.RenderHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

import java.util.Iterator;
import java.util.List;

public class StructureRenderHelper {
	public static final StructurePlaceSettings PLACEMENT_SETTINGS = (new StructurePlaceSettings()).setIgnoreEntities(true).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);

	public static boolean initializeTemplateWorld(StructureTemplate template, Level world, BlockPos pos, BlockPos offPos, StructurePlaceSettings placementSettings, int placeFlag) {
		RenderHandler.templateWorld = null;
		if (template.palettes.isEmpty()) {
			return false;
		} else {
			FakeWorld templateWorld = new FakeWorld(world);
			List<StructureTemplate.StructureBlockInfo> list = placementSettings.getRandomPalette(template.palettes, pos).blocks();
			if (!list.isEmpty() && template.size.getX() >= 1 && template.size.getY() >= 1 && template.size.getZ() >= 1) {
				BoundingBox boundingBox = placementSettings.getBoundingBox();
				List<BlockPos> list1 = Lists.newArrayListWithCapacity(placementSettings.shouldKeepLiquids() ? list.size() : 0);
				List<Pair<BlockPos, CompoundTag>> list2 = Lists.newArrayListWithCapacity(list.size());
				int i = Integer.MAX_VALUE;
				int j = Integer.MAX_VALUE;
				int k = Integer.MAX_VALUE;
				int l = Integer.MIN_VALUE;
				int i1 = Integer.MIN_VALUE;
				int j1 = Integer.MIN_VALUE;

				for(StructureTemplate.StructureBlockInfo template$blockinfo : StructureTemplate.processBlockInfos(templateWorld, pos, offPos, placementSettings, list, template)) {
					BlockPos blockpos = template$blockinfo.pos;
					if((blockpos.getY() - pos.getY()) < RenderHandler.layer) {
						if (boundingBox == null || boundingBox.isInside(blockpos)) {
							FluidState fluidstate = placementSettings.shouldKeepLiquids() ? templateWorld.getFluidState(blockpos) : null;
							BlockState blockstate = template$blockinfo.state.mirror(placementSettings.getMirror()).rotate(placementSettings.getRotation());
							if (template$blockinfo.nbt != null) {
								BlockEntity blockEntity = templateWorld.getBlockEntity(blockpos);
								Clearable.tryClear(blockEntity);
								templateWorld.setBlock(blockpos, Blocks.BARRIER.defaultBlockState(), 20);
							}

							if (templateWorld.setBlock(blockpos, blockstate, placeFlag)) {
								i = Math.min(i, blockpos.getX());
								j = Math.min(j, blockpos.getY());
								k = Math.min(k, blockpos.getZ());
								l = Math.max(l, blockpos.getX());
								i1 = Math.max(i1, blockpos.getY());
								j1 = Math.max(j1, blockpos.getZ());
								list2.add(Pair.of(blockpos, template$blockinfo.nbt));

								if (fluidstate != null && blockstate.getBlock() instanceof LiquidBlockContainer) {
									((LiquidBlockContainer)blockstate.getBlock()).placeLiquid(templateWorld, blockpos, blockstate, fluidstate);
									if (!fluidstate.isSource()) {
										list1.add(blockpos);
									}
								}
							}
						}
					}
				}

				boolean flag = true;
				Direction[] directions = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

				while(flag && !list1.isEmpty()) {
					flag = false;
					Iterator<BlockPos> iterator = list1.iterator();

					while(iterator.hasNext()) {
						BlockPos blockpos2 = iterator.next();
						BlockPos blockpos3 = blockpos2;
						FluidState fluidstate2 = templateWorld.getFluidState(blockpos2);

						for(int k1 = 0; k1 < directions.length && !fluidstate2.isSource(); ++k1) {
							BlockPos blockpos1 = blockpos3.relative(directions[k1]);
							FluidState fluidstate1 = templateWorld.getFluidState(blockpos1);
							if (fluidstate1.getHeight(templateWorld, blockpos1) > fluidstate2.getHeight(templateWorld, blockpos3) || fluidstate1.isSource() && !fluidstate2.isSource()) {
								fluidstate2 = fluidstate1;
								blockpos3 = blockpos1;
							}
						}

						if (fluidstate2.isSource()) {
							BlockState blockstate2 = templateWorld.getBlockState(blockpos2);
							Block block = blockstate2.getBlock();
							if (block instanceof LiquidBlockContainer) {
								((LiquidBlockContainer)block).placeLiquid(templateWorld, blockpos2, blockstate2, fluidstate2);
								flag = true;
								iterator.remove();
							}
						}
					}
				}

				if (i <= l) {
					if (!placementSettings.getKnownShape()) {
						DiscreteVoxelShape discreteVoxelShape = new BitSetDiscreteVoxelShape(l - i + 1, i1 - j + 1, j1 - k + 1);
						int l1 = i;
						int i2 = j;
						int j2 = k;

						for(Pair<BlockPos, CompoundTag> pair1 : list2) {
							BlockPos blockpos5 = pair1.getFirst();
							discreteVoxelShape.fill(blockpos5.getX() - l1, blockpos5.getY() - i2, blockpos5.getZ() - j2);
						}

						StructureTemplate.updateShapeAtEdge(templateWorld, placeFlag, discreteVoxelShape, l1, i2, j2);
					}

					for(Pair<BlockPos, CompoundTag> pair : list2) {
						BlockPos firstPos = pair.getFirst();
						if (!placementSettings.getKnownShape()) {
							BlockState state = templateWorld.getBlockState(firstPos);
							BlockState updatedState = Block.updateFromNeighbourShapes(state, templateWorld, firstPos);
							if (state != updatedState) {
								templateWorld.setBlock(firstPos, updatedState, placeFlag & -2 | 16);
							}

							templateWorld.blockUpdated(firstPos, updatedState.getBlock());
						}
					}
				}
				RenderHandler.templateWorld = templateWorld;
				RenderHandler.renderBuffer = null;
				return true;
			} else {
				RenderHandler.renderBuffer = null;
				return false;
			}
		}
	}
}
