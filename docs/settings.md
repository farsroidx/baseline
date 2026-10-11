# Baseline preferences

Open **Settings / Editor / Baseline**. Preferences belong to the current project and are
saved in `.idea/baseline.xml`. All options default to **enabled**, preserving the approved
phase-one formatter behavior. Newly added options also default to enabled when loading an
older state file.

Changes take effect after **Apply** or **OK**, on the next whole-file Reformat operation.
**Restore defaults** changes the displayed form only; Apply/OK is still required to save it.
Cancel discards unapplied changes. The formatter captures one independent snapshot per run.

Disabling Baseline or a language leaves that request to IntelliJ's native formatter.
Disabling an individual layout rule removes that custom override; it does not disable the
native formatter. Native code style can therefore produce similar whitespace. Disabling
opening/closing blank lines explicitly removes those optional blank lines after other passes.

## Export and import

1. Open **Settings / Editor / Baseline** and choose the desired checkbox values.
2. Click **Export settings...** and save `baseline-settings.xml`.
3. Send that file to your colleague through your normal sharing channel.
4. Your colleague opens the same page and clicks **Import settings...**.
5. Select the XML file, review the imported checkboxes, then click **Apply** or **OK**.

Export includes the currently displayed values even if they have not been applied. Import
changes the form only: Reset discards it, and Cancel leaves saved project preferences unchanged.
Exporting does not save or alter the current project's preferences. Existing export destinations
require overwrite confirmation. File cancellation leaves the form and saved preferences unchanged.

All 50 options are exported with stable property names. Profiles contain no project path or
source code. They do not export IntelliJ's code-style scheme, language right margins or save/Git
preferences. Set matching Java/Kotlin right margins separately for matching wrapped output.

Malformed XML, unsupported schema versions, unknown or duplicate options, invalid Boolean values
and files larger than 64 KiB are rejected before any checkbox is changed. Missing options in a
valid version-one profile retain enabled defaults. Only lower-case `true` and `false` are accepted.
Document types and external entities are prohibited.

A minimal valid partial profile looks like this; normal exports include every option:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<baseline-settings formatVersion="1" pluginVersion="1.0.0">
    <option name="enabled" value="true"/>
    <option name="sortFields" value="false"/>
