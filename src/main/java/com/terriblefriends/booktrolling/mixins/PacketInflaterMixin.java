package com.terriblefriends.booktrolling.mixins;

import net.minecraft.network.PacketInflater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PacketInflater.class)
public class PacketInflaterMixin {
    @ModifyConstant(method="decode", constant = @Constant(intValue = 8388608))
    private int booktrolling$removeDecodingByteLimits(int constant) {
        return Integer.MAX_VALUE;
    }
}
