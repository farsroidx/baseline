package ir.farsroidx.baseline.layout

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import ir.farsroidx.baseline.edit.TextEdits
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

/**
 * Detects fluent call roots and places successive call links on continuation lines.
 *
 * Receiver traversal preserves evaluation order and does not treat plain property access as
 * an additional call. Nested argument chains are handled by the same structural rules.
 */
internal class ChainLayout {

    /**
     * Plans assignment-chain breaks and keeps conditional branch chains inline when they fit.
     *
     * Preserves receiver evaluation and argument order. No PSI changes occur until the edits are applied.
     *
     * @param source committed Java or Kotlin file
     * @param rightMargin preferred maximum column
     *
     * @return whitespace edits for qualified call chains
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun links(source: PsiFile, rightMargin: Int): TextEdits {

        val edits = TextEdits()

        for (root in PsiTreeUtil.collectElements( source ) { isCall( it ) && root( it ) === it }) {

            val calls = calls( root )

            if (calls.size < MINIMUM_CHAIN_CALLS) {

                continue
            }

            val conditional = isConditionalBranch( root )

            if (conditional && compactBranch(root, rightMargin, edits)) {

                continue
            }

            val assignment = isAssignmentValue( root )

            val firstCallOverflows = conditional && !firstCallFits(calls.first(), rightMargin)

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

                if (index == 0 && !propertyReceiver && !assignment && !firstCallOverflows) {

                    continue
                }

                edits.gap( receiver, dot, NEW_LINE + indentation( root ) + INDENT )

            }
        }

        return edits

    }

    /**
     * Finds the outer parentheses enclosing a value without crossing another expression.
     *
     * @param element chain value
     * @return the value or its outer parenthesized expression
     */
    private fun valueOwner(element: PsiElement): PsiElement {

        var current = element

        while (current.parent is PsiParenthesizedExpression || current.parent is KtParenthesizedExpression) {

            current = current.parent

        }

        return current

    }

    /**
     * Identifies a declaration initializer or assignment value without inspecting nested arguments.
     *
     * @param element chain value
     * @return whether the chain supplies the complete assigned value
     */
    private fun isAssignmentValue(element: PsiElement): Boolean {

        val value = valueOwner( element )

        return when (val owner = value.parent) {

            is PsiVariable             -> owner.initializer === value

            is PsiAssignmentExpression -> owner.rExpression === value

            is KtProperty              -> owner.initializer === value

            is KtBinaryExpression      -> owner.operationToken == KtTokens.EQ && owner.right === value

            else                       -> false

        }
    }

    /**
     * Recognizes Java ternary and Kotlin inline if branches through transparent parentheses.
     *
     * @param element chain value
     * @return whether the chain is the complete branch expression
     */
    private fun isConditionalBranch(element: PsiElement): Boolean {

        val value = valueOwner( element )

        val parent = value.parent

        val owner = if (parent.parent is KtIfExpression) parent.parent else parent

        return when (owner) {

            is PsiConditionalExpression -> owner.thenExpression === value || owner.elseExpression === value

            is KtIfExpression           -> owner.then === value || owner.`else` === value

            else                        -> false

        }
    }

    /**
     * Checks the first call at its current column before deciding whether its receiver must break.
     *
     * @param element first call in evaluation order
     * @param rightMargin preferred maximum column
     * @return whether the call fits or cannot safely be rendered inline
     */
    private fun firstCallFits(element: PsiElement, rightMargin: Int): Boolean {

        val flat = SyntaxLayout().flatInline( element ) ?: return true

        val source = element.containingFile.text

        val start = element.textRange.startOffset

        val lineStart = source.lastIndexOf( '\n', (start - 1).coerceAtLeast( 0 ) ) + 1

        return start - lineStart + flat.length <= rightMargin

    }

    /**
     * Plans a compact branch only when its tokens, suffix and nested arguments can remain intact.
     *
     * Comments, blocks and nested chains retain their existing structural formatting rules.
     *
     * @param element complete branch chain
     * @param rightMargin preferred maximum column
     * @param edits destination for the unapplied replacement
     * @return whether a safe inline replacement was planned
     */
    private fun compactBranch(element: PsiElement, rightMargin: Int, edits: TextEdits): Boolean {

        if (SyntaxLayout().flatInline( element ) == null) {

            return false

        }

        val nestedChains = PsiTreeUtil
            .collectElements( element ) {

                it !== element && isCall( it ) && root( it ) === it && calls( it ).size >= MINIMUM_CHAIN_CALLS

            }

        if (nestedChains.isNotEmpty()) {

            return false

        }

        val leaves = PsiTreeUtil.collectElements( element ) { it.node.firstChildNode == null }

        val flat = leaves
            .mapIndexed { index, leaf ->

                if (leaf !is PsiWhiteSpace) leaf.text else {

                    val before = leaves.getOrNull( index - 1 )?.text

                    val after = leaves.getOrNull( index + 1 )?.text

                    if (before == "." || before == "?." || after == "." || after == "?.") "" else " "

                }

            }
            .joinToString( "" )
            .trim()

        val source = element.containingFile.text

        val start = element.textRange.startOffset

        val lineStart = source.lastIndexOf( '\n', (start - 1).coerceAtLeast( 0 ) ) + 1

        val end = element.textRange.endOffset

        val lineEnd = source
            .indexOf( '\n', end )
            .let { if (it < 0) source.length else it }

        val suffix = source
            .substring( end, lineEnd )
            .trimEnd()

        if (start - lineStart + flat.length + suffix.length > rightMargin) {

            return false

        }

        edits.replace( element.textRange, flat )

        return true

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