# Implementation guide

## Responsibilities

| Package | Responsibility |
| --- | --- |
| `config` | Boolean state, option catalog, project persistence and portable profiles |
| `settings` | Categorized form, live preview, option targets and Apply/Reset/import/export |
| `integration` | Whole-file Java/Kotlin adapter and recursive-formatting guard |
| `format` | Bounded formatter passes and temporary native code-style overrides |
| `structure` | Safe field/constructor grouping, ordering, alignment and modifier metadata |
| `layout` | PSI-based spacing, argument padding, wrapping, chains, conditions and annotations |
| `edit` | Range-based edit batches applied from right to left |
| `model` | Field grouping and eligibility data |

## Formatting

`FormatProcessor.processText` captures a settings snapshot and checks the master/language
switches, whole-file range and reentry guard. `Formatter.format` runs at most four passes,
stopping when text stabilizes. It skips syntax-error files and enabled formatter-off regions.

Each pass rearranges eligible declarations, applies temporary native code style, then runs
specialized layout planners. Edits are committed before the next planner reads PSI.
`ArgumentSpacing` resolves call and annotation-array padding after wrapping and boundary rules.
Native temporary settings and the reentry guard are restored in `finally` blocks.

Field cascades measure the declared type plus variable name. Constructor cascades use the
reverse direction within eligible independent runs. Comments, business calls, repeated writes
and unsafe initializers form barriers. Ordinary method statements and signature/argument order
are never rearranged. `TextEdits` rejects partial overlap and preserves non-whitespace gaps.

## Settings and preview

`BaselineSettings` stores project preferences in `.idea/baseline.xml`. `FormatOption` supplies
stable keys, labels, groups and descriptions; snapshots copy every catalog value.
Shared-prefix grouping defaults to false; other preferences default to true.

Checkboxes edit the form only. Apply/OK saves, Reset reloads, and Restore defaults edits the
form without saving. Import validates before updating controls; Export includes pending edits.

`BaselineConfigurable` implements `Configurable.NoScroll`. `SettingsPanel` also tracks both
viewport dimensions if a host wraps it. The option lists and IDE preview editor scroll inside
their own bounds; headers and action buttons remain visible.

`FormatPreview` coalesces changes with a 250 ms Swing timer and formats fresh in-memory PSI
samples from `resources/preview`. A file-local options key supplies pending preferences without
changing the project service. Formatting runs under a write action on the event dispatch thread.
`PreviewTarget` maps every option to its example and selects a required language when needed.
The example is highlighted for 1.4 seconds, even when the current rules produce no layout change.
Unchanged refreshes without an option selection preserve the current code position.
Disposal stops timers and releases the IDE editor. Preview failures are reported in the UI/log.

## Profiles

Profiles use UTF-8 XML schema version 1 with stable option property names. The plugin version
is informational. Missing split annotation/call preferences inherit their legacy shared values
on import; other missing values use their defaults. The retired generated-source key is
validated and ignored. New exports contain formatting preferences only.

The codec rejects duplicate/unknown keys, invalid Booleans, unsupported schemas, external
entities and documents larger than 64 KiB. `ProfileFiles` writes a temporary sibling and uses
atomic replacement when available. I/O failures leave saved project preferences untouched.

## Verification

PSI fixtures exercise native Reformat Code integration, parsing, safe ordering and repeated
formatting. Settings tests cover pending values, persistence, profiles, independent padding and
annotation controls, viewport sizing, mouse-wheel scrolling, every option's preview target,
explanations and transient highlighting. They do not simulate Marketplace installation or the
IDE's save/commit/push dialogs. The current source uses canonical methods without deprecated
numbered implementations.
