package com.terriblefriends.booktrolling.mixins;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LecternScreen.class)
public abstract class LecternScreenMixin extends Screen {
    protected LecternScreenMixin(Component title) {
        super(title);
    }

    @Inject(method="init", at=@At("HEAD"))
    private void booktrolling$addGuiButtons(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.literal("Shadow Book"), (button) -> {
            if (this.minecraft == null || this.minecraft.player == null || this.minecraft.gameMode == null) {
                return;
            }

            int slotToFill = this.minecraft.player.getInventory().getFreeSlot();
            if (slotToFill != -1) {
                this.minecraft.gameMode.handleInventoryMouseClick(this.minecraft.player.containerMenu.containerId, 0, slotToFill, ClickType.SWAP, this.minecraft.player);
                this.minecraft.gui.getChat().addMessage(Component.literal("<BookTrolling> Shadowed book...").withStyle(ChatFormatting.RED));
            }
            else {
                this.minecraft.gui.getChat().addMessage(Component.literal("<BookTrolling> Error! No room in inventory!").withStyle(ChatFormatting.RED));
            }
        }).bounds(this.width - 98, this.height - 40, 98, 20).build());
    }
}
