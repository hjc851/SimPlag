package me.haydencheers.simplag.util

import java.lang.reflect.Field
import java.lang.reflect.Modifier

object ReflectionUtils {
    fun getAllDeclaredInstanceFields(cls: Class<*>): List<Field> {
        val allFields = mutableListOf<Field>()
        var currentClass: Class<*>? = cls
        while (currentClass != null) {
            allFields.addAll(currentClass.declaredFields)
            currentClass = currentClass.superclass
        }
        return allFields.distinctBy { it.name }
            .filter { !Modifier.isStatic(it.modifiers) }
    }
}