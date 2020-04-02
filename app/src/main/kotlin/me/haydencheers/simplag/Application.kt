package me.haydencheers.simplag

import javax.enterprise.inject.se.SeContainerInitializer

object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        val initialiser = SeContainerInitializer.newInstance()
        initialiser.initialize().use { container ->
            container.select(SimPlag::class.java)
                .get()
                .run(args)
        }
    }
}