package com.terriblefriends.booktrolling.mixins;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.LecternScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LecternScreen.class)
public abstract class LecternScreenMixin extends Screen {
    protected LecternScreenMixin(Text title) {
        super(title);
    }

    @Inject(method="init", at=@At("HEAD"))
    private void booktrolling$addGuiButtons(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Shadow Book"), (button) -> {
            if (this.client == null || this.client.player == null || this.client.interactionManager == null) {
                return;
            }

            int slotToFill = this.client.player.getInventory().getEmptySlot();
            if (slotToFill != -1) {
                this.client.interactionManager.clickSlot(this.client.player.currentScreenHandler.syncId, 0, slotToFill, SlotActionType.SWAP, this.client.player);
                this.client.inGameHud.getChatHud().addMessage(Text.literal("<BookTrolling> Shadowed book...").formatted(Formatting.RED));
            }
            else {
                this.client.inGameHud.getChatHud().addMessage(Text.literal("<BookTrolling> Error! No room in inventory!").formatted(Formatting.RED));
            }
        }).dimensions(this.width - 98, this.height - 40, 98, 20).build());
    }
}
