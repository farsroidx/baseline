package ir.farsroidx.baseline.format

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** Verifies chain context, margin handling, stable formatting and exact token order. */
class ChainContextTest : BasePlatformTestCase() {

    /** Verifies java assignment chain starts at receiver. */
    fun testJavaAssignmentChainStartsAtReceiver() {

        val source = "class Sample { void run() { UpdateUserOperationAddressClientResponse apiResponse = updateUserOperationAddressService.execute(requestHeader, userMapper.toUpdateUserOperationAddressClientRequest(clientRequest)).getBody(); consume(apiResponse); } }"

        for (margin in listOf( 120, 100, 85 )) {

            val result = format( "Sample.java", source, margin )

            assertTrue( result, result.contains( "= updateUserOperationAddressService\n            .execute(" ) )

            assertTrue( result, result.contains( "\n            .getBody();" ) )

            assertTrue( result, result.lines().filterNot { it.contains( "= updateUserOperationAddressService" ) }.all { it.length <= margin } )

            if (margin == 120) {

                assertTrue( result, result.contains( ".execute( requestHeader, userMapper.toUpdateUserOperationAddressClientRequest( clientRequest ) )" ) )

            }

            assertEquals( result, format( "Sample.java", result, margin ) )

        }

    }

    /** Verifies kotlin assignment chain starts at receiver. */
    fun testKotlinAssignmentChainStartsAtReceiver() {

        val source = "fun run() { val apiResponse: UpdateUserOperationAddressClientResponse = updateUserOperationAddressService.execute(requestHeader, userMapper.toUpdateUserOperationAddressClientRequest(clientRequest)).getBody(); consume(apiResponse) }"

        for (margin in listOf( 120, 100, 85 )) {

            val result = format( "Sample.kt", source, margin )

            assertTrue( result, result.contains( "= updateUserOperationAddressService\n        .execute(" ) )

            assertTrue( result, result.contains( "\n        .getBody()" ) )

            assertEquals( result, format( "Sample.kt", result, margin ) )

        }

    }

    /** Verifies single call keeps its receiver. */
    fun testSingleCallKeepsItsReceiver() {

        for ((name, source) in listOf(
            "Sample.java" to "class Sample { void run() { Object apiResponse = updateUserOperationAddressService.execute(requestHeader, userMapper.toUpdateUserOperationAddressClientRequest(clientRequest)); consume(apiResponse); } }",
            "Sample.kt" to "fun run() { val apiResponse = updateUserOperationAddressService.execute(requestHeader, userMapper.toUpdateUserOperationAddressClientRequest(clientRequest)); consume(apiResponse) }"
        )) {

            val result = format( name, source, 100 )

            assertTrue( result, result.contains( "= updateUserOperationAddressService.execute(\n" ) )

            assertEquals( result, format( name, result, 100 ) )

        }

    }

    /** Verifies java ternary keeps short branch chain inline. */
    fun testJavaTernaryKeepsShortBranchChainInline() {

        val source = "class Sample { void run() { int resultCount = (serviceResponse != null && serviceResponse.getAddressListResults() != null) ? serviceResponse.getAddressListResults().size() : 0; consume(resultCount); } }"

        val result = format( "Sample.java", source, 120 )

        assertTrue( result, result.contains( "? serviceResponse.getAddressListResults().size()" ) )

        assertEquals( result, format( "Sample.java", result, 120 ) )

    }

    /** Verifies java ternary wraps overlong branch chain. */
    fun testJavaTernaryWrapsOverlongBranchChain() {

        val source = "class Sample { void run() { int count = ready ? serviceResponse.getExtraordinarilyLongAddressListResults().size() : 0; consume(count); } }"

        val result = format( "Sample.java", source, 70 )

        assertTrue( result, result.contains( "\n                .size()" ) || result.contains( "\n                    .size()" ) )

        assertTrue( result, result.lines().all { it.length <= 70 } )

        assertEquals( result, format( "Sample.java", result, 70 ) )

    }

    /** Verifies previously wrapped input and comments stay safe. */
    fun testPreviouslyWrappedInputAndCommentsStaySafe() {

        val source = "class Sample { void run() { Object result = service.execute(\nfirst, mapper.convert( second )\n)\n.getBody(); int count = ready ? response.results()\n.size() : 0; consume(result, count); } }"

        val result = format( "Sample.java", source, 100 )

        assertTrue( result, result.contains( "= service\n            .execute(" ) )

        assertTrue( result, result.contains( "? response.results().size()" ) )

        assertEquals( result, format( "Sample.java", result, 100 ) )

        val commented = "class Sample { void run() { int count = ready ? response.results() /* retain */ .size() : 0; consume(count); } }"

        val safe = format( "Sample.java", commented, 100 )

        assertTrue( safe, safe.contains( "/* retain */" ) )

        assertEquals( safe, format( "Sample.java", safe, 100 ) )

    }

    /** Verifies kotlin conditional branch keeps short chain inline. */
    fun testKotlinConditionalBranchKeepsShortChainInline() {

        val source = "fun run() { val count = if (ready) response.results().size() else 0; consume(count) }"

        val result = format( "Sample.kt", source, 100 )

        assertTrue( result, result.contains( "response.results().size()" ) )

        assertEquals( result, format( "Sample.kt", result, 100 ) )

    }

    /** Verifies kotlin safe assignment and nested argument chains. */
    fun testKotlinSafeAssignmentAndNestedArgumentChains() {

        val source = "fun run() { val response = service?.execute(header, mapper.convert(request))?.getBody(); consume(response); consume(variable.first().second()) }"

        val result = format( "Sample.kt", source, 100 )

        assertTrue( result, result.contains( "= service\n        ?.execute(" ) )

        assertTrue( result, result.contains( "\n        ?.getBody()" ) )

        assertTrue( result, result.contains( "variable.first()\n            .second()" ) )

        assertEquals( result, format( "Sample.kt", result, 100 ) )

    }

    /** Verifies conditional branch with nested chain preserves argument layout. */
    fun testConditionalBranchWithNestedChainPreservesArgumentLayout() {

        val source = "class Sample { void run() { int count = ready ? response.convert(value.first().second()).size() : 0; consume(count); } }"

        val result = format( "Sample.java", source, 100 )

        assertTrue( result, result.contains( "value.first()\n" ) )

        assertEquals( result, format( "Sample.java", result, 100 ) )

    }

    /**
     * Runs native Reformat Code and checks syntax and the original token sequence.
     *
     * @param name fixture name selecting the language
     * @param source input source
     * @param margin preferred maximum column
     * @return formatted source
     */
    private fun format(name: String, source: String, margin: Int): String {

        val file = myFixture.configureByText( name, source )

        val before = PsiTreeUtil.collectElements( file ) {

            it.node.firstChildNode == null && it !is PsiWhiteSpace

        }.map { it.text }

        val manager = CodeStyleSettingsManager.getInstance( project )

        val settings = manager.cloneSettings( CodeStyle.getSettings( file ) )

        settings.setRightMargin( file.language, margin )

        manager.doWithTemporarySettings( settings, Runnable {

            WriteCommandAction.runWriteCommandAction( project ) {

                CodeStyleManager.getInstance( project ).reformat( file )

            }

        } )

        assertNull( PsiTreeUtil.findChildOfType( file, PsiErrorElement::class.java ) )

        val after = PsiTreeUtil.collectElements( file ) {

            it.node.firstChildNode == null && it !is PsiWhiteSpace

        }.map { it.text }

        assertEquals( before, after )

        return file.text

    }
}
