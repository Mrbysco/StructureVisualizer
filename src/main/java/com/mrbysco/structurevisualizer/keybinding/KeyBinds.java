package com.mrbysco.structurevisualizer.keybinding;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mrbysco.structurevisualizer.StructureVisualizer;
import com.mrbysco.structurevisualizer.render.RenderHandler;
import com.mrbysco.structurevisualizer.screen.TemplateSelectionScreen;
import com.mrbysco.structurevisualizer.util.StructureRenderHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fmlclient.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public class KeyBinds {
	public static KeyMapping KEY_TOGGLE = new KeyMapping(
			"key." + StructureVisualizer.MOD_ID + ".open",
			Type.KEYSYM,
			GLFW.GLFW_KEY_KP_9,
			"category." + StructureVisualizer.MOD_ID + ".main");
	public static KeyMapping KEY_RENDER = new KeyMapping(
			"key." + StructureVisualizer.MOD_ID + ".render",
			Type.KEYSYM,
			GLFW.GLFW_KEY_KP_3,
			"category." + StructureVisualizer.MOD_ID + ".main");

	public static KeyMapping KEY_X_DOWN = new KeyMapping(
			"key." + StructureVisualizer.MOD_ID + ".x_down",
			Type.KEYSYM,
			GLFW.GLFW_KEY_KP_7,
			"category." + StructureVisualizer.MOD_ID + ".main");
	public static KeyMapping KEY_X_UP = new KeyMapping(
			"key." + StructureVisualizer.MOD_ID + ".x_up",
			Type.KEYSYM,
			GLFW.GLFW_KEY_KP_8,
			"category." + StructureVisualizer.MOD_ID + ".main");
	public static KeyMapping KEY_Y_DOWN = new KeyMapping(
			"key." + StructureVisualizer.MOD_ID + ".y_down",
			Type.KEYSYM,
			GLFW.GLFW_KEY_KP_4,
			"category." + StructureVisualizer.MOD_ID + ".main");
	public static KeyMapping KEY_Y_UP = new KeyMapping(
			"key." + StructureVisualizer.MOD_ID + ".y_up",
			Type.KEYSYM,
			GLFW.GLFW_KEY_KP_5,
			"category." + StructureVisualizer.MOD_ID + ".main");
	public static KeyMapping KEY_Z_DOWN = new KeyMapping(
			"key." + StructureVisualizer.MOD_ID + ".z_down",
			Type.KEYSYM,
			GLFW.GLFW_KEY_KP_1,
			"category." + StructureVisualizer.MOD_ID + ".main");
	public static KeyMapping KEY_Z_UP = new KeyMapping(
			"key." + StructureVisualizer.MOD_ID + ".z_up",
			Type.KEYSYM,
			GLFW.GLFW_KEY_KP_2,
			"category." + StructureVisualizer.MOD_ID + ".main");
	public static KeyMapping KEY_COORDINATE = new KeyMapping(
			"key." + StructureVisualizer.MOD_ID + ".coordinate",
			Type.KEYSYM,
			GLFW.GLFW_KEY_KP_6,
			"category." + StructureVisualizer.MOD_ID + ".main");
	public static KeyMapping KEY_LAYER_DOWN = new KeyMapping(
			"key." + StructureVisualizer.MOD_ID + ".layer_down",
			Type.KEYSYM,
			GLFW.GLFW_KEY_KP_SUBTRACT,
			"category." + StructureVisualizer.MOD_ID + ".main");
	public static KeyMapping KEY_LAYER_UP = new KeyMapping(
			"key." + StructureVisualizer.MOD_ID + ".layer_up",
			Type.KEYSYM,
			GLFW.GLFW_KEY_KP_ADD,
			"category." + StructureVisualizer.MOD_ID + ".main");

	public static void registerKeybinds(final FMLClientSetupEvent event) {
		ClientRegistry.registerKeyBinding(KEY_TOGGLE);
		ClientRegistry.registerKeyBinding(KEY_RENDER);
		ClientRegistry.registerKeyBinding(KEY_X_DOWN);
		ClientRegistry.registerKeyBinding(KEY_X_UP);
		ClientRegistry.registerKeyBinding(KEY_Y_DOWN);
		ClientRegistry.registerKeyBinding(KEY_Y_UP);
		ClientRegistry.registerKeyBinding(KEY_Z_DOWN);
		ClientRegistry.registerKeyBinding(KEY_Z_UP);
		ClientRegistry.registerKeyBinding(KEY_COORDINATE);
		ClientRegistry.registerKeyBinding(KEY_LAYER_DOWN);
		ClientRegistry.registerKeyBinding(KEY_LAYER_UP);
	}

	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {
		final Minecraft minecraft = Minecraft.getInstance();
		if(minecraft.screen != null && event.getAction() != GLFW.GLFW_PRESS)

		if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), 292)) { return; }

		if (KEY_TOGGLE.consumeClick()) {
			minecraft.setScreen(new TemplateSelectionScreen());
		}

		if (KEY_COORDINATE.consumeClick()) {
			BlockPos pos = RenderHandler.position;
			StructureTemplate template = RenderHandler.cachedTemplate;
			if(pos != null && template != null) {
				pos = pos.offset((template.size.getX() / 2), 0, (template.size.getZ() / 2));

				minecraft.player.sendMessage(new TranslatableComponent("structurevisualizer.coordinates",
						new TextComponent(String.valueOf(pos.getX())).withStyle(ChatFormatting.RED),
						new TextComponent(String.valueOf(pos.getY())).withStyle(ChatFormatting.GREEN),
						new TextComponent(String.valueOf(pos.getZ())).withStyle(ChatFormatting.BLUE)), Util.NIL_UUID);
			}
		}

		if (KEY_X_DOWN.consumeClick()){
			changePosition(minecraft, -1, 0, 0);
		}
		if (KEY_X_UP.consumeClick())
			changePosition(minecraft, 1, 0, 0);
		if (KEY_Y_DOWN.consumeClick()) {
			changePosition(minecraft, 0, -1, 0);
		}
		if (KEY_Y_UP.consumeClick())
			changePosition(minecraft, 0, 1, 0);
		if (KEY_Z_DOWN.consumeClick())
			changePosition(minecraft, 0, 0, -1);
		if (KEY_Z_UP.consumeClick())
			changePosition(minecraft, 0, 0, 1);

		if (KEY_RENDER.consumeClick()) {
			if(RenderHandler.templateWorld == null) {
				minecraft.player.sendMessage(new TranslatableComponent("structurevisualizer.render.fail"), Util.NIL_UUID);
			} else {
				RenderHandler.renderStructure = !RenderHandler.renderStructure;
			}
		}

		if (KEY_LAYER_DOWN.consumeClick()) {
			layerDown(minecraft);
		}
		if (KEY_LAYER_UP.consumeClick()) {
			layerUp(minecraft);
		}
	}

	public void layerDown(Minecraft minecraft) {
		if(RenderHandler.cachedTemplate != null) {
			RenderHandler.renderBuffer = null;
			RenderHandler.templateWorld = null;
			int downLayer = RenderHandler.layer - 1;
			if(downLayer == 0) {
				RenderHandler.layer = RenderHandler.templateHeight;
			} else {
				RenderHandler.layer = downLayer;
			}
			minecraft.player.displayClientMessage(new TranslatableComponent("structurevisualizer.layer", RenderHandler.layer, RenderHandler.templateHeight).withStyle(ChatFormatting.YELLOW), true);

			StructureRenderHelper.initializeTemplateWorld(RenderHandler.cachedTemplate, minecraft.level, RenderHandler.position, RenderHandler.position, RenderHandler.placementSettings, 2);
		}
	}

	public void layerUp(Minecraft minecraft) {
		if(RenderHandler.cachedTemplate != null) {
			RenderHandler.renderBuffer = null;
			RenderHandler.templateWorld = null;
			int upLayer = RenderHandler.layer + 1;
			if(upLayer > RenderHandler.templateHeight) {
				RenderHandler.layer = 1;
			} else {
				RenderHandler.layer = upLayer;
			}
			minecraft.player.displayClientMessage(new TranslatableComponent("structurevisualizer.layer", RenderHandler.layer, RenderHandler.templateHeight).withStyle(ChatFormatting.YELLOW), true);

			StructureRenderHelper.initializeTemplateWorld(RenderHandler.cachedTemplate, minecraft.level, RenderHandler.position, RenderHandler.position, RenderHandler.placementSettings, 2);
		}
	}

	public void changePosition(Minecraft minecraft, int x, int y, int z) {
		if(RenderHandler.cachedTemplate != null) {
			RenderHandler.renderBuffer = null;
			RenderHandler.templateWorld = null;
			RenderHandler.position = RenderHandler.position.offset(x, y, z);

			System.out.println(RenderHandler.position);

			StructureRenderHelper.initializeTemplateWorld(RenderHandler.cachedTemplate, minecraft.level, RenderHandler.position, RenderHandler.position, RenderHandler.placementSettings, 2);
		}
	}
}
