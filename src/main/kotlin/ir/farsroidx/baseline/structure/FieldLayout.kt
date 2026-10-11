package ir.farsroidx.baseline.structure

import ir.farsroidx.baseline.config.FormatOptions
import ir.farsroidx.baseline.edit.TextEdits
import ir.farsroidx.baseline.model.FieldEntry
import ir.farsroidx.baseline.model.FieldGroup

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Groups eligible fields by static status, visibility and type, and aligns their initializers.
 *
 * Name-length ordering is descending within groups. Nonmovable declarations and comments
 * are barriers. The option-aware methods keep sorting, grouping and whitespace independent.
 */
internal class FieldLayout {

    /** Identity of a static/instance, visibility and type field group. */
    private data class Group(val static: Boolean, val field: FieldGroup)

    /** Field declaration paired with its complete grouping identity. */
    private data class Entry(val field: FieldEntry, val group: Group)

    /**
     * Plans independent field runs in static, visibility, type and descending-name-length order.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @deprecated Use [reorder2] for the replacement revision.
     * @see reorder2
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    @Deprecated( "Use reorder2 instead." )
    fun reorder(source: PsiFile): TextEdits {

        val edits = TextEdits()

        for (container in MemberLayout().containers( source )) {

            var run = mutableListOf<Entry>()

            /**
             * Emits the accumulated independent run, then clears it before the next semantic barrier.
             *
             * @author Mohammad ali Riazati
             * @since 1.0.0
             */
            fun flush() {

                if (run.size > 1) {

                    val sorted = run.sortedWith(
                        compareBy<Entry> { !it.group.static }.thenBy { it.group.field.access }
                            .thenBy { it.group.field.type.lowercase() }
                            .thenByDescending { it.field.name.length }
                    )

                    val replacement = sorted
                        .mapIndexed { index, entry ->

                            val gap = when {

                                index == 0                             -> ""

                                sorted[index - 1].group == entry.group -> NEW_LINE

                                else                                   -> BLANK_LINE

                            }

                            gap + entry.field.element.text

                        }
                        .joinToString( "" )

                    edits.replace(
                        TextRange(
                            run.first().field.element.textRange.startOffset,
                            run.last().field.element.textRange.endOffset
                        ),
                        replacement
                    )

                }

                run = mutableListOf()

            }

            for (child in container.children) {

                if (child is PsiWhiteSpace || child.text == ";") {

                    continue
                }

                val entry = entry( child )

                if (entry == null || !entry.field.movable) flush() else run.add( entry )

            }

