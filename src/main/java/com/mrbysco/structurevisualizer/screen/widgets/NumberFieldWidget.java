package com.mrbysco.structurevisualizer.screen.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.math.NumberUtils;

public class NumberFieldWidget extends EditBox {
	public NumberFieldWidget(Font font, int x, int y, int width, int height, Component defaultValue) {
		super(font, x, y, width, height, defaultValue);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void setValue(String textToWrite) {
		if (this.isNumeric(textToWrite)) super.setValue(textToWrite);
	}

	@Override
	public String getValue() {
		return (this.isNumeric(super.getValue()) ? super.getValue() : "0");
	}

	public double getDouble() {
		return NumberUtils.toDouble(super.getValue(), 0.0D);
	}

	@Override
	protected void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused) {
			this.setHighlightPos(this.getValue().length());
			this.moveCursorToEnd();
		}
	}

	private boolean isNumeric(String value) {
		return NumberUtils.isParsable(value) && getDouble() == (int)getDouble();
	}
}