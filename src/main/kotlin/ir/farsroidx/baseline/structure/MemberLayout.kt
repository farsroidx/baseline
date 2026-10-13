package ir.farsroidx.baseline.structure

import ir.farsroidx.baseline.config.FormatOptions
import ir.farsroidx.baseline.edit.TextEdits
import ir.farsroidx.baseline.model.FieldEntry
import ir.farsroidx.baseline.model.FieldGroup

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

/**
 * Describes Java/Kotlin fields and constructs conservative member and constructor edits.
 *
 * Sorting is limited to eligible independent runs. Comments, business statements, repeated
 * assignment targets and unsafe initializers stop rearrangement. Method and constructor
 * parameter order is outside this class's responsibility and is preserved.
 */
internal class MemberLayout {

    /**
     * Orders contiguous Java/Kotlin modifier tokens without crossing annotation boundaries.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return unapplied edits against the current PSI snapshot
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun modifiers(source: PsiFile): TextEdits {

        val edits = TextEdits()

        val lists =
            PsiTreeUtil
                .findChildrenOfType( source, PsiModifierList::class.java ) + PsiTreeUtil
                .findChildrenOfType( source, KtModifierList::class.java )

        for (list in lists) {

            val order = if (list is PsiModifierList) JAVA_MODIFIERS else KOTLIN_MODIFIERS

            val tokens = list.node
                .getChildren( null )
                .map { it.psi }
                .filter { it.text in order }

            var run = mutableListOf<PsiElement>()

            /**
             * Emits the accumulated independent run, then clears it before the next semantic barrier.
             *
             * @author Mohammad ali Riazati
             * @since 1.0.0
             */
            fun flush() {

                val sorted = run.sortedBy { order.indexOf( it.text ) }

                for ( (original, replacement) in run.zip( sorted ) ) {

                    edits.replace( original.textRange, replacement.text )

                }

                run = mutableListOf()

            }

            for (token in tokens) {

                val previous = run.lastOrNull()

                if (
                    previous != null &&
                    !between( previous, token ).all { it.isWhitespace() }
                ) {

                    flush()

                }

                run.add( token )

            }

