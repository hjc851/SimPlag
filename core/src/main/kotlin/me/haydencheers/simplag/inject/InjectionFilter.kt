package me.haydencheers.simplag.inject

import me.haydencheers.simplag.*
import me.haydencheers.simplag.manifest.Manifests
import me.haydencheers.simplag.manifest.MutableAssignmentManifest
import me.haydencheers.simplag.ast.ASTCloner
import me.haydencheers.simplag.ledger.*
import org.eclipse.jdt.core.dom.CompilationUnit
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

val fileNameCounter = AtomicInteger(0)

abstract class InjectionFilter {
    @Inject
    protected lateinit var seedLibrary: SeedLibrary

    @Inject
    protected lateinit var analytics: Analytics

    abstract fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest)
}

@Singleton
class FileInjectionFilter : InjectionFilter() {

    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        try {
            if (config.random.nextDouble(0.0, 1.0) <= config.inject.injectFileChance) {
                val assignment = seedLibrary.assignmentManifests.random(config.random)
                val fileToInject = assignment.files.random(config.random)

                var relativeName = assignment.root.relativize(fileToInject.path).toString()

                if (Files.exists(manifest.root.resolve(relativeName))) {
                    relativeName = "injected-${fileNameCounter.getAndIncrement()}.java"
                }

                val fileManifest = Manifests.buildForFile(fileToInject.path, assignment.root)
                fileManifest.relativeName = relativeName
                manifest.files.add(fileManifest)

                analytics.injectedFiles.add(
                    InjectedFileRecord(
                        fileToInject.path.toAbsolutePath().toString(),
                        manifest.name,
                        manifest.variantId,
                        relativeName
                    )
                )
            }
        } catch (e: NoSuchElementException) {
        }
    }
}

@Singleton
class ClassInjectionFilter : InjectionFilter() {

    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        try {
            for (injectFile in manifest.files) {
                if (config.random.nextDouble(0.0, 1.0) <= config.inject.injectClassChance) {
                    for (i in 0 .. config.random.nextInt(1, config.inject.injectClassMaxCount+1)) {
                        val seedAssignment = seedLibrary.assignmentManifests.random(config.random)
                        val seedFile = seedAssignment.files.random(config.random)
                        val seedClass = seedFile.types.random(config.random)

                        val injectCU = injectFile.ast as CompilationUnit
                        val newClass = ASTCloner.clone(seedClass.node, injectCU.ast)
                        injectCU.types().add(newClass)

                        analytics.injectedClasses.add(
                            InjectedClassRecord(
                                manifest.name,
                                manifest.variantId,
                                seedAssignment.name,
                                seedFile.path.toString(),
                                seedClass.name,
                                injectFile.relativeName
                            )
                        )
                    }
                }
            }
        } catch (e: NoSuchElementException) {
        }
    }
}

@Singleton
class MethodInjectionFilter : InjectionFilter() {

    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        try {
            for (injectFile in manifest.files) {
                for (injectClass in injectFile.types) {
                    if (config.random.nextDouble(0.0, 1.0) <= config.inject.injectMethodChance) {
                        for (i in 0 .. config.random.nextInt(1, config.inject.injectMethodMaxCount+1)) {
                            val seedAssignment = seedLibrary.assignmentManifests.random(config.random)
                            val seedFile = seedAssignment.files.random(config.random)
                            val seedClass = seedFile.types.random(config.random)
                            val seedMethod = seedClass.methods.random(config.random)

                            val injectNode = injectClass.node

                            val clonedMethod = ASTCloner.clone(seedMethod.node, injectNode.ast)
                            injectNode.bodyDeclarations().add(clonedMethod)

                            analytics.injectedMethods.add(
                                InjectedMethodRecord(
                                    manifest.name,
                                    manifest.variantId,

                                    seedAssignment.name,
                                    seedFile.path.toString(),
                                    seedClass.name,
                                    seedMethod.name,

                                    injectFile.relativeName,
                                    injectClass.name
                                )
                            )
                        }
                    }
                }
            }
        }
        catch (e: NoSuchElementException) {
        }
    }
}

@Singleton
class BlockInjectionFilter : InjectionFilter() {

    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        try {
            for (injectFile in manifest.files) {
                for (injectClass in injectFile.types) {
                    for (injectMethod in injectClass.methods) {
                        if (config.random.nextDouble(0.0, 1.0) <= config.inject.injectBlockChance) {
                            for (i in 0 until config.random.nextInt(1, config.inject.injectBlockMaxStatements+1)) {
                                val seedAssignment = seedLibrary.assignmentManifests.random(config.random)
                                val seedFile = seedAssignment.files.random(config.random)
                                val seedClass = seedFile.types.random(config.random)
                                val seedMethod = seedClass.methods.random(config.random)
                                val seedBlock = seedMethod.blocks.random(config.random)

                                val injectStatement = ASTCloner.clone(seedBlock.node, injectMethod.node.ast)

                                val methodStatements = injectMethod.node.body.statements()
                                val idx = if (methodStatements.isEmpty()) 0 else config.random.nextInt(
                                    0,
                                    methodStatements.size
                                )
                                methodStatements.add(idx, injectStatement)

                                analytics.injectedStatements.add(
                                    InjectedStatementRecord(
                                        manifest.name,
                                        manifest.variantId,

                                        seedAssignment.name,
                                        seedFile.path.toString(),
                                        seedClass.name,
                                        seedMethod.name,
                                        seedBlock.node.toString(),

                                        injectFile.relativeName,
                                        injectClass.name,
                                        injectMethod.name
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: NoSuchElementException) {
        }
    }
}