package com.mrbysco.structurevisualizer;

import com.mrbysco.structurevisualizer.keybinding.KeyBinds;
import com.mrbysco.structurevisualizer.render.RenderHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.IExtensionPoint.DisplayTest;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

@Mod(StructureVisualizer.MOD_ID)
public class StructureVisualizer {
	public static final String MOD_ID = "structurevisualizer";

	public static final Logger LOGGER = LogManager.getLogger();
	public static final File structureFolder = new File(FMLPaths.MODSDIR.get().toFile(), "/structures");
	public static String structurePath = "";

	public StructureVisualizer() {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			if (!structureFolder.exists()) {
				structureFolder.mkdirs();
			}
			try {
				structurePath = structureFolder.getCanonicalPath();
			} catch (IOException e) {
				e.printStackTrace();
			}

			FMLJavaModLoadingContext.get().getModEventBus().addListener(KeyBinds::registerKeybinds);
			MinecraftForge.EVENT_BUS.register(new KeyBinds());
			MinecraftForge.EVENT_BUS.register(new RenderHandler());

			//Make sure the mod being absent on the other network side does not cause the client to display the server as incompatible
			ModLoadingContext.get().registerExtensionPoint(DisplayTest.class, () ->
					new IExtensionPoint.DisplayTest(() -> "Trans Rights Are Human Rights",
							(remoteVersionString, networkBool) -> networkBool));
		});
	}
}
