package me.haydencheers.simplag.ast

import me.haydencheers.simplag.util.ReflectionUtils
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTNode
import java.lang.Exception
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
        val cloner = ClonerImp()
        return cloner.doCloneNode(node, ast)
    }

    private class ClonerImp {

        val store = IdentityHashMap<Any, Any>(0)

        /**
         * Clones ASTNode @node, and places it in the AST @ast
         */
        fun doCloneNode(node: ASTNode, ast: AST): ASTNode {
            if (store.containsKey(node))
                return store[node] as ASTNode

            // Get reflection meta data
            val ncls = node.javaClass
            val cnstr = ncls.getDeclaredConstructor(AST::class.java)
            cnstr.isAccessible = true

            // Clone the instance + store in cache
            val nodeClone = cnstr.newInstance(ast)
            store[node] = nodeClone

            val fields = ReflectionUtils.getAllDeclaredInstanceFields(ncls)
                .filterNot { it.name == "parent" }
                .filterNot { it.name == "ast" }
                .filterNot { it.name == "alternateRoot" }

            for (field in fields) {
                field.isAccessible = true
                val existingValue = field.get(node)

                val clonedValue = when {

                    // Just an ASTNode
                    existingValue is ASTNode -> doCloneNode(existingValue, ast)

                    // Inner ASTNodeList of values
                    nodeListCls.isInstance(existingValue) -> doCloneList(nodeClone, existingValue as AbstractList<*>, ast)

                    // probably a primitive or non-node value
                    else -> existingValue
                }

                field.set(nodeClone, clonedValue)
                if (clonedValue is ASTNode)
                    nodeParentField.set(clonedValue, nodeClone)
            }

            return nodeClone
        }

        fun doCloneList(outer: ASTNode, lst: AbstractList<*>, ast: AST): AbstractList<*> {
            if (store.containsKey(lst))
                return store[lst] as AbstractList<*>

            val propertyDescValue = pdescField.get(lst)
            val newList = nodeListConstructor.newInstance(outer, propertyDescValue) as AbstractList<Any>
            store[lst] = newList

            for (obj in lst) {

                val clonedObj = if (obj is ASTNode) {
                    doCloneNode(obj, outer.ast)
//                        .apply {
//                        nodeParentField.set(this, outer)
//                    }
                } else {
                    obj
                }

                try {
                    newList.add(clonedObj)
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            }

            return newList
        }
    }
}