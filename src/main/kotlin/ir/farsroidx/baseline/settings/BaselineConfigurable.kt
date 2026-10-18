package ir.farsroidx.baseline.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import ir.farsroidx.baseline.config.BaselineSettings
import ir.farsroidx.baseline.config.FormatOption
import ir.farsroidx.baseline.config.FormatOptions
import ir.farsroidx.baseline.config.ProfileCodec
import ir.farsroidx.baseline.config.ProfileFiles
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.io.IOException
import java.nio.file.Files
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JComboBox
import javax.swing.JSplitPane
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Project Settings page for every supported Baseline formatting preference.
 *
 * Editing checkboxes changes only the form until [apply] is called. UI callbacks run on the
 * Swing event dispatch thread; disposing the form releases all component references.
 *
 * @param project project whose persisted preferences are displayed and edited
 */
class BaselineConfigurable(private val project: Project) : SearchableConfigurable, Configurable.NoScroll {

    private val controls = linkedMapOf<FormatOption, JCheckBox>()

    private var panel: JPanel? = null

    internal var preview: FormatPreview? = null
        private set

    /** Returns the stable identifier registered in `plugin.xml`. */
    override fun getId(): String {

        return "ir.farsroidx.baseline.settings"

    }

    /** Returns the name shown under Editor in project Settings. */
    override fun getDisplayName(): String {

        return "Baseline"

    }

    /** Creates or reuses the form and initializes it from the current project preferences. */
    override fun createComponent(): JComponent {

        return createSettingsPanel()

    }

    /** Reports whether the form differs from persisted preferences without mutating state. */
    override fun isModified(): Boolean {

        val saved = project.getService( BaselineSettings::class.java )
            .snapshot()

        return controls.any { (option, control) -> control.isSelected != option.property.get( saved ) }
    }

    /** Persists all displayed values; unopened forms leave existing preferences untouched. */
    override fun apply() {

        applySettings()

    }

    /** Discards unapplied form changes and reloads stored project preferences. */
    override fun reset() {

        display(
            project.getService( BaselineSettings::class.java )
                .snapshot()
        )

    }

    /** Releases the form so reopening Settings creates fresh components. */
    override fun disposeUIResources() {

        preview?.dispose()

        preview = null

        controls.clear()

        panel = null

    }

    /** Copies [options] into checkboxes without changing the persistent project state. */
    private fun display(options: FormatOptions) {

        for ( (option, control) in controls ) {

            control.isSelected = option.property.get( options )

        }

        preview?.schedule()
    }

    /**
     * Builds the categorized options form and its disposable Java/Kotlin live preview.
     *
     * Checkbox edits, defaults and imports remain pending until Apply or OK.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.1
     */
    fun createSettingsPanel(): JComponent {

        panel?.let { return it }

        val cards = CardLayout()

        val content = JPanel( cards )

        val groups = FormatOption.entries.groupBy { it.group }

        val categories = JComboBox( groups.keys.toTypedArray() )

        for ( (group, options) in groups ) {

            val section = JPanel()

            section.layout = BoxLayout(section, BoxLayout.Y_AXIS)

            section.border = BorderFactory.createEmptyBorder( SECTION_PADDING, SECTION_PADDING, SECTION_PADDING, SECTION_PADDING )

            for (option in options) {

                val checkbox = JCheckBox( option.label )

                checkbox.toolTipText = option.description

                controls[option] = checkbox

                checkbox.addActionListener { preview?.showOption( option ) }

                section.add( checkbox )

            }

            val page = JPanel( BorderLayout() )

            page.add( section, BorderLayout.NORTH )

            val scroll = JBScrollPane( page )

            scroll.border = BorderFactory.createEmptyBorder()

            scroll.isWheelScrollingEnabled = true

            scroll.verticalScrollBar.unitIncrement = SCROLL_INCREMENT

            scroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

            content.add( scroll, group )

        }

        val defaults = JButton( "Restore defaults" )

        defaults.addActionListener { display( FormatOptions() ) }

        val result = SettingsPanel()

        result.add(
            JLabel( "Choose a category. Change an option to see its effect." ), BorderLayout.NORTH
        )

        categories.addActionListener {

            cards.show( content, categories.selectedItem as String )

            preview?.showCategory( categories.selectedItem as String )

        }

        val preferences = JPanel( BorderLayout() )

        preferences.add( categories, BorderLayout.NORTH )

        preferences.add( content, BorderLayout.CENTER )

        preferences.minimumSize = Dimension( OPTIONS_MIN_WIDTH, MIN_HEIGHT )

        val livePreview = FormatPreview( project ) { formOptions() }

        preview = livePreview

        val split = JSplitPane( JSplitPane.HORIZONTAL_SPLIT, preferences, livePreview.component )

        split.resizeWeight = OPTIONS_WEIGHT

        split.dividerLocation = OPTIONS_WIDTH

        result.add( split, BorderLayout.CENTER )

        val actions = JPanel()

        val export = JButton( "Export settings..." )

        export.toolTipText = "Export all displayed values, including unapplied changes."

        export.addActionListener { exportFile() }

        val importButton = JButton( "Import settings..." )

        importButton.toolTipText = "Load a profile into this form; Apply or OK saves the imported values."

        importButton.addActionListener { importFile() }

        actions.add( defaults )

        actions.add( export )

        actions.add( importButton )

        result.add( actions, BorderLayout.SOUTH )

        panel = result

        reset()

        return result

    }

