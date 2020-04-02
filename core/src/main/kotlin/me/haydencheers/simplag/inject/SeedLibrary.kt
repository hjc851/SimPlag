package me.haydencheers.simplag.inject

import me.haydencheers.simplag.Parser
import me.haydencheers.simplag.util.FileUtils
import org.eclipse.jdt.core.dom.*
import java.nio.file.Path
import java.util.*
import javax.inject.Singleton

@Singleton
class SeedLibrary {

    val assignmentManifests = Collections.synchronizedList(mutableListOf<AssignmentManifest>())

    fun buildSeedLibrary(seedSubmissions: List<Path>) {
        seedSubmissions.parallelStream()
            .forEach { submission ->

                try {
                    val manifest = makeAssignmentManifest(submission)
                    assignmentManifests.add(manifest)
                } catch (e: Exception) {
                    System.err.println("Error processing seed ${submission}")
                    e.printStackTrace()
                }
            }
    }

    private fun makeAssignmentManifest(submission: Path): AssignmentManifest {
        val sources = FileUtils.listFiles(submission, ".java")

        return AssignmentManifest(
            submission.fileName.toString(),
            submission,
            sources,
            sources.map { makeFileManifest(it) }
        )
    }

    private fun makeFileManifest(file: Path): FileManifest {
        val typeVisitor = object : ASTVisitor() {
            val types = mutableListOf<AbstractTypeDeclaration>()

            override fun visit(node: TypeDeclaration): Boolean {
                types.add(node)
                return true
            }

            override fun visit(node: EnumDeclaration): Boolean {
                types.add(node)
                return true
            }

            override fun visit(node: AnnotationTypeDeclaration): Boolean {
                types.add(node)
                return true
            }
        }

        val ast = Parser.parse(file)
        ast.accept(typeVisitor)

        return FileManifest(
            file.fileName.toString(),
            file,
            ast,
            typeVisitor.types.map { makeClassManifest(it) }
        )
    }

    private fun makeClassManifest(cls: AbstractTypeDeclaration): TypeManifest {
        val methodVisitor = object : ASTVisitor() {
            val methods = mutableListOf<MethodDeclaration>()

            override fun visit(node: MethodDeclaration): Boolean {
                methods.add(node)
                return false
            }
        }

        cls.accept(methodVisitor)

        return TypeManifest(
            cls.name.identifier,
            cls,
            methodVisitor.methods.map { makeMethodManifest(it) }
        )
    }

    private fun makeMethodManifest(method: MethodDeclaration): MethodManifest {
        val blockVisitor = object : ASTVisitor() {
            val statements = mutableListOf<Statement>()

            override fun visit(node: Block): Boolean {
                if (node.parent !is MethodDeclaration)
                    statements.add(node)
                return true
            }

            override fun visit(node: DoStatement): Boolean {
                statements.add(node)
                return true
            }

            override fun visit(node: EnhancedForStatement): Boolean {
                statements.add(node)
                return true
            }

            override fun visit(node: ExpressionStatement): Boolean {
                statements.add(node)
                return true
            }

            override fun visit(node: ForStatement): Boolean {
                statements.add(node)
                return true
            }

            override fun visit(node: IfStatement): Boolean {
                statements.add(node)
                return true
            }

            override fun visit(node: SwitchStatement): Boolean {
                statements.add(node)
                return true
            }

            override fun visit(node: TryStatement): Boolean {
                statements.add(node)
                return true
            }

            override fun visit(node: WhileStatement): Boolean {
                statements.add(node)
                return true
            }
        }

        method.accept(blockVisitor)

        return MethodManifest(
            method.name.identifier,
            method,
            blockVisitor.statements.map { BlockManifest(it) }
        )
    }

    class AssignmentManifest(
        val name: String,
        val root: Path,
        val sources: List<Path>,
        val files: List<FileManifest>
    )

    class FileManifest(
        val name: String,
        val path: Path,
        val ast: ASTNode,
        val types: List<TypeManifest>
    )

    class TypeManifest(
        val name: String,
        val node: AbstractTypeDeclaration,
        val methods: List<MethodManifest>
    )

    class MethodManifest(
        val name: String,
        val node: MethodDeclaration,
        val blocks: List<BlockManifest>
    )

    class BlockManifest(
        val node: Statement
    )
}