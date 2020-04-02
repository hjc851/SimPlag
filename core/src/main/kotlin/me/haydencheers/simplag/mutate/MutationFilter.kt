package me.haydencheers.simplag.mutate

import com.thedeanda.lorem.LoremIpsum
import me.haydencheers.simplag.ledger.Analytics
import me.haydencheers.simplag.SimPlagConfig
import me.haydencheers.simplag.manifest.MutableAssignmentManifest
import me.haydencheers.simplag.synonyms.ISynonymClient
import me.haydencheers.simplag.ast.ASTCloner
import me.haydencheers.simplag.ledger.*
import me.haydencheers.simplag.util.ReflectionUtils
import org.eclipse.jdt.core.dom.*
import javax.inject.Inject
import javax.inject.Singleton

abstract class MutationFilter {
    @Inject
    protected open lateinit var analytics: Analytics

    abstract fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest)
}

@Singleton
class AddCommentFilter: MutationFilter() {

    val lorem = LoremIpsum.getInstance()

    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        for (file in manifest.files) {
            for (type in file.types) {
                if (config.random.nextDouble(0.0, 1.0) <= config.mutate.commenting.addChance) {
                    val text = lorem.getWords(5, 15)
                    val node = type.node
                    val comment = makeComment(node.ast, text)
                    node.javadoc = comment

                    analytics.mutateAddComment.add(AddCommentRecord(
                        manifest.name,
                        manifest.variantId,
                        file.relativeName,
                        type.name,
                        null
                    ))
                }

                for (method in type.methods) {
                    if (config.random.nextDouble(0.0, 1.0) <= config.mutate.commenting.addChance) {
                        val text = lorem.getWords(5, 15)
                        val node = method.node
                        val comment = makeComment(node.ast, text)
                        node.javadoc = comment

                        analytics.mutateAddComment.add(AddCommentRecord(
                            manifest.name,
                            manifest.variantId,
                            file.relativeName,
                            type.name,
                            method.name
                        ))
                    }
                }
            }
        }
    }

    private fun makeComment(ast: AST, text: String): Javadoc {
        val comment = ast.newJavadoc()
        val tag = ast.newTagElement()
        val textElement = ast.newTextElement()
        textElement.text = text

        tag.fragments().add(textElement)
        comment.tags().add(tag)

        return comment
    }
}

@Singleton
class RemoveCommentFilter: MutationFilter() {
    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        for (file in manifest.files) {
            for (type in file.types) {
                if (type.node.javadoc != null && config.random.nextDouble(0.0, 1.0) <= config.mutate.commenting.removeChance) {
                    type.node.javadoc = null

                    analytics.mutateRemoveComment.add(RemoveCommentRecord(
                        manifest.name,
                        manifest.variantId,
                        file.relativeName,
                        type.name,
                        null
                    ))
                }

                for (method in type.methods) {
                    if (method.node.javadoc != null && config.random.nextDouble(0.0, 1.0) <= config.mutate.commenting.removeChance) {
                        method.node.javadoc = null

                        analytics.mutateRemoveComment.add(RemoveCommentRecord(
                            manifest.name,
                            manifest.variantId,
                            file.relativeName,
                            type.name,
                            method.name
                        ))
                    }
                }
            }
        }
    }
}

@Singleton
class MutateCommentFilter: MutationFilter() {

