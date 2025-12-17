package dev.rodrick.acer.mixin;

import dev.rodrick.acer.callbacks.EntityDespawnCallback;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityDespawnMixin {
    @Inject(at = @At("TAIL"), method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V")
    private void onRemove(CallbackInfo info) {
        EntityDespawnCallback.Companion.getEVENT().invoker().onDespawn((Entity) (Object) this);
    }
}
