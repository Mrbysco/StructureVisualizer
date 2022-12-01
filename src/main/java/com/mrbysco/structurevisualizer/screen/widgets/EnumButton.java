package com.mrbysco.structurevisualizer.screen.widgets;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TextComponent;

public class EnumButton extends Button {
	private Enum value;

	public EnumButton(int x, int y, int width, int height, Enum defaultValue, OnPress pressedAction) {
		super(x, y, width, height, new TextComponent(defaultValue.toString()), pressedAction);
		this.value = defaultValue;
	}

	public Enum getValue() {
		return this.value;
	}

	public void setValue(Enum value) {
		this.value = value;
		this.setMessage(new TextComponent(value.toString()));
	}
}
