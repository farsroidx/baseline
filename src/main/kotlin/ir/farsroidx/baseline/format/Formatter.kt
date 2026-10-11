package ir.farsroidx.baseline.format

import ir.farsroidx.baseline.config.FormatOptions
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.util.PsiTreeUtil
import ir.farsroidx.baseline.edit.TextEdits
import ir.farsroidx.baseline.layout.*
import ir.farsroidx.baseline.structure.FieldLayout
import ir.farsroidx.baseline.structure.MemberLayout

/**
 * Coordinates bounded, whole-file Java/Kotlin formatting passes and commits each edit batch.
 *
 * The caller must hold a write action. Syntax-error files and enabled formatter-off regions
 * are skipped. Temporary native code style is restored even if formatting fails. Legacy
 * numbered methods are retained for compatibility; [format9] is the configurable entry point.
 */
internal class Formatter {

    /**
     * Formats the entire file using this retained formatter revision.
     *
     * Mutates and commits the document. Requires a caller-owned write action; enabled
     * formatter-off regions and syntax-error files are left to the native formatter.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param settings native code style, right margin and formatter-marker preferences
     *
     * @deprecated Use [format2] for the replacement revision.
     * @see format2
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    @Deprecated( "Use format2 instead." )
    fun format(source: PsiFile, settings: CodeStyleSettings) {

        if (PsiTreeUtil.findChildOfType( source, PsiErrorElement::class.java ) != null) {

            return
        }

        val disabledRegion = PsiTreeUtil.findChildrenOfType( source, PsiComment::class.java )
            .any { comment ->

                if (settings.FORMATTER_TAGS_ACCEPT_REGEXP) {

                    settings.formatterOffPattern
                        ?.matcher( comment.text )
                        ?.find() == true

                } else comment.text.contains( settings.FORMATTER_OFF_TAG )

            }

        if (settings.FORMATTER_TAGS_ENABLED && disabledRegion) {

            return
        }

        val manager = PsiDocumentManager.getInstance( source.project )

        val document = manager.getDocument( source ) ?: return

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val structure = MemberLayout()

        apply(document, manager, structure.fields( source ))

        apply(document, manager, structure.assignments( source ))

        apply(document, manager, structure.localVariables( source ))

        apply(document, manager, structure.modifiers( source ))

        val settingsManager = CodeStyleSettingsManager.getInstance( source.project )

        val canonical = settingsManager.cloneSettings( settings )

        canonical.AUTODETECT_INDENTS = false

        val common = canonical.getCommonSettings( source.language )

        val indent = common.indentOptions ?: common.initIndentOptions()

        indent.INDENT_SIZE = INDENT_SIZE

        indent.CONTINUATION_INDENT_SIZE = INDENT_SIZE

        indent.USE_TAB_CHARACTER = false

        common.BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.CLASS_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.METHOD_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_METHODS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_CLASSES_IN_ONE_LINE = false

        common.KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE = false

        common.ALIGN_MULTILINE_PARAMETERS = false

        common.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = false

        common.METHOD_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.CALL_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.FORCE_REARRANGE_MODE = CommonCodeStyleSettings.REARRANGE_NEVER

        val previous = settingsManager.temporarySettings

        settingsManager.setTemporarySettings( canonical )

        try {

            CodeStyleManager.getInstance( source.project )
                .reformat( source )

        } finally {

            settingsManager.dropTemporarySettings()

            if (previous != null) settingsManager.setTemporarySettings( previous )

        }

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val layout = SpaceLayout()

        apply(document, manager, layout.spacing( source ))

        for (level in 0..layout.parameterDepth( source )) {

            apply(document, manager, layout.wrapping( source, settings.getRightMargin( source.language ), level ))

        }

        val end = document.text.trimEnd()

        if (end.length != document.textLength) {

            document.deleteString( end.length, document.textLength )

            manager.commitDocument( document )

        }
    }

    @Deprecated( "Use format3 instead." )
    /**
         * Formats the entire file using this retained formatter revision.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * Mutates and commits the document. Requires a caller-owned write action; enabled
         * formatter-off regions and syntax-error files are left to the native formatter.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param settings native code style, right margin and formatter-marker preferences
         *
         * @see format
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun format2(source: PsiFile, settings: CodeStyleSettings) {

        repeat( MAX_LAYOUT_PASSES ) {

            val previous = source.text

            formatPass(source, settings)

            if (source.text == previous) {

                return
            }

        }
    }

    @Deprecated( "Use format4 instead." )
    /**
         * Formats the entire file using this retained formatter revision.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * Mutates and commits the document. Requires a caller-owned write action; enabled
         * formatter-off regions and syntax-error files are left to the native formatter.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param settings native code style, right margin and formatter-marker preferences
         *
         * @see format2
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun format3(source: PsiFile, settings: CodeStyleSettings) {

        repeat( MAX_LAYOUT_PASSES ) {

            val previous = source.text

            formatPass2(source, settings)

            if (source.text == previous) {

                return
            }

        }
    }

    @Deprecated( "Use format5 instead." )
    /**
         * Formats the entire file using this retained formatter revision.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * Mutates and commits the document. Requires a caller-owned write action; enabled
         * formatter-off regions and syntax-error files are left to the native formatter.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param settings native code style, right margin and formatter-marker preferences
         *
         * @see format3
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun format4(source: PsiFile, settings: CodeStyleSettings) {

        repeat( MAX_LAYOUT_PASSES ) {

            val previous = source.text

            formatPass3(source, settings)

            if (source.text == previous) {

                return
            }

        }
    }

    /**
     * Retains the previous layout behavior.
     *
     * @deprecated Use [format6] instead.
     * @see format6
     */
    @Deprecated( "Use format6 instead." )
    /**
         * Formats the entire file using this retained formatter revision.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * Mutates and commits the document. Requires a caller-owned write action; enabled
         * formatter-off regions and syntax-error files are left to the native formatter.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param settings native code style, right margin and formatter-marker preferences
         *
         * @see format4
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun format5(source: PsiFile, settings: CodeStyleSettings) {

        repeat( MAX_LAYOUT_PASSES ) {

            val previous = source.text

            formatPass4(source, settings)

            if (source.text == previous) {

                return
            }

        }
    }

    /** Retains the previous behavior.
     * @since 1.0.0
     * @deprecated Use [format7].
     * @see format7
     */
    @Deprecated( "Use format7 instead." )
    /**
         * Formats the entire file using this retained formatter revision.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * Mutates and commits the document. Requires a caller-owned write action; enabled
         * formatter-off regions and syntax-error files are left to the native formatter.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param settings native code style, right margin and formatter-marker preferences
         *
         * @see format5
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun format6(source: PsiFile, settings: CodeStyleSettings) {

        repeat( MAX_LAYOUT_PASSES ) {

            val previous = source.text

            formatPass5(source, settings)

            if (source.text == previous) {

                return
            }

        }
    }

    /** Retains the previous behavior.
     * @since 1.0.0
     * @deprecated Use [format8].
     * @see format8
     */
    @Deprecated( "Use format8 instead." )
    /**
         * Formats the entire file using this retained formatter revision.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * Mutates and commits the document. Requires a caller-owned write action; enabled
         * formatter-off regions and syntax-error files are left to the native formatter.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param settings native code style, right margin and formatter-marker preferences
         *
         * @see format6
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun format7(source: PsiFile, settings: CodeStyleSettings) {

        repeat( MAX_LAYOUT_PASSES ) {

            val previous = source.text

            formatPass6(source, settings)

            if (source.text == previous) {

                return
            }

        }
    }

    /** Applies the current layout rules.
     * @since 1.0.0
     * @see format7
     */
    @Deprecated( "Use format9 instead." )
    /**
         * Formats the entire file using this retained formatter revision.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * Mutates and commits the document. Requires a caller-owned write action; enabled
         * formatter-off regions and syntax-error files are left to the native formatter.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param settings native code style, right margin and formatter-marker preferences
         *
         * @see format7
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun format8(source: PsiFile, settings: CodeStyleSettings) {

        repeat( MAX_LAYOUT_PASSES ) {

            val previous = source.text

            formatPass7(source, settings)

            if (source.text == previous) {

                return
            }

        }
    }

    @Deprecated( "Use formatPass2 instead." )
    /**
     * Performs one structural, native-formatting and custom-layout cycle for this revision.
     *
     * Mutates and commits the document. Requires a caller-owned write action; enabled
     * formatter-off regions and syntax-error files are left to the native formatter.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param settings native code style, right margin and formatter-marker preferences
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun formatPass(source: PsiFile, settings: CodeStyleSettings) {

        if (PsiTreeUtil.findChildOfType( source, PsiErrorElement::class.java ) != null) {

            return
        }

        val disabledRegion = PsiTreeUtil.findChildrenOfType( source, PsiComment::class.java )
            .any { comment ->

                if (settings.FORMATTER_TAGS_ACCEPT_REGEXP) {

                    settings.formatterOffPattern
                        ?.matcher( comment.text )
                        ?.find() == true

                } else comment.text.contains( settings.FORMATTER_OFF_TAG )

            }

        if (settings.FORMATTER_TAGS_ENABLED && disabledRegion) {

            return
        }

        val manager = PsiDocumentManager.getInstance( source.project )

        val document = manager.getDocument( source ) ?: return

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val structure = MemberLayout()

        apply(document, manager, structure.fields( source ))

        apply(document, manager, structure.assignments( source ))

        apply(document, manager, structure.localVariables( source ))

        apply(document, manager, structure.modifiers( source ))

        val settingsManager = CodeStyleSettingsManager.getInstance( source.project )

        val canonical = settingsManager.cloneSettings( settings )

        canonical.AUTODETECT_INDENTS = false

        val common = canonical.getCommonSettings( source.language )

        val indent = common.indentOptions ?: common.initIndentOptions()

        indent.INDENT_SIZE = INDENT_SIZE

        indent.CONTINUATION_INDENT_SIZE = INDENT_SIZE

        indent.USE_TAB_CHARACTER = false

        common.BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.CLASS_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.METHOD_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.KEEP_LINE_BREAKS = false

        common.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_METHODS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_CLASSES_IN_ONE_LINE = false

        common.KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE = false

        common.ALIGN_MULTILINE_PARAMETERS = false

        common.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = false

        common.METHOD_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.CALL_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.FORCE_REARRANGE_MODE = CommonCodeStyleSettings.REARRANGE_NEVER

        val previous = settingsManager.temporarySettings

        settingsManager.setTemporarySettings( canonical )

        try {

            CodeStyleManager.getInstance( source.project )
                .reformat( source )

        } finally {

            settingsManager.dropTemporarySettings()

            if (previous != null) settingsManager.setTemporarySettings( previous )

        }

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val layout = SpaceLayout()

        apply(document, manager, layout.spacing2( source ))

        for (level in layout.parameterDepth( source ) downTo 0) {

            apply(document, manager, layout.wrapping2( source, Int.MAX_VALUE, level ))

        }

        for (level in 0..layout.parameterDepth( source )) {

            apply(document, manager, layout.wrapping2( source, settings.getRightMargin( source.language ), level ))

        }

        apply(document, manager, layout.controlClosings( source ))

        val end = document.text.trimEnd()

        if (end.length != document.textLength) {

            document.deleteString( end.length, document.textLength )

            manager.commitDocument( document )

        }
    }

    @Deprecated( "Use formatPass3 instead." )
    /**
     * Performs one structural, native-formatting and custom-layout cycle for this revision.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * Mutates and commits the document. Requires a caller-owned write action; enabled
     * formatter-off regions and syntax-error files are left to the native formatter.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param settings native code style, right margin and formatter-marker preferences
     *
     * @see formatPass
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun formatPass2(source: PsiFile, settings: CodeStyleSettings) {

        if (PsiTreeUtil.findChildOfType( source, PsiErrorElement::class.java ) != null) {

            return
        }

        val disabledRegion = PsiTreeUtil.findChildrenOfType( source, PsiComment::class.java )
            .any { comment ->

                if (settings.FORMATTER_TAGS_ACCEPT_REGEXP) {

                    settings.formatterOffPattern
                        ?.matcher( comment.text )
                        ?.find() == true

                } else comment.text.contains( settings.FORMATTER_OFF_TAG )

            }

        if (settings.FORMATTER_TAGS_ENABLED && disabledRegion) {

            return
        }

        val manager = PsiDocumentManager.getInstance( source.project )

        val document = manager.getDocument( source ) ?: return

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val structure = MemberLayout()

        apply(document, manager, structure.fields( source ))

        apply(document, manager, structure.assignments( source ))

        apply(document, manager, structure.localVariables( source ))

        apply(document, manager, structure.modifiers( source ))

        val settingsManager = CodeStyleSettingsManager.getInstance( source.project )

        val canonical = settingsManager.cloneSettings( settings )

        canonical.AUTODETECT_INDENTS = false

        val common = canonical.getCommonSettings( source.language )

        val indent = common.indentOptions ?: common.initIndentOptions()

        indent.INDENT_SIZE = INDENT_SIZE

        indent.CONTINUATION_INDENT_SIZE = INDENT_SIZE

        indent.USE_TAB_CHARACTER = false

        common.BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.CLASS_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.METHOD_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.KEEP_LINE_BREAKS = false

        common.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_METHODS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_CLASSES_IN_ONE_LINE = false

        common.KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE = false

        common.ALIGN_MULTILINE_ASSIGNMENT = true

        common.ALIGN_MULTILINE_BINARY_OPERATION = true

        common.ALIGN_MULTILINE_TERNARY_OPERATION = true

        common.TERNARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_ALWAYS

        common.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE = true

        common.ALIGN_MULTILINE_PARAMETERS = false

        common.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = false

        common.METHOD_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.CALL_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.FORCE_REARRANGE_MODE = CommonCodeStyleSettings.REARRANGE_NEVER

        val previous = settingsManager.temporarySettings

        settingsManager.setTemporarySettings( canonical )

        try {

            CodeStyleManager.getInstance( source.project )
                .reformat( source )

        } finally {

            settingsManager.dropTemporarySettings()

            if (previous != null) settingsManager.setTemporarySettings( previous )

        }

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val layout = SpaceLayout()

        apply(document, manager, layout.spacing2( source ))

        for (level in layout.parameterDepth( source ) downTo 0) {

            apply(document, manager, layout.wrapping3( source, Int.MAX_VALUE, level ))

        }

        for (level in 0..layout.parameterDepth( source )) {

            apply(document, manager, layout.wrapping3( source, settings.getRightMargin( source.language ), level ))

        }

        apply(document, manager, layout.controlClosings( source ))

        val advanced = BlockLayout()

        apply(document, manager, advanced.chains( source ))

        apply(document, manager, advanced.statementSpacing( source ))

        apply(document, manager, advanced.braces( source ))

        val end = document.text.trimEnd()

        if (end.length != document.textLength) {

            document.deleteString( end.length, document.textLength )

            manager.commitDocument( document )

        }
    }

    @Deprecated( "Use formatPass4 instead." )
    /**
     * Performs one structural, native-formatting and custom-layout cycle for this revision.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * Mutates and commits the document. Requires a caller-owned write action; enabled
     * formatter-off regions and syntax-error files are left to the native formatter.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param settings native code style, right margin and formatter-marker preferences
     *
     * @see formatPass2
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun formatPass3(source: PsiFile, settings: CodeStyleSettings) {

        if (PsiTreeUtil.findChildOfType( source, PsiErrorElement::class.java ) != null) {

            return
        }

        val disabledRegion = PsiTreeUtil.findChildrenOfType( source, PsiComment::class.java )
            .any { comment ->

                if (settings.FORMATTER_TAGS_ACCEPT_REGEXP) {

                    settings.formatterOffPattern
                        ?.matcher( comment.text )
                        ?.find() == true

                } else comment.text.contains( settings.FORMATTER_OFF_TAG )

            }

        if (settings.FORMATTER_TAGS_ENABLED && disabledRegion) {

            return
        }

        val manager = PsiDocumentManager.getInstance( source.project )

        val document = manager.getDocument( source ) ?: return

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val structure = MemberLayout()

        apply(document, manager, FieldLayout().reorder( source ))

        apply(document, manager, structure.assignments( source ))

        apply(document, manager, structure.localVariables( source ))

        apply(document, manager, structure.modifiers( source ))

        val settingsManager = CodeStyleSettingsManager.getInstance( source.project )

        val canonical = settingsManager.cloneSettings( settings )

        canonical.AUTODETECT_INDENTS = false

        val common = canonical.getCommonSettings( source.language )

        val indent = common.indentOptions ?: common.initIndentOptions()

        indent.INDENT_SIZE = INDENT_SIZE

        indent.CONTINUATION_INDENT_SIZE = INDENT_SIZE

        indent.USE_TAB_CHARACTER = false

        common.BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.CLASS_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.METHOD_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.KEEP_LINE_BREAKS = false

        common.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_METHODS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_CLASSES_IN_ONE_LINE = false

        common.KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE = false

        common.ALIGN_MULTILINE_ASSIGNMENT = true

        common.ALIGN_MULTILINE_BINARY_OPERATION = false

        common.BINARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED

        common.ALIGN_MULTILINE_TERNARY_OPERATION = true

        common.TERNARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_ALWAYS

        common.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE = true

        common.ALIGN_MULTILINE_PARAMETERS = false

        common.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = false

        common.METHOD_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.CALL_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.FORCE_REARRANGE_MODE = CommonCodeStyleSettings.REARRANGE_NEVER

        val previous = settingsManager.temporarySettings

        settingsManager.setTemporarySettings( canonical )

        try {

            CodeStyleManager.getInstance( source.project )
                .reformat( source )

        } finally {

            settingsManager.dropTemporarySettings()

            if (previous != null) settingsManager.setTemporarySettings( previous )

        }

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val layout = SpaceLayout()

        apply(document, manager, layout.spacing2( source ))

        for (level in layout.parameterDepth( source ) downTo 0) {

            apply(document, manager, layout.wrapping4( source, Int.MAX_VALUE, level ))

        }

        for (level in 0..layout.parameterDepth( source )) {

            apply(document, manager, layout.wrapping4( source, settings.getRightMargin( source.language ), level ))

        }

        apply(document, manager, layout.controlClosings( source ))

        val advanced = BlockLayout()

        apply(document, manager, advanced.chains( source ))

        apply(document, manager, advanced.statementSpacing2( source ))

        apply(document, manager, advanced.braces( source ))

        val syntax = SyntaxLayout()

        apply(document, manager, syntax.conditions( source, settings.getRightMargin( source.language ) ))

        apply(document, manager, syntax.annotations( source ))

        apply(document, manager, syntax.branches( source ))

        apply(document, manager, FieldLayout().align( source ))

        apply(document, manager, advanced.braces( source ))

        val end = document.text.trimEnd()

        if (end.length != document.textLength) {

            document.deleteString( end.length, document.textLength )

            manager.commitDocument( document )

        }
    }

    /**
     * Retains the previous layout behavior.
     *
     * @deprecated Use [formatPass5] instead.
     * @see formatPass5
     */
    @Deprecated( "Use formatPass5 instead." )
    /**
     * Performs one structural, native-formatting and custom-layout cycle for this revision.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * Mutates and commits the document. Requires a caller-owned write action; enabled
     * formatter-off regions and syntax-error files are left to the native formatter.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param settings native code style, right margin and formatter-marker preferences
     *
     * @see formatPass3
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun formatPass4(source: PsiFile, settings: CodeStyleSettings) {

        if (PsiTreeUtil.findChildOfType( source, PsiErrorElement::class.java ) != null) {

            return
        }

        val disabledRegion = PsiTreeUtil.findChildrenOfType( source, PsiComment::class.java )
            .any { comment ->

                if (settings.FORMATTER_TAGS_ACCEPT_REGEXP) {

                    settings.formatterOffPattern
                        ?.matcher( comment.text )
                        ?.find() == true

                } else comment.text.contains( settings.FORMATTER_OFF_TAG )

            }

        if (settings.FORMATTER_TAGS_ENABLED && disabledRegion) {

            return
        }

        val manager = PsiDocumentManager.getInstance( source.project )

        val document = manager.getDocument( source ) ?: return

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val structure = MemberLayout()

        apply2(document, manager, FieldLayout().reorder( source ))

        apply2(document, manager, structure.assignments( source ))

        apply2(document, manager, structure.localVariables( source ))

        apply2(document, manager, structure.modifiers( source ))

        val settingsManager = CodeStyleSettingsManager.getInstance( source.project )

        val canonical = settingsManager.cloneSettings( settings )

        canonical.AUTODETECT_INDENTS = false

        val common = canonical.getCommonSettings( source.language )

        val indent = common.indentOptions ?: common.initIndentOptions()

        indent.INDENT_SIZE = INDENT_SIZE

        indent.CONTINUATION_INDENT_SIZE = INDENT_SIZE

        indent.USE_TAB_CHARACTER = false

        common.BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.CLASS_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.METHOD_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.KEEP_LINE_BREAKS = false

        common.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_METHODS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_CLASSES_IN_ONE_LINE = false

        common.KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE = false

        common.ALIGN_MULTILINE_ASSIGNMENT = true

        common.ALIGN_MULTILINE_BINARY_OPERATION = false

        common.BINARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED

        common.ALIGN_MULTILINE_TERNARY_OPERATION = true

        common.TERNARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_ALWAYS

        common.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE = true

        common.ALIGN_MULTILINE_PARAMETERS = false

        common.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = false

        common.METHOD_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.CALL_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.FORCE_REARRANGE_MODE = CommonCodeStyleSettings.REARRANGE_NEVER

        val previous = settingsManager.temporarySettings

        settingsManager.setTemporarySettings( canonical )

        try {

            CodeStyleManager.getInstance( source.project )
                .reformat( source )

        } finally {

            settingsManager.dropTemporarySettings()

            if (previous != null) settingsManager.setTemporarySettings( previous )

        }

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val layout = SpaceLayout()

        apply2(document, manager, layout.spacing2( source ))

        apply2(document, manager, ChainLayout().links( source ))

        for (level in layout.parameterDepth( source ) downTo 0) {

            apply2(document, manager, layout.wrapping5( source, Int.MAX_VALUE, level ))

        }

        for (level in 0..layout.parameterDepth( source )) {

            apply2(document, manager, ChainLayout().links( source ))

            apply2(document, manager, layout.wrapping5( source, settings.getRightMargin( source.language ), level ))

        }

        apply2(document, manager, layout.controlClosings( source ))

        val advanced = BlockLayout()

        apply2(document, manager, ChainLayout().links( source ))

        apply2(document, manager, advanced.statementSpacing2( source ))

        apply2(document, manager, advanced.braces( source ))

        val syntax = SyntaxLayout()

        apply2(document, manager, syntax.conditions( source, settings.getRightMargin( source.language ) ))

        apply2(document, manager, syntax.annotations( source ))

        apply2(document, manager, syntax.branches( source ))

        apply2(document, manager, FieldLayout().align( source ))

        apply2(document, manager, advanced.braces( source ))

        val conditional = BranchLayout()

        apply2(document, manager, conditional.keywordHeaders( source ))

        apply2(document, manager, conditional.branches( source, settings.getRightMargin( source.language ) ))

        val end = document.text.trimEnd()

        if (end.length != document.textLength) {

            document.deleteString( end.length, document.textLength )

            manager.commitDocument( document )

        }
    }

    /**
     * Applies the current layout rules.
     *
     * @since 1.0.0
     * @see formatPass4
     */

