package ir.farsroidx.baseline.layout

import ir.farsroidx.baseline.edit.TextEdits

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*

/**
 * Formats nested control parentheses and simplifies safe single-expression Kotlin branches.
 *
 * Declarations, comments and multiple statements keep their braces. No expression is reordered.
 */
internal class BranchLayout {

    /**
     * Adds inner spaces to single-line control headers beginning with nested parentheses.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun keywordHeaders(source: PsiFile): TextEdits {

        val edits = TextEdits()

        val owners = PsiTreeUtil
            .collectElements( source ) {

                it is KtForExpression || it is KtWhileExpression || it is KtDoWhileExpression ||
                    it is KtWhenExpression || it is PsiForStatement || it is PsiForeachStatement ||
                    it is PsiWhileStatement || it is PsiDoWhileStatement || it is PsiSwitchBlock

            }

        for (owner in owners) {

            val children = owner.node
                .getChildren( null )
                .map { it.psi }
                .filter { it !is PsiWhiteSpace }

            val opening = children.firstOrNull { it.text == "(" } ?: continue

            val closing = children.firstOrNull { it.text == ")" } ?: continue

            val content = children
                .filter {

                    it.textRange.startOffset > opening.textRange.startOffset &&
                        it.textRange.endOffset < closing.textRange.endOffset

                }

            if (content.isEmpty()) {

                continue
            }

            val header = source.text.substring( opening.textRange.endOffset, closing.textRange.startOffset )

            if (header.contains( '\n' )) {

                continue
            }

            val padding = if (
                content.first().text
                .trimStart()
                .startsWith( "(" )
            ) " " else ""

            edits.gap( opening, content.first(), padding )

            edits.gap( content.last(), closing, padding )

        }

        return edits

    }

    /**
     * Removes safe Kotlin data-branch braces while retaining scoped declarations, comments and multi-statement bodies.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param rightMargin maximum preferred column from the language code style
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun branches(source: PsiFile, rightMargin: Int): TextEdits {

        val edits = TextEdits()

        for (expression in PsiTreeUtil.findChildrenOfType( source, KtIfExpression::class.java )) {

            if (
                PsiTreeUtil.findChildrenOfType( expression, KtIfExpression::class.java )
                .isNotEmpty() || PsiTreeUtil.findChildrenOfType( expression, PsiComment::class.java )
                    .isNotEmpty()
            ) {

                continue
            }

            val condition = expression.condition ?: continue

            val thenBranch = expression.then ?: continue

            val elseBranch = expression.`else`

            val first = dataText( thenBranch )

            val second = elseBranch?.let { dataText( it ) }

            val conditionText = SyntaxLayout().flatInline( condition )

            if (
                conditionText != null && !BlockLayout() .hasChainedCall( condition ) && first != null &&
                (elseBranch == null || second != null)
            ) {

                val replacement = "if (" + conditionText + ") " + first + if (second == null) "" else " else " + second

                val lineStart =
                    source.text.lastIndexOf( '\n', (expression.textRange.startOffset - 1).coerceAtLeast( 0 ) ) + 1

                if (expression.textRange.startOffset - lineStart + replacement.length <= rightMargin) {

                    edits.replace( expression.textRange, replacement )

                    continue
                }
            }

            val keyword = expression.node
                .getChildren( null )
                .map { it.psi }
                .firstOrNull { it.text == "else" } ?: continue

            if (elseBranch != null && second != null) {

                if (elseBranch is KtBlockExpression) edits.replace( elseBranch.textRange, second )

                edits.gap( thenBranch, keyword, " " )

                edits.gap( keyword, elseBranch, " " )

            }
        }

        return edits

    }

    /**
     * Returns whether a Kotlin block contains exactly one safe data expression and no comments.
     *
     * @param block Kotlin block inspected for safe brace removal
     *
     * @return whether the documented structural predicate holds
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun isDataBlock(block: KtBlockExpression): Boolean {

        return dataText( block ) != null

    }

    /**
     * Returns a safe brace-free Kotlin branch expression, or null if the block must remain intact.
     *
     * @param branch Kotlin branch whose single expression may be rendered without braces
     *
     * @return rendered text or null when this structure cannot be processed safely
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun dataText(branch: KtExpression): String? {

        val value = if (branch is KtBlockExpression) {

            if (
                PsiTreeUtil.findChildrenOfType( branch, PsiComment::class.java )
                .isNotEmpty()
            ) {

                return null

            }

            branch.statements.singleOrNull() ?: return null

        } else {

            branch

        }

        if (
            value is KtDeclaration ||
            value is KtReturnExpression ||
            value is KtThrowExpression ||
            value is KtBreakExpression ||
            value is KtContinueExpression ||
            value is KtIfExpression ||
            value is KtWhenExpression ||
            value is KtLoopExpression ||
            BlockLayout() .hasChainedCall( value )
        ) {

            return null

        }

        return SyntaxLayout().flatInline( value )

    }
}