package dev.rodr.acer.events

import dev.rodr.acer.annotations.Init
import dev.rodr.acer.config.AcerConfig
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.BlockItem
import kotlin.math.pow

object SaplingPlanter {
    private val SAPLINGS = ItemTags.SAPLINGS

    @Init
    fun init() = ServerEntityEvents.ENTITY_UNLOAD.register { entity, level ->
        val (enabled, chance) = AcerConfig.data.replantSaplings

        if (enabled) {
            if (entity is ItemEntity && entity.isSapling && entity.inBlockState.isAir) {
                val stack = entity.item
                val block = (stack.item as BlockItem).block

                if (block.defaultBlockState().canSurvive(entity.level(), entity.blockPosition())) {
                    if (chance >= 1 || Math.random() >= (1 - chance).pow(stack.count)) {
                        level.setBlockAndUpdate(entity.blockPosition(), block.defaultBlockState())
                        level.sendParticles(
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
