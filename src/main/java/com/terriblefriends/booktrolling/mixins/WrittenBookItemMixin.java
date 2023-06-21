package com.terriblefriends.booktrolling.mixins;

import net.minecraft.item.WritableBookItem;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WrittenBookItem.class)
public class WrittenBookItemMixin {
    @Inject(at=@At("HEAD"),method="isValid",cancellable = true)
    private static void allowLongTitles(NbtCompound nbt, CallbackInfoReturnable<Boolean> cir) {
        if (!WritableBookItem.isValid(nbt)) {
            cir.setReturnValue(false);
        } else if (!nbt.contains("title", 8)) {
            cir.setReturnValue(false);
        } else {
            cir.setReturnValue(nbt.contains("title") && nbt.contains("author", 8));
        }
        cir.cancel();
    }
}
