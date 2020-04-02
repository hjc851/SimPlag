package me.haydencheers.simplag.mutate

import me.haydencheers.simplag.SimPlagConfig
import me.haydencheers.simplag.transformationscope.inject
import javax.inject.Singleton

@Singleton
class MutationFilterFactory {
    fun buildFilterStack(config: SimPlagConfig): List<MutationFilter> {
        val filters = mutableListOf<MutationFilter>()

        if (config.mutate.commenting.add) filters.add(inject<AddCommentFilter>())
        if (config.mutate.commenting.mutate) filters.add(inject<MutateCommentFilter>())
        if (config.mutate.commenting.remove) filters.add(inject<RemoveCommentFilter>())
        if (config.mutate.renameIdentifiers) filters.add(inject<RenameIdentifiersFilter>())
        if (config.mutate.reorderStatements) filters.add(inject<ReorderStatementsFilter>())
        if (config.mutate.reorderMemberDeclarations) filters.add(inject<ReorderMemberDeclarationsFilter>())
        if (config.mutate.swapOperands) filters.add(inject<SwapOperandsFilter>())
        if (config.mutate.upcastDataTypes) filters.add(inject<UpcastDataTypeFilter>())
//        if (config.mutate.switchToIf) filters.add(inject<SwitchToIfFilter>())
        if (config.mutate.forToWhile) filters.add(inject<ForToWhileFilter>())
        if (config.mutate.expandCompoundAssignment) filters.add(inject<ExpandCompoundAssignmentFilter>())
        if (config.mutate.expandIncDec) filters.add(inject<ExpandIncrementDecrementFilter>())
        if (config.mutate.splitVariableDeclarations) filters.add(inject<SplitVariableDeclarationsFilter>())
        if (config.mutate.assignDefaultValue) filters.add(inject<AssignDefaultValueFilter>())
        if (config.mutate.splitDeclAndAssignment) filters.add(inject<SplitDeclarationAndAssignmentFilter>())
//        if (config.mutate.groupVariableDeclarations) filters.add(inject<GroupVariableDeclarationsFilter>())

        return filters
    }
}