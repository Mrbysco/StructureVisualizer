package com.mrbysco.structurevisualizer.keybinding;

import com.mrbysco.structurevisualizer.StructureVisualizer;
import com.mrbysco.structurevisualizer.render.RenderHandler;
import com.mrbysco.structurevisualizer.screen.TemplateSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Type;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public class KeyBinds {
	public static KeyBinding KEY_TOGGLE = new KeyBinding(
			"key." + StructureVisualizer.MOD_ID + ".open",
			Type.KEYSYM,
			GLFW.GLFW_KEY_KP_8,
			"category." + StructureVisualizer.MOD_ID + ".main");
	public static KeyBinding KEY_RENDER = new KeyBinding(
			"key." + StructureVisualizer.MOD_ID + ".render",
			Type.KEYSYM,
			GLFW.GLFW_KEY_KP_5,
			"category." + StructureVisualizer.MOD_ID + ".main");

	public static void registerKeybinds(final FMLClientSetupEvent event) {
		ClientRegistry.registerKeyBinding(KEY_TOGGLE);
		ClientRegistry.registerKeyBinding(KEY_RENDER);
	}

	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {
		final Minecraft minecraft = Minecraft.getInstance();
		if(minecraft.screen != null && event.getAction() != GLFW.GLFW_PRESS)

		if (InputMappings.isKeyDown(minecraft.getWindow().getWindow(), 292)) { return; }

		if (KEY_TOGGLE.consumeClick()) {
			minecraft.setScreen(new TemplateSelectionScreen());
		}

		if (KEY_RENDER.consumeClick()) {
			if(RenderHandler.blocksToRender.isEmpty()) {
				minecraft.player.sendMessage(new TranslationTextComponent("structurevisualizer.render.fail"), Util.NIL_UUID);
			} else {
				RenderHandler.renderStructure = !RenderHandler.renderStructure;
			}
		}
	}
}
