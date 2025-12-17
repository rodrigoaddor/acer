package dev.rodrick.acer.events

import dev.rodrick.acer.config.AcerConfig
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.enchantment.Enchantments

object FarmlandFall {
    fun shouldPrevent(entity: Entity?): Boolean {
        if (!AcerConfig.data.featherFallOnFarmland) return false
        if (entity !is Player) return false

        val boots = entity.getItemBySlot(EquipmentSlot.FEET)
        if (boots.isEmpty) return false

        val featherFalling = entity.level().registryAccess().getOrThrow(Enchantments.FEATHER_FALLING)
        return boots.enchantments.getLevel(featherFalling) > 0
    }
}