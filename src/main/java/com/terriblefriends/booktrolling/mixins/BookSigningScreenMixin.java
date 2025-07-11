package com.terriblefriends.booktrolling.mixins;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookSigningScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(BookSigningScreen.class)
public abstract class BookSigningScreenMixin extends Screen {

    @Shadow private TextFieldWidget bookTitleTextField;
    @Shadow @Final private PlayerEntity player;
    @Shadow @Final private List<String> pages;
    @Shadow @Final private Hand hand;

    protected BookSigningScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void booktrolling$initBookSigningScreen(CallbackInfo ci) {
        this.bookTitleTextField.setMaxLength(32); // set max title length to the max the component allows
    }

    @Override
    public void close() {
        // update contents but don't sign if screen was closed
        int i = this.hand == Hand.MAIN_HAND ? this.player.getInventory().getSelectedSlot() : 40;
        this.client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(i, this.pages, Optional.empty()));
        super.close();
    }
}
