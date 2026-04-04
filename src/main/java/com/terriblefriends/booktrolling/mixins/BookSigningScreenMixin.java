package com.terriblefriends.booktrolling.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

@Mixin(BookSignScreen.class)
public abstract class BookSigningScreenMixin extends Screen {

    @Shadow private EditBox titleBox;
    @Shadow @Final private Player owner;
    @Shadow @Final private List<String> pages;
    @Shadow @Final private InteractionHand hand;

    protected BookSigningScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void booktrolling$initBookSigningScreen(CallbackInfo ci) {
        this.titleBox.setMaxLength(32); // set max title length to the max the component allows
    }

    @Override
    public void onClose() {
        // update contents but don't sign if screen was closed
        int i = this.hand == InteractionHand.MAIN_HAND ? this.owner.getInventory().getSelectedSlot() : 40;
        this.minecraft.getConnection().send(new ServerboundEditBookPacket(i, this.pages, Optional.empty()));
        super.onClose();
    }
}
