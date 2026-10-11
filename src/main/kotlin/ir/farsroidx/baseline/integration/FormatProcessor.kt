package ir.farsroidx.baseline.integration

import com.intellij.openapi.util.TextRange
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
     * Handles a native reformat request and returns the resulting affected text range.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param rangeToReformat range requested by the native formatter; structural processing requires the whole file
     * @param settings native code style, right margin and formatter-marker preferences
     *
     * @return updated whole-file range, or the original range when custom processing is skipped
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {

        return processText9(source, rangeToReformat, settings)

    }

    /**
     * Handles a native reformat request and returns the resulting affected text range.
     *
     * This numbered revision is retained alongside its predecessor for compatibility.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param rangeToReformat range requested by the native formatter; structural processing requires the whole file
     * @param settings native code style, right margin and formatter-marker preferences
     *
     * @return updated whole-file range, or the original range when custom processing is skipped
     *
     * @deprecated Use [processText2] for the replacement revision.
     * @see processText2
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    @Deprecated( "Use processText2 instead." )
    fun processText1(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {

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

            Formatter().format( source, settings )

            return source.textRange

        } finally {

            active.remove()

        }
    }

    @Deprecated( "Use processText3 instead." )
    /**
         * Handles a native reformat request and returns the resulting affected text range.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param rangeToReformat range requested by the native formatter; structural processing requires the whole file
         * @param settings native code style, right margin and formatter-marker preferences
         *
         * @return updated whole-file range, or the original range when custom processing is skipped
         *
         * @see processText
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun processText2(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {

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

            Formatter().format2( source, settings )

            return source.textRange

        } finally {

            active.remove()

        }
    }

    @Deprecated( "Use processText4 instead." )
    /**
         * Handles a native reformat request and returns the resulting affected text range.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param rangeToReformat range requested by the native formatter; structural processing requires the whole file
         * @param settings native code style, right margin and formatter-marker preferences
         *
         * @return updated whole-file range, or the original range when custom processing is skipped
         *
         * @see processText2
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun processText3(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {

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

            Formatter().format3( source, settings )

            return source.textRange

        } finally {

            active.remove()

        }
    }

    @Deprecated( "Use processText5 instead." )
    /**
         * Handles a native reformat request and returns the resulting affected text range.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param rangeToReformat range requested by the native formatter; structural processing requires the whole file
         * @param settings native code style, right margin and formatter-marker preferences
         *
         * @return updated whole-file range, or the original range when custom processing is skipped
         *
         * @see processText3
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun processText4(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {

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

            Formatter().format4( source, settings )

            return source.textRange

        } finally {

            active.remove()

        }
    }

    /**
     * Retains the previous layout behavior.
     *
     * @deprecated Use [processText6] instead.
     * @see processText6
     */
    @Deprecated( "Use processText6 instead." )
    /**
         * Handles a native reformat request and returns the resulting affected text range.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param rangeToReformat range requested by the native formatter; structural processing requires the whole file
         * @param settings native code style, right margin and formatter-marker preferences
         *
         * @return updated whole-file range, or the original range when custom processing is skipped
         *
         * @see processText4
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun processText5(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {

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

            Formatter().format5( source, settings )

            return source.textRange

        } finally {

            active.remove()

        }
    }

    /**
     * Applies the current layout rules.
     *
     * @since 1.0.0
     * @see processText5
     */

    /** Retains the previous behavior.
     * @since 1.0.0
     * @deprecated Use [processText7].
     * @see processText7
     */
    @Deprecated( "Use processText7 instead." )
    /**
         * Handles a native reformat request and returns the resulting affected text range.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param rangeToReformat range requested by the native formatter; structural processing requires the whole file
         * @param settings native code style, right margin and formatter-marker preferences
         *
         * @return updated whole-file range, or the original range when custom processing is skipped
         *
         * @see processText5
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun processText6(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {

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

            Formatter().format6( source, settings )

            return source.textRange

        } finally {

            active.remove()

        }
    }

    /** Applies the updated formatting rules.
     * @since 1.0.0
     * @see processText6
     */

    /** Retains the previous behavior.
     * @since 1.0.0
     * @deprecated Use [processText8].
     * @see processText8
     */
    @Deprecated( "Use processText8 instead." )
    /**
         * Handles a native reformat request and returns the resulting affected text range.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param rangeToReformat range requested by the native formatter; structural processing requires the whole file
         * @param settings native code style, right margin and formatter-marker preferences
         *
         * @return updated whole-file range, or the original range when custom processing is skipped
         *
         * @see processText6
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun processText7(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {

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

            Formatter().format7( source, settings )

            return source.textRange

        } finally {

            active.remove()

        }
    }

    /** Applies the current layout rules.
     * @since 1.0.0
     * @see processText7
     */
    @Deprecated( "Use processText9 instead." )
    /**
         * Handles a native reformat request and returns the resulting affected text range.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param rangeToReformat range requested by the native formatter; structural processing requires the whole file
         * @param settings native code style, right margin and formatter-marker preferences
         *
         * @return updated whole-file range, or the original range when custom processing is skipped
         *
         * @see processText7
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun processText8(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {

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

            Formatter().format8( source, settings )

            return source.textRange

        } finally {

            active.remove()

        }
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
     * Handles a native reformat request and returns the resulting affected text range.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param rangeToReformat range requested by the native formatter; structural processing requires the whole file
     * @param settings native code style, right margin and formatter-marker preferences
     *
     * @return updated whole-file range, or the original range when custom processing is skipped
     *
     * @see processText8
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun processText9(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {

        val options = source.project
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

            Formatter().format9( source, settings, options )

            return source.textRange

        } finally {

            active.remove()

        }
    }

    companion object {

        private val active = ThreadLocal.withInitial { false }
    }
}