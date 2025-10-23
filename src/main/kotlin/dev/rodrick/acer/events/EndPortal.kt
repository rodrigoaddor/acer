package dev.rodrick.acer.events

import dev.rodrick.acer.annotations.Init
import dev.rodrick.acer.config.AcerConfig
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.ActionResult

object EndPortal {
    @Init
    fun init() = UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
        if (!AcerConfig.data.disableEndPortal) return@register ActionResult.PASS

        val block = world.getBlockState(hitResult.blockPos).block
        val item = player.getStackInHand(hand).item

        if (block == Blocks.END_PORTAL_FRAME && item == Items.ENDER_EYE) {
            if (world is ServerWorld) {
                val pos = hitResult.blockPos.toCenterPos()
                world.spawnParticles(
                    ParticleTypes.INFESTED, pos.x, pos.y + 0.4, pos.z, 8, 0.1, 0.0, 0.1, 0.0
                )

                world.playSound(
                    null, hitResult.blockPos, SoundEvents.BLOCK_DECORATED_POT_INSERT_FAIL, SoundCategory.BLOCKS
                )
            }

            return@register ActionResult.SUCCESS
        }

        ActionResult.PASS
    }
}
