package me.haydencheers.simplag.manifest

import me.haydencheers.simplag.Parser
import me.haydencheers.simplag.util.FileUtils
import org.eclipse.jdt.core.dom.*
import java.nio.file.Path

object Manifests {
    fun buildForSubmission(submission: Path, variantId: Int): MutableAssignmentManifest {
        val sources = FileUtils.listFiles(submission, ".java")

        return MutableAssignmentManifest(
            submission.fileName.toString(),
            submission,
            variantId,
            sources.map { buildForFile(it, submission) }.toMutableList()
        )
    }

    fun buildForFile(file: Path, root: Path): MutableFileManifest {
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

        return MutableFileManifest(
            root.relativize(file).toString(),
            file,
            ast,
            typeVisitor.types.map { buildForTypeDeclaration(it) }.toMutableList()
        )
    }

    fun buildForTypeDeclaration(node: AbstractTypeDeclaration): MutableTypeManifest {
        val methodVisitor = object : ASTVisitor() {
            val methods = mutableListOf<MethodDeclaration>()

            override fun visit(node: MethodDeclaration): Boolean {
                if (node.body != null) methods.add(node)
                return false
            }
        }

        node.accept(methodVisitor)

        return MutableTypeManifest(
            node.name.identifier,
            node,
            methodVisitor.methods.map { buildForMethod(it) }.toMutableList()
        )
    }

    fun buildForMethod(method: MethodDeclaration): MutableMethodManifest {
        val blockVisitor = object : ASTVisitor() {
            val statements = mutableListOf<Statement>()

            override fun visit(node: Block): Boolean {
                if (node.parent !is MethodDeclaration) statements.add(node)
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

            override fun visit(node: BreakStatement): Boolean {
                statements.add(node)
                return true
            }

            override fun visit(node: ContinueStatement): Boolean {
                statements.add(node)
                return true
            }

            override fun visit(node: EmptyStatement): Boolean {
                statements.add(node)
                return true
            }

            override fun visit(node: ReturnStatement): Boolean {
                statements.add(node)
                return true
            }

            override fun visit(node: ThrowStatement): Boolean {
                statements.add(node)
                return true
            }
        }

        method.accept(blockVisitor)

        return MutableMethodManifest(
            method.name.identifier,
            method,
            blockVisitor.statements.map { MutableStatementManifest(it) }
        )
    }
}

class MutableAssignmentManifest(
    var name: String,
    var root: Path,
    var variantId: Int,
    var files: MutableList<MutableFileManifest>
)

class MutableFileManifest(
    var relativeName: String,
    var originalPath: Path,
    var ast: ASTNode,
    var types: MutableList<MutableTypeManifest>
)

class MutableTypeManifest(
    var name: String,
    var node: AbstractTypeDeclaration,
    var methods: List<MutableMethodManifest>
)

class MutableMethodManifest(
    var name: String,
    var node: MethodDeclaration,
    var statements: List<MutableStatementManifest>
)

class MutableStatementManifest(
    var node: Statement
)