            flush()

        }

        return edits

    }

    /**
     * Returns the name/type of an eligible literal-initialized or uninitialized local declaration, or null.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return declared name/type pair, or null when ineligible
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun localVariable(element: PsiElement): Pair<String, String>? {

        if (element is PsiDeclarationStatement) {

            val variable = element.declaredElements.singleOrNull() as? PsiLocalVariable ?: return null

            if (
                variable.modifierList?.annotations?.isNotEmpty() == true ||
                variable.initializer != null && variable.initializer !is PsiLiteralExpression
            ) {

                return null

            }

            return variable.name to variable.type.canonicalText

        }

        if (element is KtProperty && element.isLocal && !element.hasDelegate() && element.annotationEntries.isEmpty()) {

            val initializer = element.initializer

            val safe = initializer == null || isLiteral( initializer )

            if (safe) {

                return (element.name ?: return null) to (element.typeReference?.text ?: inferredType( initializer ))

            }
        }

        return null

    }

    /**
     * Describes a field/property, its visibility/type group and its conservative rearrangement eligibility.
     *
     * @param element PSI element whose structural role or source position is inspected
     *
     * @return field metadata, or null when the element is not a supported field
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun field(element: PsiElement): FieldEntry? {

        if (element is PsiField && element !is PsiEnumConstant) {

            val access = when {

                element.hasModifierProperty( PsiModifier.PUBLIC )    -> PUBLIC

                element.hasModifierProperty( PsiModifier.PROTECTED ) -> PROTECTED

                element.hasModifierProperty( PsiModifier.PRIVATE )   -> PRIVATE

                else                                                 -> PACKAGE

            }

            val safe = (element.initializer == null || element.initializer is PsiLiteralExpression) && element.text
                .trimEnd()
                .endsWith( ";" ) && element.children.none { it.text == "," } && element.typeElement?.textRange
                    ?.let { element.textRange.contains( it ) } == true

            return FieldEntry(element, element.name, FieldGroup(access, element.type.canonicalText), safe)

        }
        if (element is KtProperty && !element.isLocal) {

            val access = when {

                element.hasModifier( KtTokens.PRIVATE_KEYWORD )   -> PRIVATE

                element.hasModifier( KtTokens.PROTECTED_KEYWORD ) -> PROTECTED

                element.hasModifier( KtTokens.INTERNAL_KEYWORD )  -> PACKAGE

                else                                              -> PUBLIC

            }

            val type = element.typeReference?.text ?: inferredType( element.initializer )

            val safe = !element.hasDelegate() && element.accessors.isEmpty() &&
                (element.initializer == null || isLiteral( element.initializer ))

            return FieldEntry(element, element.name ?: return null, FieldGroup(access, type), safe)

        }

        return null

    }

    /**
     * Returns Java classes, Kotlin class bodies and Kotlin file containers that may hold fields.
     *
     * @param source committed Java/Kotlin PSI file to inspect or format
     *
     * @return matching elements in the traversal order described above
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    fun containers(source: PsiFile): List<PsiElement> {

        return PsiTreeUtil.findChildrenOfType( source, PsiClass::class.java )
            .toList() + PsiTreeUtil.findChildrenOfType( source, KtClassBody::class.java )
                .toList() + if (source is KtFile) listOf( source ) else emptyList()

    }


    /**
     * Returns source text strictly between two ordered PSI elements.
     *
     * @param left element immediately before the gap
     * @param right element immediately after the gap
     *
     * @return computed source text or whitespace prefix
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun between(left: PsiElement, right: PsiElement): String {

        return left.containingFile.text.substring( left.textRange.endOffset, right.textRange.startOffset )

    }

    /**
     * Infers a grouping key from Kotlin literal spelling; unknown expressions retain an inferred marker.
     *
     * @param initializer literal initializer supplying a conservative type-group key
     *
     * @return computed source text or whitespace prefix
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun inferredType(initializer: KtExpression?): String {

        if (initializer is KtStringTemplateExpression) {

            return "String"

        }

        val text = initializer?.text ?: return "<inferred>"

        val kind = initializer.node.elementType

        return when {

            text == "true" || text == "false"                                 -> "Boolean"

            text.startsWith( "'" )                                            -> "Char"

            kind == KtNodeTypes.FLOAT_CONSTANT && text.endsWith( "F", true )    -> "Float"

            kind == KtNodeTypes.FLOAT_CONSTANT                                -> "Double"

            kind == KtNodeTypes.INTEGER_CONSTANT && text.endsWith( "UL", true ) -> "ULong"

            kind == KtNodeTypes.INTEGER_CONSTANT && text.endsWith( "U", true )  -> "UInt"

            kind == KtNodeTypes.INTEGER_CONSTANT && text.endsWith( "L", true )  -> "Long"

            kind == KtNodeTypes.INTEGER_CONSTANT                              -> "Int"

            text == "null"                                                    -> "Nothing?"

            else                                                              -> "<inferred>"

        }
    }

    /**
     * Recognizes Kotlin constants and string templates containing only literal entries.
     *
     * @param expression expression checked for constant or non-interpolated string syntax
     *
     * @return whether the documented structural predicate holds
     *
     * @author Mohammad ali Riazati
     * @since 1.0.0
     */
    private fun isLiteral(expression: KtExpression?): Boolean {

        return expression is KtConstantExpression || expression is KtStringTemplateExpression && expression.entries
            .all { it is KtLiteralStringTemplateEntry }
    }

    private fun reorderAssignments(
        statements: List<PsiElement>,
        edits: TextEdits,
        options: FormatOptions,
        sort: Boolean,
        describe: (PsiElement) -> Pair<String, String>?
    ) {

        var run = mutableListOf<Pair<PsiElement, Pair<String, String>>>()

        /**
         * Emits the accumulated independent run, then clears it before the next semantic barrier.
         *
         * @author Mohammad ali Riazati
         * @since 1.0.0
         */
        fun flush() {

            if (
                run.size > 1 &&
                run.map { it.second.first }
                .distinct().size == run.size
            ) {

                val widths = run.groupBy { it.second.second }
                    .mapValues { (_, entries) ->
                        entries.minOf { constructorWidth(it.first, it.second) }

                    }

                val sorted = run.sortedWith(
                    compareBy<Pair<PsiElement, Pair<String, String>>> {

                        if (options.groupConstructorVariables && sort) widths[it.second.second] else 0

                    }.thenBy { if (options.groupConstructorVariables) it.second.second.lowercase() else "" }
                        .thenBy { if (sort) constructorWidth(it.first, it.second) else 0 }
                )

                val replacement = sorted
                    .mapIndexed { index, entry ->

                        val separator =
                            if (index == 0) "" else if (
                                !options.groupConstructorVariables ||
                                sorted[index - 1].second.second == entry.second.second
                            ) "\n" else "\n\n"

                        separator + entry.first.text

                    }
                    .joinToString( "" )

                edits.replace(
                    TextRange(run.first().first.textRange.startOffset, run.last().first.textRange.endOffset),
                    replacement
                )

            }

            run = mutableListOf()

        }

        for (statement in statements) {

            val previous = run.lastOrNull()?.first

            val description = describe( statement )

            if (
                description == null ||
                previous != null && !between( previous, statement )
                    .all { it.isWhitespace() || it == ';' }
            ) {

                flush()

            }

            if (description != null) run.add( statement to description )

        }

        flush()

    }

    /**
     * Sorts eligible independent constructor assignment runs by ascending declared type/name width.
     *
     * @param source committed Java or Kotlin PSI file
     * @param options constructor sorting and grouping preferences
     * @return unapplied edits that preserve parameter order and business-statement barriers
     */
    fun assignments(source: PsiFile, options: FormatOptions): TextEdits {

        val edits = TextEdits()

        for (method in PsiTreeUtil.findChildrenOfType( source, PsiMethod::class.java )) {

            if (!method.isConstructor) {

                continue
            }

            val statements = method.body?.statements?.toList() ?: continue

            val fields = method.containingClass?.fields?.associateBy { it.name } ?: continue

            val parameters = method.parameterList.parameters
                .map { it.name }
                .toSet()

            reorderAssignments(statements, edits, options, options.sortConstructorAssignments) { statement ->

                val assignment = (statement as? PsiExpressionStatement)?.expression as? PsiAssignmentExpression

                val reference = assignment?.lExpression as? PsiReferenceExpression

                val target = fields[reference?.referenceName]

                val qualifier = reference?.qualifierExpression

                val right = assignment?.rExpression

                if (
                    assignment?.operationSign?.text == "=" && qualifier is PsiThisExpression && target != null &&
                    !target.hasModifierProperty( PsiModifier.VOLATILE ) &&
                    (right is PsiLiteralExpression ||
                    right is PsiReferenceExpression && right.qualifierExpression == null &&
                    right.referenceName in parameters)
                ) {

                    target.name to target.type.canonicalText

                } else null

            }
        }
        for (constructor in PsiTreeUtil.findChildrenOfType( source, KtSecondaryConstructor::class.java )) {

            val statements = constructor.bodyExpression?.statements ?: continue

            val properties =
                constructor.getContainingClassOrObject().body?.properties?.associateBy { it.name } ?: continue

            val parameters = constructor.valueParameters
                .mapNotNull { it.name }
                .toSet()

            reorderAssignments(statements, edits, options, options.sortConstructorAssignments) { statement ->

                val assignment = statement as? KtBinaryExpression

                val targetExpression = assignment?.left as? KtDotQualifiedExpression

                val name = (targetExpression?.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()

                val property = properties[name]

                val right = assignment?.right

                if (
                    assignment?.operationToken == KtTokens.EQ &&
                    targetExpression?.receiverExpression is KtThisExpression && property != null && property.isVar &&
                    property.accessors.isEmpty() && !property.hasDelegate() && property.annotationEntries.isEmpty() &&
                    !property.hasModifier( KtTokens.OPEN_KEYWORD ) &&
                    !property.hasModifier( KtTokens.OVERRIDE_KEYWORD ) &&
                    (isLiteral( right ) || right is KtNameReferenceExpression && right.getReferencedName() in parameters)
                ) {

                    name!! to (property.typeReference?.text ?: inferredType( property.initializer ))

                } else null

            }
        }

        return edits

    }

    /**
     * Groups and sorts eligible constructor-local declarations without reordering ordinary method bodies.
     *
     * @param source committed Java or Kotlin PSI file
     * @param options constructor-local grouping and sorting preferences
     * @return unapplied edits for safe declaration runs only
     */
    fun localVariables(source: PsiFile, options: FormatOptions): TextEdits {

        val edits = TextEdits()

        for (method in PsiTreeUtil.findChildrenOfType( source, PsiMethod::class.java )) {

            if (method.isConstructor) {

                reorderAssignments(
                    method.body?.statements?.toList() ?: emptyList(), edits, options, options.sortConstructorLocals,
                    ::localVariable
                )

            }
        }

        for (constructor in PsiTreeUtil.findChildrenOfType( source, KtSecondaryConstructor::class.java )) {

            reorderAssignments(
                constructor.bodyExpression?.statements ?: emptyList(), edits, options, options.sortConstructorLocals,
                ::localVariable
            )

        }

        return edits

    }

    private fun constructorWidth(element: PsiElement, description: Pair<String, String>): Int {

        val type = when (element) {

            is PsiDeclarationStatement -> {

                val variable = element.declaredElements.singleOrNull() as? PsiLocalVariable

                if (variable?.typeElement?.text == "var") variable.type.presentableText else variable?.typeElement?.text

            }

            is PsiExpressionStatement  -> {

                val method = PsiTreeUtil.getParentOfType( element, PsiMethod::class.java )

                method?.containingClass?.fields?.firstOrNull { it.name == description.first }?.typeElement?.text

            }

            is KtProperty              -> element.typeReference?.text

            is KtBinaryExpression      -> {

                val owner = PsiTreeUtil.getParentOfType( element, KtClassOrObject::class.java )

                owner?.body?.properties?.firstOrNull { it.name == description.first }?.typeReference?.text

            }

            else                       -> null

        }

        return CascadeWidth.measure( type ?: description.second, description.first )

    }

    companion object {

        private const val PROTECTED = 1
        private const val PACKAGE   = 2
        private const val PRIVATE   = 3
        private const val PUBLIC    = 0

        private val JAVA_MODIFIERS   = listOf(
            "public", "protected", "private", "abstract", "static", "final", "sealed", "non-sealed", "transient",
            "volatile", "synchronized", "native", "strictfp", "default"
        )
        private val KOTLIN_MODIFIERS = listOf(
            "public", "protected", "internal", "private", "expect", "actual", "final", "open", "abstract", "sealed",
            "const", "external", "override", "lateinit", "tailrec", "vararg", "suspend", "inner", "enum", "annotation",
            "companion", "inline", "value", "infix", "operator", "data", "in", "out", "noinline", "crossinline",
            "reified"
        )

    }
}