    val lorem = LoremIpsum.getInstance()

    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        for (file in manifest.files) {
            for (type in file.types) {
                if (type.node.javadoc != null && config.random.nextDouble(0.0, 1.0) <= config.mutate.commenting.mutateChance) {
                    val text = lorem.getWords(5, 15)
                    val javadoc = type.node.javadoc
                    val ast = type.node.ast

                    val tag = ast.newTagElement()
                    val textElement = ast.newTextElement()
                    tag.fragments().add(textElement)
                    textElement.text = text

                    for (i in javadoc.tags().size-1 downTo 0) {
                        if (config.random.nextDouble(0.0, 1.0) <= config.mutate.commenting.mutateChance) {
                            javadoc.tags().removeAt(i)
                        }
                    }

                    javadoc.tags().add(tag)
                    analytics.mutateModifyComment.add(MutateCommentRecord(
                        manifest.name,
                        manifest.variantId,
                        file.relativeName,
                        type.name,
                        null
                    ))
                }

                for (method in type.methods) {
                    if (method.node.javadoc != null && config.random.nextDouble(0.0, 1.0) <= config.mutate.commenting.mutateChance) {
                        val text = lorem.getWords(5, 15)
                        val javadoc = method.node.javadoc
                        val ast = type.node.ast

                        val tag = ast.newTagElement()
                        val textElement = ast.newTextElement()
                        tag.fragments().add(textElement)
                        textElement.text = text

                        for (i in javadoc.tags().size-1 downTo 0) {
                            if (config.random.nextDouble(0.0, 1.0) <= config.mutate.commenting.mutateChance) {
                                javadoc.tags().removeAt(i)
                            }
                        }

                        javadoc.tags().add(tag)
                        analytics.mutateModifyComment.add(MutateCommentRecord(
                            manifest.name,
                            manifest.variantId,
                            file.relativeName,
                            type.name,
                            method.name
                        ))
                    }
                }
            }
        }
    }
}

@Singleton
class RenameIdentifiersFilter: MutationFilter() {

    @Inject
    lateinit var client: ISynonymClient

    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        if (config.random.nextDouble(0.0, 1.0) <= config.mutate.renameIdentifiersChance) {
            val nameIdentifierVisitor = object : ASTVisitor() {
                val names = mutableSetOf<String>()

                override fun visit(node: TypeDeclaration): Boolean {
                    names.add(node.name.identifier)
                    return super.visit(node)
                }

                override fun visit(node: AnnotationTypeDeclaration): Boolean {
                    names.add(node.name.identifier)
                    return super.visit(node)
                }

                override fun visit(node: EnumDeclaration): Boolean {
                    names.add(node.name.identifier)
                    return super.visit(node)
                }

                override fun visit(node: EnumConstantDeclaration): Boolean {
                    names.add(node.name.identifier)
                    return super.visit(node)
                }

                override fun visit(node: MethodDeclaration): Boolean {
                    names.add(node.name.identifier)
                    return super.visit(node)
                }

                override fun visit(node: VariableDeclarationFragment): Boolean {
                    names.add(node.name.identifier)
                    return super.visit(node)
                }

                override fun visit(node: SingleVariableDeclaration): Boolean {
                    names.add(node.name.identifier)
                    return super.visit(node)
                }
            }

            for (file in manifest.files) {
                file.ast.accept(nameIdentifierVisitor)
            }

            val idMappings = mutableMapOf<String, String>()
            for (name in nameIdentifierVisitor.names) {
                try {
                    val synonym = client.getSynonymsForTerm(name).random(config.random)
                    idMappings[name] = synonym
                } catch (e: Exception) {
                    e.printStackTrace()
                    idMappings[name] = name
                }
            }

            val nameMapperVisitor = object : ASTVisitor() {
                override fun visit(node: TypeDeclaration): Boolean {
                    try {
                        node.name.identifier = idMappings[node.name.identifier] ?: node.name.identifier
                    } catch (e: Exception) {}
                    return super.visit(node)
                }

                override fun visit(node: AnnotationTypeDeclaration): Boolean {
                    try {
                        node.name.identifier = idMappings[node.name.identifier] ?: node.name.identifier
                    } catch (e: Exception) {}
                    return super.visit(node)
                }

                override fun visit(node: EnumDeclaration): Boolean {
                    try {
                        node.name.identifier = idMappings[node.name.identifier] ?: node.name.identifier
                    } catch (e: Exception) {}
                    return super.visit(node)
                }

                override fun visit(node: EnumConstantDeclaration): Boolean {
                    try {
                        node.name.identifier = idMappings[node.name.identifier] ?: node.name.identifier
                    } catch (e: Exception) {}
                    return super.visit(node)
                }

                override fun visit(node: MethodDeclaration): Boolean {
                    try {
                        node.name.identifier = idMappings[node.name.identifier] ?: node.name.identifier
                    } catch (e: Exception) {}
                    return super.visit(node)
                }

                override fun visit(node: VariableDeclarationFragment): Boolean {
                    try {
                        node.name.identifier = idMappings[node.name.identifier] ?: node.name.identifier
                    } catch (e: Exception) {}
                    return super.visit(node)
                }

                override fun visit(node: SingleVariableDeclaration): Boolean {
                    try {
                        node.name.identifier = idMappings[node.name.identifier] ?: node.name.identifier
                    } catch (e: Exception) {}
                    return super.visit(node)
                }
            }

            for (file in manifest.files) {
                file.ast.accept(nameMapperVisitor)
            }

            analytics.mutateRenameIdentifiers.add(
                RenameIdentifiersRecord(
                    manifest.name,
                    manifest.variantId,
                    idMappings
                )
            )
        }
    }
}

