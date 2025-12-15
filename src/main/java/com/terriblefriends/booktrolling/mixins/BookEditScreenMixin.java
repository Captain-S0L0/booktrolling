package com.terriblefriends.booktrolling.mixins;

import com.mojang.logging.LogUtils;
import com.terriblefriends.booktrolling.Config;
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
        this.finalizeBook();
        super.close();
    }

    @Inject(method="init", at=@At("TAIL"))
    private void booktrolling$initBookEditScreen(CallbackInfo ci) {
        this.editBox.setMaxLines(Integer.MAX_VALUE);

        int y = 0;
        
        // --- Standard Actions ---
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

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), (button) -> {
            this.createBook(1, false, Config.get().autoDrop, () -> "", () -> BRANDING);
        }).dimensions(0, y, 98, 20).build());
        y+=20;

        // --- Toggles (Fixed using Standard Buttons) ---
        
        // Auto Sign
        this.addDrawableChild(ButtonWidget.builder(getToggleText("Auto Sign", Config.get().autoSign), (button) -> {
            Config.get().autoSign = !Config.get().autoSign;
            button.setMessage(getToggleText("Auto Sign", Config.get().autoSign));
        }).dimensions(0, this.height-20, 98, 20).build());

        // Randomize Chars
        this.addDrawableChild(ButtonWidget.builder(getToggleText("Randomize Chars", Config.get().randomizeCharacters), (button) -> {
            Config.get().randomizeCharacters = !Config.get().randomizeCharacters;
            button.setMessage(getToggleText("Randomize Chars", Config.get().randomizeCharacters));
        }).dimensions(0, this.height-40, 98, 20).build());

        // Auto Drop
        this.addDrawableChild(ButtonWidget.builder(getToggleText("Auto Drop", Config.get().autoDrop), (button) -> {
            Config.get().autoDrop = !Config.get().autoDrop;
            button.setMessage(getToggleText("Auto Drop", Config.get().autoDrop));
        }).dimensions(0, this.height-60, 98, 20).build());
    }

    // Helper to color text Green (ON) or Red (OFF)
    @Unique
    private Text getToggleText(String name, boolean active) {
        return Text.literal(name).formatted(active ? Formatting.GREEN : Formatting.RED);
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