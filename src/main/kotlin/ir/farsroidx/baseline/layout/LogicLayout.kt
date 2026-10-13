package ir.farsroidx.baseline.layout

import ir.farsroidx.baseline.edit.TextEdits

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*

/** Formats logical expressions through PSI while preserving operands, precedence and evaluation order. */
/**
 * Wraps Java/Kotlin logical expressions without changing operand order or precedence.
 *
 * Logical operators remain at row ends. Expressions that cannot be safely flattened use
 * conservative PSI gaps. Assignment receivers and multiline Kotlin lambdas have separate passes.
 */
internal class LogicLayout {

    /**
     * Attaches the first chain receiver beside equals and normalizes continuation indentation without moving expressions.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun assignments(source: PsiFile): TextEdits {

        val edits = TextEdits()

        for (owner in PsiTreeUtil.collectElements( source ) { it is KtProperty || it is PsiVariable }) {

            val value = when (owner) {

                is KtProperty  -> owner.initializer

                is PsiVariable -> owner.initializer

                else           -> null

            } ?: continue

            if (!BlockLayout().hasChainedCall( value )) {

                continue
            }

            val equal = owner.node
                .getChildren( null )
                .map { it.psi }
                .firstOrNull { it.text == "=" } ?: continue

            val base = indentation( owner )

            val previous = indentation( value )

            edits.gap( equal, value, " " )

            if (previous.length <= base.length) {

                continue
            }

            val shift = previous.length - base.length

            for (whitespace in PsiTreeUtil.findChildrenOfType( value, PsiWhiteSpace::class.java )) {

                val replacement = whitespace.text
                    .lines()
                    .mapIndexed { index, line ->

                        if (
                            index > 0 &&
                            line.length >= shift &&
                            line.all { it == ' ' || it == '\t' }
                        ) {

                            line.drop( shift )

                        } else line

                    }
                    .joinToString( NEW_LINE )

                edits.replace( whitespace.textRange, replacement )

            }
        }

        return edits

    }

    /**
     * Plans inline or wrapped conditions whose operands can be rendered without blocks.
     *
     * @param source committed Java or Kotlin PSI file
     * @param margin preferred maximum column
     * @return unapplied edits for safely rendered logical conditions
     */
    private fun simpleConditions(source: PsiFile, margin: Int): TextEdits {

        val edits = TextEdits()

        for (owner in PsiTreeUtil.collectElements( source ) { condition( it ) != null }) {

            val value = condition( owner ) ?: continue

            if (operator( value ) == null) {

                continue
            }

            val flat = SyntaxLayout().flatInline( value ) ?: continue

            if (BlockLayout().hasChainedCall( value )) {

                continue
            }

            val children = owner.node
                .getChildren( null )
                .map { it.psi }

            val opening = children.firstOrNull { it.text == "(" } ?: continue

            val closing = children.firstOrNull { it.text == ")" } ?: continue

            val base = indentation( owner )

            if (column( opening ) + SPACE_WIDTH + flat.length + SPACE_WIDTH <= margin) {

                edits.replace( value.textRange, flat )

                edits.gap( opening, value, "" )

                edits.gap( value, closing, "" )

                continue
            }

            val continuation = base + INDENT

            val text = render(value, continuation.length, continuation, margin, operator( value ) == "||") ?: continue

            edits.replace( value.textRange, text )

            edits.gap( opening, value, NEW_LINE + continuation )

            edits.gap( value, closing, NEW_LINE + base )

        }

        return edits

    }