@Singleton
class ReorderStatementsFilter: MutationFilter() {
    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        for (file in manifest.files) {
            for (type in file.types) {
                for (method in type.methods) {
                    for (statement in method.statements) {
                        val node = statement.node
                        if (node is Block) {
                            if (config.random.nextDouble(0.0, 1.0) <= config.mutate.reorderStatementsChance) {
                                val statements = node.statements()
                                if (statements.size > 1) {
                                    for (i in 0 until statements.size-1) {
                                        val j = config.random.nextInt(statements.size)

                                        val stmt = statements.removeAt(i)
                                        try {
                                            statements.add(j, stmt)
                                        } catch (e: Exception) {
                                            statements.add(stmt)
                                        }
                                    }
                                }
                            }

                            analytics.mutateReorderStatements.add(ReorderStatementsRecord(
                                manifest.name,
                                manifest.variantId,
                                file.relativeName,
                                type.name,
                                method.name
                            ))
                        }
                    }
                }
            }
        }
    }
}

@Singleton
class ReorderMemberDeclarationsFilter: MutationFilter() {
    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        for (file in manifest.files) {
            for (type in file.types) {
                if (config.random.nextDouble(0.0, 1.0) <= config.mutate.reorderMemberDeclarationsChance) {
                    val decl = type.node
                    val bodyDeclarations = decl.bodyDeclarations()

                    if (bodyDeclarations.size > 1) {
                        for (i in 0 until bodyDeclarations.size-1) {
                            val j = config.random.nextInt(bodyDeclarations.size)

                            val stmt = bodyDeclarations.removeAt(i)
                            try {
                                bodyDeclarations.add(j, stmt)
                            } catch (e: Exception) {
                                bodyDeclarations.add(stmt)
                            }
                        }

                        analytics.mutateReorderMembers.add(ReorderMembersRecord(
                            manifest.name,
                            manifest.variantId,
                            file.relativeName,
                            type.name
                        ))
                    }
                }
            }
        }
    }
}

@Singleton
class SwapOperandsFilter: MutationFilter() {
    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        for (file in manifest.files) {
            for (type in file.types) {
                for (method in type.methods) {
                    val operandSwapperVisitor = object : ASTVisitor() {
                        var counter = 0

                        override fun visit(node: InfixExpression): Boolean {
                            if (config.random.nextDouble(0.0, 1.0) <= config.mutate.swapOperandsChance) {
                                val ast = node.ast
                                val origLeft = ASTCloner.clone(node.leftOperand, ast) as Expression
                                val origRight = ASTCloner.clone(node.rightOperand, ast) as Expression

                                node.leftOperand = origRight
                                node.rightOperand = origLeft

                                counter++
                            }

                            return super.visit(node)
                        }
                    }

                    method.node.accept(operandSwapperVisitor)
                    if (operandSwapperVisitor.counter > 0) {
                        analytics.mutateSwapOperands.add(SwapOperandsRecord(
                            manifest.name,
                            manifest.variantId,
                            file.relativeName,
                            type.name,
                            method.name,
                            operandSwapperVisitor.counter
                        ))
                    }
                }
            }



        }
    }
}

