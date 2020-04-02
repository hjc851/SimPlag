package me.haydencheers.simplag.transformationscope

import java.util.function.Function
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes
import javax.enterprise.inject.spi.AfterBeanDiscovery
import javax.enterprise.inject.spi.BeforeBeanDiscovery
import javax.enterprise.inject.spi.Extension
import javax.enterprise.inject.spi.ProcessAnnotatedType
import javax.inject.Singleton

@Singleton
class TransformationScopeExtension : Extension {
    fun beforeBeanDiscovery(@Observes bbd: BeforeBeanDiscovery) {
        bbd.addScope(TransformationScoped::class.java, true, false)
    }

    fun processAnnotatedType(@Observes pat: ProcessAnnotatedType<TransformationScopeContext>) {
        pat.veto()
    }

    fun afterBeanDiscovery(@Observes abd: AfterBeanDiscovery) {
        val ctx = TransformationScopeContext()

        abd.addBean<TransformationScopeContext>()
            .scope(ApplicationScoped::class.java)
            .types(TransformationScopeContext::class.java)
            .id("__trns_ctx")
            .createWith(Function { ctx })

        abd.addContext(ctx)
    }
}