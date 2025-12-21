package com.terriblefriends.booktrolling;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ToggleButton extends ButtonWidget {
    public ToggleButton(int x, int y, int width, int height, net.minecraft.text.Text message, PressAction onPress, boolean active) {
        super(x, y, width, height, message, onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
        this.active = active;
    }

    public boolean isInteractable() {
        return this.visible;
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.drawButton(context);
        this.drawLabel(context.getHoverListener(this, DrawContext.HoverType.NONE));
    }

    public void onClick(Click click, boolean doubled) {
        this.active = !this.active;
        super.onPress(click);
    }
}
