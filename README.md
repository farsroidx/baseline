# Baseline

Baseline adds project-configurable Java/Kotlin formatting to IntelliJ IDEA's standard
**Reformat Code** action. It is a `PostFormatProcessor`: native language formatting runs first,
then Baseline applies the enabled custom rules. It does not replace the IDE's formatting model.

## Install and use

1. Build with `./gradlew.bat clean test buildPlugin --offline` on the configured Windows development host.
2. Install `build/distributions/Baseline-1.0.0.zip` using Settings / Plugins / Install Plugin from Disk.
3. Open **Settings / Editor / Baseline** and choose the project preferences.
4. Configure each language's right margin under **Editor / Code Style**.
5. Run **Code / Reformat Code** (`Ctrl+Alt+L`) with no fragment selected.

All 50 Boolean options default to enabled. See [the settings reference](docs/settings.md) for
individual switches, rule interactions and mandatory semantic safeguards.

## Share settings with your team

Open **Settings / Editor / Baseline**, configure the checkboxes and click **Export settings...**.
Save the portable `baseline-settings.xml` file and share it with your colleague. They use
**Import settings...**, review the values, then click **Apply** or **OK**.

Export includes pending checkbox edits. Import does not save preferences until Apply/OK, and
invalid profiles leave the form unchanged. The profile includes all Baseline Boolean preferences;
IntelliJ code-style schemes and Java/Kotlin right margins are configured separately.

See [the profile workflow and format](docs/settings.md#export-and-import) for validation,
compatibility and cancellation details.

## Examples and release notes

- [Java example](src/main/resources/JavaSample.txt)
- [Kotlin example](src/main/resources/KotlinSample.txt)
- [1.0.0 changelog](CHANGELOG.md)

Examples are distributed as text resources and are not compiled into sample classes.

## Format on save

Open **Settings / Tools / Actions on Save** and enable **Reformat code** for Java and Kotlin.
Choose **Whole file**. The same native formatter pipeline invokes Baseline, so no separate save
listener is required. Changed-lines formatting does not activate Baseline's whole-file rules.

## Commit and push

IntelliJ's **Commit Checks / Reformat code** formats added/modified code only. Baseline deliberately
skips partial ranges, so this checkbox alone does not guarantee its complete structural rules.
Use whole-file formatting on save or run whole-file Reformat before committing.

For literal mandatory enforcement immediately before **Push**, a separate pre-push integration
or hook/check is required. This plugin currently adds no Git hook, push handler, commit rewrite
or standalone command-line formatter. A push-time check should reject unformatted committed
content rather than silently modify files without creating a corresponding commit.

These integration details follow the
[JetBrains Reformat and rearrange code documentation](https://www.jetbrains.com/help/idea/reformat-and-rearrange-code.html)
and the [IntelliJ formatting extension documentation](https://plugins.jetbrains.com/docs/intellij/code-formatting.html).

## Development and verification

The current Gradle build targets JVM 17 and uses the local IntelliJ installation at
`S:\IntelliJ IDEA\idea`, including its bundled Java/Kotlin plugins. Change that path in
`build.gradle.kts` when using another development host. Offline builds require dependencies
already present in the Gradle cache.

- `./gradlew.bat test --offline`: PSI regression, repeat-format stability and settings tests.
- `./gradlew.bat buildPlugin --offline`: installable plugin ZIP.
- `./gradlew.bat clean test buildPlugin --offline`: fresh tested release build.
- `build/reports/tests/test/index.html`: test report.

See [the implementation guide](docs/architecture.md) for package responsibilities, pipeline
ordering, state persistence and the retained method revisions. Source KDoc/Javadoc describes
the contracts of classes, helpers, samples and tests. Sample values are illustrative fixtures.

## License

[Apache License 2.0](LICENSE).
