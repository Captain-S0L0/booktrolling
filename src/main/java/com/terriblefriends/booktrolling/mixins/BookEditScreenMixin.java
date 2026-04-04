package com.terriblefriends.booktrolling.mixins;

import com.mojang.logging.LogUtils;
import com.terriblefriends.booktrolling.Config;
import com.terriblefriends.booktrolling.ToggleButton;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

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
    @Shadow @Final private Player owner;
    @Shadow @Final private InteractionHand hand;
    @Shadow @Final private List<String> pages;
    @Shadow private int currentPage;
    @Shadow private MultiLineEditBox page;

    @Shadow protected abstract void saveChanges();
    @Shadow protected abstract void updatePageContent();

    protected BookEditScreenMixin(Component title) {
        super(title);
    }

    @Override
    public void onClose() {
        // update contents but don't sign if screen was closed
        this.saveChanges();
        super.onClose();
    }

    @Inject(method="init", at=@At("TAIL"))
    private void booktrolling$initBookEditScreen(CallbackInfo ci) {
        this.page.setLineLimit(Integer.MAX_VALUE); // disable checking character width

        int y = 0;
        this.addRenderableWidget(Button.builder(Component.literal("1023"), (button) -> {
            this.createBook(100, Config.get().autoSign, Config.get().autoDrop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 1023; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).bounds(0, y, 98, 20).build());
        y+=20;
        this.addRenderableWidget(Button.builder(Component.literal("Max"), (button) -> {
            this.createBook(100, Config.get().autoSign, Config.get().autoDrop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 1024; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).bounds(0, y, 98, 20).build());
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
        this.addRenderableWidget(Button.builder(Component.literal("Paper"), (button) -> {
            this.createBook(100, Config.get().autoSign, Config.get().autoDrop, () -> {
                StringBuilder builder = new StringBuilder();
                Callable<Character> charProvider = getCharProvider();
                for (int i = 0; i < 320; i++) {
                    builder.append(charProvider.call());
                }
                return builder.toString();
            }, () -> BRANDING);
        }).bounds(0, y, 98, 20).build());
        y+=20;
        /*this.addDrawableChild(ButtonWidget.builder(Text.literal("unsaveable"), (button) -> {
            this.sign(1, true, drop, () -> "", () -> "123456789012345678901234567890123");
        }).dimensions(0, y, 98, 20).build());
        y+=20;*/
        this.addRenderableWidget(Button.builder(Component.literal("Clear"), (button) -> {
            this.createBook(1, false, Config.get().autoDrop, () -> "", () -> BRANDING);
        }).bounds(0, y, 98, 20).build());
        y+=20;

        this.addRenderableWidget(new ToggleButton(0, this.height-20, 98, 20, Component.literal("Auto Sign"), (button) -> {
            Config.get().autoSign = !Config.get().autoSign;
        }, Config.get().autoSign));
        this.addRenderableWidget(new ToggleButton(0, this.height-40, 98, 20, Component.literal("Randomize Chars"), (button) -> {
            Config.get().randomizeCharacters = !Config.get().randomizeCharacters;
        }, Config.get().randomizeCharacters));
        this.addRenderableWidget(new ToggleButton(0, this.height-60, 98, 20, Component.literal("Auto Drop"), (button) -> {
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
            this.updatePageContent();

            Optional<String> title = Optional.empty();

            if (signing) {
                title = Optional.of(titleGenerator.call());
            }

            if (drop || signing) {
                int i = this.hand == InteractionHand.MAIN_HAND ? this.owner.getInventory().getSelectedSlot() : 40;
                this.minecraft.getConnection().send(new ServerboundEditBookPacket(i, this.pages, title));
                this.minecraft.setScreen(null);
            }

            if (drop) {
                this.minecraft.player.drop(true);
            }
        }
        catch (Exception e) {
            LOGGER.error("BookTrolling failed to generate book!", e);
            this.minecraft.gui.getChat().addMessage(Component.literal("<BookTrolling> Error generating book! See logs!").withStyle(ChatFormatting.DARK_RED));
        }
    }

    @Unique
    private static Callable<Character> getCharProvider() {
        return Config.get().randomizeCharacters ? RANDOM_CHAR_PROVIDER : STATIC_CHAR_PROVIDER;
    }
}