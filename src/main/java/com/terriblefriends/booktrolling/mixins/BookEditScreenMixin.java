package com.terriblefriends.booktrolling.mixins;

import com.mojang.logging.LogUtils;
import com.terriblefriends.booktrolling.Config;
import com.terriblefriends.booktrolling.ToggleButton;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    // static finals, magic numbers, constants, etc
    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();
    @Unique
    private static final Random RANDOM = new Random();
    @Unique
    private static final Callable<Character> RANDOM_CHAR_PROVIDER = () -> (char)RANDOM.nextInt(2048, 65536);
    @Unique
    private static final Callable<Character> STATIC_CHAR_PROVIDER = () -> (char)2048;
    @Unique
    private static final String BRANDING = "BookTrolling™ by Captain_S0L0";

    // accessors
    @Shadow private boolean dirty;
    @Shadow @Final private List<String> pages;
    @Shadow private String title;
    @Shadow private int currentPage;
    @Shadow private boolean signing;

    @Shadow protected abstract void finalizeBook(boolean bool);
    @Shadow protected abstract String getCurrentPageContent();
    @Shadow protected abstract void setPageContent(String newContent);
    @Shadow protected abstract String getClipboard();
    @Shadow protected abstract void setClipboard(String clipboard);
    @Shadow protected abstract void invalidatePageContent();
    @Shadow protected abstract void updateButtons();

    @Final @Shadow @Mutable private SelectionManager currentPageSelectionManager = new SelectionManager(this::getCurrentPageContent, this::setPageContent, this::getClipboard, this::setClipboard, (string) -> {
        return string.length() <= 1024;
    });;
    @Final @Shadow @Mutable private SelectionManager bookTitleSelectionManager = new SelectionManager(() -> this.title, title -> this.title = title, this::getClipboard, this::setClipboard, (string) -> {
        return string.length() <= 32;
    });

    // vars
    @Unique
    private final List<ClickableWidget> customButtons = new ArrayList<>();

    protected BookEditScreenMixin(Text title) {
        super(title);
    }

    @Unique
    private void sign(int pageCount, boolean signing, boolean drop, Callable<String> pageGenerator, Callable<String> titleGenerator) {
        try {
            this.pages.clear();

            for (int i = 0; i < pageCount; i++) {
                this.pages.add(pageGenerator.call());
            }

            this.currentPage = 0;
            this.dirty = true;
            this.invalidatePageContent();
            this.updateButtons();

            if (signing) {
                this.title = titleGenerator.call();
            }

            if (drop || signing) {
                this.finalizeBook(signing);
                this.client.setScreen(null);
            }

            if (drop) {
                this.client.player.dropSelectedItem(true);
            }
        }
        catch (Exception e) {
            LOGGER.error("BookTrolling failed to generate a book!", e);
            this.client.inGameHud.getChatHud().addMessage(Text.literal("<BookTrolling> Error generating book! See logs!").formatted(Formatting.DARK_RED));
        }
    }

    @Override
    public void close() {
        this.finalizeBook(false);
        super.close();
    }

    @Inject(at=@At("HEAD"),method="Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;init()V")
    private void booktrolling$addGuiButtons(CallbackInfo ci) {
        int y = 0;
        this.customButtons.add(this.addDrawableChild(ButtonWidget.builder(Text.literal("1023"), (button) -> {
            this.sign(100, Config.get().autoSign, Config.get().autoDrop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 1023; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).dimensions(0, y, 98, 20).build()));
        y+=20;
        this.customButtons.add(this.addDrawableChild(ButtonWidget.builder(Text.literal("Max"), (button) -> {
            this.sign(100, Config.get().autoSign, Config.get().autoDrop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 1024; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).dimensions(0, y, 98, 20).build()));
        y+=20;
        /*this.customButtons.add(this.addDrawableChild(ButtonWidget.builder(Text.literal("max signed"), (button) -> {
            this.sign(100, true, drop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 8192; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).dimensions(0, y, 98, 20).build()));
        y+=20;*/
        this.customButtons.add(this.addDrawableChild(ButtonWidget.builder(Text.literal("Paper"), (button) -> {
            this.sign(100, Config.get().autoSign, Config.get().autoDrop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 320; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).dimensions(0, y, 98, 20).build()));
        y+=20;
        /*this.customButtons.add(this.addDrawableChild(ButtonWidget.builder(Text.literal("unsaveable"), (button) -> {
            this.sign(1, true, drop, () -> "", () -> "123456789012345678901234567890123");
        }).dimensions(0, y, 98, 20).build()));
        y+=20;*/
        this.customButtons.add(this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), (button) -> {
            this.sign(1, false, Config.get().autoDrop, () -> {
                return "";
            }, () -> BRANDING);

        }).dimensions(0, y, 98, 20).build()));
        y+=20;

        this.customButtons.add(this.addDrawableChild(new ToggleButton(0, this.height-20, 98, 20, Text.literal("Auto Sign"), () -> {
            Config.get().autoSign = !Config.get().autoSign;
        }, Config.get().autoSign)));
        this.customButtons.add(this.addDrawableChild(new ToggleButton(0, this.height-40, 98, 20, Text.literal("Randomize Chars"), () -> {
            Config.get().randomizeCharacters = !Config.get().randomizeCharacters;
        }, Config.get().randomizeCharacters)));
        this.customButtons.add(this.addDrawableChild(new ToggleButton(0, this.height-60, 98, 20, Text.literal("Auto Drop"), () -> {
            Config.get().autoDrop = !Config.get().autoDrop;
        }, Config.get().autoDrop)));
    }

    @Inject(method = "updateButtons", at = @At("TAIL"))
    private void booktrolling$updateButtons(CallbackInfo ci) {
        for (ClickableWidget w : this.customButtons) {
            w.visible = !this.signing;
        }
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

    @Unique
    private static Callable<Character> getCharProvider() {
        return Config.get().randomizeCharacters ? RANDOM_CHAR_PROVIDER : STATIC_CHAR_PROVIDER;
    }
}