    /** Retains the previous behavior.
     * @since 1.0.0
     * @deprecated Use [formatPass6].
     * @see formatPass6
     */
    @Deprecated( "Use formatPass6 instead." )
    /**
     * Performs one structural, native-formatting and custom-layout cycle for this revision.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * Mutates and commits the document. Requires a caller-owned write action; enabled
     * formatter-off regions and syntax-error files are left to the native formatter.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param settings native code style, right margin and formatter-marker preferences
     *
     * @see formatPass4
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun formatPass5(source: PsiFile, settings: CodeStyleSettings) {

        if (PsiTreeUtil.findChildOfType( source, PsiErrorElement::class.java ) != null) {

            return
        }

        val disabledRegion = PsiTreeUtil.findChildrenOfType( source, PsiComment::class.java )
            .any { comment ->

                if (settings.FORMATTER_TAGS_ACCEPT_REGEXP) {

                    settings.formatterOffPattern
                        ?.matcher( comment.text )
                        ?.find() == true

                } else comment.text.contains( settings.FORMATTER_OFF_TAG )

            }

        if (settings.FORMATTER_TAGS_ENABLED && disabledRegion) {

            return
        }

        val manager = PsiDocumentManager.getInstance( source.project )

        val document = manager.getDocument( source ) ?: return

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val structure = MemberLayout()

        apply2(document, manager, FieldLayout().reorder2( source ))

        apply2(document, manager, structure.assignments2( source ))

        apply2(document, manager, structure.localVariables2( source ))

        apply2(document, manager, structure.modifiers( source ))

        val settingsManager = CodeStyleSettingsManager.getInstance( source.project )

        val canonical = settingsManager.cloneSettings( settings )

        canonical.AUTODETECT_INDENTS = false

        val common = canonical.getCommonSettings( source.language )

        val indent = common.indentOptions ?: common.initIndentOptions()

        indent.INDENT_SIZE = INDENT_SIZE

        indent.CONTINUATION_INDENT_SIZE = INDENT_SIZE

        indent.USE_TAB_CHARACTER = false

        common.BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.CLASS_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.METHOD_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.KEEP_LINE_BREAKS = false

        common.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_METHODS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_CLASSES_IN_ONE_LINE = false

        common.KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE = false

        common.ALIGN_MULTILINE_ASSIGNMENT = true

        common.ALIGN_MULTILINE_BINARY_OPERATION = false

        common.BINARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED

        common.ALIGN_MULTILINE_TERNARY_OPERATION = true

        common.TERNARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_ALWAYS

        common.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE = true

        common.ALIGN_MULTILINE_PARAMETERS = false

        common.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = false

        common.METHOD_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.CALL_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.FORCE_REARRANGE_MODE = CommonCodeStyleSettings.REARRANGE_NEVER

        val previous = settingsManager.temporarySettings

        settingsManager.setTemporarySettings( canonical )

        try {

            CodeStyleManager.getInstance( source.project )
                .reformat( source )

        } finally {

            settingsManager.dropTemporarySettings()

            if (previous != null) settingsManager.setTemporarySettings( previous )

        }

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val layout = SpaceLayout()

        apply2(document, manager, layout.spacing2( source ))

        apply2(document, manager, ChainLayout().links( source ))

        for (level in layout.parameterDepth( source ) downTo 0) {

            apply2(document, manager, layout.wrapping5( source, Int.MAX_VALUE, level ))

        }

        for (level in 0..layout.parameterDepth( source )) {

            apply2(document, manager, ChainLayout().links( source ))

            apply2(document, manager, layout.wrapping5( source, settings.getRightMargin( source.language ), level ))

        }

        apply2(document, manager, layout.controlClosings( source ))

        val advanced = BlockLayout()

        apply2(document, manager, ChainLayout().links( source ))

        apply2(document, manager, advanced.statementSpacing2( source ))

        apply2(document, manager, advanced.braces( source ))

        val syntax = SyntaxLayout()

        apply2(document, manager, syntax.conditions( source, settings.getRightMargin( source.language ) ))

        apply2(document, manager, syntax.annotations( source ))

        apply2(document, manager, syntax.branches( source ))

        apply2(document, manager, FieldLayout().align( source ))

        apply2(document, manager, advanced.braces( source ))

        val conditional = BranchLayout()

        apply2(document, manager, conditional.keywordHeaders( source ))

        apply2(document, manager, conditional.branches( source, settings.getRightMargin( source.language ) ))

        val margins = CallLayout()

        val rightMargin = settings.getRightMargin( source.language )

        apply2(document, manager, margins.receivers( source, rightMargin ))

        for (level in 0..layout.parameterDepth( source )) {

            apply2(document, manager, margins.arguments( source, rightMargin, level ))

        }

        apply2(document, manager, margins.expressionBodies( source, rightMargin ))

        apply2(document, manager, margins.conditionalBlocks( source ))

        apply2(document, manager, margins.headers( source ))

        apply2(document, manager, margins.bareReturns( source ))

        apply2(document, manager, margins.fallbacks( source ))

        val end = document.text.trimEnd()

        if (end.length != document.textLength) {

            document.deleteString( end.length, document.textLength )

            manager.commitDocument( document )

        }
    }

    /** Applies the updated formatting rules.
     * @since 1.0.0
     * @see formatPass5
     */

