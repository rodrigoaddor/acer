package dev.rodrick.acer.events

import dev.rodrick.acer.annotations.Init
import dev.rodrick.acer.config.AcerConfig
import dev.rodrick.acer.callbacks.EntityDespawnCallback
import net.minecraft.entity.ItemEntity
import net.minecraft.item.BlockItem
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.tag.ItemTags
import net.minecraft.server.world.ServerWorld
import kotlin.math.pow

object SaplingPlanter {
    private val SAPLINGS = ItemTags.SAPLINGS

    @Init
    fun init() = EntityDespawnCallback.EVENT.register { entity ->
        val (enabled, chance) = AcerConfig.data.replantSaplings

        if (enabled) {
            if (entity is ItemEntity && entity.isSapling && entity.blockStateAtPos.isAir) {
                val stack = entity.stack
                val block = (stack.item as BlockItem).block
                val world = entity.entityWorld

                if (world is ServerWorld && block.defaultState.canPlaceAt(entity.entityWorld, entity.blockPos)) {
                    if (chance >= 1 || Math.random() >= (1 - chance).pow(stack.count)) {
                        world.setBlockState(entity.blockPos, block.defaultState)
                        world.spawnParticles(
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
        get() = stack.isIn(SAPLINGS) && stack.item is BlockItem
}
