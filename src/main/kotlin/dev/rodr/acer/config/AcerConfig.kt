package dev.rodr.acer.config

import com.charleskorn.kaml.*
import dev.rodr.acer.AcerMod
import net.fabricmc.loader.api.FabricLoader
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

object AcerConfig {
    var data = AcerConfigData()

    private val file = FabricLoader.getInstance().configDir.resolve("${AcerMod.MOD_ID}.yml").toFile()
    private val yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = false,
            singleLineStringStyle = SingleLineStringStyle.PlainExceptAmbiguous,
            multiLineStringStyle = MultiLineStringStyle.Plain
        )
    )

    fun load() = try {
        FileInputStream(file).use {
            data = yaml.decodeFromStream(AcerConfigData.serializer(), it)
        }
    } catch (e: Exception) {
        when (e) {
            is FileNotFoundException, is EmptyYamlDocumentException -> {
                data = AcerConfigData()
            }

            else -> throw e
        }
    } finally {
        save()
    }

    private fun save() = FileOutputStream(file).use {
        yaml.encodeToStream(AcerConfigData.serializer(), data, it)
    }
}