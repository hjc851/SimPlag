package me.haydencheers.simplag.inject

import me.haydencheers.simplag.SimPlagConfig
import me.haydencheers.simplag.transformationscope.inject
import javax.inject.Singleton

@Singleton
class InjectionFilterFactory {
    fun buildFilterStack(config: SimPlagConfig): List<InjectionFilter> {
        val filters = mutableListOf<InjectionFilter>()

        if (config.inject.injectFile) filters.add(inject<FileInjectionFilter>())
        if (config.inject.injectClass) filters.add(inject<ClassInjectionFilter>())
        if (config.inject.injectMethod) filters.add(inject<MethodInjectionFilter>())
        if (config.inject.injectBlock) filters.add(inject<BlockInjectionFilter>())

        return filters
    }
}