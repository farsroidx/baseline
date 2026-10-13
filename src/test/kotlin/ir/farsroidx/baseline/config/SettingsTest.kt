package ir.farsroidx.baseline.config

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializer
import ir.farsroidx.baseline.settings.BaselineConfigurable
import java.awt.Container
import javax.swing.JCheckBox

/** Verifies persistence, settings lifecycle and independently configurable formatting rules. */
class SettingsTest : BasePlatformTestCase() {

    /** Restores defaults before platform fixture teardown can cache the project for another test. */
    override fun tearDown() {

        try {

            project.getService( BaselineSettings::class.java ).loadState( FormatOptions() )

        } finally {

            super.tearDown()

        }

    }

    /** Every Boolean preference must be discoverable, enabled by default and independently copied. */
    fun testCatalogAndSnapshots() {

        val options = FormatOptions()

        val fields = FormatOptions::class.java.declaredFields.filter { it.type == Boolean::class.javaPrimitiveType }

        assertEquals( fields.size, FormatOption.entries.size )

        for (option in FormatOption.entries) {

            val original = option.property.get( options )

            val copy = options.snapshot()

            option.property.set( copy, !original )

            assertEquals( !original, option.property.get( copy ) )

            assertEquals( original, option.property.get( options ) )

        }

    }

    /** Saving and reloading all disabled preferences must preserve every value. */
    fun testSerializationRoundTrip() {

        val options = FormatOptions()

        FormatOption.entries.forEach { it.property.set( options, false ) }

        val restored = XmlSerializer.deserialize( XmlSerializer.serialize( options ), FormatOptions::class.java )

        assertTrue( FormatOption.entries.all { !it.property.get( restored ) } )

        val service = project.getService( BaselineSettings::class.java )

        service.loadState( restored )

        restored.enabled = true

        assertFalse( service.snapshot().enabled )

    }

    /** Opening, applying, resetting and reopening the form must not lose project preferences. */
    fun testSettingsFormLifecycle() {

        val page = BaselineConfigurable( project )

        val panel = page.createComponent()

        val checkboxes = checkboxes( panel )

        assertEquals( FormatOption.entries.size, checkboxes.size )

        assertFalse( page.isModified )

        checkboxes.first().isSelected = false

        assertTrue( page.isModified )

        page.reset()

        assertTrue( checkboxes.first().isSelected )

        checkboxes.first().isSelected = false

        page.apply()

        assertFalse( page.isModified )

        assertFalse( project.getService( BaselineSettings::class.java ).snapshot().enabled )

        page.disposeUIResources()

        assertFalse( checkboxes( page.createComponent() ).first().isSelected )

        page.disposeUIResources()

    }

    /** Master and language switches must leave the native formatter result unchanged. */
    fun testMasterAndLanguageSwitches() {

        for (name in listOf( "Sample.java", "Sample.kt" )) {

            val source = if (name.endsWith( ".java" )) "class Sample { int x; int lengthyName; }"
                else "class Sample { var x: Int = 0; var lengthyName: Int = 0 }"

            val options = FormatOptions()

            options.enabled = false

            val native = format( name, source, options )

            options.enabled = true

            options.javaEnabled = false

            options.kotlinEnabled = false

            assertEquals( native, format( name, source, options ) )

        }

    }