    /** Returns an XML profile of the currently displayed values, including unapplied changes. */
    internal fun exportProfile(): String {

        return ProfileCodec().encode( formOptions() )

    }

    /**
     * Validates a portable profile before updating pending controls and scheduling the preview.
     *
     * Imported values remain pending in the form until Apply or OK.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.1
     */
    internal fun importProfile(text: String) {

        display( ProfileCodec().decode( text ) )

    }

    /** Captures checkboxes as an independent bean without changing project persistence. */
    private fun formOptions(): FormatOptions {

        val options = project.getService( BaselineSettings::class.java )
            .snapshot()

        for ( (option, control) in controls ) {

            option.property.set( options, control.isSelected )

        }

        return options

    }

    /** Exports the form to a user-selected XML file, reporting I/O failures in the Settings dialog. */
    private fun exportFile() {

        val chooser = profileChooser( "Export Baseline settings" )

        chooser.selectedFile = java.io.File( DEFAULT_PROFILE_NAME )

        if (chooser.showSaveDialog( panel ) != JFileChooser.APPROVE_OPTION) {

            return
        }

        val selected = chooser.selectedFile.toPath()

        val path = if (
            selected.fileName
            .toString()
            .endsWith( XML_SUFFIX, ignoreCase = true )
        ) {

            selected

        } else selected.resolveSibling( selected.fileName.toString() + XML_SUFFIX )

        if (
            Files .exists( path ) &&
            Messages .showYesNoDialog( project, "Replace the existing settings file?\n" + path.toAbsolutePath(), "Export Baseline settings", Messages.getQuestionIcon() ) != Messages.YES
        ) {

            return
        }

        try {

            ProfileFiles().write( path, formOptions() )

        } catch (exception: IOException) {

            Messages.showErrorDialog( project, "Could not export settings: " + exception.message, "Baseline" )

        }
    }

    /**
     * Imports a user-selected XML profile and reports validation or I/O failures in the Settings dialog.
     *
     * Imported values remain pending in the form until Apply or OK.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.1
     */
    private fun importFile() {

        val chooser = profileChooser( "Import Baseline settings" )

        if (chooser.showOpenDialog( panel ) != JFileChooser.APPROVE_OPTION) {

            return
        }

        try {

            val options = ProfileFiles().read( chooser.selectedFile.toPath() )

            display( options )

        } catch (exception: IOException) {

            Messages.showErrorDialog( project, "Could not read settings: " + exception.message, "Baseline" )

        } catch (exception: IllegalArgumentException) {

            Messages.showErrorDialog( project, exception.message ?: "Invalid settings profile.", "Baseline" )

        }
    }

    /** Creates an XML-only file chooser rooted at the project directory when available. */
    private fun profileChooser(title: String): JFileChooser {

        val chooser = JFileChooser( project.basePath )

        chooser.dialogTitle = title

        chooser.fileSelectionMode = JFileChooser.FILES_ONLY

        chooser.isAcceptAllFileFilterUsed = false

        chooser.fileFilter = FileNameExtensionFilter("Baseline settings (*.xml)", "xml")

        return chooser

    }

    companion object {

        private const val DEFAULT_PROFILE_NAME = "baseline-settings.xml"
        private const val XML_SUFFIX           = ".xml"

        private const val SCROLL_INCREMENT = 20
        private const val SECTION_PADDING = 8
        private const val OPTIONS_MIN_WIDTH = 260
        private const val OPTIONS_WIDTH = 340
        private const val MIN_HEIGHT = 180
        private const val OPTIONS_WEIGHT = 0.4

    }

    /** Persists formatting preferences without changing Project view presentation. */
    fun applySettings() {

        val state = project.getService( BaselineSettings::class.java )
            .snapshot()

        for ( (option, control) in controls ) {

            option.property.set( state, control.isSelected )

        }

        project.getService( BaselineSettings::class.java )
            .loadState( state )

    }

}
