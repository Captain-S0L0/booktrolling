package com.terriblefriends.booktrolling.mixins.tooltips;

import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BundleItem.class)
public class BundleItemMixin extends Item {

    public BundleItemMixin(Settings settings) {
        super(settings);
    }

    // TODO fix this, or not
    /*@Inject(at=@At("HEAD"),method="getTooltipData",cancellable = true)
    private void booktrolling$preventHugeBundleTooltipLag(ItemStack stack, CallbackInfoReturnable<Optional<TooltipData>> cir) {
        if (stack.hasNbt() && stack.getNbt().getList("Items", 10).size() > 1000) {
            cir.setReturnValue(Optional.empty());
            cir.cancel();
        }
    }*/

    @Inject(at=@At("HEAD"),method="appendTooltip")
    public void booktrolling$appendSizeTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type, CallbackInfo ci) {
        super.appendTooltip(stack, context, tooltip, type);
    }
}
