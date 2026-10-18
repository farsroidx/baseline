# Baseline

Baseline 1.0.2 adds configurable Java and Kotlin rules to IntelliJ IDEA's **Reformat Code** action.
It runs after the native formatter and uses each language's editor right margin.
Requires IntelliJ IDEA build 261 or newer with Java and Kotlin support.

## Install and use

1. Install `build/distributions/Baseline-1.0.2.zip` from **Settings / Plugins / Install Plugin from Disk**.
2. Open **Settings / Editor / Baseline** and choose a category.
3. Change an option to see its related code highlighted in the live Java/Kotlin preview.
4. Click **Apply** or **OK**, then run whole-file **Reformat Code** (`Ctrl+Alt+L`).

The option list and code preview scroll independently. The preview can show the original sample
or the formatted result. Each option has an explanation; when no layout changes, its related
example is still highlighted. Preview changes never modify project source files or saved settings.

## Formatting controls

- Group and sort eligible fields, separate static fields and align initializer equals signs.
- Group safe constructor assignments and locals; control assignment blank lines separately.
- Wrap parameters, call arguments, chains and conditions using the language right margin.
- Control spaces in single/multiple-argument calls and annotation arrays independently.
- Place method, parameter, field and class annotations independently.
- Configure block spacing, cases, Kotlin lambdas and Elvis expressions.

There are 57 Boolean preferences. Shared-prefix field grouping is off by default; the others
are on. Parameter/argument order and method/constructor declaration order are preserved.
Only eligible independent field and constructor runs are rearranged.

See [settings and common recipes](docs/settings.md) and the [changelog](CHANGELOG.md).

## Share settings

Use **Export settings...** to save a portable XML profile, including pending checkbox edits.
A colleague imports it with **Import settings...**, reviews the preview and clicks Apply/OK.
Profiles contain Baseline preferences only; configure IntelliJ code-style schemes and right
margins separately. Older profiles remain supported.

## Format on save and before commit

Enable **Tools / Actions on Save / Reformat code / Whole file** for Java and Kotlin.
Baseline uses the existing formatter pipeline; no separate save listener is required.

Baseline skips partial selections. Commit checks that reformat changed lines alone do not
apply all whole-file rules. Use whole-file formatting before committing. Baseline does not
install a Git push hook or enforce formatting before push.

## Development

The build uses JVM 17 and the local IntelliJ installation at `S:\IntelliJ IDEA\idea` with its
bundled Java/Kotlin plugins. Update that path in `build.gradle.kts` on another host.
Offline builds require cached dependencies.

```powershell
.\gradlew.bat clean test buildPlugin --offline
```

The installable ZIP is written to `build/distributions`; tests are reported under
`build/reports/tests/test`. See the [implementation guide](docs/architecture.md).
Approved layouts are included as [Java](src/main/resources/JavaSample.txt) and
[Kotlin](src/main/resources/KotlinSample.txt) text examples.

## License

[Apache License 2.0](LICENSE).
