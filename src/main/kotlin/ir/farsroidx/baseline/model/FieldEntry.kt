package ir.farsroidx.baseline.model

import com.intellij.psi.PsiElement

/**
 * Describes a field and the semantic guard used before rearranging it.
 * @param element original declaration
 * @param name declared variable name
 * @param group visibility and type group
 * @param movable whether reordering is safe
 */
internal data class FieldEntry(val element: PsiElement, val name: String, val group: FieldGroup, val movable: Boolean)