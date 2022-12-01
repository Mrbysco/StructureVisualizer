package com.mrbysco.structurevisualizer.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrbysco.structurevisualizer.render.RenderHandler;
import com.mrbysco.structurevisualizer.screen.widgets.EnumButton;
import com.mrbysco.structurevisualizer.screen.widgets.NumberFieldWidget;
import com.mrbysco.structurevisualizer.screen.widgets.StructureListWidget;
import com.mrbysco.structurevisualizer.util.StructureHelper;
import com.mrbysco.structurevisualizer.util.StructureRenderHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.fml.loading.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TemplateSelectionScreen extends Screen {
	private enum SortType {
		NORMAL,
		A_TO_Z,
		Z_TO_A;

		Button button;

		Component getButtonText() {
			return new TranslatableComponent("structurevisualizer.screen.search." + name().toLowerCase(Locale.ROOT));
		}
	}

	private static final int PADDING = 6;

	private StructureListWidget structureList;
	private StructureListWidget.ListEntry selected = null;
	private final StructurePlaceSettings placementSettings;
	private int listWidth;
	private List<String> structures;
	private final List<String> unsortedStructures;
	private Button unloadButton, loadButton;
	private EnumButton rotationButton, mirrorButton;

	private final int buttonMargin = 1;
	private final int numButtons = SortType.values().length;
	private String lastFilterText = "";

	private EditBox search;
	private NumberFieldWidget xPosField;
	private NumberFieldWidget yPosField;
	private NumberFieldWidget zPosField;

	private boolean sorted = false;
	private SortType sortType = SortType.NORMAL;

	public TemplateSelectionScreen() {
		super(new TranslatableComponent("structurevisualizer.screen.selection.title"));
		this.structures = Collections.unmodifiableList(StructureHelper.getStructures());
		this.unsortedStructures = Collections.unmodifiableList(this.structures);
		this.placementSettings = RenderHandler.placementSettings.copy();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		int centerWidth = this.width / 2;

		for (String structure : structures) {
			listWidth = Math.max(listWidth, getFontRenderer().width(structure) + 10);
		}
		listWidth = Math.max(Math.min(listWidth, width / 3), 200);
		listWidth += listWidth % numButtons != 0 ? (numButtons - listWidth % numButtons) : 0;

		int structureWidth = this.width - this.listWidth - (PADDING * 3);
		int doneButtonWidth = Math.min(structureWidth, 200);
		int y = this.height - 20 - PADDING;
		this.addRenderableWidget(new Button(centerWidth - (doneButtonWidth / 2) + PADDING, y, doneButtonWidth, 20,
				new TranslatableComponent("gui.done"), b -> TemplateSelectionScreen.this.onClose()));

		y -= 18 + PADDING;
		this.addRenderableWidget(this.unloadButton = new Button(centerWidth - (doneButtonWidth / 2) + PADDING, y, doneButtonWidth, 20,
				new TranslatableComponent("structurevisualizer.screen.selection.unload"), b -> {
			RenderHandler.templateWorld = null;
			RenderHandler.renderBuffer = null;
			RenderHandler.position = BlockPos.ZERO;
			RenderHandler.placementSettings = StructureRenderHelper.PLACEMENT_SETTINGS.copy();
			RenderHandler.templateHeight = 0;
			RenderHandler.layer = 0;
			minecraft.player.sendMessage(new TranslatableComponent("structurevisualizer.unload"), Util.NIL_UUID);
		}));
		y -= 18 + PADDING;
		this.addRenderableWidget(this.loadButton = new Button(centerWidth - (doneButtonWidth / 2) + PADDING, y, doneButtonWidth, 20,
				new TranslatableComponent("structurevisualizer.screen.selection.load"), b -> {
			if (selected != null) {
				String selectedTemplate = selected.getStructureName();
				StructureTemplate template = StructureHelper.loadFromDirectory(selectedTemplate);
				if (template != null) {
					RenderHandler.cachedTemplateName = selectedTemplate.toLowerCase(Locale.ROOT);
					RenderHandler.cachedTemplate = template;
					RenderHandler.templateHeight = template.size.getY();
					RenderHandler.layer = RenderHandler.templateHeight;
					RenderHandler.renderBuffer = null;
					RenderHandler.placementSettings = placementSettings;
					BlockPos pos = BlockPos.ZERO.offset(xPosField.getDouble() - (template.size.getX() / 2), yPosField.getDouble(), zPosField.getDouble() - (template.size.getZ() / 2));
					StructureRenderHelper.initializeTemplateWorld(template, minecraft.level, pos, pos, placementSettings, 2);
					RenderHandler.position = pos;
					minecraft.player.sendMessage(new TranslatableComponent("structurevisualizer.load", selectedTemplate).withStyle(ChatFormatting.YELLOW), Util.NIL_UUID);
				} else {
					RenderHandler.templateWorld = null;
					RenderHandler.renderBuffer = null;
					RenderHandler.position = BlockPos.ZERO;
					RenderHandler.placementSettings = StructureRenderHelper.PLACEMENT_SETTINGS.copy();
					RenderHandler.templateHeight = 0;
					RenderHandler.layer = 0;
					minecraft.player.sendMessage(new TranslatableComponent("structurevisualizer.load.fail", selectedTemplate).withStyle(ChatFormatting.RED), Util.NIL_UUID);
				}
			}
		}));

		y -= 14 + PADDING + 41;
		search = new EditBox(getFontRenderer(), centerWidth - listWidth / 2 + PADDING + 1, y, listWidth - 2, 14,
				new TranslatableComponent("structurevisualizer.screen.search"));

		int fullButtonHeight = PADDING + 20 + PADDING;
		this.structureList = new StructureListWidget(this, width, fullButtonHeight, search.y - getFontRenderer().lineHeight - PADDING);
		this.structureList.setLeftPos(0);

		addRenderableWidget(search);
		addWidget(structureList);
		search.setFocus(false);
		search.setCanLoseFocus(true);

		BlockPos pos = BlockPos.ZERO;
		if (minecraft.player != null) {
			pos = minecraft.player.blockPosition();
		}

		y += 36;
		xPosField = new NumberFieldWidget(getFontRenderer(), search.x, y, 60, 14,
				new TranslatableComponent("structurevisualizer.screen.x"));
		xPosField.setValue(String.valueOf(pos.getX()));
		xPosField.setMaxLength(10);

		addRenderableWidget(xPosField);
		xPosField.setFocus(false);
		xPosField.setCanLoseFocus(true);

		yPosField = new NumberFieldWidget(getFontRenderer(), search.x + (search.getWidth() / 3) + 4, y, 60, 14,
				new TranslatableComponent("structurevisualizer.screen.y"));
		yPosField.setValue(String.valueOf(pos.getY()));
		yPosField.setMaxLength(10);

		addRenderableWidget(yPosField);
		yPosField.setFocus(false);
		yPosField.setCanLoseFocus(true);

		zPosField = new NumberFieldWidget(getFontRenderer(), search.x + search.getWidth() - 60, y, 60, 14,
				new TranslatableComponent("structurevisualizer.screen.z"));
		zPosField.setValue(String.valueOf(pos.getZ()));
		zPosField.setMaxLength(10);

		addRenderableWidget(zPosField);
		zPosField.setFocus(false);
		zPosField.setCanLoseFocus(true);

		final int width = listWidth / numButtons;
		int x = centerWidth + PADDING - width;
		this.addRenderableWidget(mirrorButton = new EnumButton(x - (120 + 2), PADDING, 120, 20, Mirror.NONE, b -> {
			Mirror mirror = (Mirror) ((EnumButton) b).getValue();

			int index = mirror.ordinal();
			int nextIndex = index + 1;
			Mirror[] values = Mirror.values();
			nextIndex %= values.length;

			mirror = values[nextIndex];
			((EnumButton) b).setValue(mirror);

			placementSettings.setMirror(mirror);
		}));

		this.addRenderableWidget(SortType.A_TO_Z.button = new Button(x, PADDING, width - buttonMargin, 20, SortType.A_TO_Z.getButtonText(), b -> resortStructures(SortType.A_TO_Z)));
		x += width + buttonMargin;
		this.addRenderableWidget(SortType.Z_TO_A.button = new Button(x, PADDING, width - buttonMargin, 20, SortType.Z_TO_A.getButtonText(), b -> resortStructures(SortType.Z_TO_A)));
		x += width + buttonMargin;
		this.addRenderableWidget(rotationButton = new EnumButton(x, PADDING, 120, 20, Rotation.NONE, b -> {
			Rotation rotation = (Rotation) ((EnumButton) b).getValue();

			int index = rotation.ordinal();
			int nextIndex = index + 1;
			Rotation[] rotations = Rotation.values();
			nextIndex %= rotations.length;

			rotation = rotations[nextIndex];
			((EnumButton) b).setValue(rotation);

			placementSettings.setRotation(rotation);
		}));

		resortStructures(SortType.A_TO_Z);
		updateCache();
	}

	@Override
	public void tick() {
		search.tick();
		xPosField.tick();
		yPosField.tick();
		zPosField.tick();
		structureList.setSelected(selected);

		if (!search.getValue().equals(lastFilterText)) {
			reloadStructures();
			sorted = false;
		}

		if (!sorted) {
			reloadStructures();
			if (sortType == SortType.A_TO_Z) {
				Collections.sort(structures);
			} else if (sortType == SortType.Z_TO_A) {
				structures.sort(Collections.reverseOrder());
			}
			structureList.refreshList();
			if (selected != null) {
				selected = structureList.children().stream().filter(e -> e == selected).findFirst().orElse(null);
				updateCache();
			}
			sorted = true;
		}
	}

	public <T extends ObjectSelectionList.Entry<T>> void buildStructureList(Consumer<T> ListViewConsumer, Function<String, T> newEntry) {
		structures.forEach(mod -> ListViewConsumer.accept(newEntry.apply(mod)));
	}

	private void reloadStructures() {
		this.structures = this.unsortedStructures.stream().
				filter(struc -> StringUtils.toLowerCase(struc).contains(StringUtils.toLowerCase(search.getValue()))).collect(Collectors.toList());
		lastFilterText = search.getValue();
	}

	private void resortStructures(SortType newSort) {
		this.sortType = newSort;

		for (SortType sort : SortType.values()) {
			if (sort.button != null)
				sort.button.active = sortType != sort;
		}
		sorted = false;
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		this.structureList.render(poseStack, mouseX, mouseY, partialTicks);

		Component text = new TranslatableComponent("structurevisualizer.screen.search");
		drawCenteredString(poseStack, getFontRenderer(), text, this.width / 2 + PADDING,
				search.y - getFontRenderer().lineHeight - 2, 0xFFFFFF);

		Component posText = new TranslatableComponent("structurevisualizer.screen.position");
		drawCenteredString(poseStack, getFontRenderer(), posText, this.width / 2 + PADDING,
				search.y - getFontRenderer().lineHeight - 2 + 34, 0xFFFFFF);

		if (isHovering(mirrorButton.x, mirrorButton.y, mirrorButton.getWidth(), mirrorButton.getHeight(), mouseX, mouseY)) {
			List<Component> textComponentList = new ArrayList<>();
			textComponentList.add(new TranslatableComponent("structurevisualizer.screen.mirror")
					.withStyle(ChatFormatting.GRAY));
			renderComponentTooltip(poseStack, textComponentList, mouseX, mouseY);
		}

		if (isHovering(rotationButton.x, rotationButton.y, rotationButton.getWidth(), rotationButton.getHeight(), mouseX, mouseY)) {
			List<Component> textComponentList = new ArrayList<>();
			textComponentList.add(new TranslatableComponent("structurevisualizer.screen.rotation")
					.withStyle(ChatFormatting.GRAY));
			renderComponentTooltip(poseStack, textComponentList, mouseX, mouseY);
		}

		if (isHovering(xPosField.x, xPosField.y, xPosField.getWidth(), xPosField.getHeight(), mouseX, mouseY)) {
			List<Component> textComponentList = new ArrayList<>();
			textComponentList.add(new TranslatableComponent("structurevisualizer.screen.x")
					.withStyle(ChatFormatting.GRAY));
			renderComponentTooltip(poseStack, textComponentList, mouseX, mouseY);
		}

		if (isHovering(yPosField.x, yPosField.y, yPosField.getWidth(), yPosField.getHeight(), mouseX, mouseY)) {
			List<Component> textComponentList = new ArrayList<>();
			textComponentList.add(new TranslatableComponent("structurevisualizer.screen.y")
					.withStyle(ChatFormatting.GRAY));
			renderComponentTooltip(poseStack, textComponentList, mouseX, mouseY);
		}

		if (isHovering(zPosField.x, zPosField.y, zPosField.getWidth(), zPosField.getHeight(), mouseX, mouseY)) {
			List<Component> textComponentList = new ArrayList<>();
			textComponentList.add(new TranslatableComponent("structurevisualizer.screen.z")
					.withStyle(ChatFormatting.GRAY));
			renderComponentTooltip(poseStack, textComponentList, mouseX, mouseY);

		}

		super.render(poseStack, mouseX, mouseY, partialTicks);
	}

	private boolean isHovering(Slot slot, double x, double y) {
		return this.isHovering(slot.x, slot.y, 16, 16, x, y);
	}

	protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
		return mouseX >= (double) (x - 1) && mouseX < (double) (x + width + 1) && mouseY >= (double) (y - 1) && mouseY < (double) (y + height + 1);
	}

	public Font getFontRenderer() {
		return font;
	}

	public void setSelected(StructureListWidget.ListEntry entry) {
		this.selected = entry == this.selected ? null : entry;
		updateCache();
	}

	private void updateCache() {
		if (selected == null) {
			this.loadButton.active = false;
		} else {
			this.loadButton.active = true;
		}
	}

	@Override
	public void resize(Minecraft mc, int width, int height) {
		String s = this.search.getValue();
		SortType sort = this.sortType;
		StructureListWidget.ListEntry selected = this.selected;
		this.init(mc, width, height);
		this.xPosField.setValue(s);
		this.yPosField.setValue(s);
		this.zPosField.setValue(s);
		this.search.setValue(s);
		this.selected = selected;
		if (!this.search.getValue().isEmpty())
			reloadStructures();
		if (sort != SortType.NORMAL)
			resortStructures(sort);
		updateCache();
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(null);
	}
}
