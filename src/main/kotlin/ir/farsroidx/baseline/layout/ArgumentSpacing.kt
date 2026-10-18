package ir.farsroidx.baseline.layout

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import ir.farsroidx.baseline.config.FormatOptions
import ir.farsroidx.baseline.edit.TextEdits
import org.jetbrains.kotlin.psi.*

/** Keeps call padding and annotation-array padding independent without moving comments or line breaks. */
internal class ArgumentSpacing {

    /** Returns whitespace-only delimiter edits for [source] using the captured [options]. */
    fun spacing(source: PsiFile, options: FormatOptions): TextEdits {

        val edits = TextEdits()

        for (element in PsiTreeUtil.collectElements( source ) {
            it is PsiExpressionList || it is KtValueArgumentList || it is PsiArrayInitializerMemberValue ||
                it is KtCollectionLiteralExpression
        }) {

            val annotationArray = element is PsiArrayInitializerMemberValue ||
                element is KtCollectionLiteralExpression &&
                PsiTreeUtil.getParentOfType( element, KtAnnotationEntry::class.java ) != null

            val call = element is PsiExpressionList &&
                (element.parent is PsiCallExpression || element.parent is PsiEnumConstant) ||
                element is KtValueArgumentList && element.parent is KtCallExpression

            if (!annotationArray && !call) {

                continue

            }

            val children = element.children.filter { it !is PsiWhiteSpace }

            val opening = element.firstChild ?: continue

            val closing = element.lastChild ?: continue

            val content = children.filter {
                it.textRange.startOffset >= opening.textRange.endOffset &&
                    it.textRange.endOffset <= closing.textRange.startOffset
            }

            if (content.isEmpty()) {

                continue

            }

            val count = when (element) {

                is PsiExpressionList -> element.expressionCount

                is KtValueArgumentList -> element.arguments.size

                else -> 0

            }

            val padded = if (annotationArray) options.padAnnotationArrays else if (count == 1) {

                options.padSingleArguments

            } else options.padMultipleArguments

            if (!annotationArray && padded) {

                continue

            }

            val padding = if (padded) " " else ""

            for ((left, right) in listOf( opening to content.first(), content.last() to closing )) {

                val gap = source.text.substring( left.textRange.endOffset, right.textRange.startOffset )

                if (gap.all { it == ' ' || it == '\t' }) {

                    edits.gap( left, right, padding )

                }

            }

        }

        return edits

    }

}