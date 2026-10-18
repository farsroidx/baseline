package ir.farsroidx.baseline.integration

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.Key
import ir.farsroidx.baseline.config.FormatOptions
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import ir.farsroidx.baseline.config.BaselineSettings
import ir.farsroidx.baseline.format.Formatter

/**
 * Integrates Baseline with the native Reformat Code pipeline as a post-formatter.
 *
 * Whole-file requests use a project-settings snapshot. Partial ranges and unsupported languages
 * retain native formatting. A thread-local guard prevents recursive invocation while the
 * formatter itself requests native reformatting. Parameter order is never rearranged.
 */
class FormatProcessor : PostFormatProcessor {

    /**
     * Formats whole-file PSI elements and returns the original element; fragments are left to native formatting.
     *
     * @param source PSI element supplied by the platform; only complete files are processed
     * @param settings native code style, right margin and formatter-marker preferences
     *
     * @return the original PSI element
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement {

        if (source is PsiFile) processText(source, source.textRange, settings)

        return source

    }

    /**
     * Reports false because enabled rules may rearrange declarations or change conditional braces.
     *
     * @return whether the documented structural predicate holds
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    override fun isWhitespaceOnly(): Boolean {

        return false

    }

    /**
     * Returns whether the PSI file belongs to Java or Kotlin.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return whether the documented structural predicate holds
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun supports(source: PsiFile): Boolean {

        return source.language.id == "JAVA" || source.language.id == "kotlin"

    }

    /**
     * Applies the current formatter with assignment and margin-aware conditional chains.
     *
     * Mutates and commits the document under the caller's write action; existing guards remain in effect.
     *
     * @param source file selected for formatting
     * @param rangeToReformat requested source range
     * @param settings native language code style
     *
     * @return updated full-file range, or the original range when custom formatting is skipped
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {

        val options = source.getUserData( PREVIEW_OPTIONS ) ?: source.project
            .getService( BaselineSettings::class.java )
            .snapshot()

        if (
            !options.enabled ||
            source.language.id == "JAVA" && !options.javaEnabled ||
            source.language.id == "kotlin" && !options.kotlinEnabled
        ) {

            return rangeToReformat

        }

        if (
            active.get() ||
            !supports( source ) ||
            rangeToReformat.startOffset != 0 ||
            rangeToReformat.endOffset < source.textLength
        ) {

            return rangeToReformat

        }

        active.set( true )

        try {

            Formatter().format( source, settings, options )

            return source.textRange

        } finally {

            active.remove()

        }
    }

    companion object {

        internal val PREVIEW_OPTIONS = Key.create<FormatOptions>( "baseline.preview.options" )

        private val active = ThreadLocal.withInitial { false }
    }
}