package com.mrbysco.structurevisualizer.screen.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrbysco.structurevisualizer.screen.TemplateSelectionScreen;
import com.mrbysco.structurevisualizer.screen.widgets.StructureListWidget.ListEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextComponent;

public class StructureListWidget extends ObjectSelectionList<ListEntry> {
	private final int listWidth;

	private final TemplateSelectionScreen parent;

	public StructureListWidget(TemplateSelectionScreen parent, int listWidth, int top, int bottom) {
		super(parent.getMinecraft(), listWidth, parent.height, top, bottom, parent.getFontRenderer().lineHeight * 2 + 8);
		this.parent = parent;
		this.listWidth = listWidth;
		this.refreshList();
	}

	@Override
	protected int getScrollbarPosition() {
		return this.listWidth;
	}

	@Override
	public int getRowWidth() {
		return this.listWidth;
	}

	public void refreshList() {
		this.clearEntries();
		parent.buildStructureList(this::addEntry, mod -> new ListEntry(mod, this.parent));
	}

	@Override
	protected void renderBackground(PoseStack mStack) {
		this.parent.renderBackground(mStack);
	}

	public class ListEntry extends ObjectSelectionList.Entry<ListEntry> {
		private final String structureName;
		private final TemplateSelectionScreen parent;

		ListEntry(String name, TemplateSelectionScreen parent) {
			this.structureName = name;
			this.parent = parent;
		}

		@Override
		public void render(PoseStack mStack, int entryIdx, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean p_194999_5_, float partialTicks) {
			Component name = new TextComponent(structureName);
			Font font = this.parent.getFontRenderer();
			font.draw(mStack, Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(name, listWidth)))
					, (this.parent.width / 2) - (font.width(structureName) / 2) + 3, top + 6, 0xFFFFFF);
		}

		@Override
		public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
			parent.setSelected(this);
			StructureListWidget.this.setSelected(this);
			return false;
		}

		public String getStructureName() {
			return structureName;
		}

		@Override
		public Component getNarration() {
			return new TextComponent(structureName);
		}
	}
}
