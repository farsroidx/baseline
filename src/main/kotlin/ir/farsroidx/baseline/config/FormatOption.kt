package ir.farsroidx.baseline.config

import kotlin.reflect.KMutableProperty1

/**
 * Single catalog used by persistence copying, the Settings UI, and option coverage tests.
 *
 * @property group settings section containing this option
 * @property label checkbox text
 * @property description explanation displayed as the checkbox tooltip
 * @property property mutable Boolean property on [FormatOptions]
 */
enum class FormatOption(
    val group: String,
    val label: String,
    val description: String,
    val property: KMutableProperty1<FormatOptions, Boolean>
) {

    ENABLED("General", "Enable Baseline", "Run custom passes after the built-in formatter.", FormatOptions::enabled),

    JAVA_ENABLED("General", "Format Java files", "Apply Baseline to whole Java files.", FormatOptions::javaEnabled),

    KOTLIN_ENABLED(
        "General", "Format Kotlin files", "Apply Baseline to whole Kotlin files.", FormatOptions::kotlinEnabled
    ),

    SEPARATE_STATIC_FIELDS(
        "Fields", "Separate static and instance fields",
        "Place static-like fields before instance fields within safe runs.", FormatOptions::separateStaticFields
    ),

    GROUP_FIELDS_BY_VISIBILITY(
        "Fields", "Group fields by visibility",
        "Order public, protected, internal/package and private visibility groups.",
        FormatOptions::groupFieldsByVisibility
    ),

    GROUP_FIELDS_BY_TYPE(
        "Fields", "Group fields by type", "Keep matching types together within the enabled outer groups.",
        FormatOptions::groupFieldsByType
    ),

    SORT_FIELDS(
        "Fields", "Sort fields by name length", "Sort safe field groups from longest to shortest name.",
        FormatOptions::sortFields
    ),

    SPACE_FIELD_GROUPS(
        "Fields", "Blank lines between field groups", "Separate enabled field groups with one blank line.",
        FormatOptions::spaceFieldGroups
    ),

    ALIGN_FIELD_EQUALS(
        "Fields", "Align field equals signs", "Align initializers within consecutive field groups.",
        FormatOptions::alignFieldEquals
    ),

    SORT_CONSTRUCTOR_ASSIGNMENTS(
        "Constructors", "Sort independent constructor assignments",
        "Order eligible assignments from shortest to longest target name.", FormatOptions::sortConstructorAssignments
    ),

    SORT_CONSTRUCTOR_LOCALS(
        "Constructors", "Sort independent constructor locals",
        "Order eligible local declarations from shortest to longest name.", FormatOptions::sortConstructorLocals
    ),

    GROUP_CONSTRUCTOR_VARIABLES(
        "Constructors", "Group constructor variables by type",
        "Group eligible assignment and declaration runs by type.", FormatOptions::groupConstructorVariables
    ),

    NORMALIZE_MODIFIERS(
        "Structure", "Normalize modifier order", "Use the language modifier order without crossing annotations.",
        FormatOptions::normalizeModifiers
    ),

    CANONICAL_INDENTATION(
        "Structure", "Override native indentation with four spaces",
        "Override native indentation with four spaces and no tabs; custom continuation units remain four spaces.",
        FormatOptions::canonicalIndentation
    ),

    END_OF_LINE_BRACES(
        "Structure", "Place opening braces at line ends", "Use end-of-line braces for methods, classes and blocks.",
        FormatOptions::endOfLineBraces
    ),

    EXPAND_SIMPLE_BLOCKS(
        "Structure", "Expand simple blocks", "Disable compact single-line methods, classes and blocks.",
        FormatOptions::expandSimpleBlocks
    ),

    BLANK_AFTER_OPENING_BRACE(
        "Spacing", "Blank line after opening braces", "Include a blank line at the beginning of nonempty blocks.",
        FormatOptions::blankAfterOpeningBrace
    ),

    BLANK_BEFORE_CLOSING_BRACE(
        "Spacing", "Blank line before closing braces", "Include a blank line at the end of nonempty blocks.",
        FormatOptions::blankBeforeClosingBrace
    ),

    SPACE_MEMBERS(
        "Spacing", "Blank lines between members", "Separate class members and top-level declarations.",
        FormatOptions::spaceMembers
    ),

    SPACE_STATEMENTS(
        "Spacing", "Separate unrelated statements",
        "Insert blank lines between unrelated statements and after local writes.", FormatOptions::spaceStatements
    ),

    SPACE_FILE_SECTIONS(
        "Spacing", "Separate package, imports and declarations", "Insert blank lines between file header sections.",
        FormatOptions::spaceFileSections
    ),

    COMPACT_CLOSING_BRACES(
        "Spacing", "Compact consecutive closing braces",
        "Avoid blank lines between adjacent closing braces and inside empty blocks.",
        FormatOptions::compactClosingBraces
    ),

    COMPACT_BARE_EXITS(
        "Spacing", "Compact bare return, continue and break", "Remove the final blank line after operand-free exits.",
        FormatOptions::compactBareExits
    ),

    REMOVE_TRAILING_WHITESPACE(
        "Spacing", "Remove trailing file whitespace",
        "Remove whitespace, including the newline, at the end of the file.", FormatOptions::removeTrailingWhitespace
    ),

    WRAP_DECLARATIONS(
        "Parameters and calls", "Wrap declaration parameters",
        "Wrap overlong declaration lists without changing parameter order.", FormatOptions::wrapDeclarations
    ),

    WRAP_CALLS(
        "Parameters and calls", "Wrap call arguments", "Wrap overlong call lists without changing argument order.",
        FormatOptions::wrapCalls
    ),

    PAD_SINGLE_ARGUMENTS(
        "Parameters and calls", "Pad call argument lists",
        "Use inner spaces for single-argument calls and qualified packed-call lists.", FormatOptions::padSingleArguments
    ),

    COLLAPSE_SHORT_LISTS(
        "Parameters and calls", "Collapse short parameter and argument lists",
        "Flatten safe lists that fit the editor right margin.", FormatOptions::collapseShortLists
    ),

    PACK_CALL_ARGUMENTS(
        "Parameters and calls", "Pack wrapped call arguments",
        "Place as many safe arguments as fit on each continuation row.", FormatOptions::packCallArguments
    ),

    WRAP_ANNOTATED_PARAMETERS(
        "Parameters and calls", "Put annotated parameters on separate lines",
        "Force multiline declaration lists containing annotated parameters.", FormatOptions::wrapAnnotatedParameters
    ),

    FORMAT_CHAINS(
        "Parameters and calls", "Format fluent call chains",
        "Place successive method-call links on continuation lines.", FormatOptions::formatChains
    ),

    KEEP_SIMPLE_RECEIVERS(
        "Parameters and calls", "Keep simple receivers with calls",
        "Avoid splitting a simple receiver from its method unnecessarily.", FormatOptions::keepSimpleReceivers
    ),

    FORMAT_EXPRESSION_BODIES(
        "Parameters and calls", "Format Kotlin expression bodies",
        "Try moving a short expression below the equals sign before wrapping arguments.",
        FormatOptions::formatExpressionBodies
    ),

    FORMAT_CONDITIONS(
        "Conditions and branches", "Wrap logical conditions",
        "Wrap conditions at the right margin with operators at line ends.", FormatOptions::formatConditions
    ),

    FORMAT_LOGICAL_ASSIGNMENTS(
        "Conditions and branches", "Format logical initializers",
        "Keep the first boolean operand beside equals when it fits.", FormatOptions::formatLogicalAssignments
    ),

    ALIGN_ASSIGNMENTS(
        "Conditions and branches", "Align multiline assignments", "Use native alignment for multiline assignments.",
        FormatOptions::alignAssignments
    ),

    FORMAT_TERNARIES(
        "Conditions and branches", "Align and wrap Java ternaries",
        "Use native multiline ternary alignment and wrapping.", FormatOptions::formatTernaries
    ),

    FORMAT_CONTROL_HEADERS(
        "Conditions and branches", "Format multiline control headers",
        "Put multiline for, while, switch and when headers between separate parentheses.",
        FormatOptions::formatControlHeaders
    ),

    PAD_NESTED_HEADERS(
        "Conditions and branches", "Pad nested control parentheses",
        "Add inner spaces when a control header starts with a nested parenthesis.", FormatOptions::padNestedHeaders
    ),

    SIMPLIFY_DATA_BRANCHES(
        "Conditions and branches", "Simplify Kotlin data branches",
        "Remove safe braces around a single Kotlin data expression.", FormatOptions::simplifyDataBranches
    ),

    BRACE_MULTILINE_BRANCHES(
        "Conditions and branches", "Brace multiline call branches",
        "Add braces around an unbraced multiline call in a conditional branch.", FormatOptions::braceMultilineBranches
    ),

    SPACE_CASES(
        "Conditions and branches", "Blank lines between cases", "Separate Kotlin when entries and Java switch labels.",
        FormatOptions::spaceCases
    ),

    ALIGN_CASES(
        "Conditions and branches", "Align case separators", "Align Kotlin arrows and Java switch label colons.",
        FormatOptions::alignCases
    ),

    FORMAT_FALLBACKS(
        "Conditions and branches", "Format Kotlin Elvis fallbacks",
        "Keep block fallbacks beside the closing brace; wrap other multiline fallbacks.", FormatOptions::formatFallbacks
    ),

    FORMAT_ANNOTATIONS(
        "Annotations and lambdas", "Place annotations above declarations",
        "Separate declaration annotations from their declarations.", FormatOptions::formatAnnotations
    ),

    SPACE_ANNOTATED_FIELDS(
        "Annotations and lambdas", "Separate annotated fields",
        "Insert blank lines around fields carrying annotations.", FormatOptions::spaceAnnotatedFields
    ),

    SPACE_LAMBDAS(
        "Annotations and lambdas", "Space multiline Kotlin lambdas",
        "Include blank lines inside multiline lambda bodies.", FormatOptions::spaceLambdas
    ),

    KEEP_ASSIGNMENT_RECEIVER(
        "Parameters and calls", "Keep a chain receiver beside equals",
        "Attach the first receiver to the assignment and normalize its continuation indentation.",
        FormatOptions::keepAssignmentReceiver
    ),

    NORMALIZE_LINE_BREAKS(
        "Structure", "Recompute native line breaks",
        "Allow native formatting to recompute existing line breaks before custom wrapping.",
        FormatOptions::normalizeLineBreaks
    ),

    SEPARATE_STATEMENTS(
        "Structure", "One statement per native line", "Require one statement per line during native formatting.",
        FormatOptions::separateStatements
    );

}