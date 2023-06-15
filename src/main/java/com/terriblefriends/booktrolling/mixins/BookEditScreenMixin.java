package com.terriblefriends.booktrolling.mixins;

import com.terriblefriends.booktrolling.ToggleButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    @Shadow private Hand hand;
    @Shadow @Final private PlayerEntity player;
    @Shadow protected abstract void finalizeBook(boolean bool);
    @Shadow protected abstract String getCurrentPageContent();
    @Shadow protected abstract void setPageContent(String newContent);
    @Shadow protected abstract String getClipboard();
    @Shadow protected abstract void setClipboard(String clipboard);
    @Shadow private String title;


    @Final @Shadow @Mutable private SelectionManager currentPageSelectionManager = new SelectionManager(this::getCurrentPageContent, this::setPageContent, this::getClipboard, this::setClipboard, (string) -> {
        return MinecraftClient.getInstance().isInSingleplayer() ? string.length() <= 32767 : string.length() <= 8192;
    });;
    @Final @Shadow @Mutable private SelectionManager bookTitleSelectionManager = new SelectionManager(() -> this.title, title -> this.title = title, this::getClipboard, this::setClipboard, (string) -> {
        return string.length() <= 128;
    });
    private boolean overloading = false;
    private boolean use3ByteChars = false;
    private boolean use4ByteChars = false;
    private boolean useOneChar = false;
    private boolean clear = false;
    private int overloadAmount = 0;
    private static boolean sign = false;



    protected BookEditScreenMixin(Text title) {
        super(title);
    }



    @Inject(at=@At("HEAD"),method="finalizeBook", cancellable = true)
    private void finalizeBookMixin(boolean signBook, CallbackInfo ci) {
        if (overloading) {
            Random rand = new Random();
            List<String> pages = new ArrayList<>();
            StringBuilder stringBuilder;
            if (!clear) {
                for (int page = 0; page < 100; page++) {
                    stringBuilder = new StringBuilder();

                    if (use4ByteChars) {
                        for (int characters = 0; characters < overloadAmount; characters++) {
                            stringBuilder.append(Character.toChars(rand.nextInt(65536, 1114111)));
                        }
                    }
                    if (use3ByteChars) {
                        for (int characters = 0; characters < overloadAmount; characters++) {
                            stringBuilder.append(Character.toChars(rand.nextInt(2048, 65536)));
                        }
                    }
                    if (useOneChar) {
                        stringBuilder.append(String.valueOf((char) 2048).repeat(Math.max(0, overloadAmount)));
                    }

                    String currentPage = stringBuilder.toString();
                    //System.out.println(currentPage.length());
                    pages.add(currentPage);
                }
            }

            Optional title = Optional.empty();

            if (sign) {
                title = Optional.of("Book");
            }

            int i = this.hand == Hand.MAIN_HAND ? this.player.getInventory().selectedSlot : 40;
            client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(i, pages, title));

            ci.cancel();
        }
    }

    @Inject(at=@At("HEAD"),method="Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;init()V")
    private void addNewButtons(CallbackInfo ci) {
        int y = 0;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("vanilla"), (button) -> {
            this.overloading = true;
            this.use3ByteChars = true;
            this.overloadAmount = 1023;
            this.finalizeBook(false);
            this.client.setScreen(null);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("singleplayer"), (button) -> {
            this.overloading = true;
            this.use3ByteChars = true;
            this.overloadAmount = 21845;//21837 if signing
            this.finalizeBook(false);
            this.client.setScreen(null);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("multiplayer"), (button) -> {
            this.overloading = true;
            this.use3ByteChars = true;
            this.overloadAmount = 8192;
            this.finalizeBook(false);
            this.client.setScreen(null);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("paper"), (button) -> {
            this.overloading = true;
            this.use3ByteChars = true;
            this.overloadAmount = 320;
            this.finalizeBook(false);
            this.client.setScreen(null);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("paper bookban"), (button) -> {
            this.overloading = true;
            this.useOneChar = true;
            this.overloadAmount = 320;
            this.finalizeBook(false);
            this.client.setScreen(null);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("bookban 1"), (button) -> {
            this.overloading = true;
            this.useOneChar = true;
            this.overloadAmount = 8192;
            this.finalizeBook(false);
            this.client.setScreen(null);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("bookban 2"), (button) -> {
            this.overloading = true;
            this.useOneChar = true;
            this.overloadAmount = 2246;
            this.finalizeBook(false);
            this.client.setScreen(null);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("clear"), (button) -> {
            this.overloading = true;
            this.clear = true;
            this.finalizeBook(false);
            this.client.setScreen(null);
        }).dimensions(0, y, 98, 20).build());
        y+=20;

        this.addDrawableChild(new ToggleButton(0, this.height-20, 98, 20, Text.literal("AutoSign"), () -> {
            sign = !sign;
        }, sign));
    }
}