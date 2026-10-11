package ir.farsroidx.baseline.layout

import ir.farsroidx.baseline.edit.TextEdits

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression

/**
 * Detects fluent call roots and places successive call links on continuation lines.
 *
 * Receiver traversal preserves evaluation order and does not treat plain property access as
 * an additional call. Nested argument chains are handled by the same structural rules.
 */
internal class ChainLayout {

    /**
     * Plans continuation-line breaks at successive fluent-call links, preserving the original call sequence.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun links(source: PsiFile): TextEdits {

        val edits = TextEdits()

        for (root in PsiTreeUtil.collectElements( source ) { isCall( it ) && root( it ) === it }) {

            val calls = calls( root )

            if (calls.size < MINIMUM_CHAIN_CALLS) {

                continue
            }

            for ( (index, call) in calls.withIndex() ) {

                val reference = if (call is PsiMethodCallExpression) call.methodExpression else call

                val children = reference.node
                    .getChildren( null )
                    .map { it.psi }
                    .filter { it !is PsiWhiteSpace }

                val dot = children.firstOrNull { it.text == "." || it.text == "?." } ?: continue

                val receiver = children.lastOrNull { it.textRange.endOffset <= dot.textRange.startOffset } ?: continue

                val propertyReceiver = receiver is KtQualifiedExpression ||
                    receiver is PsiReferenceExpression && receiver.qualifierExpression != null

                if (index == 0 && !propertyReceiver) {

                    continue
                }

                edits.gap( receiver, dot, NEW_LINE + indentation( root ) + INDENT )

            }
        }

        return edits

    }

    /**
     * Returns whether an argument list belongs to a qualified fluent chain with at least two calls.
     *
     * @param list declaration or call list whose parentheses and items are formatted
     *
     * @return whether the documented structural predicate holds
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun isLink(list: PsiElement): Boolean {

        val call = when (val owner = list.parent) {

            is PsiMethodCallExpression -> owner

            is KtCallExpression        -> owner.parent as? KtQualifiedExpression

            else                       -> null

        } ?: return false

        return calls( root( call ) ).size >= MINIMUM_CHAIN_CALLS

    }

    /**
     * Traverses receivers and returns chain calls in their original evaluation order.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return matching elements in the traversal order described above
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun calls(element: PsiElement): List<PsiElement> {

        val result = mutableListOf<PsiElement>()

        var current: PsiElement? = element

        while (current != null && isCall( current )) {

            result.add( current )

            current = when (current) {

                is PsiMethodCallExpression -> current.methodExpression.qualifierExpression

                is KtQualifiedExpression   -> current.receiverExpression

                else                       -> null

            }
        }

        return result.reversed()

    }

    /**
     * Climbs receiver ownership to the outermost qualified call in the same chain.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return original PSI element or structurally identified owner
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun root(element: PsiElement): PsiElement {

        var current = element

        while (true) {

            val next = when (val parent = current.parent) {

                is PsiReferenceExpression -> parent.parent as? PsiMethodCallExpression

                is KtQualifiedExpression  -> if (parent.receiverExpression === current && isCall( parent )) parent else null

                else                      -> null

            } ?: return current

            if (next is PsiMethodCallExpression && next.methodExpression.qualifierExpression !== current) {

                return current

            }

            current = next

        }
    }

    /**
     * Recognizes Java method calls and Kotlin qualified expressions whose selector is a call.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return whether the documented structural predicate holds
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun isCall(element: PsiElement): Boolean {

        return element is PsiMethodCallExpression ||
            element is KtQualifiedExpression && element.selectorExpression is KtCallExpression

    }

    /**
     * Returns the spaces/tabs at the beginning of the element's current source line.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return computed source text or whitespace prefix
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun indentation(element: PsiElement): String {

        val source = element.containingFile.text

        val offset = element.textRange.startOffset

        val start = source.lastIndexOf( '\n', (offset - 1).coerceAtLeast( 0 ) ) + 1

        return source.substring( start, offset )
            .takeWhile { it == ' ' || it == '\t' }
    }

    companion object {

        private const val NEW_LINE = "\n"

        private const val MINIMUM_CHAIN_CALLS = 2

        private const val INDENT = "    "

    }
}