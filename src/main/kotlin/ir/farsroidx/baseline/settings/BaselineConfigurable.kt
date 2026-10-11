package ir.farsroidx.baseline.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import ir.farsroidx.baseline.config.BaselineSettings
import ir.farsroidx.baseline.config.FormatOption
import ir.farsroidx.baseline.config.FormatOptions
import ir.farsroidx.baseline.config.ProfileCodec
import ir.farsroidx.baseline.config.ProfileFiles
import java.awt.BorderLayout
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
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Project Settings page for every supported Baseline formatting preference.
 *
 * Editing checkboxes changes only the form until [apply] is called. UI callbacks run on the
 * Swing event dispatch thread; disposing the form releases all component references.
 *
 * @param project project whose persisted preferences are displayed and edited
 */
class BaselineConfigurable(private val project: Project) : SearchableConfigurable {

    private val controls = linkedMapOf<FormatOption, JCheckBox>()

    private var panel: JPanel? = null

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

        return createComponent2()

    }

    /** Reports whether the form differs from persisted preferences without mutating state. */
    override fun isModified(): Boolean {

        val saved = project.getService( BaselineSettings::class.java )
            .snapshot()

        return controls.any { (option, control) -> control.isSelected != option.property.get( saved ) }
    }

    /** Persists all displayed values; unopened forms leave existing preferences untouched. */
    override fun apply() {

        val state = project.getService( BaselineSettings::class.java )
            .snapshot()

        for ( (option, control) in controls ) {

            option.property.set( state, control.isSelected )

        }

        project.getService( BaselineSettings::class.java )
            .loadState( state )

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

        controls.clear()

        panel = null

    }

    /** Copies [options] into checkboxes without changing the persistent project state. */
    private fun display(options: FormatOptions) {

        for ( (option, control) in controls ) {

            control.isSelected = option.property.get( options )

        }
    }

    /**
     * Retains the original settings form without profile import/export buttons.
     *
     * @return initialized settings form
     * @deprecated Use [createComponent2] for portable settings support.
     * @see createComponent2
     * @since 1.0.0
     */
    @Deprecated( "Use createComponent2 instead." )
    fun createComponent1(): JComponent {

        panel?.let { return it }

        val content = JPanel()

        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)

        for ( (group, options) in FormatOption.entries.groupBy { it.group } ) {

            val section = JPanel()

            section.layout = BoxLayout(section, BoxLayout.Y_AXIS)

            section.border = BorderFactory.createTitledBorder( group )

            for (option in options) {

                val checkbox = JCheckBox( option.label )

                checkbox.toolTipText = option.description

                controls[option] = checkbox

                section.add( checkbox )

            }

            content.add( section )

        }

        val defaults = JButton( "Restore defaults" )

        defaults.addActionListener { display( FormatOptions() ) }

        val result = JPanel( BorderLayout() )

        result.add(
            JLabel( "Whole-file Java/Kotlin formatting. Disabled rules use IDE formatting." ), BorderLayout.NORTH
        )

        result.add( JScrollPane( content ), BorderLayout.CENTER )

        result.add( defaults, BorderLayout.SOUTH )

        panel = result

        reset()

        return result

    }

    /**
     * Creates the current settings form with defaults and portable profile actions.
     *
     * Import updates the form only after successful validation. Export includes unapplied
     * checkbox values. Both actions leave project persistence unchanged until Apply/OK.
     *
     * @return initialized settings form, reused until disposal
     * @see createComponent1
     * @since 1.0.0
     */
    fun createComponent2(): JComponent {

        panel?.let { return it }

        val content = JPanel()

        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)

        for ( (group, options) in FormatOption.entries.groupBy { it.group } ) {

            val section = JPanel()

            section.layout = BoxLayout(section, BoxLayout.Y_AXIS)

            section.border = BorderFactory.createTitledBorder( group )

            for (option in options) {

                val checkbox = JCheckBox( option.label )

                checkbox.toolTipText = option.description

                controls[option] = checkbox

                section.add( checkbox )

            }

            content.add( section )

        }

        val defaults = JButton( "Restore defaults" )

        defaults.addActionListener { display( FormatOptions() ) }

        val result = JPanel( BorderLayout() )

        result.add(
            JLabel( "Whole-file Java/Kotlin formatting. Disabled rules use IDE formatting." ), BorderLayout.NORTH
        )

        result.add( JScrollPane( content ), BorderLayout.CENTER )

        val actions = JPanel()

        val export = JButton( "Export settings..." )

        export.toolTipText = "Export all displayed values, including unapplied changes."

        export.toolTipText = "Export all displayed values, including unapplied changes."

        export.addActionListener { exportFile() }

        val importButton = JButton( "Import settings..." )

        importButton.toolTipText = "Load a profile into this form; Apply or OK saves the imported values."

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
     * Validates [text] completely, then replaces the displayed preferences without saving them.
     *
     * @throws IllegalArgumentException if the profile is malformed or unsupported; the form is unchanged
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

    /** Imports a selected profile into the form only after the entire file is validated. */
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

    }
}