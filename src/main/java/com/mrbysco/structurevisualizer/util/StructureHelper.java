package com.mrbysco.structurevisualizer.util;

import com.mrbysco.structurevisualizer.StructureVisualizer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.ResourceLocationException;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

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

	public static StructureTemplate loadFromDirectory(String structureName) {
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

	private static StructureTemplate readStructure(InputStream stream) throws IOException {
		CompoundTag tag = NbtIo.readCompressed(stream);
		return readStructure(tag);
	}

	public static StructureTemplate readStructure(CompoundTag tag) {
		if (!tag.contains("DataVersion", 99)) {
			tag.putInt("DataVersion", 500);
		}

		StructureTemplate template = new StructureTemplate();
		template.load(NbtUtils.update(DataFixers.getDataFixer(), DataFixTypes.STRUCTURE, tag, tag.getInt("DataVersion")));
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
