package com.mrbysco.structurevisualizer.util;

import com.mrbysco.structurevisualizer.StructureVisualizer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.datafix.DefaultTypeReferences;
import net.minecraft.world.gen.feature.template.Template;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class StructureHelper {

	public static Template loadFromDirectory(String structureName) {
		String name;
		if(structureName.endsWith(".nbt")) {
			name = structureName;
		} else {
			name = structureName + ".nbt";
		}

		File structureFile = getStructure(name);
		if(structureFile != null) {
			Path path = createAndValidatePathToStructure(name);

			try (InputStream inputstream = new FileInputStream(path.toFile())) {
				return readStructure(inputstream);
			} catch (FileNotFoundException filenotfoundexception) {
				return null;
			} catch (IOException ioexception) {
				StructureVisualizer.LOGGER.error("Couldn't load structure from {}", path, ioexception);
				return null;
			}
		}
		return null;
	}

	private static Path createAndValidatePathToStructure(String fileName) {
		try {
			Path path = Paths.get(StructureVisualizer.structurePath).resolve(fileName);
			return path;
		} catch (InvalidPathException invalidpathexception) {
			throw new ResourceLocationException("Invalid resource path: " + fileName, invalidpathexception);
		}
	}

	private static Template readStructure(InputStream p_209205_1_) throws IOException {
		CompoundNBT compoundnbt = CompressedStreamTools.readCompressed(p_209205_1_);
		return readStructure(compoundnbt);
	}

	public static Template readStructure(CompoundNBT p_227458_1_) {
		if (!p_227458_1_.contains("DataVersion", 99)) {
			p_227458_1_.putInt("DataVersion", 500);
		}

		Template template = new Template();
		template.load(NBTUtil.update(DataFixesManager.getDataFixer(), DefaultTypeReferences.STRUCTURE, p_227458_1_, p_227458_1_.getInt("DataVersion")));
		return template;
	}

	public static List<String> getStructures() {
		List<File> structureList = getStructuresFiles();
		List<String> structureNames = new ArrayList<>();
		structureList.forEach((file) -> {
			String name = file.getName();
			name = name.substring(0, name.length() - 4);
			structureNames.add(name);
		});

		return structureNames;
	}

	public static List<File> getStructuresFiles() {
		List<File> fileList = new ArrayList<>();
		File[] structureFiles = StructureVisualizer.structureFolder.listFiles((file, name) -> name.endsWith(".nbt"));

		if(structureFiles != null) {
			fileList = Arrays.asList(structureFiles);
		}
		return fileList;
	}

	@Nullable
	public static File getStructure(String structureName) {
		File[] structureFiles = StructureVisualizer.structureFolder.listFiles((file, name) -> name.toLowerCase(Locale.ROOT).equals(structureName.toLowerCase(Locale.ROOT)));

		File structureFile = null;
		if(structureFiles.length > 0) {
			structureFile = structureFiles[0];
		}
		return structureFile;
	}
}
