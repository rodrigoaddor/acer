package dev.rodrick.acer

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo

class InitializerProcessor(
    private val codeGenerator: CodeGenerator, private val logger: KSPLogger, private val options: Map<String, String>
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val initSymbols = resolver.getSymbolsWithAnnotation("dev.rodrick.acer.annotations.Init")
            .filterIsInstance<KSFunctionDeclaration>().filter { it.parentDeclaration?.isObject ?: false }

        val commandSymbols = resolver.getSymbolsWithAnnotation("dev.rodrick.acer.annotations.InitCommand")
            .filterIsInstance<KSFunctionDeclaration>().filter { it.parentDeclaration?.isObject ?: false }

        if (!initSymbols.iterator().hasNext() && !commandSymbols.iterator().hasNext()) return emptyList()

        val commandDispatcher = ClassName(
            "com.mojang.brigadier", "CommandDispatcher"
        ).parameterizedBy(ClassName("net.minecraft.commands", "CommandSourceStack"))

        val commandRegistryAccess = ClassName("net.minecraft.commands", "CommandBuildContext")

        val registrationEnvironment =
            ClassName("net.minecraft.commands.Commands", "CommandSelection")

        val file = FileSpec.builder("dev.rodrick.acer", "Initializer").addType(
            TypeSpec.objectBuilder("Initializer").addFunction(
                FunSpec.builder("init").apply {
                    initSymbols.forEach { symbol ->
                        addStatement(
                            "%M()", MemberName(
                                "${symbol.packageName.asString()}.${symbol.parentDeclaration?.simpleName?.asString()}",
                                symbol.simpleName.asString()
                            )
                        )
                    }
                }.build()
            ).addFunction(
                FunSpec.builder("initCommands").apply {
                    addParameter("dispatcher", commandDispatcher)
                    addParameter("registryAccess", commandRegistryAccess)
                    addParameter("environment", registrationEnvironment)
                    commandSymbols.forEach { symbol ->
                        val func = MemberName(
                            "${symbol.packageName.asString()}.${symbol.parentDeclaration?.simpleName?.asString()}",
                            symbol.simpleName.asString()
                        )
                        addStatement(
                            "%M(dispatcher, registryAccess, environment)", func
                        )
                    }
                }.build()
            ).build()
        ).build()

        file.writeTo(codeGenerator, Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()))

        return initSymbols.filterNot { it.validate() }.toList()
    }

    private val KSDeclaration.isObject: Boolean
        get() = (this as? KSClassDeclaration)?.classKind == ClassKind.OBJECT
}