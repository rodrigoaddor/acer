package dev.rodrick.acer.events

import dev.rodrick.acer.annotations.Init
import dev.rodrick.acer.callbacks.EntityDespawnCallback
import dev.rodrick.acer.config.AcerConfig
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.BlockItem
import kotlin.math.pow

object SaplingPlanter {
    private val SAPLINGS = ItemTags.SAPLINGS

    @Init
    fun init() = EntityDespawnCallback.EVENT.register { entity ->
        val (enabled, chance) = AcerConfig.data.replantSaplings

        if (enabled) {
            if (entity is ItemEntity && entity.isSapling && entity.inBlockState.isAir) {
                val stack = entity.item
                val block = (stack.item as BlockItem).block
                val world = entity.level()

                if (world is ServerLevel && block.defaultBlockState()
                        .canSurvive(entity.level(), entity.blockPosition())
                ) {
                    if (chance >= 1 || Math.random() >= (1 - chance).pow(stack.count)) {
                        world.setBlockAndUpdate(entity.blockPosition(), block.defaultBlockState())
                        world.sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            entity.blockX + .5,
                            entity.blockY + .5,
                            entity.blockZ + .5,
                            15,
                            0.22,
                            0.22,
                            0.22,
                            0.0
                        )
                    }
                }
            }
        }
    }

    private val ItemEntity.isSapling: Boolean
        get() = item.`is`(SAPLINGS) && item.item is BlockItem
}
