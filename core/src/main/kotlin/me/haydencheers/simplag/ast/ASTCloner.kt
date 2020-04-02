package me.haydencheers.simplag.ast

import me.haydencheers.simplag.util.ReflectionUtils
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTNode
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

object ASTCloner {

    private val nodeCls = Class.forName("org.eclipse.jdt.core.dom.ASTNode")
    private val nodeListCls = Class.forName("org.eclipse.jdt.core.dom.ASTNode\$NodeList")
    private val nodeListConstructor = nodeListCls.declaredConstructors.single()
    private val pdescField = nodeListCls.getDeclaredField("propertyDescriptor")
    val nodeParentField = nodeCls.getDeclaredField("parent")

    init {
        nodeListConstructor.isAccessible = true
        pdescField.isAccessible = true
        nodeParentField.isAccessible = true
    }

    fun clone(node: ASTNode, ast: AST): ASTNode {
        val ncls = node.javaClass
        val cnstr = ncls.getDeclaredConstructor(AST::class.java)
        cnstr.isAccessible = true

        val clone = cnstr.newInstance(ast)

        val fields = ReflectionUtils.getAllDeclaredInstanceFields(ncls)
            .filterNot { it.name == "parent" }  // Causes stack overflow due to recursion
            .filterNot { it.name == "ast" }     // Causes clone to be part of original AST

        for (field in fields) {
            field.isAccessible = true
            val value = field.get(node)

            val clonedValue = when {
                value is ASTNode -> {
                    clone(value, ast)
                }

                nodeListCls.isInstance(value) -> {
                    cloneNodeList(clone, value as AbstractList<*>, ast)
                }

                else -> {
                    value
                }
            }

            field.set(clone, clonedValue)
            if (clonedValue is ASTNode)
                nodeParentField.set(clonedValue, clone)
        }

        return clone
    }

    private fun cloneNodeList(outer: ASTNode, lst: AbstractList<*>, ast: AST): AbstractList<*> {
        val propertyDescValue = pdescField.get(lst)
        val newList = nodeListConstructor.newInstance(outer, propertyDescValue) as AbstractList<Any>

        for (obj in lst) {

            val clonedObj = if (obj is ASTNode) {
                clone(obj, outer.ast)
            } else {
                obj
            }

            newList.add(clonedObj)
        }

        return newList
    }
}