package ir.farsroidx.baseline.config

/**
 * Mutable project state serialized by the IntelliJ platform.
 *
 * Every option defaults to the approved phase-one behavior. Use [snapshot] before formatting
 * so changes made in Settings cannot alter a pass already in progress.
 */
class FormatOptions {

    /** Order eligible assignments from shortest to longest declared type/name width. */
    var sortConstructorAssignments: Boolean = true
    /** Group eligible assignment and declaration runs by type. */
    var groupConstructorVariables: Boolean  = true

    /** When disabled, keep consecutive this-qualified constructor assignments together across type groups. */
    var spaceConstructorAssignments: Boolean = true

    /** Remove whitespace, including the newline, at the end of the file. */
    var removeTrailingWhitespace: Boolean   = true
    /** Keep the first boolean operand beside equals when it fits. */
    var formatLogicalAssignments: Boolean   = true
    /** Order public, protected, internal/package and private visibility groups. */
    var groupFieldsByVisibility: Boolean    = true
    /** Include a blank line at the end of nonempty blocks. */
    var blankBeforeClosingBrace: Boolean    = true
    /** Force multiline declaration lists containing annotated parameters. */
    var wrapAnnotatedParameters: Boolean    = true
    /** Include a blank line at the beginning of nonempty blocks. */
    var blankAfterOpeningBrace: Boolean     = true
    /** Try moving a short expression below the equals sign before wrapping arguments. */
    var formatExpressionBodies: Boolean     = true
    /** Add braces around an unbraced multiline call in a conditional branch. */
    var braceMultilineBranches: Boolean     = true
    /** Attach the first receiver to the assignment and normalize its continuation indentation. */
    var keepAssignmentReceiver: Boolean     = true
    /** Order eligible local declarations from shortest to longest declared type/name width. */
    var sortConstructorLocals: Boolean      = true
    /** Place static-like fields before instance fields within safe runs. */
    var separateStaticFields: Boolean       = true
    /** Override native indentation with four spaces and no tabs; custom continuation units remain four spaces. */
    var canonicalIndentation: Boolean       = true
    /** Avoid blank lines between adjacent closing braces and inside empty blocks. */
    var compactClosingBraces: Boolean       = true
    /** Put multiline for, while, switch and when headers between separate parentheses. */
    var formatControlHeaders: Boolean       = true
    /** Remove safe braces around a single Kotlin data expression. */
    var simplifyDataBranches: Boolean       = true
    /** Insert blank lines around fields carrying annotations. */
    var spaceAnnotatedFields: Boolean       = true
    /** Avoid splitting a simple receiver from its method unnecessarily. */
    var keepSimpleReceivers: Boolean        = true
    /** Allow native formatting to recompute existing line breaks before custom wrapping. */
    var normalizeLineBreaks: Boolean        = true
    /** Use the language modifier order without crossing annotations. */
    var normalizeModifiers: Boolean         = true
    /** Disable compact single-line methods, classes and blocks. */
    var expandSimpleBlocks: Boolean         = true
    /** Use inner spaces for single-argument calls. */
    var padSingleArguments: Boolean         = true

    /** Use inner spaces in eligible packed calls with multiple arguments. */
    var padMultipleArguments: Boolean = true

    /** Use inner spaces in Java annotation-array braces and Kotlin annotation collection brackets. */
    var padAnnotationArrays: Boolean = true

    /** Flatten safe lists that fit the editor right margin. */
    var collapseShortLists: Boolean         = true
    /** Require one statement per line during native formatting. */
    var separateStatements: Boolean         = true
    /** Keep matching types together within the enabled outer groups. */
    var groupFieldsByType: Boolean          = true
    /** Insert blank lines between file header sections. */
    var spaceFileSections: Boolean          = true
    /** Place as many safe arguments as fit on each continuation row. */
    var packCallArguments: Boolean          = true
    /** Place method and function annotations above the declaration. */
    var formatAnnotations: Boolean          = true

    /** Place parameter annotations on separate lines instead of beside the parameter type/name. */
    var formatParameterAnnotations: Boolean = true

    /** Place field and property annotations above the declaration. */
    var formatFieldAnnotations: Boolean = true

    /** Place class annotations above the declaration. */
    var formatClassAnnotations: Boolean = true

    /** Separate enabled field groups with one blank line. */
    var spaceFieldGroups: Boolean           = true
    /** Align initializers within consecutive field groups. */
    var alignFieldEquals: Boolean           = true
    /** Remove the final blank line after operand-free exits. */
    var compactBareExits: Boolean           = true
    /** Wrap overlong declaration lists without changing parameter order. */
    var wrapDeclarations: Boolean           = true
    /** Wrap conditions at the right margin with operators at line ends. */
    var formatConditions: Boolean           = true
    /** Use native alignment for multiline assignments. */
    var alignAssignments: Boolean           = true
    /** Add inner spaces when a control header starts with a nested parenthesis. */
    var padNestedHeaders: Boolean           = true
    /** Use end-of-line braces for methods, classes and blocks. */
    var endOfLineBraces: Boolean            = true
    /** Insert blank lines between unrelated statements and after local writes. */
    var spaceStatements: Boolean            = true
    /** Use native multiline ternary alignment and wrapping. */
    var formatTernaries: Boolean            = true
    /** Keep block fallbacks beside the closing brace; wrap other multiline fallbacks. */
    var formatFallbacks: Boolean            = true
    /** Apply Baseline to whole Kotlin files. */
    var kotlinEnabled: Boolean              = true
    /** Separate class members and top-level declarations. */
    var spaceMembers: Boolean               = true
    /** Place successive method-call links on continuation lines. */
    var formatChains: Boolean               = true
    /** Include blank lines inside multiline lambda bodies. */
    var spaceLambdas: Boolean               = true
    /** Apply Baseline to whole Java files. */
    var javaEnabled: Boolean                = true
    /** Sort safe field groups from longest to shortest declared type/name width. */
    var sortFields: Boolean                 = true
    /** Separate Kotlin when entries and Java switch labels. */
    var spaceCases: Boolean                 = true
    /** Align Kotlin arrows and Java switch label colons. */
    var alignCases: Boolean                 = true
    /** Wrap overlong call lists without changing argument order. */
    var wrapCalls: Boolean                  = true
    /** Run custom passes after the built-in formatter. */
    var enabled: Boolean                    = true

    /** Group shared snake_case/camelCase field prefixes within visibility/static boundaries; disabled by default. */
    var groupFieldsByPrefix: Boolean = false

    /** Returns an independent copy; modifying it never changes the stored project state. */
    fun snapshot(): FormatOptions {

        val copy = FormatOptions()

        for (option in FormatOption.entries) {

            option.property.set( copy, option.property.get( this ) )

        }

        return copy

    }
}