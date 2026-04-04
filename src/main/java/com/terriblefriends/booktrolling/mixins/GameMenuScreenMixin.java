package com.terriblefriends.booktrolling.mixins;

import com.terriblefriends.booktrolling.Config;
import com.terriblefriends.booktrolling.ToggleButton;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Component title) {
        super(title);
    }

    @Inject(at=@At("TAIL"),method="createPauseMenu()V")
    private void booktrolling$addGuiButtons(CallbackInfo ci) {
        this.addRenderableWidget(new ToggleButton(0, this.height-20, 98, 20, Component.literal("Item Size Debug"), (button) -> {
            Config.get().itemSizeDebug = !Config.get().itemSizeDebug;
        }, Config.get().itemSizeDebug));
        this.addRenderableWidget(new ToggleButton(0, this.height-40, 98, 20, Component.literal("Raw Sizes"), (button) -> {
            Config.get().itemSizeDebugRawSizes = !Config.get().itemSizeDebugRawSizes;
        }, Config.get().itemSizeDebugRawSizes));
    }
}
