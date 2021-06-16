package com.mrbysco.structurevisualizer.util;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mrbysco.structurevisualizer.render.FakeWorld;
import com.mrbysco.structurevisualizer.render.RenderHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.IClearable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.math.shapes.BitSetVoxelShapePart;
import net.minecraft.util.math.shapes.VoxelShapePart;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.BlockIgnoreStructureProcessor;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TemplateHelper {
	public static final PlacementSettings PLACEMENT_SETTINGS = (new PlacementSettings()).setIgnoreEntities(true).addProcessor(BlockIgnoreStructureProcessor.STRUCTURE_BLOCK);

	public static boolean setRenderInfo(Template template, IWorld world, BlockPos pos, BlockPos offsetPos, PlacementSettings placementSettings) {
		if (template.palettes.isEmpty()) {
			RenderHandler.renderBuffer = null;
			return false;
		} else {
			Map<BlockPos, BlockState> renderInfoMap = new HashMap<>();
			List<Template.BlockInfo> list = placementSettings.getRandomPalette(template.palettes, pos).blocks();
			if (!list.isEmpty() && template.size.getX() >= 1 && template.size.getY() >= 1 && template.size.getZ() >= 1) {
				MutableBoundingBox mutableboundingbox = placementSettings.getBoundingBox();
				for(Template.BlockInfo template$blockinfo : Template.processBlockInfos(world, pos, offsetPos, placementSettings, list, template)) {
					BlockPos blockpos = template$blockinfo.pos;
					if (mutableboundingbox == null || mutableboundingbox.isInside(blockpos)) {
						BlockState blockstate = template$blockinfo.state.mirror(placementSettings.getMirror()).rotate(placementSettings.getRotation());
						renderInfoMap.put(blockpos, blockstate);
					}
				}

				RenderHandler.renderBuffer = null;
				return true;
			} else {
				RenderHandler.renderBuffer = null;
				return false;
			}
		}
	}

	public static boolean initializeTemplateWorld(Template template, World world, BlockPos pos, BlockPos offPos, PlacementSettings placementSettings, int placeFlag) {
		if (template.palettes.isEmpty()) {
			RenderHandler.templateWorld = null;
			return false;
		} else {
			FakeWorld templateWorld = new FakeWorld(world);
			List<Template.BlockInfo> list = placementSettings.getRandomPalette(template.palettes, pos).blocks();
			if (!list.isEmpty() && template.size.getX() >= 1 && template.size.getY() >= 1 && template.size.getZ() >= 1) {
				MutableBoundingBox mutableboundingbox = placementSettings.getBoundingBox();
				List<BlockPos> list1 = Lists.newArrayListWithCapacity(placementSettings.shouldKeepLiquids() ? list.size() : 0);
				List<Pair<BlockPos, CompoundNBT>> list2 = Lists.newArrayListWithCapacity(list.size());
				int i = Integer.MAX_VALUE;
				int j = Integer.MAX_VALUE;
				int k = Integer.MAX_VALUE;
				int l = Integer.MIN_VALUE;
				int i1 = Integer.MIN_VALUE;
				int j1 = Integer.MIN_VALUE;

				for(Template.BlockInfo template$blockinfo : Template.processBlockInfos(templateWorld, pos, offPos, placementSettings, list, template)) {
					BlockPos blockpos = template$blockinfo.pos;
					if (mutableboundingbox == null || mutableboundingbox.isInside(blockpos)) {
						FluidState fluidstate = placementSettings.shouldKeepLiquids() ? templateWorld.getFluidState(blockpos) : null;
						BlockState blockstate = template$blockinfo.state.mirror(placementSettings.getMirror()).rotate(placementSettings.getRotation());
						if (template$blockinfo.nbt != null) {
							TileEntity tileentity = templateWorld.getBlockEntity(blockpos);
							IClearable.tryClear(tileentity);
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

							if (fluidstate != null && blockstate.getBlock() instanceof ILiquidContainer) {
								((ILiquidContainer)blockstate.getBlock()).placeLiquid(templateWorld, blockpos, blockstate, fluidstate);
								if (!fluidstate.isSource()) {
									list1.add(blockpos);
								}
							}
						}
					}
				}

				boolean flag = true;
				Direction[] adirection = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

				while(flag && !list1.isEmpty()) {
					flag = false;
					Iterator<BlockPos> iterator = list1.iterator();

					while(iterator.hasNext()) {
						BlockPos blockpos2 = iterator.next();
						BlockPos blockpos3 = blockpos2;
						FluidState fluidstate2 = templateWorld.getFluidState(blockpos2);

						for(int k1 = 0; k1 < adirection.length && !fluidstate2.isSource(); ++k1) {
							BlockPos blockpos1 = blockpos3.relative(adirection[k1]);
							FluidState fluidstate1 = templateWorld.getFluidState(blockpos1);
							if (fluidstate1.getHeight(templateWorld, blockpos1) > fluidstate2.getHeight(templateWorld, blockpos3) || fluidstate1.isSource() && !fluidstate2.isSource()) {
								fluidstate2 = fluidstate1;
								blockpos3 = blockpos1;
							}
						}

						if (fluidstate2.isSource()) {
							BlockState blockstate2 = templateWorld.getBlockState(blockpos2);
							Block block = blockstate2.getBlock();
							if (block instanceof ILiquidContainer) {
								((ILiquidContainer)block).placeLiquid(templateWorld, blockpos2, blockstate2, fluidstate2);
								flag = true;
								iterator.remove();
							}
						}
					}
				}

				if (i <= l) {
					if (!placementSettings.getKnownShape()) {
						VoxelShapePart voxelshapepart = new BitSetVoxelShapePart(l - i + 1, i1 - j + 1, j1 - k + 1);
						int l1 = i;
						int i2 = j;
						int j2 = k;

						for(Pair<BlockPos, CompoundNBT> pair1 : list2) {
							BlockPos blockpos5 = pair1.getFirst();
							voxelshapepart.setFull(blockpos5.getX() - l1, blockpos5.getY() - i2, blockpos5.getZ() - j2, true, true);
						}

						Template.updateShapeAtEdge(templateWorld, placeFlag, voxelshapepart, l1, i2, j2);
					}

					for(Pair<BlockPos, CompoundNBT> pair : list2) {
						BlockPos blockpos4 = pair.getFirst();
						if (!placementSettings.getKnownShape()) {
							BlockState blockstate1 = templateWorld.getBlockState(blockpos4);
							BlockState blockstate3 = Block.updateFromNeighbourShapes(blockstate1, templateWorld, blockpos4);
							if (blockstate1 != blockstate3) {
								templateWorld.setBlock(blockpos4, blockstate3, placeFlag & -2 | 16);
							}

							templateWorld.blockUpdated(blockpos4, blockstate3.getBlock());
						}
					}
				}
				RenderHandler.templateWorld = templateWorld;
				RenderHandler.renderBuffer = null;
				return true;
			} else {
				RenderHandler.templateWorld = null;
				RenderHandler.renderBuffer = null;
				return false;
			}
		}
	}
}
