package ir.farsroidx.baseline.settings

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import ir.farsroidx.baseline.config.FormatOptions
import ir.farsroidx.baseline.config.FormatOption
import ir.farsroidx.baseline.integration.FormatProcessor
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.Timer
import javax.swing.JTextArea

/** Owns a read-only IDE editor and debounced formatting of disposable, nonphysical sample files. */
internal class FormatPreview(
    private val project: Project,
    private val options: () -> FormatOptions
) {

    private val document = EditorFactory.getInstance().createDocument( "" )

    private val editor = EditorFactory.getInstance().createViewer( document, project ) as EditorEx

    private val language = JComboBox( arrayOf( "Java", "Kotlin" ) )

    private val mode = JComboBox( arrayOf( "Formatted", "Original sample" ) )

    private val status = JTextArea( "Preview uses the editor right margin.", STATUS_ROWS, 0 )

    private val timer = Timer( UPDATE_DELAY ) { refresh() }

    private var disposed = false

    private var category = "General"

    private var optionChanged = false

    private var changedOption: FormatOption? = null

    private var categoryChanged = false

    private var highlight: RangeHighlighter? = null

    private val highlightTimer = Timer( HIGHLIGHT_DURATION ) { clearHighlight() }

    val component = JPanel( BorderLayout() )

    init {

        timer.isRepeats = false

        highlightTimer.isRepeats = false

        status.isEditable = false

        status.isFocusable = false

        status.isOpaque = false

        status.lineWrap = true

        status.wrapStyleWord = true

        status.font = JLabel().font

        status.foreground = JLabel().foreground

        editor.settings.isLineNumbersShown = true

        editor.settings.isRightMarginShown = true

        editor.component.minimumSize = Dimension( 0, 0 )

        component.minimumSize = Dimension( 0, 0 )

        component.preferredSize = Dimension( PREVIEW_WIDTH, PREVIEW_HEIGHT )

        val toolbar = JPanel( BorderLayout() )

        toolbar.add( JLabel( "Live preview  " ), BorderLayout.WEST )

        val selectors = JPanel()

        selectors.add( language )

        selectors.add( mode )

        toolbar.add( selectors, BorderLayout.EAST )

        component.add( toolbar, BorderLayout.NORTH )

        component.add( editor.component, BorderLayout.CENTER )

        component.add( status, BorderLayout.SOUTH )

        language.addActionListener { schedule() }

        mode.addActionListener { schedule() }

    }

    /** Coalesces rapid checkbox, reset and import events into one formatting operation. */
    fun schedule() {

        if (!disposed) {

            timer.restart()

        }

    }

    /** Keeps the example for the selected settings category visible after the next update. */
    fun showCategory(group: String) {

        category = group

        categoryChanged = true

        optionChanged = false

        changedOption = null

        schedule()

    }

    /** Schedules a preview update that explains [option] and reveals its example, even without visible changes. */
    fun showOption(option: FormatOption) {

        PreviewTarget.language( option )?.let { selectKotlin( it ) }

        category = option.group

        optionChanged = true

        changedOption = option

        categoryChanged = false

        schedule()

    }

    /** Formats a fresh sample using pending form values; project files and saved settings are untouched. */
    internal fun refresh() {

        timer.stop()

        if (disposed || project.isDisposed) {

            return

        }

        val extension = if (language.selectedIndex == 0) "java" else "kt"

        val fileType = FileTypeManager.getInstance().getFileTypeByExtension( extension )

        val sample = javaClass.getResource( "/preview/Sample.$extension" )?.readText()
            ?: error( "Missing formatter preview sample: $extension" )

        val captured = options()

        val focusedOption = changedOption

        val previous = document.text

        val previousCaret = editor.caretModel.offset

        val previousView = editor.scrollingModel.visibleArea

        try {

            CommandProcessor.getInstance().runUndoTransparentAction {

                ApplicationManager.getApplication().runWriteAction {

                    val file = PsiFileFactory.getInstance( project )
                        .createFileFromText( "BaselinePreview.$extension", fileType, sample, 0L, true )

                    file.putUserData( FormatProcessor.PREVIEW_OPTIONS, captured )

                    if (mode.selectedIndex == 0) {

                        val enabled = captured.enabled &&
                            (if (extension == "java") captured.javaEnabled else captured.kotlinEnabled)

                        if (enabled) {

                            FormatProcessor().processText( file, file.textRange, CodeStyle.getSettings( file ) )

                        } else {

                            CodeStyleManager.getInstance( project ).reformat( file )

                        }

                    }

                    clearHighlight()

                    document.setText( file.text )

                    editor.highlighter = EditorHighlighterFactory.getInstance()
                        .createEditorHighlighter( project, fileType )

                    editor.settings.setRightMargin( CodeStyle.getSettings( file ).getRightMargin( file.language ) )

                    val marker = when (category) {

                        "Constructors" -> if (extension == "java") "Sample(" else "constructor("

                        "Calls", "Annotations" -> "@GetMapping"

                        "Conditions" -> if (extension == "java") "public boolean valid" else "fun valid"

                        else -> "class Sample"

                    }

                    val changed = optionChanged && previous.isNotEmpty()

                    editor.scrollingModel.disableAnimation()

                    try {

                        if (changed) {

                            revealChange( previous )

                        } else if (categoryChanged || previous.isEmpty()) {

                            editor.caretModel.moveToOffset( document.text.indexOf( marker ).coerceAtLeast( 0 ) )

                            editor.scrollingModel.scrollToCaret( ScrollType.CENTER )

                        } else {

                            editor.caretModel.moveToOffset( previousCaret.coerceAtMost( document.textLength ) )

                            editor.scrollingModel.scrollHorizontally( previousView.x )

                            editor.scrollingModel.scrollVertically( previousView.y )

                        }

                    } finally {

                        editor.scrollingModel.enableAnimation()

                    }

                    optionChanged = false

                    changedOption = null

                    categoryChanged = false

                }

            }

            if (focusedOption != null) {

                val feedback = if (previous == document.text) {

                    "No visible change for this sample; related code is highlighted."

                } else "Changed code is highlighted. Apply saves your settings."

                status.text = focusedOption.label + "\n" + focusedOption.description + "\n" + feedback

                status.toolTipText = focusedOption.description + " " + feedback

                status.caretPosition = 0

            }

        } catch (exception: RuntimeException) {

            if (exception is com.intellij.openapi.progress.ProcessCanceledException) {

                throw exception

            }

            LOG.warn( "Could not update Baseline formatting preview.", exception )

            status.text = "Preview unavailable. See the IDE log for details."

        }

    }

    /** Exposes rendered text for lifecycle and pending-settings regression tests. */
    internal fun text(): String {

        return document.text

    }

    /** Reveals the selected option's example, falling back to the first changed line, and briefly highlights it. */
    private fun revealChange(previous: String) {

        val current = document.text

        val prefix = previous.commonPrefixWith( current ).length.coerceAtMost( current.length )

        val marker = changedOption?.let { PreviewTarget.marker( it, language.selectedIndex == 1 ) }

        val target = marker?.let {

            val offset = if (changedOption == FormatOption.REMOVE_TRAILING_WHITESPACE) {

                current.lastIndexOf( it )

            } else current.indexOf( it )

            offset.takeIf { position -> position >= 0 }

        } ?: prefix

        val line = document.getLineNumber( target )

        val start = document.getLineStartOffset( line )

        val suffix = previous.substring( prefix ).commonSuffixWith( current.substring( prefix ) ).length

        val changedEnd = if (previous == current) target else (current.length - suffix).coerceAtLeast( prefix )

        val finalLine = document.getLineNumber( changedEnd )
            .coerceAtLeast( line )
            .coerceAtMost( line + HIGHLIGHT_LINES - 1 )

        val end = document.getLineEndOffset( finalLine )

        editor.caretModel.moveToOffset( target )

        editor.scrollingModel.scrollToCaret( ScrollType.CENTER )

        highlight = editor.markupModel.addRangeHighlighter(
            start, end, HighlighterLayer.SELECTION - 1,
            editor.colorsScheme.getAttributes( EditorColors.SEARCH_RESULT_ATTRIBUTES ),
            HighlighterTargetArea.EXACT_RANGE
        )

        highlightTimer.restart()

    }

    /** Removes transient feedback before replacing the document or releasing the editor. */
    private fun clearHighlight() {

        highlightTimer.stop()

        highlight?.let { editor.markupModel.removeHighlighter( it ) }

        highlight = null

    }

    /** Returns the transiently emphasized text for preview-navigation regression checks. */
    internal fun highlightedText(): String {

        val range = highlight ?: return ""

        return document.text.substring( range.startOffset, range.endOffset )

    }

    /** Returns the current code position to verify that unchanged refreshes preserve navigation. */
    internal fun focusOffset(): Int {

        return editor.caretModel.offset

    }

    /** Returns the selected preference explanation and whether the current sample visibly changed. */
    internal fun explanation(): String {

        return status.text

    }

    /** Selects the sample language without persisting any project preferences. */
    internal fun selectKotlin(kotlin: Boolean) {

        language.selectedIndex = if (kotlin) 1 else 0

    }

    /** Cancels pending work and releases the IDE editor exactly once. */
    fun dispose() {

        if (disposed) {

            return

        }

        disposed = true

        timer.stop()

        clearHighlight()

        EditorFactory.getInstance().releaseEditor( editor )

    }

    companion object {

        private const val UPDATE_DELAY = 250

        private const val STATUS_ROWS = 5

        private const val HIGHLIGHT_DURATION = 1400
        private const val HIGHLIGHT_LINES = 6

        private const val PREVIEW_WIDTH = 480
        private const val PREVIEW_HEIGHT = 360

        private val LOG = Logger.getInstance( FormatPreview::class.java )

    }

}