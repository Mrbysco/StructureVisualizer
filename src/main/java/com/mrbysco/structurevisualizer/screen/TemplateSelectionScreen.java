package com.mrbysco.structurevisualizer.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mrbysco.structurevisualizer.render.RenderHandler;
import com.mrbysco.structurevisualizer.util.StructureHelper;
import com.mrbysco.structurevisualizer.util.TemplateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.fml.loading.StringUtils;

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

		ITextComponent getButtonText() {
			return new TranslationTextComponent("structurevisualizer.screen.search." + name().toLowerCase(Locale.ROOT));
		}
	}

	private static final int PADDING = 6;

	private StructureListWidget structureList;
	private StructureListWidget.ListEntry selected = null;
	private int listWidth;
	private List<String> structures;
	private final List<String> unsortedStructures;
	private Button unloadButton, loadButton;

	private int buttonMargin = 1;
	private int numButtons = SortType.values().length;
	private String lastFilterText = "";

	private TextFieldWidget search;
	private NumberFieldWidget xPosField;
	private NumberFieldWidget yPosField;
	private NumberFieldWidget zPosField;

	private boolean sorted = false;
	private SortType sortType = SortType.NORMAL;

	public TemplateSelectionScreen() {
		super(new TranslationTextComponent("structurevisualizer.screen.selection.title"));
		this.structures = Collections.unmodifiableList(StructureHelper.getStructures());
		this.unsortedStructures = Collections.unmodifiableList(this.structures);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		int centerWidth = this.width / 2;

		for (String structure : structures) {
			listWidth = Math.max(listWidth,getFontRenderer().width(structure) + 10);
		}
		listWidth = Math.max(Math.min(listWidth, width/3), 200);
		listWidth += listWidth % numButtons != 0 ? (numButtons - listWidth % numButtons) : 0;

		int structureWidth = this.width - this.listWidth - (PADDING * 3);
		int doneButtonWidth = Math.min(structureWidth, 200);
		int y = this.height - 20 - PADDING;
		this.addButton(new Button(centerWidth - (doneButtonWidth / 2) + PADDING, y, doneButtonWidth, 20,
				new TranslationTextComponent("gui.done"), b -> TemplateSelectionScreen.this.onClose()));

		y -= 18 + PADDING;
		this.addButton(this.unloadButton = new Button(centerWidth - (doneButtonWidth / 2) + PADDING, y, this.listWidth, 20,
				new TranslationTextComponent("structurevisualizer.screen.selection.unload"), b -> {
			RenderHandler.cachedTemplateName = "";
			RenderHandler.cachedTemplate = null;
			RenderHandler.blocksToRender.clear();
			minecraft.player.sendMessage(new TranslationTextComponent("structurevisualizer.unload"), Util.NIL_UUID);
		}));
		y -= 18 + PADDING;
		this.addButton(this.loadButton = new Button(centerWidth - (doneButtonWidth / 2) + PADDING, y, this.listWidth, 20,
				new TranslationTextComponent("structurevisualizer.screen.selection.load"), b -> {
			if(selected != null) {
				String selectedTemplate = selected.getStructureName();
				Template template = StructureHelper.loadFromDirectory(selectedTemplate);
				if(template != null) {
					RenderHandler.cachedTemplateName = selectedTemplate.toLowerCase(Locale.ROOT);
					RenderHandler.cachedTemplate = template;
					RenderHandler.renderBuffer = null;
					BlockPos pos = BlockPos.ZERO.offset(xPosField.getDouble() - (template.size.getX() / 2), yPosField.getDouble(), zPosField.getDouble() - (template.size.getZ() / 2));
					TemplateHelper.setRenderInfo(template, minecraft.level, pos, pos, TemplateHelper.PLACEMENT_SETTINGS);
					minecraft.player.sendMessage(new TranslationTextComponent("structurevisualizer.load", selectedTemplate).withStyle(TextFormatting.YELLOW), Util.NIL_UUID);
				} else {
					RenderHandler.blocksToRender.clear();
					RenderHandler.renderBuffer = null;
					minecraft.player.sendMessage(new TranslationTextComponent("structurevisualizer.load.fail", selectedTemplate).withStyle(TextFormatting.RED), Util.NIL_UUID);
				}
			}
		}));

		y -= 14 + PADDING + 41;
		search = new TextFieldWidget(getFontRenderer(), centerWidth - listWidth / 2 + PADDING + 1, y, listWidth - 2, 14,
				new TranslationTextComponent("structurevisualizer.screen.search"));

		int fullButtonHeight = PADDING + 20 + PADDING;
		this.structureList = new StructureListWidget(this, width, fullButtonHeight, search.y - getFontRenderer().lineHeight - PADDING);
		this.structureList.setLeftPos(0);

		children.add(search);
		children.add(structureList);
		search.setFocus(false);
		search.setCanLoseFocus(true);

		BlockPos pos = BlockPos.ZERO;
		if(minecraft.player != null) {
			pos = minecraft.player.blockPosition();
		}

		y += 36;
		xPosField = new NumberFieldWidget(getFontRenderer(), search.x, y, 60, 14,
				new TranslationTextComponent("structurevisualizer.screen.x"));
		xPosField.setValue(String.valueOf(pos.getX()));
		xPosField.setMaxLength(10);

		children.add(xPosField);
		xPosField.setFocus(false);
		xPosField.setCanLoseFocus(true);

		yPosField = new NumberFieldWidget(getFontRenderer(), search.x + (search.getWidth() / 3) + 4, y, 60, 14,
				new TranslationTextComponent("structurevisualizer.screen.y"));
		yPosField.setValue(String.valueOf(pos.getY()));
		yPosField.setMaxLength(10);

		children.add(yPosField);
		yPosField.setFocus(false);
		yPosField.setCanLoseFocus(true);

		zPosField = new NumberFieldWidget(getFontRenderer(), search.x + search.getWidth() - 60, y, 60, 14,
				new TranslationTextComponent("structurevisualizer.screen.z"));
		zPosField.setValue(String.valueOf(pos.getZ()));
		zPosField.setMaxLength(10);

		children.add(zPosField);
		zPosField.setFocus(false);
		zPosField.setCanLoseFocus(true);

		final int width = listWidth / numButtons;
		int x = centerWidth + PADDING - width;
		addButton(SortType.A_TO_Z.button = new Button(x, PADDING, width - buttonMargin, 20, SortType.A_TO_Z.getButtonText(), b -> resortStructures(SortType.A_TO_Z)));
		x += width + buttonMargin;
		addButton(SortType.Z_TO_A.button = new Button(x, PADDING, width - buttonMargin, 20, SortType.Z_TO_A.getButtonText(), b -> resortStructures(SortType.Z_TO_A)));
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
			if(sortType == SortType.A_TO_Z) {
				Collections.sort(structures);
			} else if(sortType == SortType.Z_TO_A) {
				Collections.sort(structures, Collections.reverseOrder());
			}
			structureList.refreshList();
			if (selected != null) {
				selected = structureList.children().stream().filter(e -> e == selected).findFirst().orElse(null);
				updateCache();
			}
			sorted = true;
		}
	}

	public <T extends ExtendedList.AbstractListEntry<T>> void buildStructureList(Consumer<T> ListViewConsumer, Function<String, T> newEntry) {
		structures.forEach(mod->ListViewConsumer.accept(newEntry.apply(mod)));
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
	public void render(MatrixStack poseStack, int mouseX, int mouseY, float partialTicks) {
		this.structureList.render(poseStack, mouseX, mouseY, partialTicks);

		ITextComponent text = new TranslationTextComponent("structurevisualizer.screen.search");
		drawCenteredString(poseStack, getFontRenderer(), text, this.width / 2 + PADDING,
				search.y - getFontRenderer().lineHeight - 2, 0xFFFFFF);

		ITextComponent posText = new TranslationTextComponent("structurevisualizer.screen.position");
		drawCenteredString(poseStack, getFontRenderer(), posText, this.width / 2 + PADDING,
				search.y - getFontRenderer().lineHeight - 2 + 34, 0xFFFFFF);

		this.xPosField.render(poseStack, mouseX , mouseY, partialTicks);
		this.yPosField.render(poseStack, mouseX , mouseY, partialTicks);
		this.zPosField.render(poseStack, mouseX , mouseY, partialTicks);
		this.search.render(poseStack, mouseX , mouseY, partialTicks);
		super.render(poseStack, mouseX, mouseY, partialTicks);
	}

	public FontRenderer getFontRenderer() {
		return font;
	}

	public void setSelected(StructureListWidget.ListEntry entry) {
		this.selected = entry == this.selected ? null : entry;
		updateCache();
	}

	private void updateCache() {
		if (selected == null) {
			this.loadButton.active = false;
			return;
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
