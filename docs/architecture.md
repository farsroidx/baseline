# Implementation guide

## Packages

| Package | Responsibility |
| --- | --- |
| `config` | Boolean state, option catalog, project persistence and portable XML/file transfer |
| `settings` | Settings form, profile actions and Apply/Reset/defaults lifecycle |
| `integration` | Native PostFormatProcessor adapter, language/range checks and reentry guard |
| `format` | Bounded orchestration and temporary native code-style overrides |
| `structure` | Conservative field and constructor-run rearrangement, metadata and modifiers |
| `layout` | PSI-based whitespace, call/chain, condition, case, annotation and boundary planners |
| `edit` | Deferred replacement batches applied against a single PSI snapshot |
| `model` | Field group identity and field eligibility metadata |
| `resources/*.txt` | Java/Kotlin examples loaded as test fixtures and distributed as text |

The shared option catalog acts as metadata for copying state, constructing the Settings form
and documenting preference coverage. The settings service owns persistence; layout planners
consume snapshots and do not depend on Swing or storage. This keeps presentation, integration
and source-layout responsibilities separate without adding redundant abstraction layers.

## Current entry points

`FormatProcessor.processText` checks the project master switch,
language switches, reentry and whole-file range, then calls `Formatter.format` with a settings
snapshot. `processElement` only redirects a complete `PsiFile`; element fragments stay native.
The thread-local guard is removed in a `finally` block. The platform invokes this pipeline in
its formatting/write-action context; direct callers of Formatter must own a write action.

`format` runs `formatPass` at most four times, stopping when the file text no longer changes.
The cap prevents unbounded loops; it is not a mathematical convergence guarantee for every
possible language construct. Tests cover stability for supported layouts and option changes.

## Pass sequence

1. Reject syntax errors and enabled formatter-off regions. Obtain/unblock/commit the document.
2. Plan eligible field grouping/sorting, independent constructor assignments/locals and modifiers.
3. Clone the language code style and apply enabled native indentation/brace/wrapping overrides.
   Disable native rearrangement, run native formatting and restore temporary state in `finally`.
4. Commit PSI. Plan block, member, statement and file-section whitespace.
5. Normalize parameter lists from inner depth to outer depth, then apply the real right margin.
   Chain links are refreshed between depth levels when enabled.
6. Apply statement/brace, condition/annotation/case and field-equals layout.
7. Apply safe branch simplification, receiver attachment, argument packing, expression bodies,
   control headers, bare exits and Elvis fallbacks.
8. Apply assignment receiver, lambda, logical-condition and logical-expression layout.
9. Resolve explicitly disabled boundary spacing and optionally remove trailing file whitespace.

Each batch is applied and committed before a following planner observes PSI. Some passes touch
the same boundary; ordering is intentional. Closing-brace compaction and bare-exit compaction
can override general trailing blank lines. The final preference pass removes block blanks
explicitly disabled in Settings.

## Rearrangement safety

Field eligibility is syntactic: Java fields must be single declarations with absent/literal
initializers; Kotlin properties must have no delegate/custom accessors and absent/literal
initializers. Visibility and type groups are conservative text/type keys, not an import
optimizer or whole-program dependency analysis.

Constructor assignment sorting accepts this-qualified assignments to eligible fields using
literals or original constructor parameter references. Kotlin open/override properties,
custom setters, delegates and annotations, and Java volatile fields are excluded. Duplicate
assignment names prevent run sorting. Comments/business statements are barriers. Local
constructor declarations are similarly limited to safe initializers. Statements in ordinary
methods are never rearranged. Parameter and argument order is never changed.

`SpaceLayout.reads`/`writes` use names for immediate statement spacing, not semantic data-flow
analysis. They do not authorize rearrangement of arbitrary business code.

## Replacement batches

`TextEdits.replace` stores text by exact range, so a later same-range edit wins. `gap` only
accepts whitespace/semicolon gaps and preserves an existing semicolon. Application sorts by
descending start offset and descending end offset. Partial overlaps fail with an explicit
`IllegalStateException`; already applied replacements are not rolled back by that helper.
A caller must never reuse a batch after committing changed source offsets.

## Persistence and UI

`BaselineSettings` is a project light service implementing `PersistentStateComponent`.
Its mutable `FormatOptions` bean is stored in `.idea/baseline.xml`. `loadState` and `snapshot`
copy every Boolean via `FormatOption`, avoiding shared mutable UI/run state. The single catalog
also supplies checkbox labels, groups and tooltips. All defaults are true for backward behavior.

`BaselineConfigurable` is registered as an Editor child project configurable. Creating the
form reads saved state. Apply writes a copy; Reset discards unapplied changes. Restore defaults
updates only the form. Export captures pending form values, and Import validates before
updating controls. Dispose releases component references. UI lifecycle methods execute on
the Swing event dispatch thread through the platform Settings dialog.

## Portable profile transfer

`ProfileCodec` serializes stable `FormatOption.property.name` keys to a version-one XML profile.
It exports every Boolean deterministically and uses enabled defaults for missing keys. Import
rejects unrelated roots, unsupported schema versions, duplicate/unknown entries, nested content
and non-Boolean values. The parser prohibits document types, external entities and external
schema/DTD access. Profiles are bounded to 64 KiB and contain no source text or project paths.
The plugin version is informational; the schema version controls compatibility.

`ProfileFiles` reads UTF-8 text and validates it before returning a fresh bean. Export writes a
temporary sibling file, then atomically replaces the selected destination where supported.
Filesystems without atomic moves use a regular replacement move. Temporary files are cleaned
in either case, and I/O failures are reported by the UI. File I/O and XML conversion do not own
project persistence.

`BaselineConfigurable.createComponent` delegates to the current `createComponent2` form;
`createComponent1` retains the original form for compatibility. The current form adds Export
and Import actions. Export prompts for overwrite when necessary and reads current checkbox
values. Import updates checkboxes after complete validation. Apply/OK is the only persistence
boundary. Small, bounded file transfers run in the Settings dialog; no background listener,
network transfer or automatic formatting is triggered by profile import.

## Text sample resources

`src/main/resources/JavaSample.txt` and `KotlinSample.txt` demonstrate approved layouts.
They are plain resource text rather than production source files. Tests read their contents
into fixtures named `JavaSample.java` and `KotlinSample.kt` to select the proper language parser.
The release contains the text resources without compiled sample classes.

## Current methods

The final source contains one current implementation for each formatting operation.
Superseded deprecated revisions have been removed, and current methods use canonical names.
The registered extension and regression tests use the same current pipeline.

## Tests

The platform fixtures execute the registered Reformat Code pipeline, not a whitespace-only
mock. They verify parsing, parameter preservation, structural barriers and repeated output.
Settings tests verify the catalog against all Boolean bean fields, independent snapshots,
XML serialization round trips, Apply/Reset/dispose, language/master switches and rule effects.
Each independently disabled option is also exercised on Java/Kotlin for valid, stable output.
Settings-test teardown restores defaults because platform fixtures may reuse a cached project.
Portable-profile tests cover all-option round trips, invalid files, omitted defaults, oversized
input, prohibited document types, export replacement and Import/Export Apply/Reset behavior.

The suite does not simulate the desktop save or Git push UI. Native-save integration follows
from using the same registered PostFormatProcessor pipeline; Git push integration is absent.
