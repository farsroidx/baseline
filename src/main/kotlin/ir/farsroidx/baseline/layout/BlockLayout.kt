package ir.farsroidx.baseline.layout

import ir.farsroidx.baseline.edit.TextEdits

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

/**
 * Handles chained-call detection, statement separation and compact brace-only boundaries.
 */
internal class BlockLayout {

    /**
     * Returns whether any descendant contains a method-call receiver that is itself a method call.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return whether the documented structural predicate holds
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun hasChainedCall(element: PsiElement): Boolean {

        return PsiTreeUtil.collectElements( element ) { isChainedCall( it ) }
            .isNotEmpty()

    }

    /**
     * Plans the retained chain-call layout using structurally detected qualified calls.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun chains(source: PsiFile): TextEdits {

        val edits = TextEdits()

        for (expression in PsiTreeUtil.collectElements( source ) { isChainedCall( it ) }) {

            val argumentList = PsiTreeUtil
                .getParentOfType(
                    expression, PsiExpressionList::class.java, KtValueArgumentList::class.java
                )
                ?: continue

            val root = generateSequence( expression ) { it.parent }.takeWhile { it !== argumentList }
                .lastOrNull { isChainedCall( it ) } ?: expression

            val reference = if (expression is PsiMethodCallExpression) expression.methodExpression else expression

            val children = reference.node
                .getChildren( null )
                .map { it.psi }
                .filter { it !is PsiWhiteSpace }

            val dot = children.firstOrNull { it.text == "." || it.text == "?." } ?: continue

            val receiver = children.lastOrNull { it.textRange.endOffset <= dot.textRange.startOffset } ?: continue

            edits.gap( receiver, dot, NEW_LINE + indentation( root ) + INDENT )

        }

        return edits

    }

    /**
     * Separates local declarations/assignments from following statements outside constructor bodies.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @deprecated Use [statementSpacing2] for the replacement revision.
     * @see statementSpacing2
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    @Deprecated( "Use statementSpacing2 instead." )
    fun statementSpacing(source: PsiFile): TextEdits {

        val edits = TextEdits()

        for (body in PsiTreeUtil.findChildrenOfType( source, PsiCodeBlock::class.java )) {

            val owner = body.parent

            if (owner is PsiMethod && owner.isConstructor) {

                continue
            }

            for (
                (left, right) in body.statements
                    .toList()
                    .zipWithNext()
            ) {

                if (left is PsiDeclarationStatement) edits.gap( left, right, BLANK_LINE + indentation( left ) )

            }
        }

        for (body in PsiTreeUtil.findChildrenOfType( source, KtBlockExpression::class.java )) {

            if (body.parent is KtSecondaryConstructor) {

                continue
            }

            for ( (left, right) in body.statements.zipWithNext() ) {

                if (left is KtProperty) edits.gap( left, right, BLANK_LINE + indentation( left ) )

            }
        }

        return edits

    }

    /**
     * Separates local declarations/assignments from following statements outside constructor bodies.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @see statementSpacing
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun statementSpacing2(source: PsiFile): TextEdits {

        val edits = TextEdits()

        for (body in PsiTreeUtil.findChildrenOfType( source, PsiCodeBlock::class.java )) {

            val owner = body.parent

            if (owner is PsiMethod && owner.isConstructor) {

                continue
            }

            for (
                (left, right) in body.statements
                    .toList()
                    .zipWithNext()
            ) {

                if (left is PsiDeclarationStatement || isAssignment( left )) {

                    edits.gap( left, right, BLANK_LINE + indentation( left ) )

                }
            }
        }

        for (body in PsiTreeUtil.findChildrenOfType( source, KtBlockExpression::class.java )) {

            if (body.parent is KtSecondaryConstructor) {

                continue
            }

            for ( (left, right) in body.statements.zipWithNext() ) {

                if (left is KtProperty || isAssignment( left )) {

                    edits.gap(
                        left, right, BLANK_LINE + indentation( left )
                    )

                }
            }
        }

        return edits

    }

    /**
     * Uses one newline between an empty brace pair or consecutive closing braces.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun braces(source: PsiFile): TextEdits {

        val edits = TextEdits()

        val leaves = PsiTreeUtil
            .collectElements( source ) {

                it.node.firstChildNode == null && it !is PsiWhiteSpace

            }

        for (
            (left, right) in leaves.toList()
                .zipWithNext()
        ) {

            val leftType = left.node.elementType

            val rightType = right.node.elementType

            val closing = rightType == JavaTokenType.RBRACE || rightType == KtTokens.RBRACE

            val opening = leftType == JavaTokenType.LBRACE || leftType == KtTokens.LBRACE

            val previousClosing = leftType == JavaTokenType.RBRACE || leftType == KtTokens.RBRACE

            if (closing && (opening || previousClosing)) edits.gap( left, right, NEW_LINE + indentation( right ) )

        }

        return edits

    }

    /**
     * Recognizes Java expression-statement assignments and Kotlin equals assignments.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return whether the documented structural predicate holds
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun isAssignment(element: PsiElement): Boolean {

        return element is PsiExpressionStatement && element.expression is PsiAssignmentExpression ||
            element is KtBinaryExpression && element.operationReference.text == "="

    }

    /**
     * Recognizes a qualified Java/Kotlin call whose receiver contains another call.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return whether the documented structural predicate holds
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun isChainedCall(element: PsiElement): Boolean {

        if (element is PsiMethodCallExpression) {

            return element.methodExpression.qualifierExpression is PsiMethodCallExpression

        }

        return element is KtQualifiedExpression && element.selectorExpression is KtCallExpression &&
            element.receiverExpression is KtQualifiedExpression &&
            (element.receiverExpression as KtQualifiedExpression).selectorExpression is KtCallExpression

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

        private const val NEW_LINE   = "\n"
        private const val BLANK_LINE = "\n\n"
        private const val INDENT     = "    "

    }
}