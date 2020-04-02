package me.haydencheers.simplag

import me.haydencheers.simplag.manifest.MutableAssignmentManifest
import org.eclipse.jdt.internal.core.dom.NaiveASTFlattener
import java.nio.file.Files
import java.nio.file.Path

class VariantWriter {
    fun writeManifest(outRoot: Path, manifest: MutableAssignmentManifest) {
        manifest.files.forEach { fileManifest ->
            val file = outRoot.resolve(fileManifest.relativeName)
            if (!Files.exists(file.parent)) Files.createDirectories(file.parent)

            Files.newBufferedWriter(file).use { fout ->
                val flattener = NaiveASTFlattener()
                fileManifest.ast.accept(flattener)

                fout.appendln(flattener.result)
            }
        }
    }
}
