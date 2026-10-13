package ir.farsroidx.baseline.format

import ir.farsroidx.baseline.integration.FormatProcessor
import ir.farsroidx.baseline.layout.BranchLayout

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Files
import java.nio.file.Path

/**
 * Exercises the registered IntelliJ post-formatter with Java/Kotlin PSI fixtures.
 *
 * Assertions cover whitespace contracts, parameter order, semantic barriers, parsing and
 * repeat-format stability. All assertions exercise the current formatter pipeline.
 */
class FormatterTest : BasePlatformTestCase() {

    /**
     * Verifies java field groups and cascades.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testJavaFieldGroupsAndCascades() {

        val source = "class Sample { private String hidden; public String x; protected int count; public String lengthyName; public int number; }"

        val result = format("Sample.java", source)

        assertTrue(result.indexOf("public String lengthyName") < result.indexOf("public int number"))
        assertTrue(result.indexOf("public String lengthyName") < result.indexOf("public String x"))
        assertTrue(result.indexOf("public String x") < result.indexOf("protected int count"))
        assertTrue(result.indexOf("protected int count") < result.indexOf("private String hidden"))
        assertTrue(result.contains("public String lengthyName;\n    public String x;"))
        assertTrue(result.contains("public String x;\n\n    public int number;"))
        assertFalse(result.endsWith("\n"))
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Verifies kotlin field groups and cascades.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testKotlinFieldGroupsAndCascades() {

        val source = "class Sample { private var hidden: String = \"\"; public var x: String = \"\"; protected var count: Int = 0; public var lengthyName: String = \"\"; public var number: Int = 0 }"

        val result = format("Sample.kt", source)

        assertTrue(result.indexOf("public var lengthyName") < result.indexOf("public var number"))
        assertTrue(result.indexOf("public var lengthyName") < result.indexOf("public var x"))
        assertTrue(result.indexOf("public var x") < result.indexOf("protected var count"))
        assertTrue(result.indexOf("protected var count") < result.indexOf("private var hidden"))
        assertFalse(result.endsWith("\n"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies java constructor order and parameter order.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testJavaConstructorOrderAndParameterOrder() {

        val source = "class Sample { private String lengthyName; private String x; Sample(String lengthyName, String x) { super(); this.lengthyName = lengthyName; this.x = x; } }"

        val result = format("Sample.java", source)

        assertTrue(result.contains("Sample(String lengthyName, String x)"))
        assertTrue(result.indexOf("this.x = x;") < result.indexOf("this.lengthyName = lengthyName;"))
        assertTrue(result.contains("super();\n\n"))
        assertTrue(result.contains("this.x = x;\n        this.lengthyName = lengthyName;"))
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Verifies kotlin constructor order and parameter order.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testKotlinConstructorOrderAndParameterOrder() {

        val source = "class Sample { private var lengthyName: String; private var x: String; constructor(lengthyName: String, x: String) { this.lengthyName = lengthyName; this.x = x } }"

        val result = format("Sample.kt", source)

        assertTrue(result.contains("constructor(lengthyName: String, x: String)"))
        assertTrue(result.indexOf("this.x = x") < result.indexOf("this.lengthyName = lengthyName"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies java right margin wraps every parameter.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testJavaRightMarginWrapsEveryParameter() {

        val source = "class Sample { void execute(String firstDescription, String secondDescription, String thirdDescription) {} }"

        val result = format("Sample.java", source, RIGHT_MARGIN)

        assertTrue(result.contains("execute(\n        String firstDescription,\n        String secondDescription,\n        String thirdDescription\n    ) {"))
        assertEquals(result, format("Sample.java", result, RIGHT_MARGIN))

    }

    /**
     * Verifies kotlin right margin wraps every parameter.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testKotlinRightMarginWrapsEveryParameter() {

        val source = "class Sample { fun execute(firstDescription: String, secondDescription: String, thirdDescription: String) {} }"

        val result = format("Sample.kt", source, RIGHT_MARGIN)

        assertTrue(result.contains("execute(\n        firstDescription: String,\n        secondDescription: String,\n        thirdDescription: String\n    ) {"))
        assertEquals(result, format("Sample.kt", result, RIGHT_MARGIN))

    }

    /**
     * Verifies dependent initializers and comments stay in order.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testDependentInitializersAndCommentsStayInOrder() {

        val source = "class Sample { private int x = compute(); public int lengthyName = x; /* keep with the next field */ public int y = 3; int compute() { return 1; } }"

        val result = format("Sample.java", source)

        assertTrue(result.indexOf("x = compute()") < result.indexOf("lengthyName = x"))
        assertTrue(result.indexOf("/* keep with the next field */") < result.indexOf("public int y"))
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Verifies dependent assignments stay in order.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testDependentAssignmentsStayInOrder() {

        val source = "class Sample { String lengthyName; String x; Sample(String value) { this.lengthyName = value; this.x = this.lengthyName; } }"

        val result = format("Sample.java", source)

        assertTrue(result.indexOf("this.lengthyName = value") < result.indexOf("this.x = this.lengthyName"))

    }

    /**
     * Verifies kotlin block spacing and related statements.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testKotlinBlockSpacingAndRelatedStatements() {

        val source = "class Sample { fun execute(): Int { val value = 1; return value } }"

        val result = format("Sample.kt", source)

        assertTrue(result.contains("class Sample {\n\n"))
        assertTrue(result.contains("execute(): Int {\n\n"))
        assertTrue(result.contains("return value\n\n    }\n}"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies reformat text uses baseline rules.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testReformatTextUsesBaselineRules() {

        val file = myFixture.configureByText("Sample.java", "class Sample { private String hidden; public String lengthyName; public String x; }")

        WriteCommandAction.runWriteCommandAction(project) {

            CodeStyleManager.getInstance(project).reformatText(file, 0, file.textLength)

        }

        assertTrue(file.text.indexOf("public String lengthyName") < file.text.indexOf("private String hidden"))
        assertTrue(file.text.contains("public String lengthyName;\n    public String x;"))

    }

    /**
     * Verifies kotlin reformat text uses baseline rules.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testKotlinReformatTextUsesBaselineRules() {

        val file = myFixture.configureByText(
            "Sample.kt",
            "class Sample { private var hidden: String = \"\"; public var lengthyName: String = \"\"; public var x: String = \"\" }"
        )

        WriteCommandAction.runWriteCommandAction(project) {

            CodeStyleManager.getInstance(project).reformatText(
                file,
                0,
                file.textLength
            )

        }

        assertTrue(file.text.indexOf("public var lengthyName") < file.text.indexOf("private var hidden"))
        assertTrue(file.text.contains("class Sample {\n\n"))

    }

    /**
     * Verifies java constructor local variables are grouped.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testJavaConstructorLocalVariablesAreGrouped() {

        val source = "class Sample { Sample() { super(); int lengthyName = 2; String description = \"text\"; int x = 1; System.out.println(x + lengthyName + description); } }"

        val result = format("Sample.java", source)

        assertTrue(result.contains("int x = 1;\n        int lengthyName = 2;\n\n        String description"))
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Verifies kotlin constructor local variables are grouped.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testKotlinConstructorLocalVariablesAreGrouped() {

        val source = "class Sample { constructor() { val lengthyName: Int = 2; val description: String = \"text\"; val x: Int = 1; println(x + lengthyName); println(description) } }"

        val result = format("Sample.kt", source)

        assertTrue(result.indexOf("val x") < result.indexOf("val lengthyName"))
        assertTrue(result.indexOf("val lengthyName") < result.indexOf("val description"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies java multi variable declaration remains valid.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testJavaMultiVariableDeclarationRemainsValid() {

        val source = "class Sample { public int x = 1, lengthyName = 2; private String hidden = \"text\"; }"

        val result = format("Sample.java", source)

        val fields = PsiTreeUtil.findChildrenOfType(myFixture.file, PsiField::class.java)

        assertEquals(setOf("x", "lengthyName", "hidden"), fields.map { it.name }.toSet())
        assertEquals(3, fields.size)
        assertEquals("1", fields.first { it.name == "x" }.initializer?.text)
        assertEquals("2", fields.first { it.name == "lengthyName" }.initializer?.text)
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Verifies kotlin templates and delegated properties are preserved.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testKotlinTemplatesAndDelegatedPropertiesArePreserved() {

        val source = "class Sample { private val first = build(); public val second = \"value: ${'$'}first\"; val third by lazy { first }; fun build() = \"text\" }"

        val result = format("Sample.kt", source)

        assertTrue(result.indexOf("first = build()") < result.indexOf("second ="))
        assertTrue(result.contains("\"value: ${'$'}first\""))
        assertTrue(result.contains("third by lazy"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies kotlin primary constructor parameter order is preserved.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testKotlinPrimaryConstructorParameterOrderIsPreserved() {

        val source = "class Sample(val lengthyName: String, val x: Int, val description: String) { private val hidden = 1; public val visible = 2 }"

        val result = format("Sample.kt", source)

        assertTrue(result.indexOf("val lengthyName") < result.indexOf("val x"))
        assertTrue(result.indexOf("val x") < result.indexOf("val description"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies repeated constructor assignment is not reordered.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testRepeatedConstructorAssignmentIsNotReordered() {

        val source = "class Sample { String x; String lengthyName; Sample(String first, String second) { this.lengthyName = first; this.x = second; this.lengthyName = second; } }"

        val result = format("Sample.java", source)

        assertTrue(result.indexOf("this.lengthyName = first") < result.indexOf("this.x = second"))
        assertTrue(result.indexOf("this.x = second") < result.indexOf("this.lengthyName = second"))

    }

    /**
     * Verifies nested calls have stable indentation.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testNestedCallsHaveStableIndentation() {

        val javaSource = "class Sample { void execute() { consume(consume(\"first long description\", \"second long description\", \"third long description\"), \"fourth long description\", \"fifth long description\"); } }"
        val kotlinSource = "class Sample { fun execute() { consume(consume(\"first long description\", \"second long description\", \"third long description\"), \"fourth long description\", \"fifth long description\") } }"

        for ((name, source) in listOf("Sample.java" to javaSource, "Sample.kt" to kotlinSource)) {

            val result = format(
                name,
                source,
                RIGHT_MARGIN
            )

            assertEquals(2, Regex("consume\\(\\n").findAll(result).count())
            assertEquals(result, format(name, result, RIGHT_MARGIN))

        }

    }

    /**
     * Verifies partial formatting preserves field order.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testPartialFormattingPreservesFieldOrder() {

        val text = "class Sample { private String hidden; public String visible; }"
        val file = myFixture.configureByText(
            "Sample.java",
            text
        )
        val start = text.indexOf("private")
        val end = text.indexOf(';') + 1

        WriteCommandAction.runWriteCommandAction(project) {

            CodeStyleManager.getInstance(project).reformatText(
                file,
                start,
                end
            )

        }

        assertTrue(file.text.indexOf("private String hidden") < file.text.indexOf("public String visible"))

    }

    /**
     * Verifies formatter off region preserves field order.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testFormatterOffRegionPreservesFieldOrder() {

        val source = "class Sample { /* @formatter:off */ private String hidden; public String visible; /* @formatter:on */ }"

        val result = format(
            "Sample.java",
            source
        )

        assertTrue(result.indexOf("private String hidden") < result.indexOf("public String visible"))

    }

    /**
     * Verifies project samples produce stable output.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testProjectSamplesProduceStableOutput() {

        val samples = listOf(
            "src/main/resources/JavaSample.txt",
            "src/main/resources/KotlinSample.txt"
        )
        val output = Path.of("build/formatted-samples")

        Files.createDirectories(output)

        for (sample in samples) {

            val path = Path.of(sample)
            val name = path.fileName.toString().removeSuffix( ".txt" ) +
                if (path.fileName.toString().startsWith( "Java" )) ".java" else ".kt"

            val source = Files.readString(path)
            val originalTokens = tokens(
                myFixture.configureByText(
                    name,
                    source
                )
            )

            val result = format(
                name,
                source
            )

            assertFalse(result.endsWith("\n"))
            assertEquals(originalTokens, tokens(myFixture.file))
            assertEquals(result, format(name, result))

            Files.writeString(
                output.resolve(name),
                result
            )

        }

    }

    /**
     * Verifies kotlin lambda statements share indentation2.
     *
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testKotlinLambdaStatementsShareIndentation() {

        val source = "class Sample { fun execute() { consume { value -> val first = value; val second = first; println(second) } } }"

        val result = format("Sample.kt", source)

        assertTrue(result.contains("            val first = value"))
        assertTrue(result.contains("\n            val second = first"))
        assertTrue(result.contains("\n            println( second )"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies kotlin short assignments lose old line breaks.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testKotlinShortAssignmentsLoseOldLineBreaks() {

        val source = "class Sample { var size: Int = 0; constructor(size: Int) { this.size =\n size } }"

        val result = format("Sample.kt", source)

        assertTrue(result.contains("this.size = size"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies short parameter lists lose old line breaks.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testShortParameterListsLoseOldLineBreaks() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { void execute(\n int first,\n int second\n) {} }",
            "Sample.kt" to "class Sample { fun execute(\n first: Int,\n second: Int\n) {} }"
        )) {

            val result = format(name, source)

            assertFalse(result.contains("execute(\n"))
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies local functions have separating blank lines.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testLocalFunctionsHaveSeparatingBlankLines() {

        val source = "fun execute() { val value = 1; fun flush() { println(value) }; flush() }"

        val result = format("Sample.kt", source)

        assertTrue(result.contains("val value = 1;\n\n    fun flush()"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies formatter sources have stable output2.
     *
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testFormatterSourcesHaveStableOutput() {

        val directory = Path.of("src/main/kotlin/ir/farsroidx/baseline")
        val output = Path.of("build/formatted-formatter")

        Files.createDirectories(output)

        Files.walk(directory).use { paths ->

            paths.filter { it.toString().endsWith(".kt") }.forEach { path ->

                val name = path.fileName.toString()
                val source = Files.readString(path)
                val originalTokens = tokens(myFixture.configureByText(name, source))
                val result = format(name, source)

                assertEquals(name, originalTokens, tokens(myFixture.file))
                assertEquals(name, result, format(name, result))

                val destination = output.resolve(directory.relativize(path))

                Files.createDirectories(destination.parent)

                Files.writeString(destination, result)

            }

        }

    }

    /**
     * Verifies consecutive closing braces have no blank lines2.
     *
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testConsecutiveClosingBracesHaveNoBlankLines() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { void execute() { if (true) { consume(); consume(); } } }",
            "Sample.kt" to "class Sample { fun execute() { if (true) { consume(); consume() } } }"
        )) {

            val result = format(name, source)

            assertTrue(result.contains("        }\n    }\n}"))
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies empty blocks have exactly one line break.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testEmptyBlocksHaveExactlyOneLineBreak() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { void execute() {} }",
            "Sample.kt" to "class Sample { fun execute() {} }"
        )) {

            val result = format(name, source)

            assertTrue(result.contains("execute() {\n    }\n}"))
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies single call arguments have spaces.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testSingleCallArgumentsHaveSpaces() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { void execute(int value) { consume(value); consume(); } }",
            "Sample.kt" to "class Sample { fun execute(value: Int) { consume(value); consume() } }"
        )) {

            val result = format(name, source)

            assertTrue(result.contains("consume( value )"))
            assertTrue(result.contains("consume()"))
            assertFalse(result.contains("execute( "))
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies long single call argument wraps completely.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testLongSingleCallArgumentWrapsCompletely() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { void execute() { consume(extraordinarilyLongDescriptionOfTheRequestedOperation); } }",
            "Sample.kt" to "class Sample { fun execute() { consume(extraordinarilyLongDescriptionOfTheRequestedOperation) } }"
        )) {

            val result = format(name, source, RIGHT_MARGIN)

            assertTrue(result.contains("consume(\n            extraordinarilyLongDescriptionOfTheRequestedOperation\n        )"))
            assertEquals(result, format(name, result, RIGHT_MARGIN))

        }

    }

    /**
     * Verifies chained call arguments wrap every link.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testChainedCallArgumentsWrapEveryLink() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { void execute() { consume(variable.test().test().test()); } }",
            "Sample.kt" to "class Sample { fun execute() { consume(variable.test().test().test()) } }"
        )) {

            val result = format(name, source)

            assertTrue(result.contains("consume(\n            variable.test()\n                .test()\n                .test()\n        )"))
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies kotlin safe call chains stay safe.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testKotlinSafeCallChainsStaySafe() {

        val source = "fun execute() { consume(variable?.test()?.test()?.test()) }"

        val result = format("Sample.kt", source)

        assertTrue(result.contains("variable?.test()\n            ?.test()\n            ?.test()"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies local declarations and return have blank lines.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testLocalDeclarationsAndReturnHaveBlankLines() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { int calculate(int first, int second) { int sum = first + second; return sum; } }",
            "Sample.kt" to "class Sample { fun calculate(first: Int, second: Int): Int { val sum = first + second; return sum } }"
        )) {

            val result = format(name, source)

            assertTrue(result.contains("first + second;\n\n        return sum"))
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies brace rules preserve strings and comments.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testBraceRulesPreserveStringsAndComments() {

        val source = "class Sample { String value = \"}\\n\\n}\"; void execute() { if (true) {} /* keep this comment */ } }"

        val result = format("Sample.java", source)

        assertTrue(result.contains("\"}\\n\\n}\""))
        assertTrue(result.contains("/* keep this comment */"))
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Verifies java conditional expression uses aligned branches.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testJavaConditionalExpressionUsesAlignedBranches() {

        val source = "class Sample { boolean execute(int value) { boolean result = value == 1 ? true : false; return result; } }"

        val result = format("Sample.java", source)

        val question = result.lines().first { it.trimStart().startsWith("? true") }

        val colon = result.lines().first { it.trimStart().startsWith(": false") }

        assertEquals(question.indexOf('?'), colon.indexOf(':'))
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Verifies static fields are separate from instance fields.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testStaticFieldsAreSeparateFromInstanceFields() {

        val source = "class Sample { public int instanceValue = 1; private static int sharedValue = 2; public static int globalValue = 3; private int localValue = 4; }"

        val result = format("Sample.java", source)

        assertTrue(result.indexOf("public static int globalValue") < result.indexOf("private static int sharedValue"))
        assertTrue(result.indexOf("private static int sharedValue") < result.indexOf("public int instanceValue"))
        assertTrue(result.contains("sharedValue = 2;\n\n    public int instanceValue"))
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Verifies group initializers align to longest declaration.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testGroupInitializersAlignToLongestDeclaration() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { public int lengthyName = 1; public int shortName = 2; public int x = 3; }",
            "Sample.kt" to "class Sample { public var lengthyName: Int = 1; public var shortName: Int = 2; public var x: Int = 3 }"
        )) {

            val result = format(name, source)

            val declarations = result.lines().filter { it.contains("public ") && it.contains(" = ") }

            assertEquals(3, declarations.size)
            assertEquals(1, declarations.map { it.indexOf('=') }.distinct().size)
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies declaration without initializer is not padded.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testDeclarationWithoutInitializerIsNotPadded() {

        val source = "class Sample { public int longestFieldName; public int x = 1; }"

        val result = format("Sample.java", source)

        assertTrue(result.contains("public int longestFieldName;"))
        assertTrue(result.contains("public int x                = 1;"))
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Verifies property assignments have separating blank lines.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testPropertyAssignmentsHaveSeparatingBlankLines() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { void execute() { indent.INDENT_SIZE = INDENT_SIZE; indent.CONTINUATION_INDENT_SIZE = INDENT_SIZE; } }",
            "Sample.kt" to "class Sample { fun execute() { indent.INDENT_SIZE = INDENT_SIZE; indent.CONTINUATION_INDENT_SIZE = INDENT_SIZE } }"
        )) {

            val result = format(name, source)

            assertTrue(result.contains("indent.INDENT_SIZE = INDENT_SIZE;\n\n        indent.CONTINUATION_INDENT_SIZE"))
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies short apply call collapses nested old line breaks.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testShortApplyCallCollapsesNestedOldLineBreaks() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { void execute() { apply(\n document, manager, structure.fields(\n source\n)\n); } }",
            "Sample.kt" to "class Sample { fun execute() { apply(\n document, manager, structure.fields(\n source\n)\n) } }"
        )) {

            val result = format(name, source)

            assertTrue(result, result.contains("apply(document, manager, structure.fields( source ))"))
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies long conditions start below opening parenthesis.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testLongConditionsStartBelowOpeningParenthesis() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { void execute() { if (active.get() || !supports(source) || rangeToReformat.startOffset != 0 || rangeToReformat.endOffset < source.textLength) { consume(); } } }",
            "Sample.kt" to "class Sample { fun execute() { if (active.get() || !supports(source) || rangeToReformat.startOffset != 0 || rangeToReformat.endOffset < source.textLength) { consume() } } }"
        )) {

            val result = format(name, source, RIGHT_MARGIN)

            assertTrue(result, result.contains("if (\n            active.get()"))
            assertTrue(result, result.contains("\n        ) {"))
            assertEquals(result, format(name, result, RIGHT_MARGIN))

        }

    }

    /**
     * Verifies lambda condition starts below opening parenthesis.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testLambdaConditionStartsBelowOpeningParenthesis() {

        val source = "fun execute() { if (description == null || previous != null && !between(previous, statement).all { it.isWhitespace() || it == ';' }) { flush() } }"

        val result = format("Sample.kt", source, RIGHT_MARGIN)

        assertTrue(result, result.contains("if (\n        description"))
        assertTrue(result, result.contains("\n    ) {"))
        assertTrue(result.contains("it == ';'"))
        assertEquals(result, format("Sample.kt", result, RIGHT_MARGIN))

    }

    /**
     * Verifies when branches align their arrows.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testWhenBranchesAlignTheirArrows() {

        val source = "fun select(value: Int) = when { value == 1 -> \"first\"; value == 123 -> \"second\"; else -> \"other\" }"

        val result = format("Sample.kt", source)

        val branches = result.lines().filter { it.contains("->") }

        assertEquals(3, branches.size)
        assertEquals(1, branches.map { it.indexOf("->") }.distinct().size)
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies switch labels align their colons.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testSwitchLabelsAlignTheirColons() {

        val source = "class Sample { int select(int value) { switch (value) { case 1: return 1; case 123: return 2; default: return 0; } } }"

        val result = format("Sample.java", source)

        val labels = result.lines().filter { it.trimStart().startsWith("case ") || it.trimStart().startsWith("default") }

        assertEquals(3, labels.size)
        assertEquals(1, labels.map { it.indexOf(':') }.distinct().size)
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Verifies annotated parameters force multiline declarations.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testAnnotatedParametersForceMultilineDeclarations() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { @Deprecated void execute(@Deprecated String first, String second) {} }",
            "Sample.kt" to "class Sample { @Suppress(\"unused\") fun execute(@Suppress(\"unused\") first: String, second: String) {} }"
        )) {

            val result = format(name, source)

            assertTrue(result, result.contains("execute(\n"))
            assertTrue(result, result.contains("\n        second" ) || result.contains("\n        String second"))
            assertTrue(result, result.contains("@Deprecated\n        String first") ||
                result.contains("@Suppress( \"unused\" )\n        first"))
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies annotated fields keep their annotations above them.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testAnnotatedFieldsKeepTheirAnnotationsAboveThem() {

        val source = "class Sample { @Deprecated public int lengthyName = 1; public int shortName = 2; }"

        val result = format("Sample.java", source)

        assertTrue(result, result.contains("@Deprecated\n    public int lengthyName"))
        assertTrue(result, result.contains("= 1;\n\n    public int shortName"))
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Returns method/constructor names followed by their parameters in declaration order.
     *
     * @param file Java/Kotlin fixture containing the sample class
     * @return signatures used to detect accidental declaration or parameter rearrangement
     */
    private fun declarationOrder(
        file: PsiFile
    ): List<String> {

        if (file is com.intellij.psi.PsiJavaFile) {

            return file.classes.first().methods.map {

                it.name + ":" + it.parameterList.parameters.joinToString(",") { parameter -> parameter.name }

            }

        }

        val owner = (file as org.jetbrains.kotlin.psi.KtFile).declarations.first() as org.jetbrains.kotlin.psi.KtClass

        return owner.declarations.mapNotNull {

            when (it) {

                is org.jetbrains.kotlin.psi.KtNamedFunction -> it.name + ":" + it.valueParameters.joinToString(",") { parameter -> parameter.name.orEmpty() }
                is org.jetbrains.kotlin.psi.KtSecondaryConstructor -> "constructor:" + it.valueParameters.joinToString(",") { parameter -> parameter.name.orEmpty() }
                else -> null

            }

        }

    }

    /**
     * Verifies property receiver chains wrap before their first call.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testPropertyReceiverChainsWrapBeforeTheirFirstCall() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { void execute() { element.containingFile.text.substring( start, end ).lineSequence().last().trimEnd(); } }",
            "Sample.kt" to "class Sample { fun execute() { element.containingFile.text.substring( start, end ).lineSequence().last().trimEnd() } }"
        )) {

            val result = format(name, source)

            assertTrue(result, result.contains("element.containingFile.text\n            .substring( start, end )\n            .lineSequence()\n            .last()\n            .trimEnd()"))
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies long chain arguments pack without crossing margin.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testLongChainArgumentsPackWithoutCrossingMargin() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { void execute() { element.containingFile.text.substring(firstOffset, secondOffset, thirdOffset, fourthOffset, fifthOffset).lineSequence().last().trimEnd(); } }",
            "Sample.kt" to "class Sample { fun execute() { element.containingFile.text.substring(firstOffset, secondOffset, thirdOffset, fourthOffset, fifthOffset).lineSequence().last().trimEnd() } }"
        )) {

            val result = format(name, source, RIGHT_MARGIN)

            assertTrue(result, result.contains(".substring(\n"))
            assertTrue(result, result.contains("firstOffset, secondOffset"))
            assertTrue(result, result.contains("\n            )\n            .lineSequence()"))
            assertTrue(result, result.lines().all { it.length <= RIGHT_MARGIN })
            assertEquals(result, format(name, result, RIGHT_MARGIN))

        }

    }

    /**
     * Verifies nested chain parentheses stay at their own level.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testNestedChainParenthesesStayAtTheirOwnLevel() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { void execute() { consume(element.containingFile.text.substring(firstOffset, secondOffset, thirdOffset, fourthOffset, fifthOffset).lineSequence().last()); } }",
            "Sample.kt" to "class Sample { fun execute() { consume(element.containingFile.text.substring(firstOffset, secondOffset, thirdOffset, fourthOffset, fifthOffset).lineSequence().last()) } }"
        )) {

            val result = format(name, source, RIGHT_MARGIN)

            assertTrue(result, result.contains("consume(\n"))
            assertTrue(result, result.contains("\n                )\n                .lineSequence()"))
            assertTrue(result, result.contains("\n        )"))
            assertTrue(result, result.lines().all { it.length <= RIGHT_MARGIN })
            assertEquals(result, format(name, result, RIGHT_MARGIN))

        }

    }

    /**
     * Verifies very long chain arguments use separate lines.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testVeryLongChainArgumentsUseSeparateLines() {

        val source = "fun execute() { element.containingFile.text.substring(element.textRange.startOffset, element.textRange.endOffset, element.textRange.startOffset).last().trimEnd() }"

        val result = format("Sample.kt", source, NARROW_MARGIN)

        assertTrue(result, result.contains(".substring(\n            element.textRange.startOffset,\n            element.textRange.endOffset,\n            element.textRange.startOffset\n        )\n        .last()"))
        assertTrue(result, result.lines().all { it.length <= NARROW_MARGIN })
        assertEquals(result, format("Sample.kt", result, NARROW_MARGIN))

    }

    /**
     * Verifies destructuring headers have outer padding.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testDestructuringHeadersHaveOuterPadding() {

        val source = "fun execute() { for ((element, equal, length) in candidates) { consume(element) }; for (element in elements) { consume(element) }; while ((ready)) { consume() }; when ((value)) { else -> consume() } }"

        val result = format("Sample.kt", source)

        assertTrue(result, result.contains("for ( (element, equal, length) in candidates )"))
        assertTrue(result, result.contains("for (element in elements)"))
        assertTrue(result, result.contains("while ( (ready) )"))
        assertTrue(result, result.contains("when ( (value) )"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies single value conditional branches stay inline.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testSingleValueConditionalBranchesStayInline() {

        val source = "fun execute() { val result = if (prefix.isBlank()) { null } else { Triple(element, equal, prefix.length) } }"

        val result = format("Sample.kt", source)

        assertTrue(result, result.contains("if (prefix.isBlank()) null else Triple(element, equal, prefix.length)"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies multiple then statements keep block with simple else value.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testMultipleThenStatementsKeepBlockWithSimpleElseValue() {

        val source = "fun execute() { val result = if (prefix.isBlank()) { null; null } else { Triple(element, equal, prefix.length) } }"

        val result = format("Sample.kt", source)

        assertTrue(result, result.contains("if (prefix.isBlank()) {\n\n"))
        assertTrue(result, result.contains("null;\n\n        null"))
        assertTrue(result, result.contains("} else Triple(element, equal, prefix.length)"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies multiple conditional branches keep both blocks.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testMultipleConditionalBranchesKeepBothBlocks() {

        val source = "fun execute() { val result = if (prefix.isBlank()) { null; null } else { Triple(element, equal, prefix.length); Triple(element, equal, prefix.length) } }"

        val result = format("Sample.kt", source)

        assertTrue(result, result.contains("} else {\n\n"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies conditional branch comments and declarations remain scoped.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testConditionalBranchCommentsAndDeclarationsRemainScoped() {

        val source = "fun execute() { if (ready) { val value = build(); consume(value) } else { /* preserve this comment */ consume() } }"

        val result = format("Sample.kt", source)

        assertTrue(result, result.contains("/* preserve this comment */"))
        assertTrue(result, result.contains("} else {"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies qualified single calls use receiver rows in both languages.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testQualifiedSingleCallsUseReceiverRowsInBothLanguages() {

        for ((name, source) in listOf(
            "Sample.kt" to "fun execute() { val argumentList = PsiTreeUtil.getParentOfType(expression, PsiExpressionList::class.java, KtValueArgumentList::class.java) ?: continueWork() }",
            "Sample.java" to "class Sample { void execute() { Object argumentList = PsiTreeUtil.getParentOfType(expression, PsiExpressionList.class, KtValueArgumentList.class); } }"
        )) {

            val result = format(name, source, RIGHT_MARGIN)

            assertTrue(result, result.contains(if (name.endsWith("kt")) "PsiTreeUtil\n        .getParentOfType(" else "PsiTreeUtil\n            .getParentOfType("))
            assertEquals(result, format(name, result, RIGHT_MARGIN))

        }

    }

    /**
     * Verifies multiline loop header has separate parentheses and body indentation.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testMultilineLoopHeaderHasSeparateParenthesesAndBodyIndentation() {

        val result = format("Sample.kt", "fun execute() { for ((left, right) in body.statements.toList().zipWithNext()) { consume(left); consume(right) } }", RIGHT_MARGIN)

        assertTrue(result, result.contains("for (\n        (left, right) in body.statements"))
        assertTrue(result, result.contains("\n            .toList()\n            .zipWithNext()\n    ) {\n\n        consume"))
        assertEquals(result, format("Sample.kt", result, RIGHT_MARGIN))

    }

    /**
     * Verifies bare return has one line before closing brace in both languages.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testBareReturnHasOneLineBeforeClosingBraceInBothLanguages() {

        for ((name, source) in listOf(
            "Sample.kt" to "fun execute() { if (ready) { return } }",
            "Sample.java" to "class Sample { void execute() { if (ready) { return; } } }"
        )) {

            val result = format(name, source)

            assertTrue(result, result.contains(if (name.endsWith("kt")) "return\n    }" else "return;\n        }"))
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies kotlin expression body moves before splitting arguments.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testKotlinExpressionBodyMovesBeforeSplittingArguments() {

        val source = "fun extraordinarilyLongFunctionName(): Boolean = extraMethod(first, second, third, fourth, fifth)"

        val result = format("Sample.kt", source, RIGHT_MARGIN)

        assertTrue(result, result.contains("=\n    extraMethod( first, second, third, fourth, fifth )"))
        assertEquals(result, format("Sample.kt", result, RIGHT_MARGIN))

    }

    /**
     * Verifies constructor business calls remain between safe assignment runs.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testConstructorBusinessCallsRemainBetweenSafeAssignmentRuns() {

        val source = "class Sample { int extremelyLong; String x; int moderatelyLong; String y; Sample(int first, String second) { this.extremelyLong = first; this.x = second; audit(); this.moderatelyLong = first; this.y = second; } void audit() {} }"

        val result = format("Sample.java", source)

        assertTrue(result, result.indexOf("this.x = second") < result.indexOf("this.extremelyLong = first"))
        assertTrue(result, result.indexOf("this.extremelyLong = first") < result.indexOf("audit();"))
        assertTrue(result, result.indexOf("audit();") < result.indexOf("this.y = second"))
        assertTrue(result, result.indexOf("this.y = second") < result.indexOf("this.moderatelyLong = first"))
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Verifies different field types cascade as groups.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testDifferentFieldTypesCascadeAsGroups() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { public First x; public Second extraordinarilyLongName; public Third mediumName; }",
            "Sample.kt" to "class Sample { public lateinit var x: First; public lateinit var extraordinarilyLongName: Second; public lateinit var mediumName: Third }"
        )) {

            val result = format(name, source)

            assertTrue(result, result.indexOf("extraordinarilyLongName") < result.indexOf("mediumName"))
            assertTrue(result, result.indexOf("mediumName") < result.lastIndexOf(" x"))
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies expression body arguments pack at different margins.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testExpressionBodyArgumentsPackAtDifferentMargins() {

        val source = "fun execute(): Boolean = extraMethod(firstArgument, secondArgument, thirdArgument, fourthArgument, fifthArgument)"

        for (margin in listOf(DEFAULT_RIGHT_MARGIN, RIGHT_MARGIN, NARROW_MARGIN)) {

            val result = format("Sample.kt", source, margin)

            assertTrue(result, result.lines().all { it.length <= margin })
            assertTrue(result, result.indexOf("firstArgument") < result.indexOf("secondArgument"))
            assertTrue(result, result.indexOf("secondArgument") < result.indexOf("thirdArgument"))
            assertTrue(result, result.indexOf("fourthArgument") < result.indexOf("fifthArgument"))
            assertEquals(result, format("Sample.kt", result, margin))

        }

    }

    /**
     * Verifies kotlin constructor business call remains between assignment runs.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testKotlinConstructorBusinessCallRemainsBetweenAssignmentRuns() {

        val source = "class Sample { var extremelyLong: Int = 0; var x: String = \"\"; var moderatelyLong: Int = 0; var y: String = \"\"; constructor(first: Int, second: String) { this.extremelyLong = first; this.x = second; audit(); this.moderatelyLong = first; this.y = second }; fun audit() {} }"

        val result = format("Sample.kt", source)

        assertTrue(result, result.indexOf("this.x = second") < result.indexOf("this.extremelyLong = first"))
        assertTrue(result, result.indexOf("this.extremelyLong = first") < result.indexOf("audit();"))
        assertTrue(result, result.indexOf("audit();") < result.indexOf("this.y = second"))
        assertTrue(result, result.indexOf("this.y = second") < result.indexOf("this.moderatelyLong = first"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies trailing argument comma is preserved.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testTrailingArgumentCommaIsPreserved() {

        val source = "fun execute() { PsiTreeUtil.getParentOfType(firstArgument, secondArgument, thirdArgument,) }"

        val result = format("Sample.kt", source, NARROW_MARGIN)

        assertTrue(result, result.contains("thirdArgument,"))
        assertEquals(result, format("Sample.kt", result, NARROW_MARGIN))

    }

    /**
     * Verifies multiline conditional call gets its own block in both languages.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testMultilineConditionalCallGetsItsOwnBlockInBothLanguages() {

        for ((name, source) in listOf(
            "Sample.kt" to "fun execute() { if (left is KtProperty || isAssignment(left)) edits.gap(left, right, BLANK_LINE + indentation(left)) }",
            "Sample.java" to "class Sample { void execute() { if (left instanceof KtProperty || isAssignment(left)) edits.gap(left, right, BLANK_LINE + indentation(left)); } }"
        )) {

            val result = format(name, source, RIGHT_MARGIN)

            assertTrue(result, result.contains(") {\n\n"))
            assertTrue(result, result.contains("edits.gap(") || result.contains("edits\n"))
            assertEquals(result, format(name, result, RIGHT_MARGIN))

        }

    }

    /**
     * Verifies disjunction conditions keep each operand and trailing operator.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testDisjunctionConditionsKeepEachOperandAndTrailingOperator() {

        for ((name, source) in listOf(
            "Sample.kt" to "fun execute() { if (active.get() || !supports(source) || rangeToReformat.startOffset != 0 || rangeToReformat.endOffset < source.textLength) { consume() } }",
            "Sample.java" to "class Sample { void execute() { if (active.get() || !supports(source) || rangeToReformat.startOffset != 0 || rangeToReformat.endOffset < source.textLength) { consume(); } } }"
        )) {

            val result = format(name, source, RIGHT_MARGIN)

            val rows = result.lines().filter { it.trimEnd().endsWith("||") }

            assertEquals(result, 3, rows.size)
            assertTrue(result, result.contains("!supports( source ) ||"))
            assertTrue(result, result.lines().none { it.trimStart().startsWith("||") || it.trimStart().startsWith("&&") })
            assertTrue(result, result.lines().all { it.length <= RIGHT_MARGIN })
            assertEquals(result, format(name, result, RIGHT_MARGIN))

        }

    }

    /**
     * Verifies logical assignment keeps first condition beside equal.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testLogicalAssignmentKeepsFirstConditionBesideEqual() {

        for ((name, source) in listOf(
            "Sample.kt" to "fun execute() { val propertyReceiver = receiver is KtQualifiedExpression || receiver is PsiReferenceExpression && receiver.qualifierExpression != null }",
            "Sample.java" to "class Sample { void execute() { boolean propertyReceiver = receiver instanceof KtQualifiedExpression || receiver instanceof PsiReferenceExpression && receiver.qualifierExpression != null; } }"
        )) {

            val result = format(name, source, DEFAULT_RIGHT_MARGIN)

            assertTrue(result, result.contains("propertyReceiver = receiver"))
            assertTrue(result, result.lines().none { it.trimStart().startsWith("||") || it.trimStart().startsWith("&&") })
            assertTrue(result, result.lines().all { it.length <= DEFAULT_RIGHT_MARGIN })
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies lambda logical body respects margin.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testLambdaLogicalBodyRespectsMargin() {

        val source = "fun execute() { val content = children.filter { it.textRange.startOffset > opening.textRange.startOffset && it.textRange.endOffset < closing.textRange.endOffset } }"

        val result = format("Sample.kt", source)

        assertTrue(result, result.contains("val content = children\n        .filter {\n\n"))
        assertTrue(result, result.contains("it.textRange.startOffset > opening.textRange.startOffset &&\n"))
        assertTrue(result, result.lines().all { it.length <= DEFAULT_RIGHT_MARGIN })
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies continue has no blank line before closing brace.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testContinueHasNoBlankLineBeforeClosingBrace() {

        for ((name, source) in listOf(
            "Sample.kt" to "fun execute() { while (ready) { if (skip) { continue } } }",
            "Sample.java" to "class Sample { void execute() { while (ready) { if (skip) { continue; } } } }"
        )) {

            val result = format(name, source)

            assertFalse(result, Regex("continue;?\\n\\s*\\n").containsMatchIn(result))
            assertEquals(result, format(name, result))

        }

    }

    /**
     * Verifies when cases have blank lines and attached fallback.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testWhenCasesHaveBlankLinesAndAttachedFallback() {

        val source = "fun execute() { val call = when (val owner = list.parent) { is PsiMethodCallExpression -> owner; is KtCallExpression -> owner.parent as? KtQualifiedExpression; else -> null } ?: return false }"

        val result = format("Sample.kt", source)

        assertTrue(result, result.contains("} ?: return false"))
        assertTrue(result, Regex("owner;?\\n\\n").containsMatchIn(result))
        assertTrue(result, Regex("KtQualifiedExpression;?\\n\\n").containsMatchIn(result))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies simple receiver stays with wrapped call name.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testSimpleReceiverStaysWithWrappedCallName() {

        for ((name, source) in listOf(
            "Sample.kt" to "fun execute() { edits.replace(list.textRange, firstLongArgumentName + secondLongArgumentName + thirdLongArgumentName) }",
            "Sample.java" to "class Sample { void execute() { edits.replace(list.textRange, firstLongArgumentName + secondLongArgumentName + thirdLongArgumentName); } }"
        )) {

            val result = format(name, source, RIGHT_MARGIN)

            assertTrue(result, result.contains("edits.replace(\n"))
            assertFalse(result, result.contains("edits\n"))
            assertEquals(result, format(name, result, RIGHT_MARGIN))

        }

    }

    /**
     * Verifies chain receiver stays beside variable assignment.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testChainReceiverStaysBesideVariableAssignment() {

        val source = "fun execute() { val lastToken = list.node.getChildren(null).map { it.psi }.filter { it !is PsiWhiteSpace }.dropLast(1).lastOrNull() }"

        val result = format("Sample.kt", source)

        assertTrue(result, result.contains("val lastToken = list.node\n        .getChildren( null )"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies switch cases have blank lines.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testSwitchCasesHaveBlankLines() {

        val source = "class Sample { int execute(int value) { switch(value) { case 1: return 1; case 2: return 2; default: return 0; } } }"

        val result = format("Sample.java", source)

        assertTrue(result, result.contains("return 1;\n\n"))
        assertTrue(result, result.contains("return 2;\n\n"))
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Verifies mixed lambda condition keeps each operand on its own row.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testMixedLambdaConditionKeepsEachOperandOnItsOwnRow() {

        val source = "fun execute() { if (items.isEmpty() || items.any { BlockLayout().hasChainedCall(it) || it.text.contains('\\n') } || PsiTreeUtil.findChildrenOfType(list, PsiComment::class.java).isNotEmpty()) { consume() } }"

        val original = tokens(myFixture.configureByText("Sample.kt", source))

        val result = format("Sample.kt", source)

        assertEquals(original, tokens(myFixture.file))
        assertTrue(result, result.contains("items.isEmpty() ||\n        items.any {"))
        assertTrue(result, result.contains("} ||\n        PsiTreeUtil"))
        assertFalse(result, result.contains("items\n"))
        assertTrue(result, result.lines().none { it.trimStart().startsWith("||") })
        assertTrue(result, result.contains(") {\n\n        consume()\n\n    }"))
        assertEquals(result, format("Sample.kt", result))

    }

    /**
     * Verifies java block lambda condition preserves evaluation order.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testJavaBlockLambdaConditionPreservesEvaluationOrder() {

        val source = "class Sample { void execute() { if (items.isEmpty() || items.any(it -> { return hasChain(it) || it.text.contains(\"value\"); }) || Tree.find(list, Comment.class).isNotEmpty()) { consume(); } } }"

        val original = tokens(myFixture.configureByText("Sample.java", source))

        val result = format("Sample.java", source)

        assertEquals(original, tokens(myFixture.file))
        assertTrue(result, result.contains("items.isEmpty() ||\n"))
        assertTrue(result, result.indexOf("items.isEmpty()") < result.indexOf("items.any("))
        assertTrue(result, result.indexOf("items.any(") < result.indexOf("Tree.find("))
        assertTrue(result, result.lines().none { it.trimStart().startsWith("||") })
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Verifies source files match their declared packages.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testSourceFilesMatchTheirDeclaredPackages() {

        for (root in listOf(Path.of("src/main/kotlin"), Path.of("src/main/java"), Path.of("src/test/kotlin"))
            .filter { Files.isDirectory( it ) }) {

            Files.walk(root).use { paths ->

                paths.filter { it.toString().endsWith(".kt") || it.toString().endsWith(".java") }.forEach { path ->

                    val packageName = Files.readString(path).lineSequence().first { it.startsWith("package ") }
                        .removePrefix("package ").removeSuffix(";")

                    val expected = root.resolve(packageName.replace('.', '/'))

                    assertEquals(path.toString(), expected, path.parent)

                }

            }

        }

    }

    /**
     * Collects sorted non-whitespace leaf token spellings, excluding optional statement semicolons.
     *
     * @param file PSI file supplying leaf tokens
     *
     * @return sorted token spellings for preservation assertions
     *
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun tokens(
        file: PsiFile
    ): List<String> {

        return PsiTreeUtil.collectElements(file) {

            val block = it.parent as? org.jetbrains.kotlin.psi.KtBlockExpression

            val optionalBrace = (it.text == "{" || it.text == "}") && block != null &&
                (block.parent is org.jetbrains.kotlin.psi.KtIfExpression ||
                    block.parent.parent is org.jetbrains.kotlin.psi.KtIfExpression) &&
                BranchLayout().isDataBlock(block)

            val javaBlock = it.parent as? com.intellij.psi.PsiCodeBlock

            val javaConditional = javaBlock?.parent as? com.intellij.psi.PsiBlockStatement

            val optionalJavaBrace = (it.text == "{" || it.text == "}") && javaConditional?.parent is com.intellij.psi.PsiIfStatement &&
                javaBlock.statements.singleOrNull() is com.intellij.psi.PsiExpressionStatement

            it.node.firstChildNode == null && it !is PsiWhiteSpace && it.text != ";" && !optionalBrace && !optionalJavaBrace

        }.map { it.text }.sorted()

    }

    /**
     * Verifies modifiers and headers.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testModifiersAndHeaders() {

        val source = "package sample; import java.util.List; public class Sample { static final public String NAME = \"name\"; }"

        val result = format("Sample.java", source)

        assertTrue(result.contains("package sample;\n\nimport java.util.List;\n\npublic class Sample"))
        assertTrue(result.contains("public static final String NAME"))
        assertEquals(result, format("Sample.java", result))

    }

    /**
     * Verifies strings and related statements are preserved.
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun testStringsAndRelatedStatementsArePreserved() {

        val source = "class Sample { String execute() { String value = \"{ a, b; }\"; return value; } }"

        val result = format("Sample.java", source)

        assertTrue(result.contains("\"{ a, b; }\""))
        assertTrue(result.contains("String value = \"{ a, b; }\";\n\n        return value;"))
        assertTrue(result.contains("execute() {\n\n"))
        assertTrue(result.contains("return value;\n\n    }"))

    }

    /**
     * Verifies documented Java sample stability and exports its native Javadoc layout for review.
     *
     * @since 1.0.0
     */
    fun testDocumentedJavaSampleHasStableOutput() {

        val path = Path.of( "src/main/resources/JavaSample.txt" )

        val result = format( "JavaSample.java", Files.readString( path ) )

        assertEquals( result, format( "JavaSample.java", result ) )

        val output = Path.of( "build/formatted-samples/JavaSample.java" )

        Files.createDirectories( output.parent )

        Files.writeString( output, result )

    }

    /**
     * Runs the registered native Reformat Code action with a fixture-specific right margin and verifies valid PSI.
     *
     * @param name fixture filename selecting the language parser
     * @param text original source text supplied to the fixture
     * @param margin maximum preferred column from the language code style
     *
     * @return computed source text or whitespace prefix
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun format(
        name: String,
        text: String,
        margin: Int = DEFAULT_RIGHT_MARGIN
    ): String {

        val file = myFixture.configureByText(name, text)

        assertEquals(if (name.endsWith(".java")) "JAVA" else "kotlin", file.language.id)
        assertTrue(PostFormatProcessor.EP_NAME.extensionList.any { it is FormatProcessor })
        val settingsManager = CodeStyleSettingsManager.getInstance(project)
        val settings = settingsManager.cloneSettings(CodeStyle.getSettings(file))

        settings.setRightMargin(file.language, margin)
        settings.FORMATTER_TAGS_ENABLED = true

        settingsManager.doWithTemporarySettings(
            settings,
            Runnable {

                WriteCommandAction.runWriteCommandAction(project) {

                    CodeStyleManager.getInstance(project).reformat(file)

                }

            }
        )

        assertNull(PsiTreeUtil.findChildOfType(file, PsiErrorElement::class.java))

        return file.text

    }

    companion object {

        private const val NARROW_MARGIN = 50

        private const val RIGHT_MARGIN = 70
        private const val DEFAULT_RIGHT_MARGIN = 120

    }

}