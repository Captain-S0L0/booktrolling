package com.terriblefriends.booktrolling.mixins.tooltips;

import net.minecraft.client.item.TooltipData;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BundleItem.class)
public class BundleItemMixin extends Item {

    public BundleItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(at=@At("HEAD"),method="getTooltipData",cancellable = true)
    private void booktrolling$preventHugeBundleTooltipLag(ItemStack stack, CallbackInfoReturnable<Optional<TooltipData>> cir) {
        if (stack.hasNbt() && stack.getNbt().getList("Items", 10).size() > 1000) {
            cir.setReturnValue(Optional.empty());
            cir.cancel();
        }
    }
}
