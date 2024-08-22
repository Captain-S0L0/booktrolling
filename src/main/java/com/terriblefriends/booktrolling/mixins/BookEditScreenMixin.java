package com.terriblefriends.booktrolling.mixins;

import com.terriblefriends.booktrolling.ToggleButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    @Shadow private boolean dirty;
    @Shadow @Final private List<String> pages;
    @Shadow protected abstract void finalizeBook(boolean bool);
    @Shadow protected abstract String getCurrentPageContent();
    @Shadow protected abstract void setPageContent(String newContent);
    @Shadow protected abstract String getClipboard();
    @Shadow protected abstract void setClipboard(String clipboard);
    @Shadow private String title;


    @Final @Shadow @Mutable private SelectionManager currentPageSelectionManager = new SelectionManager(this::getCurrentPageContent, this::setPageContent, this::getClipboard, this::setClipboard, (string) -> {
        return string.length() <= 1024;
    });;
    @Final @Shadow @Mutable private SelectionManager bookTitleSelectionManager = new SelectionManager(() -> this.title, title -> this.title = title, this::getClipboard, this::setClipboard, (string) -> {
        return string.length() <= 32;
    });
    @Unique
    private static boolean autoSign = false;
    @Unique
    private static boolean drop = false;
    @Unique
    private static boolean randomizeChars = false;

    @Unique
    private static final Random RANDOM = new Random();

    @Unique
    private static final Callable<Character> RANDOM_CHAR_PROVIDER = () -> (char)RANDOM.nextInt(2048, 65536);
    @Unique
    private static final Callable<Character> STATIC_CHAR_PROVIDER = () -> (char)2048;
    @Unique
    private static final String BRANDING = "BookTrolling™ by Captain_S0L0";


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

            this.dirty = true;

            if (signing) {
                this.title = titleGenerator.call();
            }

            this.finalizeBook(signing);

            if (drop) {
                float[] rotation = {this.client.player.getYaw(), this.client.player.getPitch()};
                this.client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(0.0f, 0.0f, false));
                this.client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.DROP_ITEM, BlockPos.ORIGIN, Direction.DOWN));
                this.client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(rotation[0], rotation[1], false));
            }

            this.client.setScreen(null);
        }
        catch (Exception e) {
            e.printStackTrace();
            this.client.inGameHud.getChatHud().addMessage(Text.literal("<BookTrolling> Error generating book! See logs!").formatted(Formatting.DARK_RED));
        }
    }

    @Inject(at=@At("HEAD"),method="Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;init()V")
    private void booktrolling$addGuiButtons(CallbackInfo ci) {
        int y = 0;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("1023"), (button) -> {
            this.sign(100, autoSign, drop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 1023; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("max unsigned"), (button) -> {
            this.sign(100, autoSign, drop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 1024; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("max signed"), (button) -> {
            this.sign(100, true, drop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 8192; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("paper"), (button) -> {
            this.sign(100, autoSign, drop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 320; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("unsaveable"), (button) -> {
            this.sign(1, true, drop, () -> "", () -> "123456789012345678901234567890123");
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("clear"), (button) -> {
            this.sign(1, false, drop, () -> {
                return "";
            }, () -> BRANDING);

        }).dimensions(0, y, 98, 20).build());
        y+=20;

        this.addDrawableChild(new ToggleButton(0, this.height-20, 98, 20, Text.literal("AutoSign"), () -> {
            autoSign = !autoSign;
        }, autoSign));
        this.addDrawableChild(new ToggleButton(0, this.height-40, 98, 20, Text.literal("RandomizeChars"), () -> {
            randomizeChars = !randomizeChars;
        }, randomizeChars));
        this.addDrawableChild(new ToggleButton(0, this.height-60, 98, 20, Text.literal("Drop"), () -> {
            drop = !drop;
        }, drop));
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
        return randomizeChars ? RANDOM_CHAR_PROVIDER : STATIC_CHAR_PROVIDER;
    }
}