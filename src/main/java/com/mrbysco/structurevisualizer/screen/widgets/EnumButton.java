package com.mrbysco.structurevisualizer.screen.widgets;

import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;

public class EnumButton extends Button {
	private Enum value;

	public EnumButton(int x, int y, int width, int height, Enum defaultValue, IPressable pressedAction) {
		super(x, y, width, height, new StringTextComponent(defaultValue.toString()), pressedAction);
		this.value = defaultValue;
	}

	public Enum getValue() {
		return this.value;
	}

	public void setValue(Enum value) {
		this.value = value;
		this.setMessage(new StringTextComponent(value.toString()));
	}
}
