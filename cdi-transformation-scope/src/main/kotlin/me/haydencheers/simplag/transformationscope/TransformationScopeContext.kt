package me.haydencheers.simplag.transformationscope

import javax.enterprise.context.spi.Context
import javax.enterprise.context.spi.Contextual
import javax.enterprise.context.spi.CreationalContext
import javax.enterprise.inject.spi.Bean
import javax.inject.Singleton

@Singleton
open class TransformationScopeContext : Context {

    override fun isActive(): Boolean {
        val scope = threadStorage.get()
        return scope != null
    }

    override fun getScope(): Class<out Annotation> {
        return TransformationScoped::class.java
    }

    override fun <T : Any?> get(contextual: Contextual<T>, creationalContext: CreationalContext<T>): T {
        val bean = contextual as Bean<T>
        val instance = contextual.create(creationalContext)
        currentScope.setBean(
            bean.beanClass,
            instance as Any,
            contextual as Contextual<Any>,
            creationalContext as CreationalContext<Any>
        )
        return instance
    }

    override fun <T : Any?> get(contextual: Contextual<T>): T? {
        val bean = contextual as Bean<T>
        return currentScope.getBean(bean.beanClass) as T?
    }

    //
    //  Activation
    //

    open fun activate(handle: () -> Unit) {
        try {
            activateScope()
            handle()
        } catch (e: Exception) {
            throw e
        } finally {
            deactivateScope()
        }
    }

    private fun activateScope() {
        threadStorage.set(Scope())
    }

    private fun deactivateScope() {
        for ((type, ctx) in currentScope.beans) {
            val (bean, creational, creationContext) = ctx
            creational.destroy(bean, creationContext)
        }
        threadStorage.remove()
    }

    //
    //  Storage
    //

    private val threadStorage = InheritableThreadLocal<Scope>()

    private data class BeanContext(
        val bean: Any,
        val context: Contextual<Any>,
        val creationalContext: CreationalContext<Any>
    )

    private class Scope {

        val beans = mutableMapOf<Class<*>, BeanContext>()

        fun getBean(kls: Class<*>): Any? {
            return beans[kls]?.bean
        }

        fun setBean(kls: Class<*>, bean: Any, context: Contextual<Any>, creationalContext: CreationalContext<Any>) {
            beans[kls] = BeanContext(bean, context, creationalContext)
        }

        fun beanTypes(): Set<Class<*>> = beans.keys
    }

    private val currentScope: Scope
        get() = threadStorage.get()
}