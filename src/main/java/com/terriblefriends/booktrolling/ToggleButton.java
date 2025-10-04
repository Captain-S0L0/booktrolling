package com.terriblefriends.booktrolling;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ToggleButton extends ButtonWidget {
    public ToggleButton(int x, int y, int width, int height, Text message, PressAction onPress, boolean active) {
        super(x, y, width, height, message, onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
        this.active = active;
    }

    public boolean isInteractable() {
        return this.visible;
    }

    public void onClick(Click click, boolean doubled) {
        this.active = !this.active;
        super.onPress(click);
    }
}
