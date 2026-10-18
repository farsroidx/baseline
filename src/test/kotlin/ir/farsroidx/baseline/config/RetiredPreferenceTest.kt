package ir.farsroidx.baseline.config

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import ir.farsroidx.baseline.settings.BaselineConfigurable
import java.nio.file.Files

/** Verifies removal of the view preference without losing existing formatting profiles. */
class RetiredPreferenceTest : BasePlatformTestCase() {

    /** Retired values are ignored while current field and formatting preferences are preserved. */
    fun testOldProfilesKeepFormattingPreferences() {

        for (value in listOf( "true", "false" )) {

            val profile = profile( "<option name=\"showGeneratedSources\" value=\"$value\"/>" )

            val options = ProfileCodec().decode( profile )

            assertFalse( options.sortFields )

            assertTrue( options.groupFieldsByPrefix )

            val exported = ProfileCodec().encode( options )

            assertFalse( exported.contains( "showGeneratedSources" ) )

            assertTrue( exported.contains( "pluginVersion=\"1.0.2\"" ) )

            assertEquals( 57, FormatOption.entries.size )

        }

    }

    /** Retired entries retain Boolean and duplicate validation, alongside unknown-key rejection. */
    fun testRetiredOptionDoesNotWeakenValidation() {

        for (entry in listOf(
            "<option name=\"showGeneratedSources\" value=\"invalid\"/>",
            "<option name=\"showGeneratedSources\" value=\"true\"/><option name=\"showGeneratedSources\" value=\"false\"/>",
            "<option name=\"unknown\" value=\"true\"/>"
        )) {

            try {

                ProfileCodec().decode( profile( entry ) )

                fail( "Invalid preference must be rejected." )

            } catch (expected: IllegalArgumentException) {

                assertNotNull( expected.message )

            }

        }

    }

    /** The bounded file importer uses retired-option compatibility. */
    fun testOldProfileFileCanBeRead() {

        val path = Files.createTempFile( "baseline-retired-option-", ".xml" )

        try {

            Files.writeString( path, profile( "<option name=\"showGeneratedSources\" value=\"true\"/>" ) )

            assertTrue( ProfileFiles().read( path ).groupFieldsByPrefix )

        } finally {

            Files.deleteIfExists( path )

        }

    }

    /** Import stays pending until Apply, then persists formatting values only. */
    fun testSettingsImportKeepsApplyLifecycle() {

        val page = BaselineConfigurable( project )

        try {

            page.createComponent()

            page.importProfile( profile( "<option name=\"showGeneratedSources\" value=\"true\"/>" ) )

            assertFalse( project.getService( BaselineSettings::class.java ).state.groupFieldsByPrefix )

            page.apply()

            assertTrue( project.getService( BaselineSettings::class.java ).state.groupFieldsByPrefix )

            assertFalse( project.getService( BaselineSettings::class.java ).state.sortFields )

            assertFalse( page.exportProfile().contains( "showGeneratedSources" ) )

        } finally {

            page.disposeUIResources()

            project.getService( BaselineSettings::class.java ).loadState( FormatOptions() )

        }

    }

    /** Returns a legacy profile including two current preferences and the supplied extra entries. */
    private fun profile(entries: String): String {

        return "<baseline-settings formatVersion=\"1\" pluginVersion=\"1.0.0\">" +
            "<option name=\"sortFields\" value=\"false\"/>" +
            "<option name=\"groupFieldsByPrefix\" value=\"true\"/>" +
            entries + "</baseline-settings>"

    }
}
