package dev.rodrick.acer.mixin;

import dev.rodrick.acer.callbacks.NotWhitelistedCallback;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class NotWhitelistedMixin {
    @Shadow
    @Final
    Connection connection;

    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    @Nullable String requestedUsername;

    @Inject(method = "disconnect(Lnet/minecraft/network/chat/Component;)V", at = @At(value = "HEAD"))
    private void onDisconnect(Component reason, CallbackInfo ci) {
        final String key = ((TranslatableContents) reason.getContents()).getKey();
        final String ip = connection.getLoggableAddress(this.server.logIPs());

        if (key.equals("multiplayer.disconnect.not_whitelisted")) {
            NotWhitelistedCallback.Companion.getEVENT().invoker().onDisconnected(requestedUsername, ip);
        }
    }
}
