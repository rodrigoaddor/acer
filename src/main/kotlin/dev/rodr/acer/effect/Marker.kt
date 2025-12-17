package dev.rodr.acer.effect

import dev.rodr.acer.annotations.Init
import dev.rodr.acer.config.AcerConfig
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.InteractionResult
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult

object Marker {
    private const val MARKER_TAG = "acer:marker"

    @Init
    fun init() {
        ServerTickEvents.END_WORLD_TICK.register { world ->
            val duration = AcerConfig.data.finder.duration
            val entities = world.getEntities(EntityType.SHULKER) { it.isMarker }
            entities.forEach {
                if (it.health <= 0) {
                    it.remove(Entity.RemovalReason.KILLED)
                } else {
                    it.health -= 1.5f / duration
                }
            }
        }

        AttackEntityCallback.EVENT.register { _, _, _, entity, _ ->
            if (entity.isMarker) {
                entity.remove(Entity.RemovalReason.KILLED)
                InteractionResult.SUCCESS
            }

            InteractionResult.PASS
        }

        UseEntityCallback.EVENT.register { player, world, hand, entity, _ ->
            if (player as? ServerPlayer != null && !world.isClientSide && entity.isMarker) {
                player.gameMode.useItemOn(
                    player,
                    world,
                    player.getItemInHand(hand),
                    hand,
                    BlockHitResult(player.blockPosition().center, Direction.UP, entity.blockPosition(), false)
                )
                entity.remove(Entity.RemovalReason.KILLED)
                InteractionResult.SUCCESS
            }
            InteractionResult.PASS
        }

        AttackBlockCallback.EVENT.register { _, world, _, blockPos, _ ->
            removeAt(world, blockPos)
            InteractionResult.PASS
        }

        UseBlockCallback.EVENT.register { _, world, _, blockHitResult ->
            removeAt(world, blockHitResult.blockPos)
            InteractionResult.PASS
        }
    }

    private fun removeAt(world: Level, pos: BlockPos) {
        if (world.getBlockState(pos) !is Container) return

        val box = AABB(pos.x + 0.0, pos.y + 0.0, pos.z + 0.0, pos.x + 1.0, pos.y + 1.0, pos.z + 1.0)

        world.getEntities(EntityType.SHULKER, box) { it.isMarker }.forEach { marker ->
            marker.remove(Entity.RemovalReason.KILLED)
        }
    }

    fun spawn(world: Level, pos: BlockPos) {
        EntityType.SHULKER.create(world, EntitySpawnReason.COMMAND)?.run {
            isMarker = true
            isNoAi = true
            isSilent = true
            isInvisible = true
            isInvulnerable = true
            isSilent = true
            setGlowingTag(true)
            skipDropExperience()
            activeEffectsMap[MobEffects.INVISIBILITY] =
                MobEffectInstance(MobEffects.INVISIBILITY, Int.MAX_VALUE, 0, false, false)

            setPos(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
            world.addFreshEntity(this)
        } ?: throw IllegalStateException("Could not create Marker")
    }

    private var Entity.isMarker: Boolean
        get() = tags.contains(MARKER_TAG)
        set(value) {
            if (value) {
                tags.add(MARKER_TAG)
            } else {
                tags.remove(MARKER_TAG)
            }
        }
}