@Singleton
class UpcastDataTypeFilter: MutationFilter() {
    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        for (file in manifest.files) {
            for (type in file.types) {
                for (method in type.methods) {
                    val visitor = object : ASTVisitor() {
                        var counter = 0
                        override fun visit(node: SingleVariableDeclaration): Boolean {
                            val type = node.type
                            if (type is SimpleType) {
                                val name = type.name
                                if (name is SimpleName) {
                                    val typename = name.identifier
                                    if (typename == "byte" || typename == "short" || typename == "int" || typename == "long" || typename == "float") {
                                        if (config.random.nextDouble(0.0, 1.0) <= config.mutate.upcastDataTypesChance) {
                                            when (typename) {
                                                "byte" -> name.identifier = "short"
                                                "short" -> name.identifier = "int"
                                                "int" -> name.identifier = "long"
                                                "long" -> name.identifier = "float"
                                                "float" -> name.identifier = "double"
                                            }

                                            counter++
                                        }
                                    }
                                }
                            }

                            return super.visit(node)
                        }

                        override fun visit(node: VariableDeclarationStatement): Boolean {
                            if (node.type.isSimpleType && config.random.nextDouble(0.0, 1.0) <= config.mutate.upcastDataTypesChance) {
                                val type = node.type
                                if (type is SimpleType) {
                                    val name = type.name
                                    if (name is SimpleName) {
                                        val typename = name.identifier
                                        if (typename == "byte" || typename == "short" || typename == "int" || typename == "long" || typename == "float") {
                                            if (config.random.nextDouble(0.0, 1.0) <= config.mutate.upcastDataTypesChance) {
                                                when (typename) {
                                                    "byte" -> name.identifier = "short"
                                                    "short" -> name.identifier = "int"
                                                    "int" -> name.identifier = "long"
                                                    "long" -> name.identifier = "float"
                                                    "float" -> name.identifier = "double"
                                                }
                                                counter++
                                            }
                                        }
                                    }
                                }
                            }
                            return super.visit(node)
                        }

                        override fun visit(node: VariableDeclarationExpression): Boolean {
                            if (node.type.isSimpleType && config.random.nextDouble(0.0, 1.0) <= config.mutate.upcastDataTypesChance) {
                                val type = node.type
                                if (type is SimpleType) {
                                    val name = type.name
                                    if (name is SimpleName) {
                                        val typename = name.identifier
                                        if (typename == "byte" || typename == "short" || typename == "int" || typename == "long" || typename == "float") {
                                            if (config.random.nextDouble(0.0, 1.0) <= config.mutate.upcastDataTypesChance) {
                                                when (typename) {
                                                    "byte" -> name.identifier = "short"
                                                    "short" -> name.identifier = "int"
                                                    "int" -> name.identifier = "long"
                                                    "long" -> name.identifier = "float"
                                                    "float" -> name.identifier = "double"
                                                }
                                                counter++
                                            }
                                        }
                                    }
                                }
                            }
                            return super.visit(node)
                        }
                    }

                    method.node.accept(visitor)
                    if (visitor.counter > 0) {
                        analytics.mutateUpcastDataType.add(UpcastDataTypeRecord(
                            manifest.name,
                            manifest.variantId,
                            file.relativeName,
                            type.name,
                            method.name,
                            visitor.counter
                        ))
                    }
                }
            }
        }
    }
}

//@Singleton
//class SwitchToIfFilter: MutationFilter() {
//    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
//        val visitor = object : ASTVisitor() {
//            override fun visit(node: SwitchStatement): Boolean {
//                if (config.random.nextDouble(0.0, 1.0) <= config.mutate.switchToIfChance) {
//                    val ifStmt = convertSwitchToIf(node)
//                    node.parent.replace(node, ifStmt)
//                }
//
//                return super.visit(node)
//            }
//        }
//
//        for (file in manifest.files) {
//            file.ast.accept(visitor)
//        }
//    }
//
//    private fun convertSwitchToIf(node: SwitchStatement): IfStatement {
//        TODO()
//    }
//}

