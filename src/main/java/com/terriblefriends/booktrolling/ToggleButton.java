package com.terriblefriends.booktrolling;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;

public class ToggleButton extends Button {
    public ToggleButton(int x, int y, int width, int height, net.minecraft.network.chat.Component message, OnPress onPress, boolean active) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
        this.active = active;
    }

    public boolean isActive() {
        return this.visible;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        this.extractDefaultSprite(context);
        this.extractDefaultLabel(context.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }

    public void onClick(MouseButtonEvent click, boolean doubled) {
        this.active = !this.active;
        super.onPress(click);
    }
}