    /** Retains the previous behavior.
     * @since 1.0.0
     * @deprecated Use [formatPass7].
     * @see formatPass7
     */
    @Deprecated( "Use formatPass7 instead." )
    /**
     * Performs one structural, native-formatting and custom-layout cycle for this revision.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * Mutates and commits the document. Requires a caller-owned write action; enabled
     * formatter-off regions and syntax-error files are left to the native formatter.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param settings native code style, right margin and formatter-marker preferences
     *
     * @see formatPass5
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun formatPass6(source: PsiFile, settings: CodeStyleSettings) {

        if (PsiTreeUtil.findChildOfType( source, PsiErrorElement::class.java ) != null) {

            return
        }

        val disabledRegion = PsiTreeUtil.findChildrenOfType( source, PsiComment::class.java )
            .any { comment ->

                if (settings.FORMATTER_TAGS_ACCEPT_REGEXP) {

                    settings.formatterOffPattern
                        ?.matcher( comment.text )
                        ?.find() == true

                } else comment.text.contains( settings.FORMATTER_OFF_TAG )

            }

        if (settings.FORMATTER_TAGS_ENABLED && disabledRegion) {

            return
        }

        val manager = PsiDocumentManager.getInstance( source.project )

        val document = manager.getDocument( source ) ?: return

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val structure = MemberLayout()

        apply2(document, manager, FieldLayout().reorder2( source ))

        apply2(document, manager, structure.assignments2( source ))

        apply2(document, manager, structure.localVariables2( source ))

        apply2(document, manager, structure.modifiers( source ))

        val settingsManager = CodeStyleSettingsManager.getInstance( source.project )

        val canonical = settingsManager.cloneSettings( settings )

        canonical.AUTODETECT_INDENTS = false

        val common = canonical.getCommonSettings( source.language )

        val indent = common.indentOptions ?: common.initIndentOptions()

        indent.INDENT_SIZE = INDENT_SIZE

        indent.CONTINUATION_INDENT_SIZE = INDENT_SIZE

        indent.USE_TAB_CHARACTER = false

        common.BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.CLASS_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.METHOD_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.KEEP_LINE_BREAKS = false

        common.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_METHODS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_CLASSES_IN_ONE_LINE = false

        common.KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE = false

        common.ALIGN_MULTILINE_ASSIGNMENT = true

        common.ALIGN_MULTILINE_BINARY_OPERATION = false

        common.BINARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED

        common.ALIGN_MULTILINE_TERNARY_OPERATION = true

        common.TERNARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_ALWAYS

        common.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE = true

        common.ALIGN_MULTILINE_PARAMETERS = false

        common.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = false

        common.METHOD_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.CALL_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.FORCE_REARRANGE_MODE = CommonCodeStyleSettings.REARRANGE_NEVER

        val previous = settingsManager.temporarySettings

        settingsManager.setTemporarySettings( canonical )

        try {

            CodeStyleManager.getInstance( source.project )
                .reformat( source )

        } finally {

            settingsManager.dropTemporarySettings()

            if (previous != null) settingsManager.setTemporarySettings( previous )

        }

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val layout = SpaceLayout()

        apply2(document, manager, layout.spacing2( source ))

        apply2(document, manager, ChainLayout().links( source ))

        for (level in layout.parameterDepth( source ) downTo 0) {

            apply2(document, manager, layout.wrapping5( source, Int.MAX_VALUE, level ))

        }

        for (level in 0..layout.parameterDepth( source )) {

            apply2(document, manager, ChainLayout().links( source ))

            apply2(document, manager, layout.wrapping5( source, settings.getRightMargin( source.language ), level ))

        }

        apply2(document, manager, layout.controlClosings( source ))

        val advanced = BlockLayout()

        apply2(document, manager, ChainLayout().links( source ))

        apply2(document, manager, advanced.statementSpacing2( source ))

        apply2(document, manager, advanced.braces( source ))

        val syntax = SyntaxLayout()

        apply2(document, manager, syntax.conditions( source, settings.getRightMargin( source.language ) ))

        apply2(document, manager, syntax.annotations( source ))

        apply2(document, manager, syntax.branches2( source ))

        apply2(document, manager, FieldLayout().align( source ))

        apply2(document, manager, advanced.braces( source ))

        val conditional = BranchLayout()

        apply2(document, manager, conditional.keywordHeaders( source ))

        apply2(document, manager, conditional.branches( source, settings.getRightMargin( source.language ) ))

        val margins = CallLayout()

        val rightMargin = settings.getRightMargin( source.language )

        apply2(document, manager, margins.receivers2( source, rightMargin ))

        for (level in 0..layout.parameterDepth( source )) {

            apply2(document, manager, margins.arguments( source, rightMargin, level ))

        }

        apply2(document, manager, margins.expressionBodies( source, rightMargin ))

        apply2(document, manager, margins.conditionalBlocks( source ))

        apply2(document, manager, margins.headers( source ))

        apply2(document, manager, margins.bareReturns2( source ))

        apply2(document, manager, margins.fallbacks2( source ))

        val logical = LogicLayout()

        apply2(document, manager, logical.assignments( source ))

        apply2(document, manager, logical.lambdas( source ))

        apply2(document, manager, logical.conditions( source, rightMargin ))

        apply2(document, manager, logical.expressions( source, rightMargin ))

        val end = document.text.trimEnd()

        if (end.length != document.textLength) {

            document.deleteString( end.length, document.textLength )

            manager.commitDocument( document )

        }
    }

    /** Applies the current layout rules.
     * @since 1.0.0
     * @see formatPass6
     */
    @Deprecated( "Use formatPass8 instead." )
    /**
     * Performs one structural, native-formatting and custom-layout cycle for this revision.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * Mutates and commits the document. Requires a caller-owned write action; enabled
     * formatter-off regions and syntax-error files are left to the native formatter.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param settings native code style, right margin and formatter-marker preferences
     *
     * @see formatPass6
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun formatPass7(source: PsiFile, settings: CodeStyleSettings) {

        if (PsiTreeUtil.findChildOfType( source, PsiErrorElement::class.java ) != null) {

            return
        }

        val disabledRegion = PsiTreeUtil.findChildrenOfType( source, PsiComment::class.java )
            .any { comment ->

                if (settings.FORMATTER_TAGS_ACCEPT_REGEXP) {

                    settings.formatterOffPattern
                        ?.matcher( comment.text )
                        ?.find() == true

                } else comment.text.contains( settings.FORMATTER_OFF_TAG )

            }

        if (settings.FORMATTER_TAGS_ENABLED && disabledRegion) {

            return
        }

        val manager = PsiDocumentManager.getInstance( source.project )

        val document = manager.getDocument( source ) ?: return

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val structure = MemberLayout()

        apply2(document, manager, FieldLayout().reorder2( source ))

        apply2(document, manager, structure.assignments2( source ))

        apply2(document, manager, structure.localVariables2( source ))

        apply2(document, manager, structure.modifiers( source ))

        val settingsManager = CodeStyleSettingsManager.getInstance( source.project )

        val canonical = settingsManager.cloneSettings( settings )

        canonical.AUTODETECT_INDENTS = false

        val common = canonical.getCommonSettings( source.language )

        val indent = common.indentOptions ?: common.initIndentOptions()

        indent.INDENT_SIZE = INDENT_SIZE

        indent.CONTINUATION_INDENT_SIZE = INDENT_SIZE

        indent.USE_TAB_CHARACTER = false

        common.BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.CLASS_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.METHOD_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        common.KEEP_LINE_BREAKS = false

        common.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_METHODS_IN_ONE_LINE = false

        common.KEEP_SIMPLE_CLASSES_IN_ONE_LINE = false

        common.KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE = false

        common.ALIGN_MULTILINE_ASSIGNMENT = true

        common.ALIGN_MULTILINE_BINARY_OPERATION = false

        common.BINARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED

        common.ALIGN_MULTILINE_TERNARY_OPERATION = true

        common.TERNARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_ALWAYS

        common.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE = true

        common.ALIGN_MULTILINE_PARAMETERS = false

        common.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = false

        common.METHOD_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.CALL_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.FORCE_REARRANGE_MODE = CommonCodeStyleSettings.REARRANGE_NEVER

        val previous = settingsManager.temporarySettings

        settingsManager.setTemporarySettings( canonical )

        try {

            CodeStyleManager.getInstance( source.project )
                .reformat( source )

        } finally {

            settingsManager.dropTemporarySettings()

            if (previous != null) settingsManager.setTemporarySettings( previous )

        }

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val layout = SpaceLayout()

        apply2(document, manager, layout.spacing2( source ))

        apply2(document, manager, ChainLayout().links( source ))

        for (level in layout.parameterDepth( source ) downTo 0) {

            apply2(document, manager, layout.wrapping5( source, Int.MAX_VALUE, level ))

        }

        for (level in 0..layout.parameterDepth( source )) {

            apply2(document, manager, ChainLayout().links( source ))

            apply2(document, manager, layout.wrapping5( source, settings.getRightMargin( source.language ), level ))

        }

        apply2(document, manager, layout.controlClosings( source ))

        val advanced = BlockLayout()

        apply2(document, manager, ChainLayout().links( source ))

        apply2(document, manager, advanced.statementSpacing2( source ))

        apply2(document, manager, advanced.braces( source ))

        val syntax = SyntaxLayout()

        apply2(document, manager, syntax.conditions( source, settings.getRightMargin( source.language ) ))

        apply2(document, manager, syntax.annotations( source ))

        apply2(document, manager, syntax.branches2( source ))

        apply2(document, manager, FieldLayout().align( source ))

        apply2(document, manager, advanced.braces( source ))

        val conditional = BranchLayout()

        apply2(document, manager, conditional.keywordHeaders( source ))

        apply2(document, manager, conditional.branches( source, settings.getRightMargin( source.language ) ))

        val margins = CallLayout()

        val rightMargin = settings.getRightMargin( source.language )

        apply2(document, manager, margins.receivers2( source, rightMargin ))

        for (level in 0..layout.parameterDepth( source )) {

            apply2(document, manager, margins.arguments( source, rightMargin, level ))

        }

        apply2(document, manager, margins.expressionBodies( source, rightMargin ))

        apply2(document, manager, margins.conditionalBlocks( source ))

        apply2(document, manager, margins.headers( source ))

        apply2(document, manager, margins.bareReturns2( source ))

        apply2(document, manager, margins.fallbacks2( source ))

        val logical = LogicLayout()

        apply2(document, manager, logical.assignments( source ))

        apply2(document, manager, logical.lambdas( source ))

        apply2(document, manager, logical.conditions2( source, rightMargin ))

        apply2(document, manager, logical.expressions( source, rightMargin ))

        val end = document.text.trimEnd()

        if (end.length != document.textLength) {

            document.deleteString( end.length, document.textLength )

            manager.commitDocument( document )

        }
    }

    @Deprecated( "Use apply2 instead." )
    /**
     * Applies a replacement batch to the document and commits PSI when a manager is supplied.
     *
     * @param document document whose offsets match the collected replacement ranges
     * @param manager PSI/document manager used to unblock and commit edited text
     * @param edits batch receiving planned replacements; the source document is not changed here
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun apply(document: Document, manager: PsiDocumentManager, edits: TextEdits) {

        manager.doPostponedOperationsAndUnblockDocument( document )

        edits.apply( document )

        manager.commitDocument( document )

    }

    /**
     * Applies a replacement batch to the document and commits PSI when a manager is supplied.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param document document whose offsets match the collected replacement ranges
     * @param manager PSI/document manager used to unblock and commit edited text
     * @param edits batch receiving planned replacements; the source document is not changed here
     *
     * @see apply
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun apply2(document: Document, manager: PsiDocumentManager, edits: TextEdits) {

        manager.doPostponedOperationsAndUnblockDocument( document )

        edits.apply2( document )

        manager.commitDocument( document )

    }

    /**
     * Formats the whole file using the supplied project-preference snapshot.
     *
     * Runs at most four passes and stops early when the source is unchanged.
     * Earlier revisions remain available for compatibility.
     *
     * Mutates and commits the document. Requires a caller-owned write action; enabled
     * formatter-off regions and syntax-error files are left to the native formatter.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param settings native code style, right margin and formatter-marker preferences
     * @param options project preferences captured for the current operation
     *
     * @see format8
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun format9(source: PsiFile, settings: CodeStyleSettings, options: FormatOptions) {

        repeat( MAX_LAYOUT_PASSES ) {

            val previous = source.text

            formatPass8(source, settings, options)

            if (source.text == previous) {

                return
            }

        }
    }

    /**
     * Performs one structural, native-formatting and custom-layout cycle for this revision.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * Mutates and commits the document. Requires a caller-owned write action; enabled
     * formatter-off regions and syntax-error files are left to the native formatter.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param settings native code style, right margin and formatter-marker preferences
     * @param options project preferences captured for the current operation
     *
     * @see formatPass7
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun formatPass8(source: PsiFile, settings: CodeStyleSettings, options: FormatOptions) {

        if (PsiTreeUtil.findChildOfType( source, PsiErrorElement::class.java ) != null) {

            return
        }

        val disabledRegion = PsiTreeUtil.findChildrenOfType( source, PsiComment::class.java )
            .any { comment ->

                if (settings.FORMATTER_TAGS_ACCEPT_REGEXP) {

                    settings.formatterOffPattern
                        ?.matcher( comment.text )
                        ?.find() == true

                } else comment.text.contains( settings.FORMATTER_OFF_TAG )

            }

        if (settings.FORMATTER_TAGS_ENABLED && disabledRegion) {

            return
        }

        val manager = PsiDocumentManager.getInstance( source.project )

        val document = manager.getDocument( source ) ?: return

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val structure = MemberLayout()

        apply2(document, manager, FieldLayout().reorder3( source, options ))

        apply2(document, manager, structure.assignments3( source, options ))

        apply2(document, manager, structure.localVariables3( source, options ))

        if (options.normalizeModifiers) apply2(document, manager, structure.modifiers( source ))

        val settingsManager = CodeStyleSettingsManager.getInstance( source.project )

        val canonical = settingsManager.cloneSettings( settings )

        if (options.canonicalIndentation) canonical.AUTODETECT_INDENTS = false

        val common = canonical.getCommonSettings( source.language )

        val indent = common.indentOptions ?: common.initIndentOptions()

        if (options.canonicalIndentation) indent.INDENT_SIZE = INDENT_SIZE

        if (options.canonicalIndentation) indent.CONTINUATION_INDENT_SIZE = INDENT_SIZE

        if (options.canonicalIndentation) indent.USE_TAB_CHARACTER = false

        if (options.endOfLineBraces) common.BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        if (options.endOfLineBraces) common.CLASS_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        if (options.endOfLineBraces) common.METHOD_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE

        if (options.normalizeLineBreaks) common.KEEP_LINE_BREAKS = false

        if (options.expandSimpleBlocks) common.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = false

        if (options.expandSimpleBlocks) common.KEEP_SIMPLE_METHODS_IN_ONE_LINE = false

        if (options.expandSimpleBlocks) common.KEEP_SIMPLE_CLASSES_IN_ONE_LINE = false

        if (options.separateStatements) common.KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE = false

        if (options.alignAssignments) common.ALIGN_MULTILINE_ASSIGNMENT = true

        if (options.formatConditions) common.ALIGN_MULTILINE_BINARY_OPERATION = false

        if (options.formatConditions) common.BINARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED

        if (options.formatTernaries) common.ALIGN_MULTILINE_TERNARY_OPERATION = true

        if (options.formatTernaries) common.TERNARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_ALWAYS

        if (options.formatTernaries) common.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE = true

        if (options.wrapDeclarations) common.ALIGN_MULTILINE_PARAMETERS = false

        if (options.wrapCalls) common.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = false

        if (options.wrapDeclarations) common.METHOD_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        if (options.wrapCalls) common.CALL_PARAMETERS_WRAP = CommonCodeStyleSettings.DO_NOT_WRAP

        common.FORCE_REARRANGE_MODE = CommonCodeStyleSettings.REARRANGE_NEVER

        val previous = settingsManager.temporarySettings

        settingsManager.setTemporarySettings( canonical )

        try {

            CodeStyleManager.getInstance( source.project )
                .reformat( source )

        } finally {

            settingsManager.dropTemporarySettings()

            if (previous != null) settingsManager.setTemporarySettings( previous )

        }

        manager.doPostponedOperationsAndUnblockDocument( document )

        manager.commitDocument( document )

        val layout = SpaceLayout()

        apply2(document, manager, layout.spacing3( source, options ))

        if (options.formatChains) apply2(document, manager, ChainLayout().links( source ))

        for (level in layout.parameterDepth( source ) downTo 0) {

            if (options.collapseShortLists && options.wrapCalls && options.wrapDeclarations) {

                apply2(document, manager, layout.wrapping6( source, Int.MAX_VALUE, level, options ))

            }
        }

        for (level in 0..layout.parameterDepth( source )) {

            if (options.formatChains) apply2(document, manager, ChainLayout().links( source ))

            apply2(
                document, manager, layout.wrapping6(
                    source, settings.getRightMargin( source.language ), level, options
                )
            )

        }

        if (options.formatConditions) apply2(document, manager, layout.controlClosings( source ))

        val advanced = BlockLayout()

        if (options.formatChains) apply2(document, manager, ChainLayout().links( source ))

        if (options.spaceStatements) apply2(document, manager, advanced.statementSpacing2( source ))

        if (options.compactClosingBraces) apply2(document, manager, advanced.braces( source ))

        val syntax = SyntaxLayout()

        if (options.formatConditions) {

            apply2(document, manager, syntax.conditions( source, settings.getRightMargin( source.language ) ))

        }

        if (options.formatAnnotations) apply2(document, manager, syntax.annotations( source ))

        apply2(document, manager, syntax.branches3( source, options ))

        apply2(document, manager, FieldLayout().align2( source, options ))

        if (options.compactClosingBraces) apply2(document, manager, advanced.braces( source ))

        val conditional = BranchLayout()

        if (options.padNestedHeaders) apply2(document, manager, conditional.keywordHeaders( source ))

        if (options.simplifyDataBranches) {

            apply2(document, manager, conditional.branches( source, settings.getRightMargin( source.language ) ))

        }

        val margins = CallLayout()

        val rightMargin = settings.getRightMargin( source.language )

        if (options.keepSimpleReceivers) apply2(document, manager, margins.receivers2( source, rightMargin ))

        for (level in 0..layout.parameterDepth( source )) {

            if (options.packCallArguments) {

                apply2(document, manager, margins.arguments2( source, rightMargin, level, options ))

            }
        }

        if (options.formatExpressionBodies) apply2(document, manager, margins.expressionBodies( source, rightMargin ))

        if (options.braceMultilineBranches) apply2(document, manager, margins.conditionalBlocks( source ))

        if (options.formatControlHeaders) apply2(document, manager, margins.headers( source ))

        if (options.compactBareExits) apply2(document, manager, margins.bareReturns2( source ))

        if (options.formatFallbacks) apply2(document, manager, margins.fallbacks2( source ))

        val logical = LogicLayout()

        if (options.keepAssignmentReceiver) apply2(document, manager, logical.assignments( source ))

        if (options.spaceLambdas) apply2(document, manager, logical.lambdas( source ))

        if (options.formatConditions) apply2(document, manager, logical.conditions2( source, rightMargin ))

        if (options.formatLogicalAssignments) apply2(document, manager, logical.expressions( source, rightMargin ))

        apply2(document, manager, PreferenceLayout().boundaries( source, options ))

        val end = document.text.trimEnd()

        if (options.removeTrailingWhitespace && end.length != document.textLength) {

            document.deleteString( end.length, document.textLength )

            manager.commitDocument( document )

        }
    }

    companion object {

        const val INDENT_SIZE = 4

        private const val MAX_LAYOUT_PASSES = 4

    }
}