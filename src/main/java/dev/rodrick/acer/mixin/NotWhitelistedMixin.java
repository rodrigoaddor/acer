package dev.rodrick.acer.mixin;

import dev.rodrick.acer.callbacks.NotWhitelistedCallback;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class NotWhitelistedMixin {
    @Shadow
    @Final
    ClientConnection connection;

    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    @Nullable String profileName;

    @Inject(method = "disconnect(Lnet/minecraft/text/Text;)V", at = @At(value = "HEAD"))
    private void onDisconnect(Text reason, CallbackInfo ci) {
        final String key = ((TranslatableTextContent) reason.getContent()).getKey();
        final String ip = connection.getAddressAsString(this.server.shouldLogIps());

        if (key.equals("multiplayer.disconnect.not_whitelisted")) {
            NotWhitelistedCallback.Companion.getEVENT().invoker().onDisconnected(profileName, ip);
        }
    }
}