    /**
     * Plans margin-aware control-condition formatting with preserved operand order.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param margin maximum preferred column from the language code style
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun conditions(source: PsiFile, margin: Int): TextEdits {

        val edits = simpleConditions(source, margin)

        for (owner in PsiTreeUtil.collectElements( source ) { condition( it ) != null }) {

            val value = condition( owner ) ?: continue

            val sign = operator( value ) ?: continue

            if (
                SyntaxLayout()
                    .flatInline( value ) != null || PsiTreeUtil.findChildrenOfType( value, PsiComment::class.java )
                .isNotEmpty()
            ) {

                continue
            }

            val parts = operands(value, sign)

            val tokens = owner.node
                .getChildren( null )
                .map { it.psi }

            val opening = tokens.firstOrNull { it.text == "(" } ?: continue

            val closing = tokens.firstOrNull { it.text == ")" } ?: continue

            val base = indentation( owner )

            val continuation = base + INDENT

            edits.gap( opening, value, NEW_LINE + continuation )

            edits.gap( value, closing, NEW_LINE + base )

            for ( (left, right) in parts.zipWithNext() ) {

                var separator = PsiTreeUtil.nextLeaf( left )

                while (separator is PsiWhiteSpace) {

                    separator = PsiTreeUtil.nextLeaf( separator )

                }

                if (separator?.text == sign) {

                    edits.gap( left, separator, " " )

                    edits.gap( separator, right, NEW_LINE + continuation )

                }
            }

            for (part in parts) {

                val call = part as? KtQualifiedExpression ?: continue

                val selector = call.selectorExpression as? KtCallExpression ?: continue

                val lambda = selector.lambdaArguments
                    .singleOrNull()
                    ?.getLambdaExpression()
                    ?: continue

                if (BlockLayout().hasChainedCall( call.receiverExpression )) {

                    continue
                }

                val dot = call.node
                    .getChildren( null )
                    .map { it.psi }
                    .firstOrNull { it.text == "." || it.text == "?." } ?: continue

                val shift = indentation( lambda ).length - continuation.length

                edits.gap( call.receiverExpression, dot, "" )

                if (shift <= 0) {

                    continue
                }

                for (whitespace in PsiTreeUtil.findChildrenOfType( lambda, PsiWhiteSpace::class.java )) {

                    val replacement = whitespace.text
                        .lines()
                        .mapIndexed { index, line ->

                            if (
                                index > 0 &&
                                line.length >= shift &&
                                line.all { it == ' ' || it == '\t' }
                            ) {

                                line.drop( shift )

                            } else line

                        }
                        .joinToString( NEW_LINE )

                    edits.replace( whitespace.textRange, replacement )

                }
            }

            val body = when (owner) {

                is KtIfExpression   -> owner.then as? KtBlockExpression

                is KtLoopExpression -> owner.body as? KtBlockExpression

                is PsiIfStatement   -> (owner.thenBranch as? PsiBlockStatement)?.codeBlock

                is PsiLoopStatement -> (owner.body as? PsiBlockStatement)?.codeBlock

                else                -> null

            } ?: continue

            val statements = when (body) {

                is KtBlockExpression -> body.statements

                is PsiCodeBlock      -> body.statements.toList()

                else                 -> emptyList()

            }

            val first = statements.firstOrNull() ?: continue

            val previous = indentation( first )

            val shift = previous.length - continuation.length

            if (shift > 0) {

                for (whitespace in PsiTreeUtil.findChildrenOfType( body, PsiWhiteSpace::class.java )) {

                    val replacement = whitespace.text
                        .lines()
                        .mapIndexed { index, line ->

                            if (
                                index > 0 &&
                                line.length >= previous.length &&
                                line.all { it == ' ' || it == '\t' }
                            ) {

                                line.drop( shift )

                            } else line

                        }
                        .joinToString( NEW_LINE )

                    edits.replace( whitespace.textRange, replacement )

                }
            }

            val braces = body.node
                .getChildren( null )
                .map { it.psi }

            val firstBrace = braces.firstOrNull { it.text == "{" } ?: continue

            val lastBrace = braces.lastOrNull { it.text == "}" } ?: continue

            val last = statements.last()

            val exit = last is KtContinueExpression || last is KtBreakExpression ||
                last is KtReturnExpression && last.returnedExpression == null || last is PsiContinueStatement ||
                last is PsiBreakStatement || last is PsiReturnStatement && last.returnValue == null

            edits.gap( firstBrace, first, NEW_LINE + NEW_LINE + continuation )

            edits.gap(
                last,
                lastBrace,
                (if (
                    exit || last.text
                    .trimEnd()
                    .endsWith( "}" )
                ) NEW_LINE else NEW_LINE + NEW_LINE) + base
            )

        }

        return edits

    }

    /**
     * Formats logical initializers and lambda expressions, keeping the first operand beside equals when it fits.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param margin maximum preferred column from the language code style
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun expressions(source: PsiFile, margin: Int): TextEdits {

        val edits = TextEdits()

        for (value in PsiTreeUtil.collectElements( source ) { operator( it ) != null }) {

            if (generateSequence( value.parent ) { it.parent }.any { operator( it ) != null }) {

                continue
            }

            if (
                generateSequence( value.parent ) { it.parent }.any { condition( it ) === value } ||
                BlockLayout()
                .hasChainedCall( value )
            ) {

                continue
            }

            val flat = SyntaxLayout().flatInline( value ) ?: continue

            val owner = value.parent

            val equal = if (owner is KtProperty || owner is PsiVariable) {

                owner.node
                    .getChildren( null )
                    .map { it.psi }
                    .firstOrNull { it.text == "=" }
            } else null

            val base = indentation( if (equal == null) value else owner )

            var start = column( value )

            if (equal != null) {

                val first = operands( value, operator( value )!! ).firstOrNull() ?: continue

                val firstText = SyntaxLayout().flatInline( first ) ?: continue

                val inlineStart = column( equal ) + equal.textLength + SPACE_WIDTH

                val fits = inlineStart + firstText.length + OPERATOR_WIDTH <= margin

                start = if (fits) inlineStart else base.length + INDENT.length

                edits.gap( equal, value, if (fits) " " else NEW_LINE + base + INDENT )

            }

            val text = if (start + flat.length <= margin) flat else render(value, start, base + INDENT, margin, false)
                ?: continue

            edits.replace( value.textRange, text )

        }

        return edits

    }

    /**
     * Plans leading and trailing blank lines for nonempty multiline Kotlin lambda bodies.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun lambdas(source: PsiFile): TextEdits {

        val edits = TextEdits()

        for (lambda in PsiTreeUtil.findChildrenOfType( source, KtLambdaExpression::class.java )) {

            if (!lambda.text.contains( '\n' )) {

                continue
            }

            val literal = lambda.functionLiteral

            val statements = literal.bodyExpression?.statements ?: continue

            if (statements.isEmpty()) {

                continue
            }

            val tokens = literal.node
                .getChildren( null )
                .map { it.psi }

            val opening = tokens.firstOrNull { it.text == "{" } ?: continue

            val closing = tokens.lastOrNull { it.text == "}" } ?: continue

            val base = indentation( lambda )

            edits.gap( opening, statements.first(), NEW_LINE + NEW_LINE + base + INDENT )

            edits.gap( statements.last(), closing, NEW_LINE + NEW_LINE + base )

        }

        return edits

    }

    /**
     * Renders logical operands within the available width, preserving parentheses and trailing operators; unsafe text returns null.
     *
     * @param value logical expression or operand to inspect
     * @param start zero-based column of the first rendered operand
     * @param continuation indentation prefix applied to continuation rows
     * @param margin maximum preferred column from the language code style
     * @param separate whether each top-level operand must begin on its own row
     *
     * @return rendered text or null when this structure cannot be processed safely
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun render(value: PsiElement, start: Int, continuation: String, margin: Int, separate: Boolean): String? {

        val sign = operator( value ) ?: return SyntaxLayout().flatInline( value )

        val parts = operands(value, sign)

        val texts = parts.map { SyntaxLayout().flatInline( it ) ?: return null }

        val result = StringBuilder()

        var column = start

        for ( (index, part) in parts.withIndex() ) {

            val flat = texts[index]

            val suffix = if (index == parts.lastIndex) "" else " " + sign

            if (index > 0) {

                val next = column + SPACE_WIDTH + flat.length + suffix.length

                if (separate || next > margin) {

                    result.append( NEW_LINE + continuation )

                    column = continuation.length

                } else {

                    result.append( " " )

                    column += SPACE_WIDTH

                }
            }

            val parenthesized = when (part) {

                is KtParenthesizedExpression  -> part.expression

                is PsiParenthesizedExpression -> part.expression

                else                          -> null

            }

            val nested = parenthesized ?: part

            val text = if (column + flat.length + suffix.length > margin && operator( nested ) != null) {

                val inner =
                    render(nested, column + if (parenthesized == null) 0 else SPACE_WIDTH, continuation, margin, false)
                        ?: return null

                if (parenthesized == null) inner else "(" + inner + ")"

            } else flat

            result.append( text + suffix )

            column = if (text.contains( '\n' )) text.substringAfterLast(
                '\n'
            ).length + suffix.length else column + text.length + suffix.length

        }

        return result.toString()

    }

    /**
     * Flattens only nested occurrences of the same logical operator, preserving operand order and other precedence boundaries.
     *
     * @param value logical expression or operand to inspect
     * @param sign logical operator whose matching operands may be flattened
     *
     * @return matching elements in the traversal order described above
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun operands(value: PsiElement, sign: String): List<PsiElement> {

        if (operator( value ) != sign) {

            return listOf( value )

        }

        return when (value) {

            is KtBinaryExpression    -> listOfNotNull( value.left, value.right ).flatMap { operands(it, sign) }

            is PsiPolyadicExpression -> value.operands.flatMap { operands(it, sign) }

            else                     -> listOf( value )

        }
    }

    /**
     * Returns the supported logical operator spelling, or null for other expressions.
     *
     * @param value logical expression or operand to inspect
     *
     * @return rendered text or null when this structure cannot be processed safely
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun operator(value: PsiElement): String? {

        val text = when (value) {

            is KtBinaryExpression    -> value.operationReference.text

            is PsiPolyadicExpression -> value.operationTokenType.toString()

            else                     -> null

        }

        return when (text) {

            "&&", "ANDAND" -> "&&"

            "||", "OROR"   -> "||"

            else           -> null

        }
    }

    /**
     * Returns the condition child of a supported Java/Kotlin control statement, or null.
     *
     * @param owner control statement containing a condition
     *
     * @return matching PSI child, or null when unsupported
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun condition(owner: PsiElement): PsiElement? {

        return when (owner) {

            is KtIfExpression      -> owner.condition

            is KtWhileExpression   -> owner.condition

            is KtDoWhileExpression -> owner.condition

            is PsiIfStatement      -> owner.condition

            is PsiWhileStatement   -> owner.condition

            is PsiDoWhileStatement -> owner.condition

            else                   -> null

        }
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

        return offset - (element.containingFile.text.lastIndexOf( '\n', (offset - 1).coerceAtLeast( 0 ) ) + SPACE_WIDTH)

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

        val text = element.containingFile.text

        val offset = element.textRange.startOffset

        val start = text.lastIndexOf( '\n', (offset - 1).coerceAtLeast( 0 ) ) + SPACE_WIDTH

        return text.substring( start, offset )
            .takeWhile { it == ' ' || it == '\t' }
    }

    companion object {

        private const val NEW_LINE = "\n"

        private const val OPERATOR_WIDTH = 3
        private const val SPACE_WIDTH    = 1

        private const val INDENT = "    "

    }
}