package com.terriblefriends.booktrolling.mixins;

import com.terriblefriends.booktrolling.Booktrolling;
import com.terriblefriends.booktrolling.ToggleButton;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
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
        this.addDrawableChild(new ToggleButton(0, this.height-20, 98, 20, Text.literal("Item Size Debug"), () -> {
            Booktrolling.itemSizeDebug = !Booktrolling.itemSizeDebug;
        }, Booktrolling.itemSizeDebug));
    }
}
