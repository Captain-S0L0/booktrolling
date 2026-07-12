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
import java.util.function.Consumer;

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
            this.createBook(consumerFillBook(1023));
        }).bounds(0, y, 98, 20).build());
        y+=20;
        this.addRenderableWidget(Button.builder(Component.literal("Max"), (button) -> {
            this.createBook(consumerFillBook(1024));
        }).bounds(0, y, 98, 20).build());
        y+=20;
        this.addRenderableWidget(Button.builder(Component.literal("Paper"), (button) -> {
            this.createBook(consumerFillBook(633));
        }).bounds(0, y, 98, 20).build());
        y+=20;
        this.addRenderableWidget(Button.builder(Component.literal("Clear"), (button) -> {
            this.createBook((list) -> list.add(""));
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
    private void createBook(Consumer<List<String>> pageGenerator) {
        this.createBook(Config.get().autoSign, Config.get().autoDrop, pageGenerator, Config.get().autoTitle);
    }

    @Unique
    private void createBook(boolean sign, boolean drop, Consumer<List<String>> pageGenerator, String titleString) {
        try {
            this.pages.clear();

            pageGenerator.accept(this.pages);

            this.currentPage = 0;
            this.updatePageContent();

            Optional<String> title = Optional.empty();

            if (sign) {
                title = Optional.of(titleString);
            }

            if (sign || drop) {
                int i = this.hand == InteractionHand.MAIN_HAND ? this.owner.getInventory().getSelectedSlot() : 40;
                this.minecraft.getConnection().send(new ServerboundEditBookPacket(i, this.pages, title));
                this.minecraft.gui.setScreen(null);
            }

            if (drop) {
                this.minecraft.player.drop(true);
            }
        }
        catch (Exception e) {
            LOGGER.error("BookTrolling failed to generate book!", e);
            this.minecraft.gui.hud.getChat().addClientSystemMessage(Component.literal("<BookTrolling> Error generating book! See logs!").withStyle(ChatFormatting.DARK_RED));
        }
    }

    @Unique
    private Consumer<List<String>> consumerFillBook(int charactersPerPage) {
        return (list) -> {
            for (int p = 0; p < 100; p++) {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < charactersPerPage; i++) {
                    builder.appendCodePoint(getChar());
                }
                list.add(builder.toString());
            }
        };
    }

    @Unique
    private static int getChar() {
        if (Config.get().randomizeCharacters) {
            // U+D800 to U+DFFF are surrogate characters that cannot be represented in UTF-16, so Java strings cannot contain them
            int i = RANDOM.nextInt(0x800, 0x10000 - 0x800);
            if (i >= 0xD800) {
                i += 0x800;
            }
            return i;
        }
        else {
            return 2048;
        }
    }
}
