package ir.farsroidx.baseline.config

import junit.framework.TestCase
import java.io.IOException
import java.nio.file.Files

/** Tests portable settings validation, complete round trips and non-destructive file replacement. */
class ProfileCodecTest : TestCase() {

    /** Every Boolean must survive export/import with both true and false values. */
    fun testAllPreferencesRoundTrip() {

        val options = FormatOptions()

        FormatOption.entries.forEachIndexed { index, option ->

            option.property.set( options, index % ALTERNATING_VALUES == 0 )

        }

        val codec = ProfileCodec()

        val restored = codec.decode( codec.encode( options ) )

        for (option in FormatOption.entries) {

            assertEquals( option.name, option.property.get( options ), option.property.get( restored ) )

        }

        assertEquals( codec.encode( options ), codec.encode( restored ) )

    }

    /** Missing entries retain enabled defaults, permitting older version-one profiles. */
    fun testMissingPreferencesUseDefaults() {

        val options = ProfileCodec().decode( profile( "<option name=\"enabled\" value=\"false\"/>" ) )

        assertFalse( options.enabled )

        assertTrue( options.sortFields )

        assertTrue( options.wrapCalls )

    }

    /** Unsupported schema versions and unrelated project XML must be rejected. */
    fun testRejectsUnsupportedProfiles() {

        assertInvalid( "<project/>" )

        assertInvalid( "<baseline-settings formatVersion=\"2\"/>" )

        assertInvalid( "<baseline-settings/>" )

        assertInvalid( "<baseline-settings" )

    }

    /** Unknown or duplicate names cannot silently introduce inconsistent team preferences. */
    fun testRejectsUnknownAndDuplicatePreferences() {

        assertInvalid( profile( "<option name=\"unknown\" value=\"true\"/>" ) )

        val entry = "<option name=\"enabled\" value=\"false\"/>"

        assertInvalid( profile( entry + entry ) )

    }

    /** Invalid values, nested elements and unexpected content must fail complete validation. */
    fun testRejectsInvalidValuesAndContent() {

        for (value in listOf( "TRUE", "1", "", "yes" )) {

            assertInvalid( profile( "<option name=\"enabled\" value=\"" + value + "\"/>" ) )

        }

        assertInvalid( profile( "<option name=\"enabled\" value=\"true\"><nested/></option>" ) )

        assertInvalid( profile( "unexpected text" ) )

    }

    /** External entity declarations are prohibited without accessing external resources. */
    fun testRejectsDocumentTypes() {

        assertInvalid(
            "<!DOCTYPE baseline-settings [<!ENTITY external SYSTEM \"file:///unavailable\">]>" +
                "<baseline-settings formatVersion=\"1\">&external;</baseline-settings>"
        )

    }

    /** Both text and file imports enforce the bounded profile size. */
    fun testRejectsOversizedProfiles() {

        val oversized = " ".repeat( ProfileCodec.MAX_PROFILE_BYTES + 1 )

        assertInvalid( oversized )

        val file = Files.createTempFile( "baseline-oversized-", ".xml" )

        try {

            Files.writeString( file, oversized )

            try {

                ProfileFiles().read( file )

                fail( "Oversized file was accepted." )

            } catch (exception: IllegalArgumentException) {

                assertTrue( exception.message!!.contains( "too large" ) )

            }

        } finally {

            Files.deleteIfExists( file )

        }

    }

    /** Export replaces a selected profile completely and leaves no temporary sibling files. */
    fun testFileRoundTripAndReplacement() {

        val directory = Files.createTempDirectory( "baseline-profile-" )

        val path = directory.resolve( "settings.xml" )

        try {

            Files.writeString( path, "previous file" )

            val options = FormatOptions()

            options.groupFieldsByType = false

            ProfileFiles().write( path, options )

            assertFalse( ProfileFiles().read( path ).groupFieldsByType )

            Files.list( directory ).use { files ->

                assertEquals( 1L, files.count() )

            }

        } finally {

            Files.deleteIfExists( path )

            Files.deleteIfExists( directory )

        }

    }

    /** A failed destination replacement preserves existing content and cleans the temporary file. */
    fun testFailedReplacementKeepsExistingContent() {

        val directory = Files.createTempDirectory( "baseline-profile-failure-" )

        val existing = directory.resolve( "existing.txt" )

        Files.writeString( existing, "preserved" )

        try {

            try {

                ProfileFiles().write( directory, FormatOptions() )

                fail( "Replacing a nonempty directory unexpectedly succeeded." )

            } catch (exception: IOException) {

                assertEquals( "preserved", Files.readString( existing ) )

            }

        } finally {

            Files.deleteIfExists( existing )

            Files.deleteIfExists( directory )

        }

    }

    /** Wraps [entries] in a portable version-one profile for focused validation cases. */
    private fun profile(entries: String): String {

        return "<baseline-settings formatVersion=\"1\">" + entries + "</baseline-settings>"

    }

    /** Requires [text] to fail validation with a useful message rather than partial success. */
    private fun assertInvalid(text: String) {

        try {

            ProfileCodec().decode( text )

            fail( "Invalid settings profile was accepted." )

        } catch (exception: IllegalArgumentException) {

            assertNotNull( exception.message )

        }

    }

    companion object {

        private const val ALTERNATING_VALUES = 2

    }
}