@Singleton
class ForToWhileFilter: MutationFilter() {
    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        for (file in manifest.files) {
            for (type in file.types) {
                for (method in type.methods) {
                    val visitor = object : ASTVisitor() {
                        var counter = 0
                        override fun visit(node: ForStatement): Boolean {
                            if (config.random.nextDouble(0.0, 1.0) <= config.mutate.forToWhileChance) {
                                val whileStatement = forToWhile(node)
                                node.parent.replace(node, whileStatement)
                                counter++
                            }

                            return super.visit(node)
                        }
                    }

                    method.node.accept(visitor)
                    if (visitor.counter > 0) {
                        analytics.mutateSwapForToWhile.add(SwapForToWhileRecord(
                            manifest.name,
                            manifest.variantId,
                            file.relativeName,
                            type.name,
                            method.name,
                            visitor.counter
                        ))
                    }
                }
            }
        }
    }

    private fun forToWhile(stmt: ForStatement): Statement {
        val ast = stmt.ast

        val whileStatement = ast.newWhileStatement()
        whileStatement.expression = ASTCloner.clone(stmt.expression, ast) as Expression

        val whileBody = ast.newBlock()
        whileStatement.body = whileBody
        whileBody.statements().add(ASTCloner.clone(stmt.body, ast))
        for (updt in stmt.updaters()) {
            val update = ASTCloner.clone(updt as Expression, ast) as Expression
            val updateStmt = ast.newExpressionStatement(update)
            whileBody.statements().add(updateStmt)
        }

        val outerBlock = ast.newBlock()
        for (init in stmt.initializers()) {
            val initExpr = ASTCloner.clone(init as Expression, ast) as Expression
            val initStmt = ast.newExpressionStatement(initExpr)
            outerBlock.statements().add(initStmt)
        }
        outerBlock.statements().add(whileStatement)

        return outerBlock
    }
}

@Singleton
class ExpandCompoundAssignmentFilter: MutationFilter() {
    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
       for (file in manifest.files) {
           for (type in file.types) {
               for (method in type.methods) {
                   val visitor = object : ASTVisitor() {
                       var counter = 0

                       override fun visit(node: Assignment): Boolean {
                           if (node.operator != Assignment.Operator.ASSIGN) {
                               if (config.random.nextDouble(0.0, 1.0) <= config.mutate.expandCompoundAssignmentChance) {
                                   val equivalentOp = equivalentBinaryOperatorFor(node.operator)

                                   if (equivalentOp != null) {
                                       node.operator = Assignment.Operator.ASSIGN

                                       val ast = node.ast
                                       val infix = ast.newInfixExpression()
                                       infix.operator = equivalentOp
                                       infix.leftOperand = ASTCloner.clone(node.leftHandSide, ast) as Expression
                                       infix.rightOperand = ASTCloner.clone(node.rightHandSide, ast) as Expression

                                       node.rightHandSide = infix
                                       counter++
                                   }
                               }
                           }

                           return super.visit(node)
                       }
                   }

                   method.node.accept(visitor)
                   if (visitor.counter > 0) {
                       analytics.mutateExpandCompoundAssignment.add(
                           ExpandCompoundAssignmentRecord(
                               manifest.name,
                               manifest.variantId,
                               file.relativeName,
                               type.name,
                               method.name,
                               visitor.counter
                       ))
                   }
               }
           }
        }
    }

    private fun equivalentBinaryOperatorFor(op: Assignment.Operator): InfixExpression.Operator? {
        return when (op) {
            Assignment.Operator.PLUS_ASSIGN -> InfixExpression.Operator.PLUS
            Assignment.Operator.MINUS_ASSIGN -> InfixExpression.Operator.MINUS
            Assignment.Operator.TIMES_ASSIGN -> InfixExpression.Operator.TIMES
            Assignment.Operator.DIVIDE_ASSIGN -> InfixExpression.Operator.DIVIDE
            Assignment.Operator.BIT_AND_ASSIGN -> InfixExpression.Operator.AND
            Assignment.Operator.BIT_OR_ASSIGN -> InfixExpression.Operator.OR
            Assignment.Operator.BIT_XOR_ASSIGN -> InfixExpression.Operator.XOR
            Assignment.Operator.REMAINDER_ASSIGN -> InfixExpression.Operator.REMAINDER
            Assignment.Operator.LEFT_SHIFT_ASSIGN -> InfixExpression.Operator.LEFT_SHIFT
            Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN -> InfixExpression.Operator.RIGHT_SHIFT_SIGNED
            Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN -> InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED
            else -> null
        }
    }
}

