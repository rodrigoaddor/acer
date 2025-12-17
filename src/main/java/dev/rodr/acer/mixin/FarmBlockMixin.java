package dev.rodr.acer.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.rodr.acer.events.FarmlandFall;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.FarmBlock;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmBlock.class)
public abstract class FarmBlockMixin {
    @Inject(method = "turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), cancellable = true)
    private static void onTurnToDirt(CallbackInfo ci, @Local(argsOnly = true) @Nullable Entity entity) {
        if (FarmlandFall.INSTANCE.shouldPrevent(entity)) {
            ci.cancel();
        }
    }
}
