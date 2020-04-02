package me.haydencheers.simplag

import me.haydencheers.simplag.inject.InjectionFilterFactory
import me.haydencheers.simplag.inject.SeedLibrary
import me.haydencheers.simplag.inject.fileNameCounter
import me.haydencheers.simplag.ledger.Analytics
import me.haydencheers.simplag.ledger.InjectedAssignmentRecord
import me.haydencheers.simplag.manifest.Manifests
import me.haydencheers.simplag.mutate.MutationFilterFactory
import me.haydencheers.simplag.transformationscope.TransformationScopeContext
import me.haydencheers.simplag.util.FileUtils
import me.haydencheers.simplag.util.JsonSerialiser
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.IntStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.streams.toList

@Singleton
class SimPlag {

    lateinit var config: SimPlagConfig

    @Inject
    lateinit var ctx: TransformationScopeContext

    @Inject
    lateinit var seedLibrary: SeedLibrary

    @Inject
    lateinit var analytics: Analytics

    @Inject
    lateinit var injectionFilterFactory: InjectionFilterFactory

    @Inject
    lateinit var mutationFilterFactory: MutationFilterFactory

    @Inject
    lateinit var variantWriter: VariantWriter

    fun run(args: Array<String>) {
        if (args.count() != 1) throw IllegalArgumentException("Usage: SimPlag <path to config file>")

        val tmp = Files.createTempDirectory("simplag")

        val configPath = Paths.get(args[0])
        this.config = JsonSerialiser.deserialise(configPath, SimPlagConfig::class)
        this.config.random = Random(config.randomSeed)

        val src = configPath.parent.resolve(config.input)
        val seed = configPath.parent.resolve(config.injectionSources)
        val out = configPath.parent.resolve(config.output)

        // Validate config parameters
        if (!Files.exists(src) || !Files.isDirectory(src)) throw IllegalArgumentException("Input source ${src} does not exist or is not a folder")
        if (!Files.exists(seed) || !Files.isDirectory(seed)) throw IllegalArgumentException("Input seed ${seed} does not exist or is not a folder")

        if (Files.exists(out))
            Files.walk(out)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        Files.createDirectory(out)

        // Setup input
        val seedSubmissions = Files.list(seed)
            .filter { Files.isDirectory(it) && !Files.isHidden(it) }
            .toList()

        println("Building seed library of submissions")
        seedLibrary.buildSeedLibrary(seedSubmissions)

        // Setup for parsing
        val submissions = Files.list(src)
            .filter { Files.isDirectory(it) }
            .toList()

        // Validate submissions
        println("Validating Submissions")
        val validSubmissions = submissions.filter { submission ->
            try {
                FileUtils.listFiles(submission, ".java")
                    .map { Parser.parse(it) }
                return@filter true
            } catch (e: Exception) {
                System.err.println("\tError parsing $submission")
                return@filter false
            }
        }.toMutableList()

        //  Injection Setup
        if (config.inject.injectAssignment) {
            println("Injecting whole assignment plagiarism (${config.inject.injectAssignmentCount} cases)")
            for (i in 0 until config.inject.injectAssignmentCount) {
                val randomSeed = seedLibrary.assignmentManifests.random(config.random)

                // If it exists add it to a tmp directory + rename
                if (Files.exists(src.resolve(randomSeed.name))) {
                    val newName = "injected-${fileNameCounter.getAndIncrement()}"
                    val tmpDir = Files.createDirectory(tmp.resolve(newName))

                    Files.walk(randomSeed.root)
                        .forEachOrdered { path ->
                            Files.copy(path, tmpDir.resolve(randomSeed.root.relativize(path)))
                        }

                    validSubmissions.add(tmpDir)
                    analytics.injectedWholeAssignment.add(
                        InjectedAssignmentRecord(
                            randomSeed.name,
                            newName
                        )
                    )
                } else {
                    validSubmissions.add(randomSeed.root)
                    analytics.injectedWholeAssignment.add(
                        InjectedAssignmentRecord(
                            randomSeed.name,
                            randomSeed.name
                        )
                    )
                }
            }
        }

        val filters = injectionFilterFactory.buildFilterStack(config)

        // Do the transformations
        println("Transforming submissions (${config.copies} each)")
        for (submissionRoot in validSubmissions) {
            val sources = FileUtils.listFiles(submissionRoot, ".java")
            IntStream.range(0, config.copies)
                .parallel()
                .forEach { i ->
                    val submission = Manifests.buildForSubmission(submissionRoot, i)

                    // Injection
                    for (filter in filters)
                        filter.process(config, submission)

                    // Mutation
                    ctx.activate {
                        val mutationFilters = mutationFilterFactory.buildFilterStack(config)
                        for (filter in mutationFilters)
                            filter.process(config, submission)
                    }

                    // Save
                    var savePath = out.resolve("${submission.name}-${submission.variantId}")
                    if (Files.exists(savePath)) {
                        savePath =
                                out.resolve("_dup-${fileNameCounter.getAndIncrement()}-${submission.name}-${submission.variantId}")
                    }

                    Files.createDirectory(savePath)
                    variantWriter.writeManifest(savePath, submission)
                }
        }

        // Save analytics
        val analyticsFile = out.resolve("analytics.json.zip")
        JsonSerialiser.serialiseCompressed(analytics, analyticsFile)
        val an = JsonSerialiser.deserialiseCompressed(analyticsFile, Analytics::class)

        // Cleanup
        Files.walk(tmp)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::delete)
    }
}