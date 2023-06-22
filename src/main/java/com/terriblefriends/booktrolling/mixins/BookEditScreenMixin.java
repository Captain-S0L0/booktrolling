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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        return MinecraftClient.getInstance().isInSingleplayer() ? string.length() <= 65535 : string.length() <= 128;
    });
    private boolean injecting = false;
    private boolean use3ByteChars = false;
    private boolean use4ByteChars = false;
    private boolean clear = false;
    private int pages = 0;
    private int overloadAmount = 0;
    private static boolean sign = false;
    private static boolean randomizeChars = true;

    protected BookEditScreenMixin(Text title) {
        super(title);
    }


    @Inject(at=@At("HEAD"),method="finalizeBook", cancellable = true)
    private void booktrolling$injectBookPayload(boolean signBook, CallbackInfo ci) {
        if (injecting) {
            Random rand = new Random();
            List<String> pages = new ArrayList<>();
            StringBuilder stringBuilder;
            if (!clear) {
                for (int page = 0; page < this.pages; page++) {
                    stringBuilder = new StringBuilder();

                    if (randomizeChars) {
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
                    }
                    else {
                        if (use4ByteChars) {
                            stringBuilder.append(String.valueOf((char) 65536).repeat(Math.max(0, overloadAmount)));
                        }
                        if (use3ByteChars) {
                            stringBuilder.append(String.valueOf((char) 2048).repeat(Math.max(0, overloadAmount)));
                        }
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
    private void booktrolling$addGuiButtons(CallbackInfo ci) {
        int y = 0;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("1023"), (button) -> {
            this.injecting = true;
            this.pages = 100;
            this.use3ByteChars = true;
            this.overloadAmount = 1023;
            this.finalizeBook(false);
            this.client.setScreen(null);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("singleplayer"), (button) -> {
            this.injecting = true;
            this.pages = 100;
            this.use3ByteChars = true;
            this.overloadAmount = 21837;//21837 if signing, 21845 if not
            this.finalizeBook(false);
            this.client.setScreen(null);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("multiplayer"), (button) -> {
            this.injecting = true;
            this.pages = 100;
            this.use3ByteChars = true;
            this.overloadAmount = 8192;
            this.finalizeBook(false);
            this.client.setScreen(null);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("paper"), (button) -> {
            this.injecting = true;
            this.pages = 100;
            this.use3ByteChars = true;
            this.overloadAmount = 320;
            this.finalizeBook(false);
            this.client.setScreen(null);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("clear"), (button) -> {
            this.injecting = true;
            this.clear = true;
            this.finalizeBook(false);
            this.client.setScreen(null);
        }).dimensions(0, y, 98, 20).build());
        y+=20;

        this.addDrawableChild(new ToggleButton(0, this.height-20, 98, 20, Text.literal("AutoSign"), () -> {
            sign = !sign;
        }, sign));
        this.addDrawableChild(new ToggleButton(0, this.height-40, 98, 20, Text.literal("RandomizeChars"), () -> {
            randomizeChars = !randomizeChars;
        }, randomizeChars));
    }

    @Inject(at=@At("HEAD"),method="keyPressedSignMode",cancellable = true)
    private void booktrolling$allowCopyPasteTitle(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        boolean returnvalue;
        if (Screen.isSelectAll(keyCode)) {
            this.bookTitleSelectionManager.selectAll();
            returnvalue = true;
        } else if (Screen.isCopy(keyCode)) {
            this.bookTitleSelectionManager.copy();
            returnvalue = true;
        } else if (Screen.isPaste(keyCode)) {
            this.bookTitleSelectionManager.paste();
            returnvalue = true;
        } else if (Screen.isCut(keyCode)) {
            this.bookTitleSelectionManager.cut();
            returnvalue = true;
        } else {
            SelectionManager.SelectionType selectionType = Screen.hasControlDown() ? SelectionManager.SelectionType.WORD : SelectionManager.SelectionType.CHARACTER;
            switch (keyCode) {
                case 257, 335 -> {
                    this.bookTitleSelectionManager.insert("\n");
                    returnvalue = true;
                }
                case 259 -> {
                    this.bookTitleSelectionManager.delete(-1, selectionType);
                    returnvalue = true;
                }
                case 261 -> {
                    this.bookTitleSelectionManager.delete(1, selectionType);
                    returnvalue = true;
                }
                case 262 -> {
                    this.bookTitleSelectionManager.moveCursor(1, Screen.hasShiftDown(), selectionType);
                    returnvalue = true;
                }
                case 263 -> {
                    this.bookTitleSelectionManager.moveCursor(-1, Screen.hasShiftDown(), selectionType);
                    returnvalue = true;
                }
                default -> returnvalue = false;
            }
        }
        cir.setReturnValue(returnvalue);
        cir.cancel();
    }
}