@Singleton
class ExpandIncrementDecrementFilter: MutationFilter() {
    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        for (file in manifest.files) {
            for (type in file.types) {
                for (method in type.methods) {
                    val visitor = object: ASTVisitor() {
                        var counter = 0

                        override fun visit(node: PrefixExpression): Boolean {
                            if (node.operator == PrefixExpression.Operator.INCREMENT) {
                                if (config.random.nextDouble(0.0, 1.0) <= config.mutate.expandIncDecChance) {
                                    val ast = node.ast

                                    val expr = ast.newInfixExpression()
                                    expr.operator = InfixExpression.Operator.PLUS
                                    expr.leftOperand = ASTCloner.clone(node.operand, ast) as Expression
                                    expr.rightOperand = ast.newNumberLiteral("1")

                                    val asgn = ast.newAssignment()
                                    asgn.operator = Assignment.Operator.ASSIGN
                                    asgn.leftHandSide = ASTCloner.clone(node.operand, ast) as Expression
                                    asgn.rightHandSide = expr

                                    node.parent.replace(node, asgn)
                                    counter++
                                }
                            }

                            if (node.operator == PrefixExpression.Operator.DECREMENT) {
                                if (config.random.nextDouble(0.0, 1.0) <= config.mutate.expandIncDecChance) {
                                    val ast = node.ast

                                    val expr = ast.newInfixExpression()
                                    expr.operator = InfixExpression.Operator.MINUS
                                    expr.leftOperand = ASTCloner.clone(node.operand, ast) as Expression
                                    expr.rightOperand = ast.newNumberLiteral("1")

                                    val asgn = ast.newAssignment()
                                    asgn.operator = Assignment.Operator.ASSIGN
                                    asgn.leftHandSide = ASTCloner.clone(node.operand, ast) as Expression
                                    asgn.rightHandSide = expr

                                    node.parent.replace(node, asgn)
                                    counter++
                                }
                            }

                            if (node.operator == PrefixExpression.Operator.NOT) {
                                if (config.random.nextDouble(0.0, 1.0) <= config.mutate.expandIncDecChance) {
                                    val ast = node.ast

                                    val expr = ast.newInfixExpression()
                                    expr.operator = InfixExpression.Operator.EQUALS
                                    expr.leftOperand = ASTCloner.clone(node.operand, ast) as Expression
                                    expr.rightOperand = ast.newBooleanLiteral(false)

                                    node.parent.replace(node, expr)
                                    counter++
                                }
                            }

                            return super.visit(node)
                        }
                    }

                    method.node.accept(visitor)
                    if (visitor.counter > 0) {
                        analytics.mutateExpandIncrementDecrement.add(ExpandIncrementDecrementRecord(
                            manifest.name,
                            manifest.variantId,
                            file.relativeName,
                            type.name,
                            method.name,
                            visitor.counter
                        ))
                    }
                }
            }
        }
    }
}

@Singleton
class SplitVariableDeclarationsFilter: MutationFilter() {
    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        for (file in manifest.files) {
            for (type in file.types) {
                for (method in type.methods) {
                    if (config.random.nextDouble(0.0, 1.0) <= config.mutate.splitVariableDeclarationsChance) {
                        val visitor = object : ASTVisitor() {
                            var counter = 0

                            override fun visit(node: VariableDeclarationStatement): Boolean {
                                if (node.fragments().count() > 1 && node.parent is Block) {
                                    val ast = node.ast
                                    val pblock = node.parent as Block
                                    val idx = pblock.statements().indexOf(node)
                                    pblock.statements().removeAt(idx)

                                    for (i in node.fragments().count()-1 downTo 0) {
                                        val frag = ASTCloner.clone(node.fragments()[i] as ASTNode, ast) as VariableDeclarationFragment
                                        val stmt = ast.newVariableDeclarationStatement(frag)
                                        stmt.type = ASTCloner.clone(node.type, ast) as Type
                                        pblock.statements().add(idx, stmt)
                                    }

                                    counter++
                                }

                                return super.visit(node)
                            }
                        }

                        method.node.accept(visitor)
                        if (visitor.counter > 0) {
                            analytics.mutateSplitVariableDeclaration.add(
                                SplitVariableDeclarationsRecord(
                                    manifest.name,
                                    manifest.variantId,
                                    file.relativeName,
                                    type.name,
                                    method.name,
                                    visitor.counter
                            ))
                        }
                    }
                }
            }
        }
    }
}

