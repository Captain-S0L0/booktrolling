package com.terriblefriends.booktrolling;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ToggleButton extends ButtonWidget {
    protected final PressAction onPress;

    public ToggleButton(int x, int y, int width, int height, Text message, PressAction onPress, boolean active) {
        super(x, y, width, height, message, null, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
        this.onPress = onPress;
        this.active = active;
    }

    public void onPress() {
        this.toggleActive();
        this.onPress.onPress();
    }

    public void toggleActive() {
        this.active = !this.active;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.visible) {
            if (this.isValidClickButton(button)) {
                boolean bl = this.clicked(mouseX, mouseY);
                if (bl) {
                    this.playDownSound(MinecraftClient.getInstance().getSoundManager());
                    this.onClick(mouseX, mouseY);
                    return true;
                }
            }

        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && mouseX >= (double) this.getX() && mouseY >= (double)this.getY() && mouseX < (double)(this.getX() + this.width) && mouseY < (double)(this.getY() + this.height);
    }

    protected boolean clicked(double mouseX, double mouseY) {
        return this.visible && mouseX >= (double)this.getX() && mouseY >= (double)this.getY() && mouseX < (double)(this.getX() + this.width) && mouseY < (double)(this.getY() + this.height);
    }

    public interface PressAction {
        void onPress();
    }
}
