package com.terriblefriends.booktrolling.mixins;

import com.terriblefriends.booktrolling.Booktrolling;
import com.terriblefriends.booktrolling.ItemSizeThread;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Item.class)
public class ItemMixin {
    private final static ItemSizeThread itemSizeThread = new ItemSizeThread();

    static {
        itemSizeThread.start();
    }

    @Inject(at=@At("HEAD"),method = "Lnet/minecraft/item/Item;appendTooltip(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Ljava/util/List;Lnet/minecraft/client/item/TooltipContext;)V")
    private void getItemSize(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context, CallbackInfo ci) {
        if (!Booktrolling.itemSizeDebug) {
            return;
        }

        itemSizeThread.setStack(stack);
        if (itemSizeThread.diskSize == -1137) {
            tooltip.add(Text.literal("ERROR CALCULATING SIZE! See logs").formatted(Formatting.DARK_RED));
        }
        else if (itemSizeThread.diskSize == -1 || itemSizeThread.nbtSize == -1 || itemSizeThread.compressedSize == -1) {
            tooltip.add(Text.literal("Calculating...").formatted(Formatting.RED));
        }
        else {
            tooltip.add(Text.literal("DISK: " + itemSizeThread.diskSize).formatted(Formatting.RED));
            if (itemSizeThread.nbtSize > 2097152) {
                tooltip.add(Text.literal("NBT: " + itemSizeThread.nbtSize).formatted(Formatting.RED).append(Text.literal(" (OVERSIZED)").formatted(Formatting.DARK_RED)));
            }
            else {
                tooltip.add(Text.literal("NBT: " + itemSizeThread.nbtSize).formatted(Formatting.RED));
            }

            if (itemSizeThread.compressedSize == -9001) {
                tooltip.add(Text.literal("COMPRESS: "+itemSizeThread.compressedSize).formatted(Formatting.RED).append(Text.literal(" (OVERSIZED)").formatted(Formatting.DARK_RED)));
            }
            else {
                tooltip.add(Text.literal("COMPRESS: "+itemSizeThread.compressedSize).formatted(Formatting.RED));
            }
        }
    }
}
