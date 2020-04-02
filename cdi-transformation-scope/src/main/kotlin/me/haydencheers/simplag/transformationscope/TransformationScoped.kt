package me.haydencheers.simplag.transformationscope

import java.lang.annotation.Inherited
import javax.enterprise.context.NormalScope

@Inherited
@NormalScope(passivating = false)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
annotation class TransformationScoped