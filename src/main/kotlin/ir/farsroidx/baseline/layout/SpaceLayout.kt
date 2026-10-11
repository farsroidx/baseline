package ir.farsroidx.baseline.layout

import ir.farsroidx.baseline.config.FormatOptions
import ir.farsroidx.baseline.edit.TextEdits
import ir.farsroidx.baseline.structure.MemberLayout

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*

/**
 * Plans block, statement, file-section and parameter-list whitespace for Java and Kotlin.
 *
 * PSI tokens identify boundaries; comments and multiline literals prevent unsafe flattening.
 * Nested parameter lists are processed by depth so outer wrapping uses committed inner layout.
 */
internal class SpaceLayout {

    /**
     * Plans class/block, adjacent-statement and file-section spacing.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @deprecated Use [spacing2] for the replacement revision.
     * @see spacing2
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    @Deprecated( "Use spacing2 instead." )
    fun spacing(source: PsiFile): TextEdits {

        val edits = TextEdits()

        val structure = MemberLayout()

        for (container in structure.containers( source )) {

            if (container !is PsiFile) block(container, edits)

            val children = container.children.filter { it !is PsiWhiteSpace && it.text != ";" }

            for ( (left, right) in children.zipWithNext() ) {

                val first = structure.field( left )

                val second = structure.field( right )

                if (first != null && second != null) {

                    val lines = if (first.group == second.group) SINGLE_LINE else BLANK_LINE

                    val indent = if (container is PsiFile) "" else blockIndentation( container ) + INDENT

                    edits.gap( left, right, lines + indent )

                }
            }
        }

        for (body in PsiTreeUtil.findChildrenOfType( source, PsiCodeBlock::class.java )) {

            block(body, edits)

            statements(
                body.statements.toList(), edits, body.parent is PsiMethod && (body.parent as PsiMethod).isConstructor,
                blockIndentation( body ) + INDENT
            )

        }

        for (body in PsiTreeUtil.findChildrenOfType( source, KtBlockExpression::class.java )) {

            block(body, edits)

            statements(body.statements, edits, body.parent is KtSecondaryConstructor, blockIndentation( body ) + INDENT)

        }

        headers(source, edits)

        return edits

    }

    @Deprecated( "Use spacing3 instead." )
    /**
         * Plans class/block, adjacent-statement and file-section spacing.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         *
         * @return unapplied edits against the current PSI snapshot
         *
         * @see spacing
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun spacing2(source: PsiFile): TextEdits {

        val edits = TextEdits()

        val structure = MemberLayout()

        for (container in structure.containers( source )) {

            if (container !is PsiFile) block2(container, edits)

            val children = container.children.filter { it !is PsiWhiteSpace && it.text != ";" }

            for ( (left, right) in children.zipWithNext() ) {

                val first = structure.field( left )

                val second = structure.field( right )

                if (first != null && second != null) {

                    val lines = if (first.group == second.group) SINGLE_LINE else BLANK_LINE

                    val indent = if (container is PsiFile) "" else blockIndentation2( container ) + INDENT

                    edits.gap( left, right, lines + indent )

                }
            }
        }

        for (body in PsiTreeUtil.findChildrenOfType( source, PsiCodeBlock::class.java )) {

            block2(body, edits)

            statements2(
                body.statements.toList(), edits, body.parent is PsiMethod && (body.parent as PsiMethod).isConstructor,
                blockIndentation2( body ) + INDENT
            )

        }

        for (body in PsiTreeUtil.findChildrenOfType( source, KtBlockExpression::class.java )) {

            block2(body, edits)

            statements2(
                body.statements, edits, body.parent is KtSecondaryConstructor, blockIndentation2( body ) + INDENT
            )

        }

        headers(source, edits)

        return edits

    }

    /**
     * Returns the maximum nesting depth among declaration and call parameter lists; empty files return zero.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return computed count or zero-based source column
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun parameterDepth(source: PsiFile): Int {

        return parameterLists( source ).maxOfOrNull { depth( it ) } ?: 0

    }

    @Deprecated( "Use wrapping2 instead." )
    /**
         * Plans depth-specific declaration/call list wrapping without changing item order.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param rightMargin maximum preferred column from the language code style
         * @param level parameter-list nesting depth to process in this batch
         *
         * @return unapplied edits against the current PSI snapshot
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun wrapping(source: PsiFile, rightMargin: Int, level: Int): TextEdits {

        val edits = TextEdits()

        for (list in parameterLists( source ).filter { depth( it ) == level }) {

            val items: List<PsiElement> = when (list) {

                is PsiParameterList    -> list.parameters.toList()

                is PsiExpressionList   -> list.expressions.toList()

                is KtParameterList     -> list.parameters

                is KtValueArgumentList -> list.arguments

                else                   -> emptyList()

            }

            parameters(list, items, rightMargin, edits)

        }

        return edits

    }

    @Deprecated( "Use wrapping3 instead." )
    /**
         * Plans depth-specific declaration/call list wrapping without changing item order.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param rightMargin maximum preferred column from the language code style
         * @param level parameter-list nesting depth to process in this batch
         *
         * @return unapplied edits against the current PSI snapshot
         *
         * @see wrapping
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun wrapping2(source: PsiFile, rightMargin: Int, level: Int): TextEdits {

        val edits = TextEdits()

        for (list in parameterLists( source ).filter { depth( it ) == level }) {

            val items: List<PsiElement> = when (list) {

                is PsiParameterList    -> list.parameters.toList()

                is PsiExpressionList   -> list.expressions.toList()

                is KtParameterList     -> list.parameters

                is KtValueArgumentList -> list.arguments

                else                   -> emptyList()

            }

            parameters2(list, items, rightMargin, edits)

        }

        return edits

    }

    @Deprecated( "Use wrapping4 instead." )
    /**
         * Plans depth-specific declaration/call list wrapping without changing item order.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param rightMargin maximum preferred column from the language code style
         * @param level parameter-list nesting depth to process in this batch
         *
         * @return unapplied edits against the current PSI snapshot
         *
         * @see wrapping2
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun wrapping3(source: PsiFile, rightMargin: Int, level: Int): TextEdits {

        val edits = TextEdits()

        for (list in parameterLists( source ).filter { depth( it ) == level }) {

            val items: List<PsiElement> = when (list) {

                is PsiParameterList    -> list.parameters.toList()

                is PsiExpressionList   -> list.expressions.toList()

                is KtParameterList     -> list.parameters

                is KtValueArgumentList -> list.arguments

                else                   -> emptyList()

            }

            parameters3(list, items, rightMargin, edits)

        }

        return edits

    }

    @Deprecated( "Use wrapping5 instead." )
    /**
         * Plans depth-specific declaration/call list wrapping without changing item order.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param rightMargin maximum preferred column from the language code style
         * @param level parameter-list nesting depth to process in this batch
         *
         * @return unapplied edits against the current PSI snapshot
         *
         * @see wrapping3
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun wrapping4(source: PsiFile, rightMargin: Int, level: Int): TextEdits {

        val edits = TextEdits()

        for (list in parameterLists( source ).filter { depth( it ) == level }) {

            val items: List<PsiElement> = when (list) {

                is PsiParameterList    -> list.parameters.toList()

                is PsiExpressionList   -> list.expressions.toList()

                is KtParameterList     -> list.parameters

                is KtValueArgumentList -> list.arguments

                else                   -> emptyList()

            }

            parameters4(list, items, rightMargin, edits)

        }

        return edits

    }

    @Deprecated( "Use wrapping6 instead." )
    /**
         * Plans depth-specific declaration/call list wrapping without changing item order.
         *
         * This revision supports the current layout contract; earlier revisions remain available.
         *
         * @param source committed Java/Kotlin PSI file to inspect or format
         * @param rightMargin maximum preferred column from the language code style
         * @param level parameter-list nesting depth to process in this batch
         *
         * @return unapplied edits against the current PSI snapshot
         *
         * @see wrapping4
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
    fun wrapping5(source: PsiFile, rightMargin: Int, level: Int): TextEdits {

        val edits = TextEdits()

        for (list in parameterLists( source ).filter { depth( it ) == level }) {

            val items: List<PsiElement> = when (list) {

                is PsiParameterList    -> list.parameters.toList()

                is PsiExpressionList   -> list.expressions.toList()

                is KtParameterList     -> list.parameters

                is KtValueArgumentList -> list.arguments

                else                   -> emptyList()

            }

            parameters5(list, items, rightMargin, edits)

        }

        return edits

    }

    /**
     * Places the closing parenthesis of multiline Kotlin if conditions at the owning statement indentation.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun controlClosings(source: PsiFile): TextEdits {

        val edits = TextEdits()

        for (expression in PsiTreeUtil.findChildrenOfType( source, KtIfExpression::class.java )) {

            val condition = expression.condition ?: continue

            if (!condition.text.contains( '\n' )) {

                continue
            }

            val closing = expression.node
                .getChildren( null )
                .map { it.psi }
                .firstOrNull { it.text == ")" } ?: continue

            edits.gap( condition, closing, SINGLE_LINE + indentation( expression ) )

        }

        return edits

    }

    /**
     * Collects Java declaration/call lists and Kotlin declaration/value-argument lists.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return matching elements in the traversal order described above
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun parameterLists(source: PsiFile): List<PsiElement> {

        return PsiTreeUtil.findChildrenOfType( source, PsiParameterList::class.java )
            .toList() + PsiTreeUtil.findChildrenOfType( source, PsiExpressionList::class.java )
                .toList() + PsiTreeUtil.findChildrenOfType( source, KtParameterList::class.java )
                    .toList() + PsiTreeUtil.findChildrenOfType( source, KtValueArgumentList::class.java )
                        .toList()

    }

    /**
     * Counts enclosing parameter/argument lists to determine nested processing order.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return computed count or zero-based source column
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun depth(element: PsiElement): Int {

        var parent = element.parent

        var level = 0

        while (parent != null) {

            if (
                parent is PsiParameterList ||
                parent is PsiExpressionList ||
                parent is KtParameterList ||
                parent is KtValueArgumentList
            ) {

                level++

            }

            parent = parent.parent

        }

        return level

    }

    @Deprecated( "Use block2 instead." )
    /**
     * Appends block-boundary and class-member whitespace edits using structural brace children.
     *
     * @param container PSI owner whose direct children define layout boundaries
     * @param edits batch receiving planned replacements; the source document is not changed here
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun block(container: PsiElement, edits: TextEdits) {

        val children = container.node
            .getChildren( null )
            .map { it.psi }
            .filter { it !is PsiWhiteSpace && it.text != ";" }

        val opening = children.firstOrNull { it.text == "{" } ?: return

        val closing = children.lastOrNull { it.text == "}" } ?: return

        val content = children
            .filter {

                it.textRange.startOffset > opening.textRange.startOffset &&
                    it.textRange.endOffset < closing.textRange.endOffset

            }

        val base = blockIndentation( container )

        if (content.isEmpty()) {

            edits.gap( opening, closing, BLANK_LINE + base )

            return
        }

        edits.gap( opening, content.first(), BLANK_LINE + base + INDENT )

        edits.gap( content.last(), closing, BLANK_LINE + base )

        for ( (left, right) in content.zipWithNext() ) {

            if (container is PsiClass || container is KtClassBody) edits.gap( left, right, BLANK_LINE + base + INDENT )

        }
    }

    @Deprecated( "Use block3 instead." )
    /**
     * Appends block-boundary and class-member whitespace edits using structural brace children.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param container PSI owner whose direct children define layout boundaries
     * @param edits batch receiving planned replacements; the source document is not changed here
     *
     * @see block
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun block2(container: PsiElement, edits: TextEdits) {

        val children = container.node
            .getChildren( null )
            .map { it.psi }
            .filter { it !is PsiWhiteSpace && it.text != ";" }

        val opening = children.firstOrNull { it.text == "{" } ?: return

        val closing = children.lastOrNull { it.text == "}" } ?: return

        val content = children
            .filter {

                it.textRange.startOffset > opening.textRange.startOffset &&
                    it.textRange.endOffset < closing.textRange.endOffset

            }

        val base = blockIndentation2( container )

        if (content.isEmpty()) {

            edits.gap( opening, closing, BLANK_LINE + base )

            return
        }

        edits.gap( opening, content.first(), BLANK_LINE + base + INDENT )

        edits.gap( content.last(), closing, BLANK_LINE + base )

        for ( (left, right) in content.zipWithNext() ) {

            if (container is PsiClass || container is KtClassBody) edits.gap( left, right, BLANK_LINE + base + INDENT )

        }
    }

    @Deprecated( "Use statements2 instead." )
    /**
     * Appends adjacency spacing, keeping related reads/writes and same-type constructor runs together.
     *
     * @param statements statements in original execution order
     * @param edits batch receiving planned replacements; the source document is not changed here
     * @param constructor whether the statements belong to a constructor body
     * @param indent indentation prefix for the statement rows
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun statements(statements: List<PsiElement>, edits: TextEdits, constructor: Boolean, indent: String) {

        for ( (left, right) in statements.zipWithNext() ) {

            val first = assignment( left )

            val second = assignment( right )

            val firstVariable = MemberLayout().localVariable( left )

            val secondVariable = MemberLayout().localVariable( right )

            val related = writes( left ).intersect( reads( right ) )
                .isNotEmpty()

            val lines = when {

                constructor && first != null && second != null && first.second == second.second                                 -> SINGLE_LINE

                constructor && first != null && second != null                                                                  -> BLANK_LINE

                constructor && firstVariable != null && secondVariable != null &&
                    firstVariable.second == secondVariable.second -> SINGLE_LINE

                constructor && firstVariable != null && secondVariable != null                                                  -> BLANK_LINE

                related                                                                                                         -> SINGLE_LINE

                else                                                                                                            -> BLANK_LINE

            }

            edits.gap( left, right, lines + indent )

        }
    }

    @Deprecated( "Use statements3 instead." )
    /**
     * Appends adjacency spacing, keeping related reads/writes and same-type constructor runs together.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param statements statements in original execution order
     * @param edits batch receiving planned replacements; the source document is not changed here
     * @param constructor whether the statements belong to a constructor body
     * @param indent indentation prefix for the statement rows
     *
     * @see statements
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun statements2(statements: List<PsiElement>, edits: TextEdits, constructor: Boolean, indent: String) {

        for ( (left, right) in statements.zipWithNext() ) {

            val first = assignment( left )

            val second = assignment( right )

            val firstVariable = MemberLayout().localVariable( left )

            val secondVariable = MemberLayout().localVariable( right )

            val related = writes( left ).intersect( reads( right ) )
                .isNotEmpty()

            val lines = when {

                constructor && first != null && second != null && first.second == second.second                                 -> SINGLE_LINE

                constructor && first != null && second != null                                                                  -> BLANK_LINE

                constructor && firstVariable != null && secondVariable != null &&
                    firstVariable.second == secondVariable.second -> SINGLE_LINE

                constructor && firstVariable != null && secondVariable != null                                                  -> BLANK_LINE

                left is KtNamedFunction || right is KtNamedFunction                                                             -> BLANK_LINE

                related                                                                                                         -> SINGLE_LINE

                else                                                                                                            -> BLANK_LINE

            }

            edits.gap( left, right, lines + indent )

        }
    }

    /**
     * Describes a this-qualified constructor-style assignment target and its type, or returns null.
     *
     * @param statement statement inspected for assignment, declaration and reference names
     *
     * @return declared name/type pair, or null when ineligible
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun assignment(statement: PsiElement): Pair<String, String>? {

        if (statement is PsiExpressionStatement) {

            val expression = statement.expression as? PsiAssignmentExpression ?: return null

            val reference = expression.lExpression as? PsiReferenceExpression ?: return null

            if (reference.qualifierExpression !is PsiThisExpression) {

                return null

            }

            return (reference.referenceName ?: return null) to (reference.type?.canonicalText ?: reference.text)

        }

        val expression = statement as? KtBinaryExpression ?: return null

        val target = expression.left as? KtDotQualifiedExpression ?: return null

        if (target.receiverExpression !is KtThisExpression || expression.operationReference.text != "=") {

            return null

        }

        val name = (target.selectorExpression as? KtNameReferenceExpression)?.getReferencedName() ?: return null

        val owner = PsiTreeUtil.getParentOfType( statement, KtClassOrObject::class.java )

        val property = owner?.body?.properties?.firstOrNull { it.name == name }

        return name to (property?.typeReference?.text ?: "<inferred:$name>")

    }

    /**
     * Collects declared/assigned reference names used by the conservative statement-adjacency heuristic.
     *
     * @param statement statement inspected for assignment, declaration and reference names
     *
     * @return collected reference names
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun writes(statement: PsiElement): Set<String> {

        val names = mutableSetOf<String>()

        names.addAll(
            PsiTreeUtil.findChildrenOfType( statement, PsiLocalVariable::class.java )
                .map { it.name }
        )

        names.addAll(
            PsiTreeUtil.findChildrenOfType( statement, KtProperty::class.java )
                .mapNotNull { it.name }
        )

        if (statement is KtProperty) {

            statement.name?.let { names.add( it ) }
        }

        for (assignment in PsiTreeUtil.findChildrenOfType( statement, PsiAssignmentExpression::class.java )) {

            (assignment.lExpression as? PsiReferenceExpression)?.referenceName?.let { names.add( it ) }
        }

        val assignment = statement as? KtBinaryExpression

        if (assignment?.operationReference?.text == "=") {

            val left = assignment.left

            val reference = if (left is KtDotQualifiedExpression) left.selectorExpression else left

            (reference as? KtNameReferenceExpression)?.getReferencedName()
                ?.let { names.add( it ) }
        }

        return names

    }

    /**
     * Collects reference names used to recognize immediately related statements; this is not full data-flow analysis.
     *
     * @param statement statement inspected for assignment, declaration and reference names
     *
     * @return collected reference names
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun reads(statement: PsiElement): Set<String> {

        return PsiTreeUtil.findChildrenOfType( statement, PsiReferenceExpression::class.java )
            .mapNotNull { it.referenceName }
            .toSet() + PsiTreeUtil.findChildrenOfType( statement, KtNameReferenceExpression::class.java )
                .map { it.getReferencedName() }
                .toSet()

    }

    @Deprecated( "Use parameters2 instead." )
    /**
     * Appends safe width-based parameter-list edits while preserving comments, item order and trailing commas.
     *
     * @param list declaration or call list whose parentheses and items are formatted
     * @param items items in their original declaration or evaluation order
     * @param rightMargin maximum preferred column from the language code style
     * @param edits batch receiving planned replacements; the source document is not changed here
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun parameters(list: PsiElement, items: List<PsiElement>, rightMargin: Int, edits: TextEdits) {

        if (items.isEmpty()) {

            return
        }

        val children = list.node
            .getChildren( null )
            .map { it.psi }
            .filter { it !is PsiWhiteSpace }

        val opening = children.firstOrNull { it.text == "(" } ?: return

        val closing = children.lastOrNull { it.text == ")" } ?: return

        val owner = list.parent

        val source = list.containingFile.text

        val lineStart = source.lastIndexOf( '\n', (opening.textRange.startOffset - 1).coerceAtLeast( 0 ) ) + 1

        val prefix = source.substring( lineStart, opening.textRange.startOffset )

        val flat = items.joinToString( ", " ) { it.text.replace( Regex( "\\s+" ), " " ) }

        val base = indentation( owner )

        val lineEnd = source.indexOf( '\n', closing.textRange.endOffset )
            .let {

                if (it < 0) source.length else it

            }

        val suffix = source.substring( closing.textRange.endOffset, lineEnd )
            .substringBefore( '{' )

        val width = prefix.length + flat.length + suffix.length + PARENTHESES_WIDTH

        if (width <= rightMargin && !list.text.contains( '\n' )) {

            return
        }

        edits.gap( opening, items.first(), SINGLE_LINE + base + INDENT )

        for (item in items.drop( 1 )) {

            var previous = item.prevSibling

            while (previous is PsiWhiteSpace) {

                previous = previous.prevSibling

            }

            if (previous != null) edits.gap( previous, item, SINGLE_LINE + base + INDENT )

        }

        val last = children.lastOrNull { it.textRange.endOffset <= closing.textRange.startOffset && it !== opening }

        if (last != null) edits.gap( last, closing, SINGLE_LINE + base )

    }

    @Deprecated( "Use parameters3 instead." )
    /**
     * Appends safe width-based parameter-list edits while preserving comments, item order and trailing commas.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param list declaration or call list whose parentheses and items are formatted
     * @param items items in their original declaration or evaluation order
     * @param rightMargin maximum preferred column from the language code style
     * @param edits batch receiving planned replacements; the source document is not changed here
     *
     * @see parameters
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun parameters2(list: PsiElement, items: List<PsiElement>, rightMargin: Int, edits: TextEdits) {

        if (items.isEmpty()) {

            return
        }

        val children = list.node
            .getChildren( null )
            .map { it.psi }
            .filter { it !is PsiWhiteSpace }

        val opening = children.firstOrNull { it.text == "(" } ?: return

        val closing = children.lastOrNull { it.text == ")" } ?: return

        val owner = list.parent

        val source = list.containingFile.text

        val lineStart = source.lastIndexOf( '\n', (opening.textRange.startOffset - 1).coerceAtLeast( 0 ) ) + 1

        val prefix = source.substring( lineStart, opening.textRange.startOffset )

        val flat = items.joinToString( ", " ) { it.text.replace( Regex( "\\s+" ), " " ) }

        val base = indentation( owner )

        val lineEnd = source.indexOf( '\n', closing.textRange.endOffset )
            .let {

                if (it < 0) source.length else it

            }

        val suffix = source.substring( closing.textRange.endOffset, lineEnd )
            .substringBefore( '{' )

        val width = prefix.length + flat.length + suffix.length + PARENTHESES_WIDTH

        val comments = PsiTreeUtil.findChildrenOfType( list, PsiComment::class.java )

        val trailingComma = children.dropLast( 1 )
            .lastOrNull()?.text == ","

        if (
            width <= rightMargin &&
            items.none { it.text.contains( '\n' ) } &&
            comments.isEmpty() &&
            !trailingComma
        ) {

            edits.replace(
                list.textRange,
                "(" + items.joinToString( ", " ) { it.text } + ")"
            )

            return
        }

        edits.gap( opening, items.first(), SINGLE_LINE + base + INDENT )

        for (item in items.drop( 1 )) {

            var previous = item.prevSibling

            while (previous is PsiWhiteSpace) {

                previous = previous.prevSibling

            }

            if (previous != null) edits.gap( previous, item, SINGLE_LINE + base + INDENT )

        }

        val last = children.lastOrNull { it.textRange.endOffset <= closing.textRange.startOffset && it !== opening }

        if (last != null) edits.gap( last, closing, SINGLE_LINE + base )

    }

    @Deprecated( "Use parameters4 instead." )
    /**
     * Appends safe width-based parameter-list edits while preserving comments, item order and trailing commas.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param list declaration or call list whose parentheses and items are formatted
     * @param items items in their original declaration or evaluation order
     * @param rightMargin maximum preferred column from the language code style
     * @param edits batch receiving planned replacements; the source document is not changed here
     *
     * @see parameters2
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun parameters3(list: PsiElement, items: List<PsiElement>, rightMargin: Int, edits: TextEdits) {

        if (items.isEmpty()) {

            return
        }

        val children = list.node
            .getChildren( null )
            .map { it.psi }
            .filter { it !is PsiWhiteSpace }

        val opening = children.firstOrNull { it.text == "(" } ?: return

        val closing = children.lastOrNull { it.text == ")" } ?: return

        val owner = list.parent

        val source = list.containingFile.text

        val lineStart = source.lastIndexOf( '\n', (opening.textRange.startOffset - 1).coerceAtLeast( 0 ) ) + 1

        val prefix = source.substring( lineStart, opening.textRange.startOffset )

        val flat = items.joinToString( ", " ) { it.text.replace( Regex( "\\s+" ), " " ) }

        val base = indentation( owner )

        val lineEnd = source.indexOf( '\n', closing.textRange.endOffset )
            .let {

                if (it < 0) source.length else it

            }

        val suffix = source.substring( closing.textRange.endOffset, lineEnd )
            .substringBefore( '{' )

        val call = list is PsiExpressionList || list is KtValueArgumentList

        val padded = call && items.size == 1

        val paddingWidth = if (padded) PARENTHESES_WIDTH else 0

        val chained = call && items.any { BlockLayout().hasChainedCall( it ) }

        val width = prefix.length + flat.length + suffix.length + PARENTHESES_WIDTH + paddingWidth

        val comments = PsiTreeUtil.findChildrenOfType( list, PsiComment::class.java )

        val trailingComma = children.dropLast( 1 )
            .lastOrNull()?.text == ","

        if (
            !chained &&
            width <= rightMargin &&
            items.none {

                it.text.contains( '\n' )

            } &&
            comments.isEmpty() &&
            !trailingComma
        ) {

            val padding = if (padded) " " else ""

            edits.replace(
                list.textRange,
                "(" + padding + items.joinToString( ", " ) { it.text } + padding + ")"
            )

            return
        }

        edits.gap( opening, items.first(), SINGLE_LINE + base + INDENT )

        for (item in items.drop( 1 )) {

            var previous = item.prevSibling

            while (previous is PsiWhiteSpace) {

                previous = previous.prevSibling

            }

            if (previous != null) edits.gap( previous, item, SINGLE_LINE + base + INDENT )

        }

        val last = children.lastOrNull { it.textRange.endOffset <= closing.textRange.startOffset && it !== opening }

        if (last != null) edits.gap( last, closing, SINGLE_LINE + base )

    }

    @Deprecated( "Use parameters5 instead." )
    /**
     * Appends safe width-based parameter-list edits while preserving comments, item order and trailing commas.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param list declaration or call list whose parentheses and items are formatted
     * @param items items in their original declaration or evaluation order
     * @param rightMargin maximum preferred column from the language code style
     * @param edits batch receiving planned replacements; the source document is not changed here
     *
     * @see parameters3
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun parameters4(list: PsiElement, items: List<PsiElement>, rightMargin: Int, edits: TextEdits) {

        if (items.isEmpty()) {

            return
        }

        val children = list.node
            .getChildren( null )
            .map { it.psi }
            .filter { it !is PsiWhiteSpace }

        val opening = children.firstOrNull { it.text == "(" } ?: return

        val closing = children.lastOrNull { it.text == ")" } ?: return

        val owner = list.parent

        val source = list.containingFile.text

        val lineStart = source.lastIndexOf( '\n', (opening.textRange.startOffset - 1).coerceAtLeast( 0 ) ) + 1

        val prefix = source.substring( lineStart, opening.textRange.startOffset )

        val flatItems = items.map { SyntaxLayout().flatInline( it ) }

        val flat = flatItems.filterNotNull()
            .joinToString( ", " )

        val base = indentation( owner )

        val lineEnd = source.indexOf( '\n', closing.textRange.endOffset )
            .let {

                if (it < 0) source.length else it

            }

        val suffix = source.substring( closing.textRange.endOffset, lineEnd )
            .substringBefore( '{' )

        val call = list is PsiExpressionList || list is KtValueArgumentList

        val padded = call && items.size == 1

        val paddingWidth = if (padded) PARENTHESES_WIDTH else 0

        val chained = call && items.any { BlockLayout().hasChainedCall( it ) }

        val annotated = items
            .any {

                it is PsiParameter && it.modifierList?.annotations?.isNotEmpty() == true ||
                    it is KtParameter && it.annotationEntries.isNotEmpty()

            }

        val width = prefix.length + flat.length + suffix.length + PARENTHESES_WIDTH + paddingWidth

        val comments = PsiTreeUtil.findChildrenOfType( list, PsiComment::class.java )

        val trailingComma = children.dropLast( 1 )
            .lastOrNull()?.text == ","

        if (
            !chained &&
            !annotated &&
            width <= rightMargin &&
            flatItems.all { it != null } &&
            comments.isEmpty() &&
            !trailingComma
        ) {

            val padding = if (padded) " " else ""

            edits.replace(
                list.textRange,
                "(" + padding + flatItems.filterNotNull()
                    .joinToString( ", " ) + padding + ")"
            )

            return
        }

        edits.gap( opening, items.first(), SINGLE_LINE + base + INDENT )

        for (item in items.drop( 1 )) {

            var previous = item.prevSibling

            while (previous is PsiWhiteSpace) {

                previous = previous.prevSibling

            }

            if (previous != null) edits.gap( previous, item, SINGLE_LINE + base + INDENT )

        }

        val last = children.lastOrNull { it.textRange.endOffset <= closing.textRange.startOffset && it !== opening }

        if (last != null) edits.gap( last, closing, SINGLE_LINE + base )

    }

    @Deprecated( "Use parameters6 instead." )
    /**
     * Appends safe width-based parameter-list edits while preserving comments, item order and trailing commas.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param list declaration or call list whose parentheses and items are formatted
     * @param items items in their original declaration or evaluation order
     * @param rightMargin maximum preferred column from the language code style
     * @param edits batch receiving planned replacements; the source document is not changed here
     *
     * @see parameters4
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun parameters5(list: PsiElement, items: List<PsiElement>, rightMargin: Int, edits: TextEdits) {

        if (items.isEmpty()) {

            return
        }

        val children = list.node
            .getChildren( null )
            .map { it.psi }
            .filter { it !is PsiWhiteSpace }

        val opening = children.firstOrNull { it.text == "(" } ?: return

        val closing = children.lastOrNull { it.text == ")" } ?: return

        val owner = list.parent

        val source = list.containingFile.text

        val lineStart = source.lastIndexOf( '\n', (opening.textRange.startOffset - 1).coerceAtLeast( 0 ) ) + 1

        val prefix = source.substring( lineStart, opening.textRange.startOffset )

        val flatItems = items.map { SyntaxLayout().flatInline( it ) }

        val flat = flatItems.filterNotNull()
            .joinToString( ", " )

        val base = prefix.takeWhile { it == ' ' || it == '\t' }

        val lineEnd = source.indexOf( '\n', closing.textRange.endOffset )
            .let {

                if (it < 0) source.length else it

            }

        val suffix = source.substring( closing.textRange.endOffset, lineEnd )
            .substringBefore( '{' )

        val call = list is PsiExpressionList || list is KtValueArgumentList

        val padded = call && items.size == 1

        val paddingWidth = if (padded) PARENTHESES_WIDTH else 0

        val chained = call && items.any { BlockLayout().hasChainedCall( it ) }

        val annotated = items
            .any {

                it is PsiParameter && it.modifierList?.annotations?.isNotEmpty() == true ||
                    it is KtParameter && it.annotationEntries.isNotEmpty()

            }

        val width = prefix.length + flat.length + suffix.length + PARENTHESES_WIDTH + paddingWidth

        val comments = PsiTreeUtil.findChildrenOfType( list, PsiComment::class.java )

        val trailingComma = children.dropLast( 1 )
            .lastOrNull()?.text == ","

        if (
            !chained &&
            !annotated &&
            width <= rightMargin &&
            flatItems.all { it != null } &&
            comments.isEmpty() &&
            !trailingComma
        ) {

            val padding = if (padded) " " else ""

            edits.replace(
                list.textRange,
                "(" + padding + flatItems.filterNotNull()
                    .joinToString( ", " ) + padding + ")"
            )

            return
        }

        val packed =
            call && ChainLayout()
                .isLink( list ) && !chained && !annotated && comments.isEmpty() && !trailingComma && flatItems.all { it != null }

        if (packed) {

            var column = base.length + INDENT.length

            edits.gap( opening, items.first(), SINGLE_LINE + base + INDENT )

            for ( (index, item) in items.withIndex() ) {

                val text = flatItems[index] ?: continue

                val commaWidth = if (index == items.lastIndex) 0 else COMMA_WIDTH

                if (index > 0) {

                    var comma = item.prevSibling

                    while (comma is PsiWhiteSpace) {

                        comma = comma.prevSibling

                    }

                    val sameLine = column + SPACE_WIDTH + text.length + commaWidth <= rightMargin

                    if (comma != null) {

                        edits.gap( comma, item, if (sameLine) " " else SINGLE_LINE + base + INDENT )

                    }

                    column = if (sameLine) column + SPACE_WIDTH else base.length + INDENT.length

                }

                edits.replace( item.textRange, text )

                column += text.length + commaWidth

            }

            edits.gap( items.last(), closing, SINGLE_LINE + base )

            return
        }

        edits.gap( opening, items.first(), SINGLE_LINE + base + INDENT )

        for (item in items.drop( 1 )) {

            var previous = item.prevSibling

            while (previous is PsiWhiteSpace) {

                previous = previous.prevSibling

            }

            if (previous != null) edits.gap( previous, item, SINGLE_LINE + base + INDENT )

        }

        val last = children.lastOrNull { it.textRange.endOffset <= closing.textRange.startOffset && it !== opening }

        if (last != null) edits.gap( last, closing, SINGLE_LINE + base )

    }

    /**
     * Separates package/import sections and top-level declarations without inserting whitespace inside documentation.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param edits batch receiving planned replacements; the source document is not changed here
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun headers(source: PsiFile, edits: TextEdits) {

        val children = source.children.filter { it !is PsiWhiteSpace && it.text.isNotBlank() }

        for ( (left, right) in children.zipWithNext() ) {

            if (MemberLayout().field( left ) != null && MemberLayout().field( right ) != null) {

                continue
            }

            if (
                left is PsiPackageStatement ||
                left is PsiImportList ||
                left is KtPackageDirective ||
                left is KtImportList ||
                left is PsiClass ||
                left is KtDeclaration
            ) {

                edits.gap( left, right, BLANK_LINE )

            }
        }
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

        val lineStart = source.lastIndexOf( '\n', (offset - 1).coerceAtLeast( 0 ) ) + 1

        return source.substring( lineStart, offset )
            .takeWhile { it == ' ' || it == '\t' }
    }

    @Deprecated( "Use blockIndentation2 instead." )
    /**
     * Uses declaration indentation for owned bodies and the block line indentation for other containers.
     *
     * @param container PSI owner whose direct children define layout boundaries
     *
     * @return computed source text or whitespace prefix
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun blockIndentation(container: PsiElement): String {

        val parent = container.parent

        return if (
            parent is PsiMethod ||
            parent is KtNamedFunction ||
            parent is KtSecondaryConstructor ||
            parent is KtClassOrObject
        ) {

            indentation( parent )

        } else indentation( container )

    }

    /**
     * Uses declaration indentation for owned bodies and the block line indentation for other containers.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param container PSI owner whose direct children define layout boundaries
     *
     * @return computed source text or whitespace prefix
     *
     * @see blockIndentation
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun blockIndentation2(container: PsiElement): String {

        val parent = container.parent

        return if (
            parent is PsiMethod ||
            parent is KtNamedFunction ||
            parent is KtSecondaryConstructor ||
            parent is KtClassOrObject ||
            parent is KtFunctionLiteral
        ) {

            indentation( parent )

        } else indentation( container )

    }

    /**
     * Appends block-boundary and class-member whitespace edits using structural brace children.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param container PSI owner whose direct children define layout boundaries
     * @param edits batch receiving planned replacements; the source document is not changed here
     * @param options project preferences captured for the current operation
     *
     * @see block2
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun block3(container: PsiElement, edits: TextEdits, options: FormatOptions) {

        val children = container.node
            .getChildren( null )
            .map { it.psi }
            .filter { it !is PsiWhiteSpace && it.text != ";" }

        val opening = children.firstOrNull { it.text == "{" } ?: return

        val closing = children.lastOrNull { it.text == "}" } ?: return

        val content = children
            .filter {

                it.textRange.startOffset > opening.textRange.startOffset &&
                    it.textRange.endOffset < closing.textRange.endOffset

            }

        val base = blockIndentation2( container )

        if (content.isEmpty()) {

            edits.gap( opening, closing, BLANK_LINE + base )

            return
        }

        edits.gap(
            opening, content.first(), (if (options.blankAfterOpeningBrace) BLANK_LINE else SINGLE_LINE) + base + INDENT
        )

        edits.gap( content.last(), closing, (if (options.blankBeforeClosingBrace) BLANK_LINE else SINGLE_LINE) + base )

        for ( (left, right) in content.zipWithNext() ) {

            if (options.spaceMembers && (container is PsiClass || container is KtClassBody)) {

                edits.gap( left, right, BLANK_LINE + base + INDENT )

            }
        }
    }

    /**
     * Appends adjacency spacing, keeping related reads/writes and same-type constructor runs together.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param statements statements in original execution order
     * @param edits batch receiving planned replacements; the source document is not changed here
     * @param constructor whether the statements belong to a constructor body
     * @param indent indentation prefix for the statement rows
     * @param options project preferences captured for the current operation
     *
     * @see statements2
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun statements3(
        statements: List<PsiElement>,
        edits: TextEdits,
        constructor: Boolean,
        indent: String,
        options: FormatOptions
    ) {

        for ( (left, right) in statements.zipWithNext() ) {

            val first = assignment( left )

            val second = assignment( right )

            val firstVariable = MemberLayout().localVariable( left )

            val secondVariable = MemberLayout().localVariable( right )

            val related = writes( left ).intersect( reads( right ) )
                .isNotEmpty()

            val lines = when {

                options.groupConstructorVariables && constructor && first != null && second != null &&
                    first.second == second.second                                 -> SINGLE_LINE

                options.groupConstructorVariables && constructor && first != null && second != null                                                                  -> BLANK_LINE

                options.groupConstructorVariables && constructor && firstVariable != null && secondVariable != null &&
                    firstVariable.second == secondVariable.second -> SINGLE_LINE

                options.groupConstructorVariables && constructor && firstVariable != null && secondVariable != null                                                  -> BLANK_LINE

                options.spaceMembers && (left is KtNamedFunction || right is KtNamedFunction)                                                                        -> BLANK_LINE

                related                                                                                                                                              -> SINGLE_LINE

                else                                                                                                                                                 -> if (options.spaceStatements) BLANK_LINE else SINGLE_LINE

            }

            edits.gap( left, right, lines + indent )

        }
    }

    /**
     * Plans class/block, adjacent-statement and file-section spacing.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param options project preferences captured for the current operation
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @see spacing2
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun spacing3(source: PsiFile, options: FormatOptions): TextEdits {

        val edits = TextEdits()

        val structure = MemberLayout()

        for (container in structure.containers( source )) {

            if (container !is PsiFile) block3(container, edits, options)

        }

        for (body in PsiTreeUtil.findChildrenOfType( source, PsiCodeBlock::class.java )) {

            block3(body, edits, options)

            statements3(
                body.statements.toList(), edits, body.parent is PsiMethod && (body.parent as PsiMethod).isConstructor,
                blockIndentation2( body ) + INDENT, options
            )

        }

        for (body in PsiTreeUtil.findChildrenOfType( source, KtBlockExpression::class.java )) {

            block3(body, edits, options)

            statements3(
                body.statements, edits, body.parent is KtSecondaryConstructor, blockIndentation2( body ) + INDENT,
                options
            )

        }

        if (options.spaceFileSections) headers(source, edits)

        return edits

    }

    /**
     * Appends safe width-based parameter-list edits while preserving comments, item order and trailing commas.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param list declaration or call list whose parentheses and items are formatted
     * @param items items in their original declaration or evaluation order
     * @param rightMargin maximum preferred column from the language code style
     * @param edits batch receiving planned replacements; the source document is not changed here
     * @param options project preferences captured for the current operation
     *
     * @see parameters5
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun parameters6(
        list: PsiElement,
        items: List<PsiElement>,
        rightMargin: Int,
        edits: TextEdits,
        options: FormatOptions
    ) {

        if (items.isEmpty()) {

            return
        }

        val children = list.node
            .getChildren( null )
            .map { it.psi }
            .filter { it !is PsiWhiteSpace }

        val opening = children.firstOrNull { it.text == "(" } ?: return

        val closing = children.lastOrNull { it.text == ")" } ?: return

        val owner = list.parent

        val source = list.containingFile.text

        val lineStart = source.lastIndexOf( '\n', (opening.textRange.startOffset - 1).coerceAtLeast( 0 ) ) + 1

        val prefix = source.substring( lineStart, opening.textRange.startOffset )

        val flatItems = items.map { SyntaxLayout().flatInline( it ) }

        val flat = flatItems.filterNotNull()
            .joinToString( ", " )

        val base = prefix.takeWhile { it == ' ' || it == '\t' }

        val lineEnd = source.indexOf( '\n', closing.textRange.endOffset )
            .let {

                if (it < 0) source.length else it

            }

        val suffix = source.substring( closing.textRange.endOffset, lineEnd )
            .substringBefore( '{' )

        val call = list is PsiExpressionList || list is KtValueArgumentList

        val padded = options.padSingleArguments && call && items.size == 1

        val paddingWidth = if (padded) PARENTHESES_WIDTH else 0

        val chained = options.formatChains && call && items.any { BlockLayout().hasChainedCall( it ) }

        val annotated = options.wrapAnnotatedParameters && items
            .any {

                it is PsiParameter && it.modifierList?.annotations?.isNotEmpty() == true || it is KtParameter && it.annotationEntries.isNotEmpty()

            }

        val width = prefix.length + flat.length + suffix.length + PARENTHESES_WIDTH + paddingWidth

        val comments = PsiTreeUtil.findChildrenOfType( list, PsiComment::class.java )

        val trailingComma = children.dropLast( 1 )
            .lastOrNull()?.text == ","

        if (
            !options.collapseShortLists && !list.text.contains( '\n' ) && !chained && !annotated && width <= rightMargin
        ) {

            return
        }

        if (
            options.collapseShortLists &&
            !chained &&
            !annotated &&
            width <= rightMargin &&
            flatItems.all { it != null } &&
            comments.isEmpty() &&
            !trailingComma
        ) {

            val padding = if (padded) " " else ""

            edits.replace(
                list.textRange,
                "(" + padding + flatItems.filterNotNull()
                    .joinToString( ", " ) + padding + ")"
            )

            return
        }

        if ((call && !options.wrapCalls) || (!call && !options.wrapDeclarations)) {

            return
        }

        val packed =
            options.packCallArguments && call && ChainLayout()
                .isLink( list ) && !chained && !annotated && comments.isEmpty() && !trailingComma && flatItems.all { it != null }

        if (packed) {

            var column = base.length + INDENT.length

            edits.gap( opening, items.first(), SINGLE_LINE + base + INDENT )

            for ( (index, item) in items.withIndex() ) {

                val text = flatItems[index] ?: continue

                val commaWidth = if (index == items.lastIndex) 0 else COMMA_WIDTH

                if (index > 0) {

                    var comma = item.prevSibling

                    while (comma is PsiWhiteSpace) {

                        comma = comma.prevSibling

                    }

                    val sameLine = column + SPACE_WIDTH + text.length + commaWidth <= rightMargin

                    if (comma != null) {

                        edits.gap( comma, item, if (sameLine) " " else SINGLE_LINE + base + INDENT )

                    }

                    column = if (sameLine) column + SPACE_WIDTH else base.length + INDENT.length

                }

                edits.replace( item.textRange, text )

                column += text.length + commaWidth

            }

            edits.gap( items.last(), closing, SINGLE_LINE + base )

            return
        }

        edits.gap( opening, items.first(), SINGLE_LINE + base + INDENT )

        for (item in items.drop( 1 )) {

            var previous = item.prevSibling

            while (previous is PsiWhiteSpace) {

                previous = previous.prevSibling

            }

            if (previous != null) edits.gap( previous, item, SINGLE_LINE + base + INDENT )

        }

        val last = children.lastOrNull { it.textRange.endOffset <= closing.textRange.startOffset && it !== opening }

        if (last != null) edits.gap( last, closing, SINGLE_LINE + base )

    }

    /**
     * Plans depth-specific declaration/call list wrapping without changing item order.
     *
     * This revision supports the current layout contract; earlier revisions remain available.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     * @param rightMargin maximum preferred column from the language code style
     * @param level parameter-list nesting depth to process in this batch
     * @param options project preferences captured for the current operation
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @see wrapping5
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun wrapping6(source: PsiFile, rightMargin: Int, level: Int, options: FormatOptions): TextEdits {

        val edits = TextEdits()

        for (list in parameterLists( source ).filter { depth( it ) == level }) {

            val items: List<PsiElement> = when (list) {

                is PsiParameterList    -> list.parameters.toList()

                is PsiExpressionList   -> list.expressions.toList()

                is KtParameterList     -> list.parameters

                is KtValueArgumentList -> list.arguments

                else                   -> emptyList()

            }

            parameters6(list, items, rightMargin, edits, options)

        }

        return edits

    }

    companion object {

        private const val SINGLE_LINE = "\n"
        private const val BLANK_LINE  = "\n\n"

        private const val PARENTHESES_WIDTH = 2
        private const val COMMA_WIDTH       = 1
        private const val SPACE_WIDTH       = 1

        private const val INDENT = "    "

    }
}