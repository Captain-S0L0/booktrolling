package com.terriblefriends.booktrolling.mixins;

import com.terriblefriends.booktrolling.Config;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(at=@At("TAIL"),method="Lnet/minecraft/client/gui/screen/GameMenuScreen;initWidgets()V")
    private void booktrolling$addGuiButtons(CallbackInfo ci) {
        // Item Size Debug Button
        this.addDrawableChild(ButtonWidget.builder(getToggleText("Item Size Debug", Config.get().itemSizeDebug), (button) -> {
            Config.get().itemSizeDebug = !Config.get().itemSizeDebug;
            button.setMessage(getToggleText("Item Size Debug", Config.get().itemSizeDebug));
        }).dimensions(0, this.height-20, 98, 20).build());

        // Raw Sizes Button
        this.addDrawableChild(ButtonWidget.builder(getToggleText("Raw Sizes", Config.get().itemSizeDebugRawSizes), (button) -> {
            Config.get().itemSizeDebugRawSizes = !Config.get().itemSizeDebugRawSizes;
            button.setMessage(getToggleText("Raw Sizes", Config.get().itemSizeDebugRawSizes));
        }).dimensions(0, this.height-40, 98, 20).build());
    }

    @Unique
    private Text getToggleText(String name, boolean active) {
        return Text.literal(name).formatted(active ? Formatting.GREEN : Formatting.RED);
    }
}