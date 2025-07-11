package com.terriblefriends.booktrolling.mixins;

import com.mojang.logging.LogUtils;
import com.terriblefriends.booktrolling.Config;
import com.terriblefriends.booktrolling.ToggleButton;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
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
    @Shadow @Final private PlayerEntity player;
    @Shadow @Final private Hand hand;
    @Shadow @Final private List<String> pages;
    @Shadow private int currentPage;
    @Shadow private EditBoxWidget editBox;

    @Shadow protected abstract void finalizeBook();
    @Shadow protected abstract void updatePage();

    protected BookEditScreenMixin(Text title) {
        super(title);
    }

    @Override
    public void close() {
        // update contents but don't sign if screen was closed
        this.finalizeBook();
        super.close();
    }

    @Inject(method="init", at=@At("TAIL"))
    private void booktrolling$initBookEditScreen(CallbackInfo ci) {
        this.editBox.setMaxLines(Integer.MAX_VALUE); // disable checking character width

        int y = 0;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("1023"), (button) -> {
            this.createBook(100, Config.get().autoSign, Config.get().autoDrop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 1023; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Max"), (button) -> {
            this.createBook(100, Config.get().autoSign, Config.get().autoDrop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 1024; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        /*this.addDrawableChild(ButtonWidget.builder(Text.literal("max signed"), (button) -> {
            this.sign(100, true, drop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 8192; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).dimensions(0, y, 98, 20).build());
        y+=20;*/
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Paper"), (button) -> {
            this.createBook(100, Config.get().autoSign, Config.get().autoDrop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 320; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).dimensions(0, y, 98, 20).build());
        y+=20;
        /*this.addDrawableChild(ButtonWidget.builder(Text.literal("unsaveable"), (button) -> {
            this.sign(1, true, drop, () -> "", () -> "123456789012345678901234567890123");
        }).dimensions(0, y, 98, 20).build());
        y+=20;*/
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), (button) -> {
            this.createBook(1, false, Config.get().autoDrop, () -> "", () -> BRANDING);
        }).dimensions(0, y, 98, 20).build());
        y+=20;

        this.addDrawableChild(new ToggleButton(0, this.height-20, 98, 20, Text.literal("Auto Sign"), () -> {
            Config.get().autoSign = !Config.get().autoSign;
        }, Config.get().autoSign));
        this.addDrawableChild(new ToggleButton(0, this.height-40, 98, 20, Text.literal("Randomize Chars"), () -> {
            Config.get().randomizeCharacters = !Config.get().randomizeCharacters;
        }, Config.get().randomizeCharacters));
        this.addDrawableChild(new ToggleButton(0, this.height-60, 98, 20, Text.literal("Auto Drop"), () -> {
            Config.get().autoDrop = !Config.get().autoDrop;
        }, Config.get().autoDrop));
    }

    @Unique
    private void createBook(int pageCount, boolean signing, boolean drop, Callable<String> pageGenerator, Callable<String> titleGenerator) {
        try {
            this.pages.clear();

            for (int i = 0; i < pageCount; i++) {
                this.pages.add(pageGenerator.call());
            }

            this.currentPage = 0;
            this.updatePage();

            Optional<String> title = Optional.empty();

            if (signing) {
                title = Optional.of(titleGenerator.call());
            }

            if (drop || signing) {
                int i = this.hand == Hand.MAIN_HAND ? this.player.getInventory().getSelectedSlot() : 40;
                this.client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(i, this.pages, title));
                this.client.setScreen(null);
            }

            if (drop) {
                this.client.player.dropSelectedItem(true);
            }
        }
        catch (Exception e) {
            LOGGER.error("BookTrolling failed to generate book!", e);
            this.client.inGameHud.getChatHud().addMessage(Text.literal("<BookTrolling> Error generating book! See logs!").formatted(Formatting.DARK_RED));
        }
    }

    @Unique
    private static Callable<Character> getCharProvider() {
        return Config.get().randomizeCharacters ? RANDOM_CHAR_PROVIDER : STATIC_CHAR_PROVIDER;
    }
}