</baseline-settings>
```

This portable format is separate from `.idea/baseline.xml`; use the Export button instead of
copying IntelliJ's internal project-state XML.

## Boolean options

Every setting below is independent in storage. Related enabled passes can still affect the
same expression: for example, fluent chains remain multiline when call wrapping is disabled,
and case/lambda whitespace interacts with block-boundary whitespace.

### General

| Option | Behavior when enabled | State property |
| --- | --- | --- |
| Enable Baseline | Run custom passes after the built-in formatter. | `enabled` |
| Format Java files | Apply Baseline to whole Java files. | `javaEnabled` |
| Format Kotlin files | Apply Baseline to whole Kotlin files. | `kotlinEnabled` |

### Fields

| Option | Behavior when enabled | State property |
| --- | --- | --- |
| Separate static and instance fields | Place static-like fields before instance fields within safe runs. | `separateStaticFields` |
| Group fields by visibility | Order public, protected, internal/package and private visibility groups. | `groupFieldsByVisibility` |
| Group fields by type | Keep matching types together within the enabled outer groups. | `groupFieldsByType` |
| Sort fields by name length | Sort safe field groups from longest to shortest name. | `sortFields` |
| Blank lines between field groups | Separate enabled field groups with one blank line. | `spaceFieldGroups` |
| Align field equals signs | Align initializers within consecutive field groups. | `alignFieldEquals` |

### Constructors

| Option | Behavior when enabled | State property |
| --- | --- | --- |
| Sort independent constructor assignments | Order eligible assignments from shortest to longest target name. | `sortConstructorAssignments` |
| Sort independent constructor locals | Order eligible local declarations from shortest to longest name. | `sortConstructorLocals` |
| Group constructor variables by type | Group eligible assignment and declaration runs by type. | `groupConstructorVariables` |

### Structure

| Option | Behavior when enabled | State property |
| --- | --- | --- |
| Normalize modifier order | Use the language modifier order without crossing annotations. | `normalizeModifiers` |
| Override native indentation with four spaces | Override native indentation with four spaces and no tabs; custom continuation units remain four spaces. | `canonicalIndentation` |
| Place opening braces at line ends | Use end-of-line braces for methods, classes and blocks. | `endOfLineBraces` |
| Expand simple blocks | Disable compact single-line methods, classes and blocks. | `expandSimpleBlocks` |
| Recompute native line breaks | Allow native formatting to recompute existing line breaks before custom wrapping. | `normalizeLineBreaks` |
| One statement per native line | Require one statement per line during native formatting. | `separateStatements` |

### Spacing

| Option | Behavior when enabled | State property |
| --- | --- | --- |
| Blank line after opening braces | Include a blank line at the beginning of nonempty blocks. | `blankAfterOpeningBrace` |
| Blank line before closing braces | Include a blank line at the end of nonempty blocks. | `blankBeforeClosingBrace` |
| Blank lines between members | Separate class members and top-level declarations. | `spaceMembers` |
| Separate unrelated statements | Insert blank lines between unrelated statements and after local writes. | `spaceStatements` |
| Separate package, imports and declarations | Insert blank lines between file header sections. | `spaceFileSections` |
| Compact consecutive closing braces | Avoid blank lines between adjacent closing braces and inside empty blocks. | `compactClosingBraces` |
| Compact bare return, continue and break | Remove the final blank line after operand-free exits. | `compactBareExits` |
| Remove trailing file whitespace | Remove whitespace, including the newline, at the end of the file. | `removeTrailingWhitespace` |

### Parameters and calls

| Option | Behavior when enabled | State property |
| --- | --- | --- |
| Wrap declaration parameters | Wrap overlong declaration lists without changing parameter order. | `wrapDeclarations` |
| Wrap call arguments | Wrap overlong call lists without changing argument order. | `wrapCalls` |
| Pad call argument lists | Use inner spaces for single-argument calls and qualified packed-call lists. | `padSingleArguments` |
| Collapse short parameter and argument lists | Flatten safe lists that fit the editor right margin. | `collapseShortLists` |
| Pack wrapped call arguments | Place as many safe arguments as fit on each continuation row. | `packCallArguments` |
| Put annotated parameters on separate lines | Force multiline declaration lists containing annotated parameters. | `wrapAnnotatedParameters` |
| Format fluent call chains | Place successive method-call links on continuation lines. | `formatChains` |
| Keep simple receivers with calls | Avoid splitting a simple receiver from its method unnecessarily. | `keepSimpleReceivers` |
| Format Kotlin expression bodies | Try moving a short expression below the equals sign before wrapping arguments. | `formatExpressionBodies` |
| Keep a chain receiver beside equals | Attach the first receiver to the assignment and normalize its continuation indentation. | `keepAssignmentReceiver` |

### Conditions and branches

| Option | Behavior when enabled | State property |
| --- | --- | --- |
| Wrap logical conditions | Wrap conditions at the right margin with operators at line ends. | `formatConditions` |
| Format logical initializers | Keep the first boolean operand beside equals when it fits. | `formatLogicalAssignments` |
| Align multiline assignments | Use native alignment for multiline assignments. | `alignAssignments` |
| Align and wrap Java ternaries | Use native multiline ternary alignment and wrapping. | `formatTernaries` |
| Format multiline control headers | Put multiline for, while, switch and when headers between separate parentheses. | `formatControlHeaders` |
| Pad nested control parentheses | Add inner spaces when a control header starts with a nested parenthesis. | `padNestedHeaders` |
| Simplify Kotlin data branches | Remove safe braces around a single Kotlin data expression. | `simplifyDataBranches` |
| Brace multiline call branches | Add braces around an unbraced multiline call in a conditional branch. | `braceMultilineBranches` |
| Blank lines between cases | Separate Kotlin when entries and Java switch labels. | `spaceCases` |
| Align case separators | Align Kotlin arrows and Java switch label colons. | `alignCases` |
| Format Kotlin Elvis fallbacks | Keep block fallbacks beside the closing brace; wrap other multiline fallbacks. | `formatFallbacks` |

### Annotations and lambdas

| Option | Behavior when enabled | State property |
| --- | --- | --- |
| Place annotations above declarations | Separate declaration annotations from their declarations. | `formatAnnotations` |
| Separate annotated fields | Insert blank lines around fields carrying annotations. | `spaceAnnotatedFields` |
| Space multiline Kotlin lambdas | Include blank lines inside multiline lambda bodies. | `spaceLambdas` |

## Rules that remain mandatory

These invariants are safety contracts, not style switches:

- Method/constructor declaration order and parameter/argument order are preserved.
- Reordering only crosses eligible independent declarations or constructor statements.
  Comments, business calls, repeated writes and unsafe initializers stop a run.
- Partial selections retain native formatting; Baseline does not expand a selected range.
- Files with syntax errors or an enabled formatter-off region skip custom whole-file passes.
- Comment/string contents and Kotlin trailing commas are preserved by conservative rendering.
- Native Rearrange is disabled inside Baseline's own native-formatting cycle.
- Custom continuation indentation uses a four-space unit. The native indentation option
  controls the native formatter override, not a numeric custom-indent preference.

## Common combinations

To keep declaration order, disable **Sort fields by name length**, **Group fields by type**,
**Group fields by visibility**, and **Separate static and instance fields**. Leaving a grouping
option enabled permits grouping rearrangement even when length sorting is disabled.

To preserve constructor statement order, disable both constructor sorting options and
**Group constructor variables by type**. Eligible-run safety checks still apply when sorting
is enabled. Constructor signatures are never sorted.

To keep only one newline at method boundaries, disable the opening and closing blank-line
options. **Compact bare return, continue and break** removes a trailing blank line after those
exits even if general closing blank lines are enabled. **Compact consecutive closing braces**
also takes precedence for brace-only boundaries and empty blocks.

To hand call layout entirely back to native code style, disable custom call wrapping, argument
packing, list collapsing, padding, chain layout and receiver rules. A single indivisible token
may exceed the right margin; Baseline does not split identifiers or literals.
