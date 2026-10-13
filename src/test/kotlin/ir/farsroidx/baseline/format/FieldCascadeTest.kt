package ir.farsroidx.baseline.format

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import ir.farsroidx.baseline.config.BaselineSettings
import ir.farsroidx.baseline.config.FormatOptions

class FieldCascadeTest : BasePlatformTestCase() {

    override fun tearDown() {

        try {

            project.getService( BaselineSettings::class.java ).loadState( FormatOptions() )

        } finally {

            super.tearDown()

        }

    }

    fun testTypeAndNameDetermineGroupWidths() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { public String slightlyLongerName; public ExtraordinarilyLongNamedType x; }",
            "Sample.kt" to "class Sample { public var slightlyLongerName: String = \"\"; public lateinit var x: ExtraordinarilyLongNamedType }"
        )) {

            val result = format( name, source )

            assertTrue( result, result.indexOf( " x" ) < result.indexOf( "slightlyLongerName" ) )

            assertEquals( result, format( name, result ) )

        }

    }

    fun testTypeAndNameWhenTypeGroupingIsDisabled() {

        val options = FormatOptions()

        options.groupFieldsByType = false

        val source = "class Sample { public String slightlyLongerName; public ExtraordinarilyLongNamedType x; }"

        val result = format( "Sample.java", source, options )

        assertTrue( result, result.indexOf( " x" ) < result.indexOf( "slightlyLongerName" ) )

    }

    fun testInitializerAndModifiersDoNotDetermineWidth() {

        val options = FormatOptions()

        options.groupFieldsByType = false

        options.separateStaticFields = false

        val source = "class Sample { public static final String x = \"a very long initializer that must not count\"; public int slightlyLongerName = 1; }"

        val result = format( "Sample.java", source, options )

        assertTrue( result, result.indexOf( "slightlyLongerName" ) < result.indexOf( " x" ) )

    }

    fun testSnakePrefixGroupsAcrossTypesAndAlignsInitializers() {

        val options = FormatOptions()

        options.groupFieldsByPrefix = true

        val source = "class Sample { public static int CACHE_SIZE = 1; public static String name = \"n\"; public static String CACHE_LABEL = \"c\"; public static int id = 2; }"

        val result = format( "Sample.java", source, options )

        val rows = result.lines().filter { it.contains( "CACHE_" ) }

        assertEquals( 2, rows.size )

        assertTrue( rows.first(), rows.first().contains( "CACHE_LABEL" ) )

        assertEquals( rows.first().indexOf( '=' ), rows.last().indexOf( '=' ) )

        val between = result.substring( result.indexOf( "CACHE_LABEL" ), result.indexOf( "CACHE_SIZE" ) )

        assertFalse( between, between.contains( "\n\n" ) )

        assertFalse( between, between.contains( "name" ) )

        assertEquals( result, format( "Sample.java", result, options ) )

    }

    fun testCamelPrefixGroupsAcrossKotlinTypes() {

        val options = FormatOptions()

        options.groupFieldsByPrefix = true

        val source = "class Sample { var cacheCount: Int = 1; var name: String = \"n\"; var cacheDescription: String = \"c\"; var id: Int = 2 }"

        val result = format( "Sample.kt", source, options )

        val between = result.substring( result.indexOf( "cacheDescription" ), result.indexOf( "cacheCount" ) )

        assertFalse( between, between.contains( "\n\n" ) )

        assertFalse( between, between.contains( "name" ) )

        assertEquals( result, format( "Sample.kt", result, options ) )

    }

    fun testPlainLowercaseNamesDoNotCreatePrefixGroups() {

        val options = FormatOptions()

        options.groupFieldsByPrefix = true

        val result = format( "Sample.java", "class Sample { public String alpha; public int alphabet; }", options )

        assertTrue( result, result.contains( "alphabet;\n\n" ) || result.contains( "alpha;\n\n" ) )

    }

    fun testPrefixGroupingDefaultsToDisabled() {

        assertFalse( FormatOptions().groupFieldsByPrefix )

        val source = "class Sample { public String CACHE_LABEL; public int CACHE_SIZE; }"

        val result = format( "Sample.java", source )

        assertTrue( result, result.contains( "CACHE_LABEL;\n\n" ) )

    }

    fun testPrefixGroupingDoesNotCrossBusinessInitializer() {

        val options = FormatOptions()

        options.groupFieldsByPrefix = true

        val source = "class Sample { int CACHE_SIZE = compute(); String CACHE_LABEL = \"c\"; int CACHE_LIMIT = 2; int compute() { return 1; } }"

        val result = format( "Sample.java", source, options )

        assertTrue( result, result.indexOf( "CACHE_SIZE" ) < result.indexOf( "CACHE_LABEL" ) )

        assertEquals( result, format( "Sample.java", result, options ) )

    }

    fun testSafeConstructorAssignmentsUseReverseDeclarationWidths() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { int id; String x; Sample(String x, int id) { this.x = x; this.id = id; } }",
            "Sample.kt" to "class Sample { var id: Int; var x: String; constructor(x: String, id: Int) { this.x = x; this.id = id } }"
        )) {

            val result = format( name, source )

            assertTrue( result, result.indexOf( "this.id" ) < result.indexOf( "this.x" ) )

            assertEquals( result, format( name, result ) )

            if (name.endsWith( ".java" )) {

                assertTrue( result, result.contains( "Sample(String x, int id)" ) )

            } else assertTrue( result, result.contains( "constructor(x: String, id: Int)" ) )

        }

    }

    private fun format(name: String, source: String, options: FormatOptions = FormatOptions()): String {

        project.getService( BaselineSettings::class.java ).loadState( options )

        val file = myFixture.configureByText( name, source )

        WriteCommandAction.runWriteCommandAction( project ) {

            CodeStyleManager.getInstance( project ).reformat( file )

        }

        assertNull( PsiTreeUtil.findChildOfType( file, PsiErrorElement::class.java ) )

        return file.text

    }
}
