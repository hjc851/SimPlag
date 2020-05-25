package me.haydencheers.simplag

import java.time.Instant
import javax.json.bind.annotation.JsonbTransient
import kotlin.random.Random

class SimPlagConfig {
    lateinit var input: String
    lateinit var injectionSources: String
    lateinit var output: String

    var randomSeed: Long = Instant.now().epochSecond

    var inject: InjectConfig = InjectConfig()
    var mutate: MutateConfig = MutateConfig()

    var copies: Int = 10

    @JsonbTransient
    lateinit var random: Random
}

class InjectConfig {
    var injectAssignment: Boolean = true
    var injectAssignmentCount: Int = 5

    var injectFile: Boolean = true
    var injectFileChance: Double = 1.0
    var injectFileMaxCount: Int = 1

    var injectClass: Boolean = true
    var injectClassChance: Double = 1.0
    var injectClassMaxCount: Int = 1

    var injectMethod: Boolean = true
    var injectMethodChance: Double = 1.0
    var injectMethodMaxCount = 2

    var injectBlock: Boolean = true
    var injectBlockChance: Double = 1.0
    var injectBlockMaxStatements: Int = 10
}

class MutateConfig {
    var commenting = CommentingConfig()

    var renameIdentifiers = true
    var renameIdentifiersChance = 1.0

    var reorderStatements = true
    var reorderStatementsChance = 1.0
    var reorderMemberDeclarations = true
    var reorderMemberDeclarationsChance = 1.0

    var swapOperands = true
    var swapOperandsChance = 1.0

    var upcastDataTypes = true
    var upcastDataTypesChance = 1.0

    var switchToIf = true
    var switchToIfChance = 1.0
    var forToWhile = true
    var forToWhileChance = 1.0
    var expandCompoundAssignment = true
    var expandCompoundAssignmentChance = 1.0
    var expandIncDec = true
    var expandIncDecChance= 1.0

    var splitVariableDeclarations = true
    var splitVariableDeclarationsChance = 1.0
    var assignDefaultValue = true
    var assignDefaultValueChance = 1.0
    var splitDeclAndAssignment = true
    var splitDeclAndAssignmentChance = 1.0
    var groupVariableDeclarations = true
    var groupVariableDeclarationsChance = 1.0
}

class CommentingConfig {
    var add = true
    var addChance = 0.6

    var remove = true
    var removeChance = 0.4

    var mutate = true
    var mutateChance = 1.0
}