    /** Disabling field grouping and sorting must preserve declaration order in both languages. */
    fun testFieldOrderPreservedWithGroupingAndSortingDisabled() {

        val options = FormatOptions()

        options.sortFields = false

        options.groupFieldsByType = false

        options.groupFieldsByVisibility = false

        options.separateStaticFields = false

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { private int x; public String lengthyName; public static int middle; }",
            "Sample.kt" to "class Sample { private var x: Int = 0; public var lengthyName: String = \"\"; public var middle: Int = 0 }"
        )) {

            val result = format( name, source, options )

            assertTrue( result, result.indexOf( " x" ) < result.indexOf( " lengthyName" ) )

            assertTrue( result, result.indexOf( " lengthyName" ) < result.indexOf( " middle" ) )

            assertEquals( result, format( name, result, options ) )

        }

    }

    /** Sorting may remain enabled when type grouping is disabled. */
    fun testSortWithoutTypeGrouping() {

        val options = FormatOptions()

        options.groupFieldsByType = false

        val result = format( "Sample.java", "class Sample { public int x; public String lengthyName; }", options )

        assertTrue( result, result.indexOf( "lengthyName" ) < result.indexOf( " x" ) )

    }

    /** Grouping must not reorder equal-type declarations when name sorting is disabled. */
    fun testGroupingWithoutSorting() {

        val options = FormatOptions()

        options.sortFields = false

        val result = format( "Sample.java", "class Sample { private int x; public int a; public int lengthyName; }", options )

        assertTrue( result, result.indexOf( "public int a" ) < result.indexOf( "public int lengthyName" ) )

        assertTrue( result, result.indexOf( "public int lengthyName" ) < result.indexOf( "private int x" ) )

    }

    /** Beginning and ending method blank lines can be disabled independently in Java and Kotlin. */
    fun testIndependentMethodBoundaries() {

        for ((name, source, last) in listOf(
            Triple( "Sample.java", "class Sample { int value() { return 1; } }", "return 1;" ),
            Triple( "Sample.kt", "class Sample { fun value(): Int { return 1 } }", "return 1" )
        )) {

            val options = FormatOptions()

            options.blankAfterOpeningBrace = false

            var result = format( name, source, options )

            assertTrue( result, result.contains( "{\n        return 1" ) )

            assertTrue( result, result.contains( last + "\n\n    }" ) )

            options.blankAfterOpeningBrace = true

            options.blankBeforeClosingBrace = false

            result = format( name, source, options )

            assertTrue( result, result.contains( "{\n\n        return 1" ) )

            assertTrue( result, result.contains( last + "\n    }" ) )

            assertEquals( result, format( name, result, options ) )

        }

    }

    /** Disabling constructor sorting preserves assignment order without touching parameter order. */
    fun testConstructorSortSwitch() {

        val options = FormatOptions()

        options.sortConstructorAssignments = false

        val result = format(
            "Sample.java",
            "class Sample { int longName; int x; Sample(int longName, int x) { this.longName = longName; this.x = x; } }",
            options
        )

        assertTrue( result, result.contains( "Sample(int longName, int x)" ) )

        assertTrue( result, result.indexOf( "this.longName" ) < result.indexOf( "this.x" ) )

    }

    /** Field alignment and group spacing may be disabled while sorting remains active. */
    fun testAlignmentAndGroupSpacingSwitches() {

        val options = FormatOptions()

        options.alignFieldEquals = false

        options.spaceFieldGroups = false

        val result = format( "Sample.java", "class Sample { int x = 1; int lengthyName = 2; String label = \"x\"; }", options )

        assertTrue( result, result.contains( "int x = 1;" ) )

        assertFalse( result, result.contains( "= 2;\n\n" ) )

    }

    /** Every individual switch must produce parseable, stable output on representative sources. */
    fun testEachDisabledOptionRemainsStable() {

        val sources = listOf(
            "Sample.java" to "class Sample { public static int lengthyName = 1; public int x = 2; int test(int a, int b) { int value = a + b; if (value > 0 && b > 0) { return value; } return 0; } }",
            "Sample.kt" to "class Sample { var lengthyName: Int = 1; var x: Int = 2; fun test(a: Int, b: Int): Int { val value = a + b; if (value > 0 && b > 0) { return value }; return 0 } }"
        )

        for (option in FormatOption.entries) {

            val options = FormatOptions()

            option.property.set( options, false )

            for ((name, source) in sources) {

                val result = format( name, source, options )

                assertEquals( option.name + " " + name, result, format( name, result, options ) )

            }

        }

    }

    /** Disabling argument padding must not be undone by the later argument-packing pass. */
    fun testCallPaddingSwitchInBothLanguages() {

        val options = FormatOptions()

        options.padSingleArguments = false

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { void test(String value) { System.out.println(value); } }",
            "Sample.kt" to "class Sample { fun test(value: String) { println(value) } }"
        )) {

            val result = format( name, source, options )

            assertTrue( result, result.contains( "println(value)" ) )

            assertFalse( result, result.contains( "println( value )" ) )

        }

    }

    /** Turning collapse off must not turn an already short inline list into a multiline list. */
    fun testNoCollapseKeepsShortInlineLists() {

        val options = FormatOptions()

        options.collapseShortLists = false

        val result = format( "Sample.java", "class Sample { void test(int a, int b) { System.out.println(a); } }", options )

        assertTrue( result, result.contains( "test(int a, int b)" ) )

    }

    /** Case separator alignment and case blank lines are independent preferences. */
    fun testCaseSpacingWithoutAlignment() {

        val options = FormatOptions()

        options.alignCases = false

        val result = format( "Sample.kt", "fun value(code: Int): Int = when (code) { 1 -> 1; 2222 -> 2; else -> 0 }", options )

        assertTrue( result, result.contains( "1 -> 1" ) )

        assertTrue( result, Regex( "1 -> 1;?\n\n" ).containsMatchIn( result ) )

    }

    /** Disabling custom call wrapping must leave native wrapping preferences in control. */
    fun testCallWrappingSwitch() {

        val source = "class Sample { void test() { target(firstLongArgumentName, secondLongArgumentName); } }"

        val options = FormatOptions()

        options.wrapCalls = false

        options.packCallArguments = false

        options.formatChains = false

        val result = format( "Sample.java", source, options )

        assertTrue( result, result.contains( "target(firstLongArgumentName, secondLongArgumentName)" ) )

    }

    /** Disabling lambda spacing must preserve explicit parameters and remove only body blank lines. */
    fun testLambdaSpacingPreservesExplicitParameters() {

        val options = FormatOptions()

        options.spaceLambdas = false

        val result = format(
            "Sample.kt",
            "fun test() { listOf(1).forEach { value ->\n\n println(value)\n\n } }",
            options
        )

        assertTrue( result, result.contains( "{ value ->\n" ) )

        assertFalse( result, result.contains( "value ->\n\n" ) )

        assertEquals( result, format( "Sample.kt", result, options ) )

    }

    /** Export includes unapplied edits; importing them does not persist until Apply is called. */
    fun testPortableProfilesRespectSettingsLifecycle() {

        val page = BaselineConfigurable( project )

        val controls = checkboxes( page.createComponent() )

        controls.first().isSelected = false

        val exported = page.exportProfile()

        assertFalse( ProfileCodec().decode( exported ).enabled )

        assertTrue( project.getService( BaselineSettings::class.java ).snapshot().enabled )

        page.reset()

        page.importProfile( exported )

        assertTrue( page.isModified )

        assertFalse( controls.first().isSelected )

        assertTrue( project.getService( BaselineSettings::class.java ).snapshot().enabled )

        page.apply()

        assertFalse( page.isModified )

        assertFalse( project.getService( BaselineSettings::class.java ).snapshot().enabled )

        page.disposeUIResources()

    }

    /** A partially valid profile must leave every checkbox and saved preference untouched. */
    fun testInvalidImportLeavesFormUnchanged() {

        val page = BaselineConfigurable( project )

        page.createComponent()

        val before = page.exportProfile()

        try {

            page.importProfile(
                "<baseline-settings formatVersion=\"1\"><option name=\"enabled\" value=\"false\"/>" +
                    "<option name=\"sortFields\" value=\"invalid\"/></baseline-settings>"
            )

            fail( "Invalid import unexpectedly succeeded." )

        } catch (exception: IllegalArgumentException) {

            assertEquals( before, page.exportProfile() )

            assertFalse( page.isModified )

            assertTrue( project.getService( BaselineSettings::class.java ).snapshot().enabled )

        } finally {

            page.disposeUIResources()

        }

    }

    /** Recursively collects checkboxes without depending on the Swing panel arrangement. */
    private fun checkboxes(container: Container): List<JCheckBox> {

        return container.components.flatMap {

            when (it) {

                is JCheckBox -> listOf( it )

                is Container -> checkboxes( it )

                else -> emptyList()

            }

        }

    }

    /** Runs the registered post-formatter with [options] and asserts valid PSI after formatting. */
    private fun format(name: String, source: String, options: FormatOptions): String {

        project.getService( BaselineSettings::class.java ).loadState( options )

        val file = myFixture.configureByText( name, source )

        val manager = CodeStyleSettingsManager.getInstance( project )

        val settings = manager.cloneSettings( CodeStyle.getSettings( file ) )

        settings.setRightMargin( file.language, RIGHT_MARGIN )

        manager.doWithTemporarySettings( settings, Runnable {

            WriteCommandAction.runWriteCommandAction( project ) {

                CodeStyleManager.getInstance( project ).reformat( file )

            }

        } )

        assertNull( PsiTreeUtil.findChildOfType( file, PsiErrorElement::class.java ) )

        return file.text

    }

    companion object {

        private const val RIGHT_MARGIN = 120

    }
}
