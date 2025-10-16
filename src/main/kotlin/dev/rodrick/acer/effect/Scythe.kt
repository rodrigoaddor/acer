package dev.rodrick.acer.effect

import dev.rodrick.acer.AcerMod
import dev.rodrick.acer.annotations.Init
import dev.rodrick.acer.config.AcerConfig
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.block.CropBlock
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.ItemEntity
import net.minecraft.item.BlockItem
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.loot.context.LootWorldContext
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.TagKey
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.context.ContextParameter
import net.minecraft.util.math.Vec3d

object Scythe {
    private val CROPS = BlockTags.CROPS
    private val SCYTHES = TagKey.of(RegistryKeys.ITEM, Identifier.of(AcerMod.MOD_ID, "scythes"))

    @Init
    fun init() {
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            val enabled = AcerConfig.data.scythes
            if (!enabled) return@register ActionResult.PASS

            val blockPos = hitResult.blockPos
            val blockState = world.getBlockState(blockPos)
            val heldStack = player.getStackInHand(hand)
            val blockEntity = world.getBlockEntity(blockPos)
            val serverWorld = world as? ServerWorld

            if (
                !heldStack.isIn(SCYTHES) ||
                !blockState.isIn(CROPS) ||
                (blockState.block as? CropBlock)?.isMature(blockState) != true
            ) {
                return@register ActionResult.PASS
            }

            if (player.itemCooldownManager.isCoolingDown(heldStack)) {
                return@register ActionResult.FAIL
            }

            if (serverWorld != null) {
                val lootContext = LootWorldContext.Builder(serverWorld)
                    .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(blockPos))
                    .add(LootContextParameters.TOOL, heldStack)
                    .addOptional(LootContextParameters.THIS_ENTITY, player)
                    .addOptional(LootContextParameters.BLOCK_ENTITY, blockEntity)

                blockState.getDroppedStacks(lootContext).forEach { stack ->
                    if ((stack.item as? BlockItem)?.let {
                            @Suppress("DEPRECATION")
                            it.block.registryEntry.isIn(CROPS)
                        } == true) {
                        stack.count--
                    }

                    val entity =
                        ItemEntity(serverWorld, blockPos.x + .5, blockPos.y + .0, blockPos.z + 0.5, stack).apply {
                            setPickupDelay(10)
                        }

                    serverWorld.spawnEntity(entity)
                }

                heldStack.damage(
                    1,
                    player,
                    if (hand == Hand.MAIN_HAND) EquipmentSlot.MAINHAND else EquipmentSlot.OFFHAND
                )

                player.itemCooldownManager.set(heldStack, 10)

                serverWorld.playSound(
                    null,
                    blockPos.x.toDouble(),
                    blockPos.y.toDouble(),
                    blockPos.z.toDouble(),
                    blockState.soundGroup.breakSound,
                    SoundCategory.BLOCKS,
                    0.9f,
                    1.0f,
                )

                serverWorld.setBlockState(blockPos, blockState.block.defaultState)
            }

            ActionResult.SUCCESS
        }
    }
}