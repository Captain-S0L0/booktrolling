package com.terriblefriends.booktrolling.mixins.tooltips;

import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.EntityBucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(EntityBucketItem.class)
public class EntityBucketItemMixin extends Item {
    public EntityBucketItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(at=@At("HEAD"),method="appendTooltip")
    public void booktrolling$appendSizeTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type, CallbackInfo ci) {
        super.appendTooltip(stack, context, tooltip, type);
    }
}
