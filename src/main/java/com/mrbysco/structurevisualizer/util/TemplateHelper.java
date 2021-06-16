package com.mrbysco.structurevisualizer.util;

import com.mrbysco.structurevisualizer.render.RenderHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.feature.template.BlockIgnoreStructureProcessor;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateHelper {
	public static final PlacementSettings PLACEMENT_SETTINGS = (new PlacementSettings()).setIgnoreEntities(true).addProcessor(BlockIgnoreStructureProcessor.STRUCTURE_BLOCK);

	public static boolean setRenderInfo(Template template, IWorld world, BlockPos pos, BlockPos offsetPos, PlacementSettings placementSettings) {
		if (template.palettes.isEmpty()) {
			RenderHandler.blocksToRender = new HashMap<>();
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

				RenderHandler.blocksToRender = renderInfoMap;
				RenderHandler.renderBuffer = null;
				return true;
			} else {
				RenderHandler.blocksToRender = new HashMap<>();
				RenderHandler.renderBuffer = null;
				return false;
			}
		}
	}
}
