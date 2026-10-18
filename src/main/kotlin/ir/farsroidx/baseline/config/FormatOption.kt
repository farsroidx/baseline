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
        "Fields", "Sort fields: longest first", "Sort safe field groups from longest to shortest declared type/name width.",
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
        "Constructors", "Sort assignments: shortest first",
        "Order eligible assignments from shortest to longest declared type/name width.", FormatOptions::sortConstructorAssignments
    ),

    SORT_CONSTRUCTOR_LOCALS(
        "Constructors", "Sort local variables: shortest first",
        "Order eligible local declarations from shortest to longest declared type/name width.", FormatOptions::sortConstructorLocals
    ),

    GROUP_CONSTRUCTOR_VARIABLES(
        "Constructors", "Group variables by type",
        "Group eligible assignment and declaration runs by type.", FormatOptions::groupConstructorVariables
    ),

    SPACE_CONSTRUCTOR_ASSIGNMENTS(
        "Constructors", "Blank lines between constructor assignments",
        "When disabled, keep consecutive this-qualified assignments together regardless of type grouping.",
        FormatOptions::spaceConstructorAssignments
    ),

    NORMALIZE_MODIFIERS(
        "Code layout", "Normalize modifier order", "Use the language modifier order without crossing annotations.",
        FormatOptions::normalizeModifiers
    ),

    CANONICAL_INDENTATION(
        "Code layout", "Use four-space indentation",
        "Override native indentation with four spaces and no tabs; custom continuation units remain four spaces.",
        FormatOptions::canonicalIndentation
    ),

    END_OF_LINE_BRACES(
        "Code layout", "Opening brace on the same line", "Use end-of-line braces for methods, classes and blocks.",
        FormatOptions::endOfLineBraces
    ),

    EXPAND_SIMPLE_BLOCKS(
        "Code layout", "Expand single-line blocks", "Disable compact single-line methods, classes and blocks.",
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
        "Spacing", "Blank lines between unrelated statements",
        "Insert blank lines between unrelated statements and after local writes.", FormatOptions::spaceStatements
    ),

    SPACE_FILE_SECTIONS(
        "Spacing", "Blank lines after package and imports", "Insert blank lines between file header sections.",
        FormatOptions::spaceFileSections
    ),

    COMPACT_CLOSING_BRACES(
        "Spacing", "No blank lines between closing braces",
        "Avoid blank lines between adjacent closing braces and inside empty blocks.",
        FormatOptions::compactClosingBraces
    ),

    COMPACT_BARE_EXITS(
        "Spacing", "No blank line after return / break / continue", "Remove the final blank line after operand-free exits.",
        FormatOptions::compactBareExits
    ),

    REMOVE_TRAILING_WHITESPACE(
        "Spacing", "No whitespace at end of file",
        "Remove whitespace, including the newline, at the end of the file.", FormatOptions::removeTrailingWhitespace
    ),

    WRAP_DECLARATIONS(
        "Calls", "Wrap declaration parameters",
        "Wrap overlong declaration lists without changing parameter order.", FormatOptions::wrapDeclarations
    ),

    WRAP_CALLS(
        "Calls", "Wrap call arguments", "Wrap overlong call lists without changing argument order.",
        FormatOptions::wrapCalls
    ),

    PAD_SINGLE_ARGUMENTS(
        "Calls", "Spaces in calls: one argument",
        "Example: execute( request ) instead of execute(request).", FormatOptions::padSingleArguments
    ),

    PAD_MULTIPLE_ARGUMENTS(
        "Calls", "Spaces in calls: multiple arguments",
        "Example: execute( first, second ) instead of execute(first, second).", FormatOptions::padMultipleArguments
    ),

    PAD_ANNOTATION_ARRAYS(
        "Annotations", "Spaces inside annotation array braces",
        "Example: @Mapping({ \"/path\" }) instead of @Mapping({\"/path\"}).", FormatOptions::padAnnotationArrays
    ),

    COLLAPSE_SHORT_LISTS(
        "Calls", "Keep short argument lists on one line",
        "Flatten safe lists that fit the editor right margin.", FormatOptions::collapseShortLists
    ),

    PACK_CALL_ARGUMENTS(
        "Calls", "Fit several arguments on each line",
        "Place as many safe arguments as fit on each continuation row.", FormatOptions::packCallArguments
    ),

    WRAP_ANNOTATED_PARAMETERS(
        "Calls", "Wrap lists with annotated parameters",
        "Force multiline declaration lists containing annotated parameters.", FormatOptions::wrapAnnotatedParameters
    ),

    FORMAT_CHAINS(
        "Calls", "Each chained call on a new line",
        "Place successive method-call links on continuation lines.", FormatOptions::formatChains
    ),

    KEEP_SIMPLE_RECEIVERS(
        "Calls", "Keep short receiver and call together",
        "Avoid splitting a simple receiver from its method unnecessarily.", FormatOptions::keepSimpleReceivers
    ),

    FORMAT_EXPRESSION_BODIES(
        "Calls", "Wrap Kotlin expression bodies",
        "Try moving a short expression below the equals sign before wrapping arguments.",
        FormatOptions::formatExpressionBodies
    ),

    FORMAT_CONDITIONS(
        "Conditions", "Wrap long conditions",
        "Wrap conditions at the right margin with operators at line ends.", FormatOptions::formatConditions
    ),

    FORMAT_LOGICAL_ASSIGNMENTS(
        "Conditions", "Wrap long boolean assignments",
        "Keep the first boolean operand beside equals when it fits.", FormatOptions::formatLogicalAssignments
    ),

    ALIGN_ASSIGNMENTS(
        "Conditions", "Align multiline assignments", "Use native alignment for multiline assignments.",
        FormatOptions::alignAssignments
    ),

    FORMAT_TERNARIES(
        "Conditions", "Align and wrap Java ternaries",
        "Use native multiline ternary alignment and wrapping.", FormatOptions::formatTernaries
    ),

    FORMAT_CONTROL_HEADERS(
        "Conditions", "Wrap long if / for / while headers",
        "Put multiline for, while, switch and when headers between separate parentheses.",
        FormatOptions::formatControlHeaders
    ),

    PAD_NESTED_HEADERS(
        "Conditions", "Spaces around nested header parentheses",
        "Add inner spaces when a control header starts with a nested parenthesis.", FormatOptions::padNestedHeaders
    ),

    SIMPLIFY_DATA_BRANCHES(
        "Conditions", "Remove braces from simple Kotlin branches",
        "Remove safe braces around a single Kotlin data expression.", FormatOptions::simplifyDataBranches
    ),

    BRACE_MULTILINE_BRANCHES(
        "Conditions", "Braces around multiline branches",
        "Add braces around an unbraced multiline call in a conditional branch.", FormatOptions::braceMultilineBranches
    ),

    SPACE_CASES(
        "Conditions", "Blank lines between cases", "Separate Kotlin when entries and Java switch labels.",
        FormatOptions::spaceCases
    ),

    ALIGN_CASES(
        "Conditions", "Align case arrows and colons", "Align Kotlin arrows and Java switch label colons.",
        FormatOptions::alignCases
    ),

    FORMAT_FALLBACKS(
        "Conditions", "Wrap Kotlin ?: expressions",
        "Keep block fallbacks beside the closing brace; wrap other multiline fallbacks.", FormatOptions::formatFallbacks
    ),

    FORMAT_ANNOTATIONS(
        "Annotations", "Method annotations on separate lines",
        "Place method and function annotations above the declaration.", FormatOptions::formatAnnotations
    ),

    FORMAT_PARAMETER_ANNOTATIONS(
        "Annotations", "Parameter annotations on separate lines",
        "Turn off for @RequestBody Request request on one line.", FormatOptions::formatParameterAnnotations
    ),

    FORMAT_FIELD_ANNOTATIONS(
        "Annotations", "Field annotations on separate lines",
        "Place field and property annotations above the declaration.", FormatOptions::formatFieldAnnotations
    ),

    FORMAT_CLASS_ANNOTATIONS(
        "Annotations", "Class annotations on separate lines",
        "Place class annotations above the declaration.", FormatOptions::formatClassAnnotations
    ),

    SPACE_ANNOTATED_FIELDS(
        "Annotations", "Blank lines around annotated fields",
        "Insert blank lines around fields carrying annotations.", FormatOptions::spaceAnnotatedFields
    ),

    SPACE_LAMBDAS(
        "Annotations", "Blank lines inside Kotlin lambdas",
        "Include blank lines inside multiline lambda bodies.", FormatOptions::spaceLambdas
    ),

    KEEP_ASSIGNMENT_RECEIVER(
        "Calls", "Keep chain receiver after =",
        "Attach the first receiver to the assignment and normalize its continuation indentation.",
        FormatOptions::keepAssignmentReceiver
    ),

    NORMALIZE_LINE_BREAKS(
        "Code layout", "Recalculate existing line breaks",
        "Allow native formatting to recompute existing line breaks before custom wrapping.",
        FormatOptions::normalizeLineBreaks
    ),

    SEPARATE_STATEMENTS(
        "Code layout", "One statement per line", "Require one statement per line during native formatting.",
        FormatOptions::separateStatements
    ),

    GROUP_FIELDS_BY_PREFIX(
        "Fields",
        "Group fields by name prefix",
        "Group matching snake_case or camelCase prefixes across field types inside each visibility/static section.",
        FormatOptions::groupFieldsByPrefix
    );

}