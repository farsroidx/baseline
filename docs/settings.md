# Settings

Open **Settings / Editor / Baseline**. The 57 preferences belong to the current project and are
stored in `.idea/baseline.xml`. All default to on except **Group fields by name prefix**.

Choose a category and change a checkbox. The live preview shows Java or Kotlin, scrolls to the
related example and briefly highlights it. Use **Original sample** to compare the input.
If the output does not change, the explanation says so and the related example is still shown.
Some rules require long input or depend on other enabled rules.

**Apply/OK** saves the form. **Reset** reloads saved preferences. **Restore defaults** changes the
form only. Cancel discards unapplied edits. Formatting uses a separate snapshot of saved values.
Disabling a rule generally leaves native IntelliJ formatting in effect, which may look similar.

## Common recipes

| Desired result | Setting |
| --- | --- |
| Consecutive constructor assignments, preserving the gap after `super(...)` | Turn off Constructors / Blank lines between constructor assignments. Surrounding gaps still follow statement-spacing settings. |
| `execute(getHeaders(request))` | Turn off Calls / Spaces in calls: one argument. |
| `execute(first, second)` without inner padding | Turn off Calls / Spaces in calls: multiple arguments. |
| `@GetMapping({"/path"})` | Turn off Annotations / Spaces inside annotation array braces. |
| `@RequestBody Request request` on one line | Turn off Annotations / Parameter annotations on separate lines. Also turn off Calls / Wrap lists with annotated parameters if annotations should not force multiline parameter lists. |
| Keep annotations above methods while parameter annotations stay inline | Keep Method annotations on separate lines on; turn Parameter annotations on separate lines off. |
| Keep field declaration order | Turn off field sorting and all field grouping options, including static separation and prefix grouping. |
| Keep constructor statement order | Turn off both constructor sorting options and Group variables by type. Signatures are always preserved. |

Field cascade width includes the declared type and variable name. Safe constructor runs use
ascending width; fields use descending width. Business calls, comments and unsafe writes are
barriers. No setting changes parameter/argument order or rearranges ordinary method statements.

## Export and import

1. Choose your preferences and click **Export settings...**.
2. Share the resulting `baseline-settings.xml` file.
3. Import it with **Import settings...**, review the preview, then click Apply/OK.

Export includes pending edits. Import updates the form only and validates the complete profile
before changing controls. Profiles include all 57 Boolean preferences, not source code, project
paths, IntelliJ code-style schemes or language right margins.

Older schema-version-1 profiles are supported. Missing split call/annotation values inherit the
old shared settings; other missing keys use their defaults. The retired `showGeneratedSources`
key is accepted and ignored. New exports omit it. Unsupported schemas, duplicate/unknown keys,
invalid Boolean values, external entities and files over 64 KiB are rejected.

Example of a partial profile; normal exports contain every option:

```xml
<baseline-settings formatVersion="1" pluginVersion="1.0.2">
    <option name="spaceConstructorAssignments" value="false"/>
    <option name="padSingleArguments" value="false"/>
    <option name="padAnnotationArrays" value="false"/>
</baseline-settings>
```

## Option reference

Labels below match the Settings page. Each checkbox also shows its explanation in the preview.

### Annotations

| Option | Profile key |
| --- | --- |
| Spaces inside annotation array braces | `padAnnotationArrays` |
| Method annotations on separate lines | `formatAnnotations` |
| Parameter annotations on separate lines | `formatParameterAnnotations` |
| Field annotations on separate lines | `formatFieldAnnotations` |
| Class annotations on separate lines | `formatClassAnnotations` |
| Blank lines around annotated fields | `spaceAnnotatedFields` |
| Blank lines inside Kotlin lambdas | `spaceLambdas` |

### Calls

| Option | Profile key |
| --- | --- |
| Wrap declaration parameters | `wrapDeclarations` |
| Wrap call arguments | `wrapCalls` |
| Spaces in calls: one argument | `padSingleArguments` |
| Spaces in calls: multiple arguments | `padMultipleArguments` |
| Keep short argument lists on one line | `collapseShortLists` |
| Fit several arguments on each line | `packCallArguments` |
| Wrap lists with annotated parameters | `wrapAnnotatedParameters` |
| Each chained call on a new line | `formatChains` |
| Keep short receiver and call together | `keepSimpleReceivers` |
| Wrap Kotlin expression bodies | `formatExpressionBodies` |
| Keep chain receiver after = | `keepAssignmentReceiver` |

### Code layout

| Option | Profile key |
| --- | --- |
| Normalize modifier order | `normalizeModifiers` |
| Use four-space indentation | `canonicalIndentation` |
| Opening brace on the same line | `endOfLineBraces` |
| Expand single-line blocks | `expandSimpleBlocks` |
| Recalculate existing line breaks | `normalizeLineBreaks` |
| One statement per line | `separateStatements` |

### Conditions

| Option | Profile key |
| --- | --- |
| Wrap long conditions | `formatConditions` |
| Wrap long boolean assignments | `formatLogicalAssignments` |
| Align multiline assignments | `alignAssignments` |
| Align and wrap Java ternaries | `formatTernaries` |
| Wrap long if / for / while headers | `formatControlHeaders` |
| Spaces around nested header parentheses | `padNestedHeaders` |
| Remove braces from simple Kotlin branches | `simplifyDataBranches` |
| Braces around multiline branches | `braceMultilineBranches` |
| Blank lines between cases | `spaceCases` |
| Align case arrows and colons | `alignCases` |
| Wrap Kotlin ?: expressions | `formatFallbacks` |

### Constructors

| Option | Profile key |
| --- | --- |
| Sort assignments: shortest first | `sortConstructorAssignments` |
| Sort local variables: shortest first | `sortConstructorLocals` |
| Group variables by type | `groupConstructorVariables` |
| Blank lines between constructor assignments | `spaceConstructorAssignments` |

### Fields

| Option | Profile key |
| --- | --- |
| Separate static and instance fields | `separateStaticFields` |
| Group fields by visibility | `groupFieldsByVisibility` |
| Group fields by type | `groupFieldsByType` |
| Sort fields: longest first | `sortFields` |
| Blank lines between field groups | `spaceFieldGroups` |
| Align field equals signs | `alignFieldEquals` |
| Group fields by name prefix | `groupFieldsByPrefix` |

### General

| Option | Profile key |
| --- | --- |
| Enable Baseline | `enabled` |
| Format Java files | `javaEnabled` |
| Format Kotlin files | `kotlinEnabled` |

### Spacing

| Option | Profile key |
| --- | --- |
| Blank line after opening braces | `blankAfterOpeningBrace` |
| Blank line before closing braces | `blankBeforeClosingBrace` |
| Blank lines between members | `spaceMembers` |
| Blank lines between unrelated statements | `spaceStatements` |
| Blank lines after package and imports | `spaceFileSections` |
| No blank lines between closing braces | `compactClosingBraces` |
| No blank line after return / break / continue | `compactBareExits` |
| No whitespace at end of file | `removeTrailingWhitespace` |