            flush()

        }

        return edits

    }

    /**
     * Applies the current layout rules.
     *
     * @since 1.0.0
     * @see reorder
     */
    @Deprecated( "Use reorder3 instead." )
    /**
         * Plans independent field runs in static, visibility, type and descending-name-length order.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         *
         * @return unapplied edits against the current PSI snapshot
         *
         * @see reorder
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun reorder2(source: PsiFile): TextEdits {

        val edits = TextEdits()

        for (container in MemberLayout().containers( source )) {

            var run = mutableListOf<Entry>()

            /**
             * Emits the accumulated independent run, then clears it before the next semantic barrier.
             *
             * @author Mohammad ali Riazati
             * @since 1.0.0
             */
            fun flush() {

                if (run.size > 1) {

                    val widths = run.groupBy { it.group }
                        .mapValues { (_, entries) ->
                            entries.maxOf { it.field.name.length }

                        }

                    val sorted = run.sortedWith(
                        compareBy<Entry> { !it.group.static }.thenBy { it.group.field.access }
                            .thenByDescending { widths[it.group] }
                            .thenBy { it.group.field.type.lowercase() }
                            .thenByDescending { it.field.name.length }
                    )

                    val replacement = sorted
                        .mapIndexed { index, entry ->

                            val gap = when {

                                index == 0                             -> ""

                                sorted[index - 1].group == entry.group -> NEW_LINE

                                else                                   -> BLANK_LINE

                            }

                            gap + entry.field.element.text

                        }
                        .joinToString( "" )

                    edits.replace(
                        TextRange(
                            run.first().field.element.textRange.startOffset,
                            run.last().field.element.textRange.endOffset
                        ),
                        replacement
                    )

                }

                run = mutableListOf()

            }

            for (child in container.children) {

                if (child is PsiWhiteSpace || child.text == ";") {

                    continue
                }

                val entry = entry( child )

                if (entry == null || !entry.field.movable) flush() else run.add( entry )

            }

            flush()

        }

        return edits

    }

    @Deprecated( "Use align2 instead." )
    /**
         * Plans field-group gaps and equal-sign padding; declarations without initializers receive no equals sign.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         *
         * @return unapplied edits against the current PSI snapshot
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun align(source: PsiFile): TextEdits {

        val edits = TextEdits()

        for (container in MemberLayout().containers( source )) {

            val children = container.children.filter { it !is PsiWhiteSpace && it.text != ";" }

            var run = mutableListOf<PsiElement>()

            var previous: Entry? = null

            /**
             * Emits the accumulated independent run, then clears it before the next semantic barrier.
             *
             * @author Mohammad ali Riazati
             * @since 1.0.0
             */
            fun flush() {

                alignEquals(run, edits)

                run = mutableListOf()

            }

            for (child in children) {

                val current = entry( child )

                if (current == null) {

                    flush()

                    previous = null

                    continue
                }

                if (previous != null) {

                    val sameGroup = previous.group == current.group

                    val annotated = annotated( previous.field.element ) || annotated( child )

                    edits.gap(
                        previous.field.element, child,
                        (if (sameGroup && !annotated) NEW_LINE else BLANK_LINE) + indentation( child )
                    )

                    if (!sameGroup) flush()

                }

                run.add( child )

                previous = current

            }

            flush()

        }

        return edits

    }

    /**
     * Pads existing equals-sign gaps to the longest declaration prefix in the supplied group.
     *
     * @param elements consecutive declarations sharing an alignment group
     * @param edits batch receiving planned replacements; the source document is not changed here
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun alignEquals(elements: List<PsiElement>, edits: TextEdits) {

        val candidates = elements
            .mapNotNull { element ->

                val children = element.node
                    .getChildren( null )
                    .map { it.psi }

                val equal = children.firstOrNull { it.text == "=" }

            val prefix = if (equal == null) {

                    element.text
                        .substringBeforeLast( ';' )
                        .lineSequence()
                        .last()
                        .trim()

                } else {

                    element.containingFile.text
                        .substring( element.textRange.startOffset, equal.textRange.startOffset )
                        .lineSequence()
                        .last()
                        .trim()

                }

            if (prefix.isBlank()) null else Triple(element, equal, prefix.length)

            }

        val width = candidates.maxOfOrNull { it.third } ?: return

        for ( (element, equal, length) in candidates ) {

            if (equal == null) {

                continue
            }

            val left = element.node
                .getChildren( null )
                .map { it.psi }
                .lastOrNull { it !is PsiWhiteSpace && it.textRange.endOffset <= equal.textRange.startOffset } ?: continue

            edits.gap( left, equal, " ".repeat( width - length + SPACE_WIDTH ) )

        }
    }

    /**
     * Combines a field description with Java static or Kotlin top-level/object status, or returns null.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return combined field/group metadata, or null for non-field elements
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun entry(element: PsiElement): Entry? {

        val field = MemberLayout().field( element ) ?: return null

        val static = when (element) {

            is PsiField   -> element.hasModifierProperty( PsiModifier.STATIC )

            is KtProperty -> element.parent is KtFile || element.parent.parent is KtObjectDeclaration

            else          -> false

        }

        return Entry(field, Group(static, field.group))

    }

    /**
     * Returns whether a declaration carries Java or Kotlin annotations.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return whether the documented structural predicate holds
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun annotated(element: PsiElement): Boolean {

        return when (element) {

            is PsiModifierListOwner -> element.modifierList?.annotations?.isNotEmpty() == true

            is KtModifierListOwner  -> element.annotationEntries.isNotEmpty()

            else                    -> false

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
     * Plans independent field runs in static, visibility, type and descending-name-length order.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param options project preferences captured for the current operation
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @see reorder2
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun reorder3(source: PsiFile, options: FormatOptions): TextEdits {

        val edits = TextEdits()

        for (container in MemberLayout().containers( source )) {

            var run = mutableListOf<Entry>()

            /**
             * Emits the accumulated independent run, then clears it before the next semantic barrier.
             *
             * @author Mohammad ali Riazati
             * @since 1.0.0
             */
            fun flush() {

                if (run.size > 1) {

                    val widths = run.groupBy { group2(it, options) }
                        .mapValues { (_, entries) ->
                            entries.maxOf { it.field.name.length }

                        }

                    val sorted = run.sortedWith(
                        compareBy<Entry> { if (options.separateStaticFields) !it.group.static else false }
                            .thenBy { if (options.groupFieldsByVisibility) it.group.field.access else 0 }
                            .thenByDescending {

                                if (options.groupFieldsByType && options.sortFields) widths[group2(it, options)] else 0

                            }
                            .thenBy { if (options.groupFieldsByType) it.group.field.type.lowercase() else "" }
                            .thenByDescending { if (options.sortFields) it.field.name.length else 0 }
                    )

                    val replacement = sorted
                        .mapIndexed { index, entry ->

                            val gap = when {

                                index == 0 -> ""

                                !options.spaceFieldGroups ||
                                    group2(sorted[index - 1], options) == group2(entry, options) -> NEW_LINE

                                else       -> BLANK_LINE

                            }

                            gap + entry.field.element.text

                        }
                        .joinToString( "" )

                    edits.replace(
                        TextRange(
                            run.first().field.element.textRange.startOffset,
                            run.last().field.element.textRange.endOffset
                        ),
                        replacement
                    )

                }

                run = mutableListOf()

            }

            for (child in container.children) {

                if (child is PsiWhiteSpace || child.text == ";") {

                    continue
                }

                val entry = entry( child )

                if (entry == null || !entry.field.movable) flush() else run.add( entry )

            }

            flush()

        }

        return edits

    }

    /**
     * Plans field-group gaps and equal-sign padding; declarations without initializers receive no equals sign.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param options project preferences captured for the current operation
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @see align
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun align2(source: PsiFile, options: FormatOptions): TextEdits {

        val edits = TextEdits()

        for (container in MemberLayout().containers( source )) {

            val children = container.children.filter { it !is PsiWhiteSpace && it.text != ";" }

            var run = mutableListOf<PsiElement>()

            var previous: Entry? = null

            /**
             * Emits the accumulated independent run, then clears it before the next semantic barrier.
             *
             * @author Mohammad ali Riazati
             * @since 1.0.0
             */
            fun flush() {

                if (options.alignFieldEquals) alignEquals(run, edits)

                run = mutableListOf()

            }

            for (child in children) {

                val current = entry( child )

                if (current == null) {

                    flush()

                    previous = null

                    continue
                }

                if (previous != null) {

                    val sameGroup = group2(previous, options) == group2(current, options)

                    val annotated = options.spaceAnnotatedFields &&
                        (annotated( previous.field.element ) || annotated( child ))

                    edits.gap(
                        previous.field.element,
                        child,
                        (if ((!options.spaceFieldGroups || sameGroup) && !annotated) NEW_LINE else BLANK_LINE) + indentation(
                            child
                        )
                    )

                    if (!sameGroup) flush()

                }

                run.add( child )

                previous = current

            }

            flush()

        }

        return edits

    }

    /**
     * Builds a field grouping key using only the enabled dimensions.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param entry field entry whose enabled grouping dimensions are calculated
     * @param options project preferences captured for the current operation
     *
     * @return group identity using only enabled dimensions
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun group2(entry: Entry, options: FormatOptions): Group {

        return Group(
            options.separateStaticFields && entry.group.static,
            FieldGroup(
                if (options.groupFieldsByVisibility) entry.group.field.access else 0,
                if (options.groupFieldsByType) entry.group.field.type else ""
            )
        )

    }

    companion object {

        private const val NEW_LINE   = "\n"
        private const val BLANK_LINE = "\n\n"

        private const val SPACE_WIDTH = 1

    }
}