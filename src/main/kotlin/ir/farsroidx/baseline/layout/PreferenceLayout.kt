package ir.farsroidx.baseline.layout

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import ir.farsroidx.baseline.config.FormatOptions
import ir.farsroidx.baseline.edit.TextEdits
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtWhenExpression

/** Resolves disabled block-spacing preferences after specialized layout passes have run. */
internal class PreferenceLayout {

    /**
     * Removes optional leading/trailing blank lines from nonempty blocks without moving tokens.
     *
     * @param source committed PSI containing the final layout
     * @param options immutable-for-this-operation preferences
     * @return whitespace edits; comments and mandatory line breaks are preserved
     */
    fun boundaries(source: PsiFile, options: FormatOptions): TextEdits {

        val edits = TextEdits()

        if (options.blankAfterOpeningBrace && options.blankBeforeClosingBrace && options.spaceLambdas) {

            return edits

        }

        val containers = PsiTreeUtil
            .collectElements( source ) {

                it is PsiClass || it is PsiCodeBlock || it is KtBlockExpression || it is KtClassBody ||
                    it is KtFunctionLiteral || it is KtWhenExpression

            }

        for (container in containers) {

            val children = container.node
                .getChildren( null )
                .map { it.psi }
                .filter { it !is PsiWhiteSpace }

            val opening = if (container is KtFunctionLiteral) {

                container.arrow ?: children.firstOrNull { it.text == "{" }

            } else children.firstOrNull { it.text == "{" }

            if (opening == null) {

                continue
            }

            val closing = children.lastOrNull { it.text == "}" } ?: continue

            val content = if (container is KtFunctionLiteral) {

                container.bodyExpression?.statements ?: emptyList()

            } else children.filter {

                    it.textRange.startOffset > opening.textRange.startOffset &&
                        it.textRange.endOffset < closing.textRange.endOffset

                }

            if (content.isEmpty()) {

                continue
            }

            val lambda = container is KtFunctionLiteral

            if (!options.blankAfterOpeningBrace || lambda && !options.spaceLambdas) {

                edits.gap( opening, content.first(), NEW_LINE + indentation( content.first() ) )

            }

            if (!options.blankBeforeClosingBrace || lambda && !options.spaceLambdas) {

                edits.gap( content.last(), closing, NEW_LINE + indentation( closing ) )

            }
        }

        return edits

    }

    /** Returns existing line indentation for [element], excluding non-whitespace text. */
    private fun indentation(element: PsiElement): String {

        val text = element.containingFile.text

        val offset = element.textRange.startOffset

        val start = text.lastIndexOf( '\n', (offset - 1).coerceAtLeast( 0 ) ) + 1

        return text.substring( start, offset )
            .takeWhile { it == ' ' || it == '\t' }
    }

    companion object {

        private const val NEW_LINE = "\n"

    }
}