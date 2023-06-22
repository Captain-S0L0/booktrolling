package com.terriblefriends.booktrolling.mixins;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.TimeoutException;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketEncoderException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(at=@At("HEAD"),method="exceptionCaught",cancellable = true)
    private void booktrolling$catchBasicallyEveryClientException(ChannelHandlerContext context, Throwable ex, CallbackInfo ci) {
        if (!(ex instanceof PacketEncoderException) && !(ex instanceof TimeoutException)) {
            LogUtils.getLogger().error("Connection exception thrown!", ex);
            ci.cancel();
        }
    }
}
