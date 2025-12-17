package dev.rodr.acer.events

import dev.rodr.acer.annotations.Init
import dev.rodr.acer.config.AcerConfig
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks

object EndPortal {
    @Init
    fun init() = UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
        if (!AcerConfig.data.disableEndPortal) return@register InteractionResult.PASS

        val block = world.getBlockState(hitResult.blockPos).block
        val item = player.getItemInHand(hand).item

        if (block == Blocks.END_PORTAL_FRAME && item == Items.ENDER_EYE) {
            if (world is ServerLevel) {
                val pos = hitResult.blockPos.center
                world.sendParticles(
                    ParticleTypes.INFESTED, pos.x, pos.y + 0.4, pos.z, 8, 0.1, 0.0, 0.1, 0.0
                )

                world.playSound(
                    null, hitResult.blockPos, SoundEvents.DECORATED_POT_INSERT_FAIL, SoundSource.BLOCKS
                )
            }

            return@register InteractionResult.SUCCESS
        }

        InteractionResult.PASS
    }
}
