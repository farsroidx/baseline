package ir.farsroidx.baseline.edit

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * Collects replacements against one committed PSI snapshot and applies them from right to left.
 *
 * A later replacement of exactly the same range supersedes the earlier one. Partial overlap
 * is rejected during application. Build a new batch after every document commit.
 */
internal class TextEdits {

    private val replacements = linkedMapOf<TextRange, String>()

    /**
     * Registers [replacement] for [range], superseding any earlier edit of the exact range.
     *
     * No document changes occur until [apply2]. Ranges must belong to one committed snapshot.
     */
    fun replace(range: TextRange, replacement: String) {

        replacements[range] = replacement

    }

    /**
     * Registers a whitespace-only gap between [left] and [right].
     *
     * Comments or other non-whitespace tokens make the gap ineligible. A semicolon already
     * present in the gap is preserved. Reversed boundaries are ignored.
     *
     * @param replacement desired whitespace after a preserved semicolon, if any
     */
    fun gap(left: PsiElement, right: PsiElement, replacement: String) {

        val start = left.textRange.endOffset

        val end = right.textRange.startOffset

        if (start > end) {

            return
        }

        val source = left.containingFile.text.substring( start, end )

        if (source.all { it.isWhitespace() || it == ';' }) {

            val separator = if (source.contains( ';' )) ";" else ""

            replace(TextRange(start, end), separator + replacement)

        }
    }

    /**
     * Applies a replacement batch to the document and commits PSI when a manager is supplied.
     *
     * Mutates the document from right to left. Already-applied edits are not rolled back
     * if a later overlap check fails; the caller owns the write action.
     *
     * @param document document whose offsets match the collected replacement ranges
     *
     * @throws IllegalStateException if collected ranges partially overlap
     *
     * @deprecated Use [apply2] for the replacement revision.
     * @see apply2
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    @Deprecated( "Use apply2 instead." )
    fun apply(document: Document) {

        val ordered = replacements.entries.sortedByDescending { it.key.startOffset }

        var boundary = document.textLength + 1

        for ( (range, replacement) in ordered ) {

            check( range.endOffset <= boundary ) { "Overlapping formatter edits." }

            if (document.getText( range ) != replacement) {

                document.replaceString( range.startOffset, range.endOffset, replacement )

            }

            boundary = range.startOffset

        }
    }

    /**
     * Applies a replacement batch to the document and commits PSI when a manager is supplied.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * Mutates the document from right to left. Already-applied edits are not rolled back
     * if a later overlap check fails; the caller owns the write action.
     *
     * @param document document whose offsets match the collected replacement ranges
     *
     * @throws IllegalStateException if collected ranges partially overlap
     *
     * @see apply
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun apply2(document: Document) {

        val ordered =
            replacements.entries.sortedWith(
                compareByDescending<Map.Entry<TextRange, String>> { it.key.startOffset }
                    .thenByDescending { it.key.endOffset }
            )

        var boundary = document.textLength + 1

        for ( (range, replacement) in ordered ) {

            check( range.endOffset <= boundary ) { "Overlapping formatter edits." }

            if (document.getText( range ) != replacement) {

                document.replaceString( range.startOffset, range.endOffset, replacement )

            }

            boundary = range.startOffset

        }
    }
}