# Changelog

## 1.0.0 - 2026-10-12

Initial stable release for Java and Kotlin formatting in IntelliJ IDEA.

### Added

- Standard whole-file Reformat Code integration through a post-format processor.
- Project Settings page under Editor / Baseline with 50 Boolean preferences.
- Apply, Reset and Restore defaults, with an independent preference snapshot per operation.
- Portable UTF-8 XML profile export/import for sharing every Baseline Boolean with colleagues.
- Versioned profile validation, enabled defaults for omitted entries and clear invalid-file errors.
- Bounded import size, prohibited document types/external entities and temporary-file export replacement.
- Static/instance, visibility and type field groups, descending name cascades and equals alignment.
- Eligible constructor assignment/local groups with ascending name-length ordering.
- Margin-aware declarations, packed call arguments, fluent chains and simple receiver layout.
- Block/member/statement/file-section spacing, annotation placement and multiline lambda spacing.
- Logical conditions, Java ternaries, control headers, when/switch cases and Kotlin Elvis layout.
- Configurable bare-exit/brace compaction and trailing-file whitespace removal.
- Source contracts, architecture documentation, option reference and team-sharing instructions.
- Java/Kotlin text examples in resources, excluded from compiled sample classes.
- Regression tests for parsing, order preservation, semantic barriers, repeated formatting,
  settings persistence, profile validation, file replacement and Settings import/export lifecycle.

### Compatibility and limits

- Every Boolean defaults to enabled, preserving the approved phase-one behavior.
- Parameter/argument and method/constructor declaration order remain under developer control.
- Structural rearrangement only operates within eligible independent runs.
- Partial ranges, syntax-error files and enabled formatter-off regions retain native formatting.
- Whole-file Actions on Save uses the registered formatter without a separate save listener.
- Native commit checks format changed code only; no Git push handler or standalone formatter is included.
- Profiles share Baseline preferences only. Configure the same Java/Kotlin IDE right margins separately.
- Previous behavior-changing method revisions remain available with deprecation references.
