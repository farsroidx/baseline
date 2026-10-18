package ir.farsroidx.baseline.layout

import ir.farsroidx.baseline.config.FormatOptions
import ir.farsroidx.baseline.edit.TextEdits

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*

/** Applies margin-sensitive layouts without changing expression or parameter order. */
/**
 * Plans width-aware call, expression-body, control-header and Kotlin fallback layouts.
 *
 * Argument packing preserves order and skips comment-bearing or unsafe multiline argument
 * lists. A single indivisible token may exceed the right margin; it is never split.
 */
internal class CallLayout {

    /**
     * Plans simple-receiver attachment and margin-aware receiver breaks without rewriting true fluent chains.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param margin maximum preferred column from the language code style
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun receivers(source: PsiFile, margin: Int): TextEdits {

        val edits = TextEdits()

        for (
            expression in PsiTreeUtil
                .collectElements( source ) {

                    it is PsiMethodCallExpression ||
                        it is KtQualifiedExpression && it.selectorExpression is KtCallExpression

                }
        ) {

            val reference = if (expression is PsiMethodCallExpression) expression.methodExpression else expression

            val children = reference.node
                .getChildren( null )
                .map { it.psi }
                .filter { it !is PsiWhiteSpace }

            val dot = children.firstOrNull { it.text == "." || it.text == "?." } ?: continue

            val receiver = children.lastOrNull { it.textRange.endOffset <= dot.textRange.startOffset } ?: continue

            val list = when (expression) {

                is PsiMethodCallExpression -> expression.argumentList

                is KtQualifiedExpression   -> (expression.selectorExpression as? KtCallExpression)?.valueArgumentList

                else                       -> null

            }

            if (list != null && ChainLayout().isLink( list )) {

            continue
            }

            val classReceiver = receiver.text
                .firstOrNull()
                ?.isUpperCase() == true

            val lambdaCall = expression is KtQualifiedExpression &&
                (expression.selectorExpression as? KtCallExpression)?.lambdaArguments?.isNotEmpty() == true

            if (!classReceiver && !lambdaCall) {

            edits.gap( receiver, dot, "" )

            continue
            }

            val flat = SyntaxLayout().flatInline( expression ) ?: expression.text.replace( Regex( "\\s+" ), " " )

            if (column( expression ) + flat.length <= margin && !expression.text.contains( '\n' )) {

            continue
            }

            if (receiver.text.contains( '\n' )) {

            continue
            }

            edits.gap( receiver, dot, NEW_LINE + indentation( expression ) + INDENT )

        }

        return edits

    }

    /**
     * Moves a Kotlin expression body below equals when that permits a complete inline body to fit.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param margin maximum preferred column from the language code style
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun expressionBodies(source: PsiFile, margin: Int): TextEdits {

        val edits = TextEdits()

        for (function in PsiTreeUtil.findChildrenOfType( source, KtNamedFunction::class.java )) {

            if (function.hasBlockBody()) {

                continue
            }

            val body = function.bodyExpression ?: continue

            val equal = function.equalsToken ?: continue

            val text = SyntaxLayout().flatInline( body ) ?: continue

            if (BlockLayout().hasChainedCall( body )) {

                continue
            }

            val base = indentation( function )

            val prefix = source.text.substring( function.textRange.startOffset, equal.textRange.endOffset )

            if (
                !prefix.contains( '\n' ) && base.length + prefix.length + SPACE_WIDTH + text.length > margin &&
                base.length + INDENT.length + text.length <= margin
            ) {

                edits.gap( equal, body, NEW_LINE + base + INDENT )

                edits.replace( body.textRange, text )

            }
        }

        return edits

    }

    /**
     * Wraps multiline for/while/switch/when headers and repairs their body continuation indentation.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun headers(source: PsiFile): TextEdits {

        val edits = TextEdits()

        for (
            owner in PsiTreeUtil
                .collectElements( source ) {

                    it is KtLoopExpression || it is KtWhenExpression || it is PsiLoopStatement || it is PsiSwitchBlock

                }
        ) {

            val children = owner.node
                .getChildren( null )
                .map { it.psi }
                .filter { it !is PsiWhiteSpace }

            val opening = children.firstOrNull { it.text == "(" } ?: continue

            val closing = children.firstOrNull { it.text == ")" } ?: continue

            val content = children
                .filter {

                    it.textRange.startOffset >= opening.textRange.endOffset &&
                        it.textRange.endOffset <= closing.textRange.startOffset

                }

            if (
                content.isEmpty() || !source.text
                .substring( opening.textRange.endOffset, closing.textRange.startOffset )
                .contains( '\n' )
            ) {

            continue
            }

            val base = indentation( owner )

            edits.gap( opening, content.first(), NEW_LINE + base + INDENT )

            edits.gap( content.last(), closing, NEW_LINE + base )

            val shift = base.length + INDENT.length - indentation( content.first() ).length

            if (shift > 0) {

            for (whitespace in PsiTreeUtil.findChildrenOfType( owner, PsiWhiteSpace::class.java )) {

                if (
                    whitespace.textRange.startOffset < content.first().textRange.startOffset ||
                    whitespace.textRange.endOffset > content.last().textRange.endOffset ||
                    !whitespace.text.contains( '\n' )
                ) {

                    continue
                }

                edits.replace(
                    whitespace.textRange,
                    whitespace.text
                        .lines()
                        .mapIndexed { index, line ->

                        if (index > 0 && line.isNotEmpty()) " ".repeat( shift ) + line else line

                        }
                        .joinToString( NEW_LINE )
                )

            }
            }

            val body = when (owner) {

                is KtLoopExpression -> owner.body

                is PsiLoopStatement -> (owner.body as? PsiBlockStatement)?.codeBlock

                else                -> null

            } ?: continue

            val statements = when (body) {

                is KtBlockExpression -> body.statements

                is PsiCodeBlock      -> body.statements.toList()

                else                 -> emptyList()

            }

            val first = statements.firstOrNull() ?: continue

            val old = indentation( first )

            val expected = base + INDENT

            val closingBrace = body.node
                .getChildren( null )
                .map { it.psi }
                .lastOrNull { it.text == "}" }

            val last = statements.lastOrNull()

            if (old == expected) {

            continue
            }

            for (whitespace in PsiTreeUtil.findChildrenOfType( body, PsiWhiteSpace::class.java )) {

            if (!whitespace.text.contains( '\n' )) {

                continue
            }

            val replacement = whitespace.text
                .lines()
                .mapIndexed { index, line ->

                    if (index > 0 && line.startsWith( old )) expected + line.removePrefix( old ) else line

                }
                .joinToString( NEW_LINE )

            edits.replace( whitespace.textRange, replacement )

            }

            if (closingBrace != null && last != null) {

            edits.gap(
                last,
                closingBrace,
                (if (
                    last.text
                    .trimEnd()
                    .endsWith( "}" )
                ) NEW_LINE else NEW_LINE + NEW_LINE) + base
            )

            }
        }

        return edits

    }

    /**
     * Adds braces around a multiline call used as an unbraced conditional then-branch.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun conditionalBlocks(source: PsiFile): TextEdits {

        val edits = TextEdits()

        for (owner in PsiTreeUtil.collectElements( source ) { it is KtIfExpression || it is PsiIfStatement }) {

            val branch = when (owner) {

                is KtIfExpression -> owner.then

                is PsiIfStatement -> owner.thenBranch

                else              -> null

            } ?: continue

            val call = when (branch) {

                is PsiExpressionStatement -> branch.expression is PsiMethodCallExpression

                is KtCallExpression       -> true

                is KtQualifiedExpression  -> branch.selectorExpression is KtCallExpression

                else                      -> false

            }

            if (
                !call || !branch.text.contains( '\n' ) || PsiTreeUtil.findChildrenOfType(
                    branch, PsiComment::class.java
                )
                .isNotEmpty()
            ) {

                continue
            }

            val base = indentation( owner )

            val lines = branch.text.lines()

            val text = lines
                .mapIndexed { index, line ->

                    if (index == 0) base + INDENT + line else INDENT + line

                }
                .joinToString( NEW_LINE )

            edits.replace( branch.textRange, "{" + NEW_LINE + NEW_LINE + text + NEW_LINE + NEW_LINE + base + "}" )

        }

        return edits

    }

    /**
     * Plans Kotlin Elvis placement for multiline left operands.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun fallbacks(source: PsiFile): TextEdits {

        val edits = TextEdits()

        for (expression in PsiTreeUtil.findChildrenOfType( source, KtBinaryExpression::class.java )) {

            if (expression.operationReference.text != "?:") {

                continue
            }

            val left = expression.left ?: continue

            if (
                left.text
                .trimEnd()
                .endsWith( "}" )
            ) {

                edits.gap( left, expression.operationReference, " " )

                continue
            }

            if (left.text.contains( '\n' )) {

                edits.gap( left, expression.operationReference, NEW_LINE + indentation( expression ) + INDENT )

            }
        }

        return edits

    }

    /**
     * Removes the blank line before a closing brace after an operand-free exit.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun bareReturns(source: PsiFile): TextEdits {

        val edits = TextEdits()

        for (
            statement in PsiTreeUtil
                .collectElements( source ) {

                    it is PsiReturnStatement && it.returnValue == null ||
                        it is KtReturnExpression && it.returnedExpression == null || it is PsiContinueStatement ||
                        it is PsiBreakStatement || it is KtContinueExpression || it is KtBreakExpression

                }
        ) {

            var next = statement.nextSibling

            while (next is PsiWhiteSpace) {

            next = next.nextSibling

            }

            if (next?.text == "}") edits.gap( statement, next, NEW_LINE + indentation( next ) )

        }

        return edits

    }

    /**
     * Returns the zero-based column of the element in its current source line.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return computed count or zero-based source column
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun column(element: PsiElement): Int {

        val offset = element.textRange.startOffset

        return offset - (element.containingFile.text.lastIndexOf( '\n', (offset - 1).coerceAtLeast( 0 ) ) + 1)

    }

    /**
     * Returns the spaces/tabs at the beginning of the element's current source line.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return computed source text or whitespace prefix
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun indentation(element: PsiElement): String {

        val source = element.containingFile.text

        val offset = element.textRange.startOffset

        val start = source.lastIndexOf( '\n', (offset - 1).coerceAtLeast( 0 ) ) + 1

        return source.substring( start, offset )
            .takeWhile { it == ' ' || it == '\t' }
    }

    /**
     * Packs safe argument lists into margin-limited continuation rows without changing their order.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param margin maximum preferred column from the language code style
     * @param level parameter-list nesting depth to process in this batch
     * @param options project preferences captured for the current operation
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun arguments(source: PsiFile, margin: Int, level: Int, options: FormatOptions): TextEdits {

        val edits = TextEdits()

        val lists =
            PsiTreeUtil
                .findChildrenOfType( source, PsiExpressionList::class.java ) + PsiTreeUtil
                .findChildrenOfType( source, KtValueArgumentList::class.java )

        for (list in lists) {

            val depth = generateSequence( list.parent ) { it.parent }
                .count {

                    it is PsiExpressionList || it is KtValueArgumentList || it is PsiParameterList ||
                        it is KtParameterList

                }

            if (depth != level) {

                continue
            }

            val owner = list.parent

            val qualified = owner is PsiMethodCallExpression && owner.methodExpression.qualifierExpression != null ||
                owner is KtCallExpression && owner.parent is KtQualifiedExpression

            val function = owner.parent as? KtNamedFunction

            val expressionBody = function != null && !function.hasBlockBody()

            if (!qualified && !expressionBody && !list.text.contains( '\n' )) {

                continue
            }

            val items: List<PsiElement> = when (list) {

                is PsiExpressionList   -> list.expressions.toList()

                is KtValueArgumentList -> list.arguments

                else                   -> emptyList()

            }

            if (
                items.isEmpty() ||
                items.any {

                    options.formatChains && BlockLayout().hasChainedCall( it ) || it.text.contains( '\n' )

                } ||
                PsiTreeUtil.findChildrenOfType( list, PsiComment::class.java )
                    .isNotEmpty()
            ) {

                continue
            }

            val texts = items.map { SyntaxLayout().flatInline( it ) }

            if (
                texts.any { it == null } ||
                list.text
                .trimEnd()
                .endsWith( ",)" )
            ) {

                continue
            }

            val lastToken = list.node
                .getChildren( null )
                .map { it.psi }
                .filter { it !is PsiWhiteSpace }
                .dropLast( 1 )
                .lastOrNull()

            if (lastToken?.text == ",") {

                continue
            }

            val flat = texts.filterNotNull()

            val base = indentation( list )

            val padded = if (flat.size == 1) options.padSingleArguments else options.padMultipleArguments && (qualified || expressionBody)

            val padding = if (padded) " " else ""

            val inline = "(" + padding + flat.joinToString( ", " ) + padding + ")"

            val end = source.text
                .indexOf( '\n', list.textRange.endOffset )
                .let { if (it < 0) source.textLength else it }

            val suffix = source.text
                .substring( list.textRange.endOffset, end )
                .substringBefore( '{' )

            if (
                !options.collapseShortLists && !list.text.contains( '\n' ) &&
                column( list ) + inline.length + suffix.length <= margin
            ) {

                continue
            }

            if (options.collapseShortLists && column( list ) + inline.length + suffix.length <= margin) {

                edits.replace( list.textRange, inline )

                continue
            }

            if (!options.wrapCalls) {

                continue
            }

            val rows = mutableListOf<String>()

            var row = ""

            for ( (index, text) in flat.withIndex() ) {

                val value = text + if (index == flat.lastIndex) "" else ","

                if (row.isNotEmpty() && base.length + INDENT.length + row.length + SPACE_WIDTH + value.length > margin) {

                    rows.add( row )

                    row = ""

                }

                row += (if (row.isEmpty()) "" else " ") + value

            }

            rows.add( row )

            edits.replace(
                list.textRange,
                "(" + NEW_LINE + rows.joinToString( NEW_LINE ) { base + INDENT + it } + NEW_LINE + base + ")"
            )

        }

        return edits

    }

    companion object {

        private const val INDENT   = "    "
        private const val NEW_LINE = "\n"

        private const val SPACE_WIDTH = 1

    }
}