package com.linecorp.lich.static_analysis.extensions

import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression

/**
 * By default finds the closest parent of a certain type to this node. When an [ancestorLevel] is
 * provided it will keep going up on the tree until the parent matches the ancestor level and the
 * type.
 *
 * @param ancestorLevel when provided it will go up in the tree until it matches the ancestor level.
 */
inline fun <reified T : UElement> UElement?.findClosestParentByType(ancestorLevel: Int = 1): T? {
    var node: UElement? = this?.uastParent
    var currentAncestorLevel = 0
    while (node != null) {
        if (node is T) {
            currentAncestorLevel++
        }
        if (currentAncestorLevel == ancestorLevel) {
            return node as? T
        }
        node = node.uastParent
    }
    return null
}

/**
 * Evaluates this [UExpression] and returns its value. Returns [defaultValue] when the
 * [UExpression] couldn't be evaluated.
 */
inline fun <reified T> UExpression.evaluateOrDefault(defaultValue: T): T =
    evaluate() as? T ?: defaultValue
