package dev.rodrick.acer.effect

import dev.rodrick.acer.annotations.Init
import dev.rodrick.acer.config.AcerConfig
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.inventory.Inventory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.world.World

object Marker {
    private const val MARKER_TAG = "acer:marker"

    @Init
    fun init() {
        ServerTickEvents.END_WORLD_TICK.register { world ->
            val duration = AcerConfig.data.finder.duration
            val entities = world.getEntitiesByType(EntityType.SHULKER) { it.isMarker }
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
                ActionResult.SUCCESS
            }

            ActionResult.PASS
        }

        UseEntityCallback.EVENT.register { player, world, hand, entity, _ ->
            if (player as? ServerPlayerEntity != null && !world.isClient && entity.isMarker) {
                player.interactionManager.interactBlock(
                    player,
                    world,
                    player.getStackInHand(hand),
                    hand,
                    BlockHitResult(player.blockPos.toCenterPos(), Direction.UP, entity.blockPos, false)
                )
                entity.remove(Entity.RemovalReason.KILLED)
                ActionResult.SUCCESS
            }
            ActionResult.PASS
        }

        AttackBlockCallback.EVENT.register { _, world, _, blockPos, _ ->
            removeAt(world, blockPos)
            ActionResult.PASS
        }

        UseBlockCallback.EVENT.register { _, world, _, blockHitResult ->
            removeAt(world, blockHitResult.blockPos)
            ActionResult.PASS
        }
    }

    private fun removeAt(world: World, pos: BlockPos) {
        if (world.getBlockState(pos) !is Inventory) return

        val box = Box(pos.x + 0.0, pos.y + 0.0, pos.z + 0.0, pos.x + 1.0, pos.y + 1.0, pos.z + 1.0)

        world.getEntitiesByType(EntityType.SHULKER, box) { it.isMarker }.forEach { marker ->
            marker.remove(Entity.RemovalReason.KILLED)
        }
    }

    fun spawn(world: World, pos: BlockPos) {
        EntityType.SHULKER.create(world, SpawnReason.COMMAND)?.run {
            isMarker = true
            isGlowing = true
            isAiDisabled = true
            isSilent = true
            isInvisible = true
            isInvulnerable = true
            isSilent = true
            disableExperienceDropping()
            activeStatusEffects[StatusEffects.INVISIBILITY] =
                StatusEffectInstance(StatusEffects.INVISIBILITY, Int.MAX_VALUE, 0, false, false)

            setPosition(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
            world.spawnEntity(this)
        } ?: throw IllegalStateException("Could not create Marker")
    }

    private var Entity.isMarker: Boolean
        get() = commandTags.contains(MARKER_TAG)
        set(value) {
            if (value) {
                commandTags.add(MARKER_TAG)
            } else {
                commandTags.remove(MARKER_TAG)
            }
        }
}