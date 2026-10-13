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
     * Groups eligible fields and sorts their declared type/name widths without crossing semantic barriers.
     *
     * @param source committed Java or Kotlin PSI file
     * @param options field grouping and sorting preferences
     * @return unapplied declaration and separator edits; parameter and method order remain intact
     */
    fun reorder(source: PsiFile, options: FormatOptions): TextEdits {

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

                    val prefixes = prefixMap(run, options)

                    val widths = run.groupBy { group(it, options, prefixes) }
                        .mapValues { (_, entries) ->
                            entries.maxOf { declarationWidth( it ) }

                        }

                    val sorted = run.sortedWith(
                        compareBy<Entry> { if (options.separateStaticFields) !it.group.static else false }
                            .thenBy { if (options.groupFieldsByVisibility) it.group.field.access else 0 }
                            .thenByDescending {

                                if ((options.groupFieldsByType || options.groupFieldsByPrefix) && options.sortFields) widths[group(
                                it, options, prefixes
                            )] else 0

                            }
                            .thenBy { group( it, options, prefixes ).prefix }
                            .thenBy { group( it, options, prefixes ).type.lowercase() }
                            .thenByDescending { if (options.sortFields) declarationWidth( it ) else 0 }
                    )

                    val replacement = sorted
                        .mapIndexed { index, entry ->

                            val gap = when {

                                index == 0 -> ""

                                !options.spaceFieldGroups ||
                                    group(sorted[index - 1], options, prefixes) == group(entry, options, prefixes) -> NEW_LINE

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
     * Aligns initializer equals signs within the enabled field groups.
     *
     * @param source committed Java or Kotlin PSI file
     * @param options grouping, annotation and alignment preferences
     * @return unapplied whitespace edits; declarations without initializers remain unchanged
     */
    fun align(source: PsiFile, options: FormatOptions): TextEdits {

        val edits = TextEdits()

        for (container in MemberLayout().containers( source )) {

            val children = container.children.filter { it !is PsiWhiteSpace && it.text != ";" }

            val prefixes = movablePrefixes(children, options)

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

                    val sameGroup = group(previous, options, prefixes) == group(current, options, prefixes)

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

    private data class CascadeGroup(
        val static: Boolean,
        val access: Int,
        val type: String,
        val prefix: String
    )

    private fun declarationWidth(entry: Entry): Int {

        val type = when (val element = entry.field.element) {

            is PsiField   -> element.typeElement?.text ?: entry.group.field.type

            is KtProperty -> element.typeReference?.text ?: entry.group.field.type

            else          -> entry.group.field.type

        }

        return CascadeWidth.measure( type, entry.field.name )

    }

    /**
     * Computes the visibility, static status and optional type key used by cascade groups.
     *
     * @param entry eligible field metadata
     * @param options grouping preferences
     * @return the base field group without a shared-prefix key
     */
    private fun typeGroup(entry: Entry, options: FormatOptions): Group {

        return Group(
            options.separateStaticFields && entry.group.static,
            FieldGroup(
                if (options.groupFieldsByVisibility) entry.group.field.access else 0,
                if (options.groupFieldsByType) entry.group.field.type else ""
            )
        )

    }


    private fun group(entry: Entry, options: FormatOptions, prefixes: Map<PsiElement, String>): CascadeGroup {

        val group = typeGroup(entry, options)

        val prefix = prefixes[entry.field.element].orEmpty()

        return CascadeGroup(group.static, group.field.access, if (prefix.isEmpty()) group.field.type else "", prefix)

    }

    private fun prefixMap(entries: List<Entry>, options: FormatOptions): Map<PsiElement, String> {

        if (!options.groupFieldsByPrefix) {

            return emptyMap()

        }

        val result = mutableMapOf<PsiElement, String>()

        for (
            section in entries
                .groupBy {

                    val group = typeGroup(it, options)

                    group.static to group.field.access

                }.values
        ) {

            val groups = section.groupBy { fieldPrefix( it.field.name ) }

            for ( (prefix, fields) in groups ) {

            if (prefix.isEmpty() || fields.size < MINIMUM_PREFIX_FIELDS) {

                continue
            }

            for (field in fields) {

                result[field.field.element] = prefix

            }
            }
        }

        return result

    }

    private fun fieldPrefix(name: String): String {

        return SNAKE_PREFIX.find( name )?.groupValues?.get( PREFIX_GROUP )
            ?: CAMEL_PREFIX.find( name )?.groupValues?.get( PREFIX_GROUP )
            ?: ""

    }

    private fun movablePrefixes(children: List<PsiElement>, options: FormatOptions): Map<PsiElement, String> {

        val result = mutableMapOf<PsiElement, String>()

        val run = mutableListOf<Entry>()

        fun flush() {

            result.putAll( prefixMap(run, options) )

            run.clear()

        }

        for (child in children) {

            val field = entry( child )

            if (field == null || !field.field.movable) flush() else run.add( field )

        }

        flush()

        return result

    }

    companion object {

        private val SNAKE_PREFIX = Regex( "^([A-Za-z][A-Za-z0-9]*_)" )

        private val CAMEL_PREFIX = Regex( "^([a-z]+|[A-Z][a-z]+)(?=[A-Z])" )

        private const val MINIMUM_PREFIX_FIELDS = 2

        private const val PREFIX_GROUP = 1

        private const val NEW_LINE   = "\n"
        private const val BLANK_LINE = "\n\n"

        private const val SPACE_WIDTH = 1

    }
}