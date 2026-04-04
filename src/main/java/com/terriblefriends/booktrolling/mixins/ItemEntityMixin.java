package com.terriblefriends.booktrolling.mixins;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {
    @Shadow @Final private static EntityDataAccessor<ItemStack> DATA_ITEM;

    public ItemEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(at=@At("HEAD"),method = "defineSynchedData",cancellable = true)
    private void bookTrolling$preventLargeInvisibleItems(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(DATA_ITEM, new ItemStack(Items.BARRIER));
        ci.cancel();
    }
}
