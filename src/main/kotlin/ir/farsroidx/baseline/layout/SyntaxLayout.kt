package ir.farsroidx.baseline.layout

import ir.farsroidx.baseline.config.FormatOptions
import ir.farsroidx.baseline.edit.TextEdits

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*

/**
 * Formats conditions, declaration annotations and case separators using structural PSI boundaries.
 *
 * Inline rendering is conservative: comments, block bodies and multiline token contents are
 * rejected rather than flattened into source-changing text.
 */
internal class SyntaxLayout {

    /**
     * Returns token-preserving inline text, or null when comments, blocks or multiline tokens make it unsafe.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return rendered text or null when this structure cannot be processed safely
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun flatInline(element: PsiElement): String? {

        val unsafe = PsiTreeUtil
            .collectElements( element ) {

                it is PsiComment || it is PsiCodeBlock || it is KtBlockExpression ||
                    it.node.firstChildNode == null && it !is PsiWhiteSpace && it.text.contains( '\n' )

            }

        if (unsafe.isNotEmpty()) {

            return null

        }

        return PsiTreeUtil.collectElements( element ) { it.node.firstChildNode == null }
            .joinToString( "" ) { if (it is PsiWhiteSpace) " " else it.text }
            .trim()

    }

    /**
     * Plans margin-aware control-condition formatting with preserved operand order.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param rightMargin maximum preferred column from the language code style
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun conditions(source: PsiFile, rightMargin: Int): TextEdits {

        val edits = TextEdits()

        val owners = PsiTreeUtil
            .collectElements( source ) {

                it is PsiIfStatement || it is PsiWhileStatement || it is PsiDoWhileStatement || it is KtIfExpression ||
                    it is KtWhileExpression || it is KtDoWhileExpression

            }

        for (owner in owners) {

            val condition = when (owner) {

                is PsiIfStatement      -> owner.condition

                is PsiWhileStatement   -> owner.condition

                is PsiDoWhileStatement -> owner.condition

                is KtIfExpression      -> owner.condition

                is KtWhileExpression   -> owner.condition

                is KtDoWhileExpression -> owner.condition

                else                   -> null

            } ?: continue

            val children = owner.node
                .getChildren( null )
                .map { it.psi }

            val opening = children.firstOrNull { it.text == "(" } ?: continue

            val closing = children.firstOrNull { it.text == ")" } ?: continue

            val base = indentation( owner )

            val flat = flatInline( condition )

            val width = base.length + CONDITION_PREFIX_WIDTH + (flat?.length ?: condition.text.length)

            val multiline = width > rightMargin ||
                condition.text.contains( '\n' ) && (flat == null || BlockLayout() .hasChainedCall( condition ))

            if (multiline) {

                edits.gap( opening, condition, NEW_LINE + base + INDENT )

                edits.gap( condition, closing, NEW_LINE + base )

            } else {

                if (flat != null) edits.replace( condition.textRange, flat )

                edits.gap( opening, condition, "" )

                edits.gap( condition, closing, "" )

            }
        }

        return edits

    }

    /**
     * Places modifier-list annotations above their declarations while retaining annotation order.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun annotations(source: PsiFile, options: FormatOptions): TextEdits {

        val edits = TextEdits()

        val entries = PsiTreeUtil
            .collectElements( source ) {

                it is PsiAnnotation && it.parent is PsiModifierList ||
                    it is KtAnnotationEntry && it.parent is KtModifierList

            }

        for (annotation in entries) {

            val owner = annotation.parent.parent

            val separate = when (owner) {

                is PsiParameter, is KtParameter -> options.formatParameterAnnotations

                is PsiField, is KtProperty -> options.formatFieldAnnotations

                is PsiClass, is KtClassOrObject -> options.formatClassAnnotations

                else -> options.formatAnnotations

            }

            var next = PsiTreeUtil.nextLeaf( annotation )

            while (next is PsiWhiteSpace) {

                next = PsiTreeUtil.nextLeaf( next )

            }

            if (next != null && owner.textRange.contains( next.textRange )) {

                edits.gap( annotation, next, if (separate) NEW_LINE + indentation( owner ) else " " )

            }
        }

        return edits

    }

    /**
     * Pads gaps before arrows/colons to the widest single-line case prefix; multiline prefixes are skipped.
     *
     * @param branches case entries or switch labels in source order
     * @param edits batch receiving planned replacements; the source document is not changed here
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun alignBranches(branches: List<PsiElement>, edits: TextEdits) {

        val candidates = branches
            .mapNotNull { branch ->

                val children = branch.node
                    .getChildren( null )
                    .map { it.psi }
                    .filter { it !is PsiWhiteSpace }

                val separator = children.firstOrNull { it.text == "->" || it.text == ":" } ?: return@mapNotNull null

            val left = children.lastOrNull { it.textRange.endOffset <= separator.textRange.startOffset } ?: return@mapNotNull null

            val prefix =
                    branch.containingFile.text.substring( branch.textRange.startOffset, left.textRange.endOffset )

            if (prefix.contains( '\n' )) null else Triple(left, separator, prefix.length)

            }

        val width = candidates.maxOfOrNull { it.third } ?: return

        for ( (left, separator, length) in candidates ) {

            edits.gap( left, separator, " ".repeat( width - length + SPACE_WIDTH ) )

        }
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

    /**
     * Plans Kotlin when/Java switch case spacing and aligned separators.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param options project preferences captured for the current operation
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun branches(source: PsiFile, options: FormatOptions): TextEdits {

        val edits = TextEdits()

        for (expression in PsiTreeUtil.findChildrenOfType( source, KtWhenExpression::class.java )) {

            val entries = expression.entries

            val children = expression.node
                .getChildren( null )
                .map { it.psi }

            val opening = children.firstOrNull { it.text == "{" }

            val closing = children.lastOrNull { it.text == "}" }

            val base = indentation( expression )

            if (entries.isNotEmpty() && opening != null && closing != null) {

                edits.gap( opening, entries.first(), (if (options.spaceCases) BLANK_LINE else "\n") + base + INDENT )

                for ( (left, right) in entries.zipWithNext() ) {

                    edits.gap( left, right, (if (options.spaceCases) BLANK_LINE else "\n") + base + INDENT )

                }

                edits.gap( entries.last(), closing, (if (options.blankBeforeClosingBrace) BLANK_LINE else "\n") + base )

            }

            if (options.alignCases) alignBranches(entries, edits)

        }

        for (expression in PsiTreeUtil.findChildrenOfType( source, PsiSwitchBlock::class.java )) {

            val labels = expression.body?.statements?.filterIsInstance<PsiSwitchLabelStatementBase>() ?: continue

            val statements = expression.body?.statements?.toList() ?: emptyList()

            for (label in labels.drop( 1 )) {

                val previous = statements.getOrNull( statements.indexOf( label ) - 1 ) ?: continue

                edits.gap( previous, label, (if (options.spaceCases) BLANK_LINE else "\n") + indentation( label ) )

            }

            if (options.alignCases) alignBranches(labels, edits)

        }

        return edits

    }

    companion object {

        private const val NEW_LINE   = "\n"
        private const val BLANK_LINE = "\n\n"

        private const val CONDITION_PREFIX_WIDTH = 7
        private const val SPACE_WIDTH            = 1

        private const val INDENT = "    "

    }
}