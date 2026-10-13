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
 * are skipped. Temporary native code style is restored even if formatting fails.
 * [format] uses a project-settings snapshot for the configurable layout pipeline.
 */
internal class Formatter {

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
     * Applies the current formatter with assignment and margin-aware conditional chains.
     *
     * Mutates and commits the document under the caller's write action; existing guards remain in effect.
     *
     * @param source committed Java or Kotlin file
     * @param settings native language code style
     * @param options enabled formatting preferences
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun format(source: PsiFile, settings: CodeStyleSettings, options: FormatOptions) {

        repeat( MAX_LAYOUT_PASSES ) {

            val previous = source.text

            formatPass(source, settings, options)

            if (source.text == previous) {

                return
            }

        }
    }

    /**
     * Applies the current formatter with assignment and margin-aware conditional chains.
     *
     * Mutates and commits the document under the caller's write action; existing guards remain in effect.
     *
     * @param source committed Java or Kotlin file
     * @param settings native language code style
     * @param options enabled formatting preferences
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun formatPass(source: PsiFile, settings: CodeStyleSettings, options: FormatOptions) {

        if (PsiTreeUtil.findChildOfType( source, PsiErrorElement::class.java ) != null) {

            return
        }

        val disabledRegion = PsiTreeUtil
            .findChildrenOfType( source, PsiComment::class.java )
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

        apply(document, manager, FieldLayout().reorder( source, options ))

        apply(document, manager, structure.assignments( source, options ))

        apply(document, manager, structure.localVariables( source, options ))

        if (options.normalizeModifiers) apply(document, manager, structure.modifiers( source ))

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

        apply(document, manager, layout.spacing( source, options ))

        if (options.formatChains) {

            apply(document, manager, ChainLayout().links( source, settings.getRightMargin( source.language ) ))

        }

        for (level in layout.parameterDepth( source ) downTo 0) {

            if (options.collapseShortLists && options.wrapCalls && options.wrapDeclarations) {

                apply(document, manager, layout.wrapping( source, Int.MAX_VALUE, level, options ))

            }
        }

        for (level in 0..layout.parameterDepth( source )) {

            if (options.formatChains) {

                apply(document, manager, ChainLayout().links( source, settings.getRightMargin( source.language ) ))

            }

            apply(
                document, manager, layout.wrapping(
                    source, settings.getRightMargin( source.language ), level, options
                )
            )

        }

        if (options.formatConditions) apply(document, manager, layout.controlClosings( source ))

        val advanced = BlockLayout()

        if (options.formatChains) {

            apply(document, manager, ChainLayout().links( source, settings.getRightMargin( source.language ) ))

        }

        if (options.spaceStatements) apply(document, manager, advanced.statementSpacing( source ))

        if (options.compactClosingBraces) apply(document, manager, advanced.braces( source ))

        val syntax = SyntaxLayout()

        if (options.formatConditions) {

            apply(document, manager, syntax.conditions( source, settings.getRightMargin( source.language ) ))

        }

        if (options.formatAnnotations) apply(document, manager, syntax.annotations( source ))

        apply(document, manager, syntax.branches( source, options ))

        apply(document, manager, FieldLayout().align( source, options ))

        if (options.compactClosingBraces) apply(document, manager, advanced.braces( source ))

        val conditional = BranchLayout()

        if (options.padNestedHeaders) apply(document, manager, conditional.keywordHeaders( source ))

        if (options.simplifyDataBranches) {

            apply(document, manager, conditional.branches( source, settings.getRightMargin( source.language ) ))

        }

        val margins = CallLayout()

        val rightMargin = settings.getRightMargin( source.language )

        if (options.keepSimpleReceivers) apply(document, manager, margins.receivers( source, rightMargin ))

        for (level in 0..layout.parameterDepth( source )) {

            if (options.packCallArguments) {

                apply(document, manager, margins.arguments( source, rightMargin, level, options ))

            }
        }

        if (options.formatExpressionBodies) apply(document, manager, margins.expressionBodies( source, rightMargin ))

        if (options.braceMultilineBranches) apply(document, manager, margins.conditionalBlocks( source ))

        if (options.formatControlHeaders) apply(document, manager, margins.headers( source ))

        if (options.compactBareExits) apply(document, manager, margins.bareReturns( source ))

        if (options.formatFallbacks) apply(document, manager, margins.fallbacks( source ))

        val logical = LogicLayout()

        if (options.keepAssignmentReceiver) apply(document, manager, logical.assignments( source ))

        if (options.spaceLambdas) apply(document, manager, logical.lambdas( source ))

        if (options.formatConditions) apply(document, manager, logical.conditions( source, rightMargin ))

        if (options.formatLogicalAssignments) apply(document, manager, logical.expressions( source, rightMargin ))

        apply(document, manager, PreferenceLayout().boundaries( source, options ))

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