@Singleton
class AssignDefaultValueFilter: MutationFilter() {
    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        for (file in manifest.files) {
            for (type in file.types) {
                for (method in type.methods) {
                    if (config.random.nextDouble(0.0, 1.0) <= config.mutate.assignDefaultValueChance) {
                        val visitor = object : ASTVisitor() {
                            var counter = 0

                            override fun visit(node: VariableDeclarationFragment): Boolean {
                                if (node.initializer == null && node.parent is VariableDeclarationStatement) {
                                    val ast = node.ast
                                    val declStmt = node.parent as VariableDeclarationStatement
                                    val declType = declStmt.type.toString()

                                    node.initializer = when (declType) {
                                        "byte", "short", "int", "long" -> ast.newNumberLiteral("0")
                                        "double", "float" -> ast.newNumberLiteral("0.0")
                                        "char" -> ast.newCharacterLiteral()
                                        "boolean" -> ast.newBooleanLiteral(false)

                                        else -> ast.newNullLiteral()
                                    }
                                    counter++
                                }
                                return super.visit(node)
                            }
                        }

                        method.node.accept(visitor)
                        if (visitor.counter > 0) {
                            analytics.mutateAssignDefaultValue.add(
                                AssignDefaultValueRecord(
                                    manifest.name,
                                    manifest.variantId,
                                    file.relativeName,
                                    type.name,
                                    method.name,
                                    visitor.counter
                                ))
                        }
                    }
                }
            }
        }
    }
}

@Singleton
class SplitDeclarationAndAssignmentFilter: MutationFilter() {
    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
        for (file in manifest.files) {
            for (type in file.types) {
                for (method in type.methods) {
                    if (config.random.nextDouble(0.0, 1.0) <= config.mutate.splitDeclAndAssignmentChance) {
                        val visitor = object : ASTVisitor() {
                            var counter = 0

                            override fun visit(node: VariableDeclarationStatement): Boolean {
                                if (node.parent is Block) {
                                    val ast = node.ast
                                    val declBlock = node.parent as Block
                                    val declIndex = declBlock.statements().indexOf(node)

                                    for (i in node.fragments().size-1 downTo 0) {
                                        val frag = node.fragments()[i] as VariableDeclarationFragment
                                        if (frag.initializer != null) {
                                            val name = ASTCloner.clone(frag.name, ast)
                                            val initaliser = ASTCloner.clone(frag.initializer, ast) as Expression
                                            val asign = ast.newAssignment()
                                            asign.operator = Assignment.Operator.ASSIGN
                                            asign.leftHandSide = name as Expression
                                            asign.rightHandSide = initaliser

                                            val exprStmt = ast.newExpressionStatement(asign)
                                            declBlock.statements().add(declIndex+1, exprStmt)

                                            frag.initializer = null
                                            counter++
                                        }
                                    }
                                }
                                return super.visit(node)
                            }
                        }

                        method.node.accept(visitor)
                        if (visitor.counter > 0) {
                            analytics.mutateSplitDeclarationAndAssignment.add(
                                SplitDeclarationAndAssignmentRecord(
                                    manifest.name,
                                    manifest.variantId,
                                    file.relativeName,
                                    type.name,
                                    method.name,
                                    visitor.counter
                                ))
                        }
                    }
                }
            }
        }
    }
}

//@Singleton
//class GroupVariableDeclarationsFilter: MutationFilter() {
//    override fun process(config: SimPlagConfig, manifest: MutableAssignmentManifest) {
//        if (config.random.nextDouble(0.0, 1.0) <= config.mutate.splitVariableDeclarationsChance) {
//            for (file in manifest.files) {
//                for (type in file.types) {
//                    for (method in type.methods) {
//                        for (i in 0 until method.statements.count()-2) {
//                            val stmt1 = method.statements[i]
//                            val stmt2 = methodsta
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

//
//  Util
//

fun ASTNode.replace(existing: ASTNode, replacement: ASTNode) {
    val fields = ReflectionUtils.getAllDeclaredInstanceFields(this.javaClass)

    for (field in fields) {
        field.isAccessible = true
        val value = field.get(this)

        if (value is ASTNode && value === existing) {
            field.set(this, replacement)
            ASTCloner.nodeParentField.set(replacement, this)
            return

        } else if (value is java.util.List<*>) {
            val index = value.indexOf(existing)
            if (index >= 0) {
                val method = value::class.java.getMethod("set", Int::class.java, Object::class.java)
                method.isAccessible = true
                method.invoke(value, index, replacement)

                ASTCloner.nodeParentField.set(replacement, this)
                return
            }
        }
    }

    throw IllegalArgumentException("Cannot replace child ${existing} of ${this} with ${replacement}")
}