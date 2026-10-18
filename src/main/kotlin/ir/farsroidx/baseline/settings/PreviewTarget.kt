package ir.farsroidx.baseline.settings

import ir.farsroidx.baseline.config.FormatOption

/** Maps every preference to its example, including preferences whose current sample already matches the rule. */
internal object PreviewTarget {

    /** Returns an example token for [option] in the selected [kotlin] sample. */
    fun marker(option: FormatOption, kotlin: Boolean): String {

        return when (option) {

            FormatOption.ENABLED,
            FormatOption.JAVA_ENABLED,
            FormatOption.KOTLIN_ENABLED,
            FormatOption.CANONICAL_INDENTATION,
            FormatOption.END_OF_LINE_BRACES,
            FormatOption.BLANK_AFTER_OPENING_BRACE -> "class Sample"

            FormatOption.SEPARATE_STATIC_FIELDS,
            FormatOption.NORMALIZE_MODIFIERS,
            FormatOption.GROUP_FIELDS_BY_PREFIX -> "MAX_REQUEST_LIMIT"

            FormatOption.GROUP_FIELDS_BY_VISIBILITY,
            FormatOption.GROUP_FIELDS_BY_TYPE,
            FormatOption.SORT_FIELDS,
            FormatOption.SPACE_FIELD_GROUPS,
            FormatOption.ALIGN_FIELD_EQUALS -> "identifier"

            FormatOption.SORT_CONSTRUCTOR_ASSIGNMENTS,
            FormatOption.GROUP_CONSTRUCTOR_VARIABLES,
            FormatOption.SPACE_CONSTRUCTOR_ASSIGNMENTS -> "this."

            FormatOption.SORT_CONSTRUCTOR_LOCALS -> "localCount"

            FormatOption.EXPAND_SIMPLE_BLOCKS,
            FormatOption.SPACE_MEMBERS,
            FormatOption.SPACE_STATEMENTS,
            FormatOption.SEPARATE_STATEMENTS -> "terminate("

            FormatOption.BLANK_BEFORE_CLOSING_BRACE,
            FormatOption.COMPACT_CLOSING_BRACES,
            FormatOption.COMPACT_BARE_EXITS -> "stop("

            FormatOption.SPACE_FILE_SECTIONS -> "import "

            FormatOption.REMOVE_TRAILING_WHITESPACE -> "}"

            FormatOption.WRAP_DECLARATIONS,
            FormatOption.FORMAT_CONDITIONS -> "valid("

            FormatOption.WRAP_CALLS,
            FormatOption.COLLAPSE_SHORT_LISTS,
            FormatOption.PACK_CALL_ARGUMENTS,
            FormatOption.PAD_MULTIPLE_ARGUMENTS -> "mapper.convert"

            FormatOption.PAD_SINGLE_ARGUMENTS -> "getHeaders"

            FormatOption.PAD_ANNOTATION_ARRAYS,
            FormatOption.FORMAT_ANNOTATIONS -> "@GetMapping"

            FormatOption.WRAP_ANNOTATED_PARAMETERS,
            FormatOption.FORMAT_PARAMETER_ANNOTATIONS -> "@RequestBody"

            FormatOption.FORMAT_CHAINS,
            FormatOption.KEEP_SIMPLE_RECEIVERS,
            FormatOption.KEEP_ASSIGNMENT_RECEIVER -> "chain("

            FormatOption.FORMAT_EXPRESSION_BODIES -> "valid("

            FormatOption.FORMAT_LOGICAL_ASSIGNMENTS,
            FormatOption.ALIGN_ASSIGNMENTS -> "allowed"

            FormatOption.FORMAT_TERNARIES,
            FormatOption.SIMPLIFY_DATA_BRANCHES,
            FormatOption.BRACE_MULTILINE_BRANCHES -> "count("

            FormatOption.FORMAT_CONTROL_HEADERS,
            FormatOption.PAD_NESTED_HEADERS -> "for ("

            FormatOption.SPACE_CASES,
            FormatOption.ALIGN_CASES -> "label("

            FormatOption.FORMAT_FALLBACKS -> "fallback("

            FormatOption.FORMAT_FIELD_ANNOTATIONS,
            FormatOption.SPACE_ANNOTATED_FIELDS -> "@Deprecated"

            FormatOption.FORMAT_CLASS_ANNOTATIONS -> "@Model"

            FormatOption.SPACE_LAMBDAS -> "values("

            FormatOption.NORMALIZE_LINE_BREAKS -> if (kotlin) "constructor(" else "Sample("

        }

    }

    /** Returns a required sample language for rules that are specific to one language. */
    fun language(option: FormatOption): Boolean? {

        return when (option) {

            FormatOption.KOTLIN_ENABLED,
            FormatOption.FORMAT_EXPRESSION_BODIES,
            FormatOption.SIMPLIFY_DATA_BRANCHES,
            FormatOption.FORMAT_FALLBACKS,
            FormatOption.SPACE_LAMBDAS -> true

            FormatOption.JAVA_ENABLED,
            FormatOption.FORMAT_TERNARIES -> false

            else -> null

        }

    }

}