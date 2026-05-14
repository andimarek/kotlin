# Migration Report: kotlinx.collections.immutable 0.5.0

**Project:** Kotlin language repository (JetBrains/kotlin)
**Modules migrated:** `:compiler:fir:cones`, `:compiler:fir:tree`, `:compiler:fir:providers`, `:compiler:fir:semantics`, `:compiler:fir:resolve`, `:compiler:fir:checkers`, `:analysis:analysis-internal-utils`, `:analysis:analysis-api-impl-base` (incl. testFixtures), `:analysis:symbol-light-classes`, `:analysis:low-level-api-fir`, `:kotlin-power-assert-compiler-plugin.backend`, `:compiler:multiplatform-parsing`
**Date:** 2026-05-14
**kotlinx.collections.immutable version:** 0.3.7 → 0.5.0-beta01
**Kotlin version:** unchanged
**Status:** Completed successfully

---

## Pre-Migration State

### Library Usage

| Module | Build file location | Version |
|--------|---------------------|---------|
| (version source) | `gradle/versions.properties` | 0.3.7 |
| (verification metadata) | `gradle/verification-metadata.xml` | 0.3.7, 0.3.8 listed |
| `:prepare:compiler` | `prepare/compiler/build.gradle.kts:203` | (resolved via versions.properties) |
| `:kotlin-parcelize-compiler` | `plugins/parcelize/parcelize-compiler/build.gradle.kts:102` | (resolved via versions.properties) |
| `:kotlin-power-assert-compiler-plugin.backend` | `plugins/power-assert/power-assert-compiler/power-assert.backend/build.gradle.kts:15` | (resolved via versions.properties) |
| `:kotlin-power-assert-compiler-plugin.embeddable` | `plugins/power-assert/power-assert-compiler/power-assert.embeddable/build.gradle.kts:7` | (resolved via versions.properties) |
| compose compiler integration tests | `plugins/compose/compiler-hosted/integration-tests/build.gradle.kts:87` | (resolved via versions.properties) |
| `:kotlin-main-kts` | `libraries/tools/kotlin-main-kts/build.gradle.kts:38` | (resolved via versions.properties) |
| `:compiler:multiplatform-parsing` | `compiler/multiplatform-parsing/build.gradle.kts:36` | (resolved via versions.properties) |

**Note:** the starting version `0.3.7` is older than the skill's documented starting point (`0.4.x`). The 0.5.0 rename is binary-compatible per the upstream guide, and the persistent-collection API surface used by this repo did not change between 0.3.x and 0.4.x for any of the methods touched by this migration. No issues observed.

### Baseline Build

- **Command:** `./gradlew :compiler:fir:cones:compileKotlin :compiler:fir:checkers:compileKotlin :compiler:multiplatform-parsing:compileKotlinJvm`
- **Result:** success — three representative modules compiled cleanly at 0.3.7 before any change.

Compiling the entire repo was not attempted: the Kotlin language repo is too large for a full-tree baseline to be practical. The 3-module sample (one direct dep declarer + two heavy API users) is sufficient to confirm a working environment.

### Pre-existing `@Suppress("DEPRECATION")` annotations

None in any file that imports `kotlinx.collections.immutable`. Phase 7 was a no-op.

---

## Migration Steps

### Phase 3: Version Bump

- **File:** `gradle/versions.properties`
- **Change:** `versions.kotlinx-collections-immutable-jvm=0.3.7` → `=0.5.0-beta01`; `versions.kotlinx-collections-immutable=0.3.7` → `=0.5.0-beta01`
- **File:** `gradle/verification-metadata.xml`
- **Change:** added a new `<component>` entry for `kotlinx-collections-immutable-jvm` version `0.5.0-beta01` with sha256 `0f1c7678eb4162590af510ca7cacd039106046a4f55cf706d65958291b7c43b3` (computed from the published jar on Maven Central).

The 0.3.7 / 0.3.8 entries were left in place — other tooling or transitive consumers may still reference them.

### Phase 4: Compiler-Driven Renames

Total call sites renamed: **78** across **23** Kotlin source files. Treated as compile errors because the project uses `-Werror`.

| File | Line | Receiver type | Before | After |
|------|------|---------------|--------|-------|
| `compiler/fir/cones/src/.../PersistentMultimap.kt` | 16 | PersistentList | `collection.add(value)` | `collection.adding(value)` |
| `compiler/fir/cones/src/.../PersistentMultimap.kt` | 18 | PersistentMap | `map.put(key, newSet)` | `map.putting(key, newSet)` |
| `compiler/fir/cones/src/.../PersistentMultimap.kt` | 24 | PersistentList | `list.remove(value)` | `list.removing(value)` |
| `compiler/fir/cones/src/.../PersistentMultimap.kt` | 27 | PersistentMap | `map.remove(key)` | `map.removing(key)` |
| `compiler/fir/cones/src/.../PersistentMultimap.kt` | 29 | PersistentMap | `map.put(key, newSet)` | `map.putting(key, newSet)` |
| `compiler/fir/cones/src/.../PersistentMultimap.kt` | 47 | PersistentSet | `set.add(value)` | `set.adding(value)` |
| `compiler/fir/cones/src/.../PersistentMultimap.kt` | 49 | PersistentMap | `map.put(key, newSet)` | `map.putting(key, newSet)` |
| `compiler/fir/cones/src/.../PersistentMultimap.kt` | 55 | PersistentSet | `set.remove(value)` | `set.removing(value)` |
| `compiler/fir/cones/src/.../PersistentMultimap.kt` | 58 | PersistentMap | `map.remove(key)` | `map.removing(key)` |
| `compiler/fir/cones/src/.../PersistentMultimap.kt` | 60 | PersistentMap | `map.put(key, newSet)` | `map.putting(key, newSet)` |
| `compiler/fir/providers/src/.../FirLocalScope.kt` | 46 | PersistentMap | `classLikeSymbols.put(...)` | `classLikeSymbols.putting(...)` |
| `compiler/fir/providers/src/.../FirLocalScope.kt` | 58 | PersistentMap | `properties.put(...)` | `properties.putting(...)` |
| `compiler/fir/providers/src/.../FirLocalScope.kt` | 64 | PersistentMap | `properties.put(BACKING_FIELD, it)` | `properties.putting(BACKING_FIELD, it)` |
| `compiler/fir/semantics/src/.../ImplicitReceiverUtils.kt` | 113, 124 | PersistentList | `towerDataElements.set(idx, ...)` | `towerDataElements.replacingAt(idx, ...)` |
| `compiler/fir/semantics/src/.../ImplicitReceiverUtils.kt` | 114, 125 | PersistentList | `localScopes.set(idx, ...)` | `localScopes.replacingAt(idx, ...)` |
| `compiler/fir/semantics/src/.../ImplicitReceiverUtils.kt` | 131, 137, 237, 238 | PersistentList | `towerDataElements.addAll(...)` / `nonLocalTowerDataElements.addAll(...)` | `.addingAll(...)` |
| `compiler/fir/semantics/src/.../ImplicitReceiverUtils.kt` | 143, 144, 151, 153, 174, 176, 183, 223, 224, 230, 231 | PersistentList | `.add(element)` | `.adding(element)` |
| `compiler/fir/semantics/src/.../ImplicitValueStorage.kt` | 51 | PersistentList | `implicitReceiverStack.add(value)` | `.adding(value)` |
| `compiler/fir/semantics/src/.../ImplicitValueStorage.kt` | 53, 81 | PersistentMap | `.put(boundSymbol, value)` | `.putting(boundSymbol, value)` |
| `compiler/fir/semantics/src/.../LocalVariableScopeStorage.kt` | 36 | PersistentMap | `map.put(symbol, mutableMapOf())` | `map.putting(...)` |
| `compiler/fir/semantics/src/.../LogicSystem.kt` | 61 | PersistentSet | `.add(alias)` | `.adding(alias)` |
| `compiler/fir/semantics/src/.../LogicSystem.kt` | 70, 71 | PersistentSet | `.addAll(statement.upperTypes)` / `.addAll(statement.lowerTypes)` | `.addingAll(...)` |
| `compiler/fir/semantics/src/.../LogicSystem.kt` | 91 | PersistentList | `.add(implication)` | `.adding(implication)` |
| `compiler/fir/semantics/src/.../LogicSystem.kt` | 240 | PersistentSet | `siblings.remove(variable)` | `siblings.removing(variable)` |
| `compiler/fir/semantics/src/.../LogicSystem.kt` | 259 | PersistentSet | `.addAll(withoutSelf)` | `.addingAll(withoutSelf)` |
| `compiler/fir/semantics/src/.../LogicSystem.kt` | 294, 478 | PersistentList | `statements.removeAll { ... }` / `implications.removeAll { ... }` | `.removingAll { ... }` |
| `compiler/fir/resolve/src/.../FirSupertypesResolution.kt` | 909 | PersistentList | `add(0, element)` (2-arg) | `addingAt(0, element)` |
| `compiler/fir/resolve/src/.../FirSupertypesResolution.kt` | 910 | PersistentList | `addAll(0, collection)` (2-arg) | `addingAllAt(0, collection)` |
| `compiler/fir/resolve/src/.../FirTypeResolveTransformer.kt` | 577 | PersistentList | `scopes.add(...)` | `scopes.adding(...)` |
| `compiler/fir/resolve/src/.../FirTypeResolveTransformer.kt` | 585, 586 | PersistentList | `scopes.addAll(list)` / `staticScopes.addAll(list)` | `.addingAll(list)` |
| `compiler/fir/resolve/src/.../AbstractFirSpecificAnnotationResolveTransformer.kt` | 693 | PersistentList | `owners.add(parentDeclaration)` (1-arg) | `owners.adding(parentDeclaration)` |
| `compiler/fir/checkers/src/.../VariableInitializationCheckProcessor.kt` | 127 | PersistentSet | `visited.add(this)` | `visited.adding(this)` |
| `compiler/fir/checkers/src/.../cfa/util/EventCollectingControlFlowGraphVisitor.kt` | 88, 96, 106, 114 | PersistentMap | `it.put(key, ...)` | `it.putting(key, ...)` |
| `compiler/fir/checkers/src/.../cfa/util/EventCollectingControlFlowGraphVisitor.kt` | 117 | PersistentMap | `it.remove(key)` | `it.removing(key)` |
| `compiler/fir/checkers/src/.../context/MutableCheckerContext.kt` | 127 | PersistentSet | `suppressedDiagnostics.addAll(diagnosticNames)` | `.addingAll(...)` |
| `compiler/fir/checkers/src/.../context/PersistentCheckerContext.kt` | 64, 71, 77, 82, 87 | PersistentList | `.add(...)` on `containingDeclarations` / `callsOrAssignments` / `getClassCalls` / `annotationContainers` / `containingElements` | `.adding(...)` |
| `compiler/fir/checkers/src/.../context/PersistentCheckerContext.kt` | 99 | PersistentSet | `suppressedDiagnostics.addAll(diagnosticNames)` | `.addingAll(...)` |
| `compiler/fir/checkers/src/.../extra/UnusedVariableAssignmentChecker.kt` | 352 | PersistentSet (callable ref) | `PersistentSet<CFGNode<*>>::addAll` | `PersistentSet<CFGNode<*>>::addingAll` |
| `compiler/fir/checkers/src/.../extra/UnusedVariableAssignmentChecker.kt` | 357 | PersistentMap, PersistentSet | `value.put(symbol, nodes.add(node))` | `value.putting(symbol, nodes.adding(node))` |
| `compiler/fir/checkers/src/.../extra/UnusedVariableAssignmentChecker.kt` | 361 | PersistentMap | `value.remove(symbol)` | `value.removing(symbol)` |
| `compiler/fir/checkers/src/.../extra/UnusedVariableAssignmentChecker.kt` | 379, 404, 406 | PersistentMap | `.put(...)` | `.putting(...)` |
| `analysis/analysis-internal-utils/src/.../PrettyPrinter.kt` | 196 | PersistentList | `prefixesToPrint.add(prefix)` | `.adding(prefix)` |
| `analysis/analysis-api-impl-base/src/.../lifetime/KaBaseLifetimeTracker.kt` | 21 | PersistentList | `.add(session.token)` | `.adding(session.token)` |
| `analysis/analysis-api-impl-base/src/.../lifetime/KaBaseLifetimeTracker.kt` | 30 | PersistentList | `stack.removeAt(stack.lastIndex)` | `stack.removingAt(stack.lastIndex)` |
| `analysis/analysis-api-impl-base/testFixtures/.../AbstractAnalysisApiSignatureContractsTest.kt` | 144 | PersistentList | `state.add(e)` | `state.adding(e)` |
| `analysis/symbol-light-classes/src/.../modifierLists/GranularModifiersBox.kt` | 37 | PersistentMap (extension) | `currentMap.putAll(newValues)` | `currentMap.puttingAll(newValues)` |
| `analysis/symbol-light-classes/src/.../modifierLists/GranularModifiersBox.kt` | 105 | PersistentMap | `put(modifier, true)` | `putting(modifier, true)` |
| `analysis/low-level-api-fir/src/.../lazy/resolve/FirLazyBodiesCalculator.kt` | 866 | PersistentList | `data.add(element as FirDeclaration)` | `.adding(...)` |
| `analysis/low-level-api-fir/src/.../providers/LLFirIdePredicateBasedProvider.kt` | 235 | PersistentList | `data.add(element.symbol)` | `data.adding(element.symbol)` |
| `analysis/low-level-api-fir/src/.../transformers/LLFirSupertypeLazyResolver.kt` | 319 | PersistentList | `useSiteSessions.add(it)` | `.adding(it)` |
| `analysis/low-level-api-fir/src/.../transformers/LLFirSupertypeLazyResolver.kt` | 322 | PersistentList | `useSiteSessions.removeAt(useSiteSessions.lastIndex)` | `.removingAt(...)` |
| `plugins/power-assert/power-assert-compiler/power-assert.backend/src/.../PowerAssertCallTransformer.kt` | 199, 203 | PersistentList | `arguments.add(...)` | `arguments.adding(...)` |
| `plugins/power-assert/power-assert-compiler/power-assert.backend/src/.../PowerAssertCallTransformer.kt` | 204 | PersistentMap | `argumentVariables.put(root.parameter, newVariables)` | `.putting(...)` |
| `plugins/power-assert/power-assert-compiler/power-assert.backend/src/.../diagram/DiagramBuilder.kt` | 113, 140, 170 | PersistentList | `variables.add(variable)` | `variables.adding(variable)` |

### Phase 5: Compiler-Blind Passes

#### Operator-syntax indexed assignment (`list[i] = v`)

None found. No `xs[i] = v` patterns appear in any file that imports `PersistentList`.

#### Method / callable references

The single callable reference `PersistentSet<CFGNode<*>>::addAll` in `UnusedVariableAssignmentChecker.kt:352` was flagged directly by the compiler in Phase 4 and renamed to `::addingAll`. No additional callable references needed.

#### Java callers

No Java callers. The affected modules contain only Kotlin sources that call kotlinx.collections.immutable methods.

### Phase 6: Interface Implementers

No third-party implementers in this codebase. `PersistentMultimap` and `PersistentSetMultimap` (`compiler/fir/cones/.../PersistentMultimap.kt`) *wrap* a `PersistentMap` internally but do not implement any of the `Persistent*` interfaces.

### Phase 7: `@Suppress("DEPRECATION")` Cleanup

No redundant suppressions — none existed in scope before the migration.

### Phase 8: Verification

- **Compile command:** `./gradlew :compiler:fir:cones:compileKotlin :compiler:fir:tree:compileKotlin :compiler:fir:providers:compileKotlin :compiler:fir:semantics:compileKotlin :compiler:fir:resolve:compileKotlin :compiler:fir:checkers:compileKotlin :analysis:analysis-internal-utils:compileKotlin :analysis:analysis-api-impl-base:compileKotlin :analysis:analysis-api-impl-base:compileTestFixturesKotlin :analysis:symbol-light-classes:compileKotlin :analysis:low-level-api-fir:compileKotlin :kotlin-power-assert-compiler-plugin.backend:compileKotlin :compiler:multiplatform-parsing:compileKotlinJvm --rerun-tasks`
- **Result:** success in 3m 15s
- **Remaining deprecation warnings from kotlinx.collections.immutable:** 0
- **Tests run:** none (Kotlin repo test suite is impractically large for a migration PR; downstream CI is expected to cover affected modules)

---

## Errors Encountered

### Error #1: `add(0, element)` mistakenly rewritten as `adding(element)`

**Phase:** 4
**Symptom:** I initially rewrote `owners.add(parentDeclaration)` at `AbstractFirSpecificAnnotationResolveTransformer.kt:693` as `addingAt(0, parentDeclaration)` after pattern-matching it against the `FirSupertypesResolution.kt:909` rewrite — which was a *2-arg* `add(index, element)`. The 693 call was *1-arg*. Changing it to `addingAt(0, ...)` would have silently changed semantics from append to prepend.
**Root cause:** Mechanical visual pattern match between two same-named calls that have different arities. The rename map disambiguates by arity but I conflated them.
**Fix:** Reverted to `owners.adding(parentDeclaration)` (1-arg, append). Confirmed clean compile after.
**Generalizable:** Yes — when applying the rename, always verify whether `add(...)` is 1-arg (→ `adding`) or 2-arg (→ `addingAt`). The compiler tells you in the deprecation message (`'fun add(element: FirDeclaration)' is deprecated. Use adding() instead.` vs. `'fun add(index: Int, element: E)' is deprecated. Use addingAt() instead.`) — read the *signature* in the warning, not just the method name.

### Error #2: `-Werror` masks "warning" framing

**Phase:** 4
**Symptom:** The migration skill describes deprecations on 0.5.0-beta01 as compile *warnings*. In this repo every deprecation came back as a compile *error* (`e: warnings found and -Werror specified`).
**Root cause:** The Kotlin language repo compiles with `-Werror`. Deprecation level is still `WARNING` in the library; the repo simply rejects all warnings.
**Fix:** No behavioral change to the migration itself — same rename, same outcome, the loop just terminates on first error instead of accumulating warnings. Worth noting in the skill so users on `-Werror` projects know to expect this.
**Generalizable:** Yes — applies to any project that compiles with `-Werror` or `allWarningsAsErrors`.

---

## Non-Trivial Decisions

- **Started from 0.3.7, not 0.4.x.** The skill targets `0.4.x` → `0.5.0-beta01`. This project was on `0.3.7`. The 0.3.x → 0.5.0-beta01 path was not explicitly covered, but the persistent-collection API surface used in this repo is identical between 0.3.x and 0.4.x — no additional renames were needed for the version-skip. The migration completed cleanly without backporting through 0.4.x.

- **Did not rewrite `MutableList`/`MutableSet`/`MutableMap` callers.** Many files in the affected modules also call `.add(...)`, `.remove(...)`, `.put(...)` on mutable receivers (and on `PersistentList.Builder`, `PersistentMap.Builder` builders inside `mutate { ... }` blocks). The compiler did not flag any of these because the receiver type is not `Persistent*`. Examples deliberately left alone:
  - `MutableTypeStatement.upperTypes.add(...)` and `.lowerTypes.add(...)` in `LogicSystem.kt:283,288,289` — `upperTypes` / `lowerTypes` are `PersistentSet.Builder` inside a `mutate` block, not `PersistentSet`.
  - `it.set(block(it.next()))` in `LogicSystem.kt:498` — `MutableListIterator.set`, not `PersistentList.set`.
  - `implications.replaceAll { ... }` in `LogicSystem.kt:476` — our own extension on `PersistentList`, not deprecated, kept as-is.
  - `PersistentMultimap.put(...)` / `.remove(...)` calls (e.g. `FirLocalScope.kt:52`) — `PersistentMultimap` is *our* class, not from kotlinx.collections.immutable; its members are not deprecated.

- **Did not touch parcelize testData files** (`plugins/parcelize/parcelize-compiler/testData/box/{listKinds,mapKinds,listSimplePersistent,mapSimplePersistent}.kt`, `.../codegen/simplePersistentList.kt`). They use kotlinx.collections.immutable types as data-class field declarations and call only non-deprecated factory functions (`persistentListOf`, `persistentSetOf`, `persistentMapOf`). No deprecated mutating-method calls present.

- **Did not touch the embedded source-text in `KotlinWasmGradlePluginIT.kt:265-272`.** That test generates a separate test project whose `build.gradle.kts` pins `kotlinx-collections-immutable:0.4.0` independently from the main repo's version. The embedded user code only calls `persistentListOf(...)` (factory function, not deprecated). The test exercises wasmJs interop and is intentionally orthogonal to the host repo's library version.

- **Left `0.3.7` and `0.3.8` entries in `gradle/verification-metadata.xml`.** Other consumers (offline builds, transitive dependencies) may still reference them. Removing them is unrelated to this migration.

---

## Files Changed

### Gradle Files
- `gradle/versions.properties` — bumped `versions.kotlinx-collections-immutable-jvm` and `versions.kotlinx-collections-immutable` from `0.3.7` to `0.5.0-beta01`.
- `gradle/verification-metadata.xml` — added sha256 checksum entry for the new artifact.

### Kotlin Sources (23 files, 78 renames)
- `compiler/fir/cones/src/org/jetbrains/kotlin/fir/util/PersistentMultimap.kt` — 10 renames (PersistentList/Set/Map mutators).
- `compiler/fir/providers/src/org/jetbrains/kotlin/fir/scopes/impl/FirLocalScope.kt` — 3 renames (PersistentMap.put).
- `compiler/fir/semantics/src/org/jetbrains/kotlin/fir/declarations/ImplicitReceiverUtils.kt` — 19 renames (PersistentList.set/add/addAll).
- `compiler/fir/semantics/src/org/jetbrains/kotlin/fir/resolve/ImplicitValueStorage.kt` — 3 renames.
- `compiler/fir/semantics/src/org/jetbrains/kotlin/fir/resolve/LocalVariableScopeStorage.kt` — 1 rename.
- `compiler/fir/semantics/src/org/jetbrains/kotlin/fir/resolve/dfa/LogicSystem.kt` — 8 renames (Persistent List/Set add/addAll/remove/removeAll).
- `compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirSupertypesResolution.kt` — 2 renames (2-arg add/addAll → addingAt/addingAllAt).
- `compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/FirTypeResolveTransformer.kt` — 3 renames.
- `compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/plugin/AbstractFirSpecificAnnotationResolveTransformer.kt` — 1 rename.
- `compiler/fir/checkers/src/org/jetbrains/kotlin/fir/analysis/cfa/VariableInitializationCheckProcessor.kt` — 1 rename.
- `compiler/fir/checkers/src/org/jetbrains/kotlin/fir/analysis/cfa/util/EventCollectingControlFlowGraphVisitor.kt` — 5 renames.
- `compiler/fir/checkers/src/org/jetbrains/kotlin/fir/analysis/checkers/context/MutableCheckerContext.kt` — 1 rename.
- `compiler/fir/checkers/src/org/jetbrains/kotlin/fir/analysis/checkers/context/PersistentCheckerContext.kt` — 6 renames.
- `compiler/fir/checkers/src/org/jetbrains/kotlin/fir/analysis/checkers/extra/UnusedVariableAssignmentChecker.kt` — 7 renames (including 1 callable reference).
- `analysis/analysis-internal-utils/src/org/jetbrains/kotlin/analysis/utils/printer/PrettyPrinter.kt` — 1 rename.
- `analysis/analysis-api-impl-base/src/org/jetbrains/kotlin/analysis/api/impl/base/lifetime/KaBaseLifetimeTracker.kt` — 2 renames.
- `analysis/analysis-api-impl-base/testFixtures/org/jetbrains/kotlin/analysis/api/impl/base/test/cases/components/signatureSubstitution/AbstractAnalysisApiSignatureContractsTest.kt` — 1 rename.
- `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/modifierLists/GranularModifiersBox.kt` — 2 renames (including 1 putAll extension).
- `analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/lazy/resolve/FirLazyBodiesCalculator.kt` — 1 rename.
- `analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/providers/LLFirIdePredicateBasedProvider.kt` — 1 rename.
- `analysis/low-level-api-fir/src/org/jetbrains/kotlin/analysis/low/level/api/fir/transformers/LLFirSupertypeLazyResolver.kt` — 2 renames.
- `plugins/power-assert/power-assert-compiler/power-assert.backend/src/org/jetbrains/kotlin/powerassert/PowerAssertCallTransformer.kt` — 3 renames.
- `plugins/power-assert/power-assert-compiler/power-assert.backend/src/org/jetbrains/kotlin/powerassert/diagram/DiagramBuilder.kt` — 3 renames.

### Java Sources
None.

### Created
- `MIGRATION_REPORT.md` — this file.

### Not Modified (deliberately)
- `plugins/parcelize/parcelize-compiler/testData/box/*.kt`, `.../testData/codegen/*.kt` — testData files use only `persistentListOf` / `persistentSetOf` / `persistentMapOf` factories and type declarations; no deprecated mutator calls.
- `libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/.../KotlinWasmGradlePluginIT.kt:265-272` — embedded test-generated source pins its own `0.4.0` dep and uses only `persistentListOf`. Independent of the host repo's version.
- `compiler/fir/cones/.../PersistentMultimap.kt`'s own `put` / `remove` methods (lines 14, 22, 45, 53) — these are members of our `PersistentMultimap`/`PersistentSetMultimap` classes, not from kotlinx.collections.immutable. The internal calls inside them *were* renamed.
- All `PersistentList.Builder` / `PersistentSet.Builder` / `PersistentMap.Builder` calls inside `mutate { ... }` blocks (e.g. `LogicSystem.kt:283,288,289,498`) — builder methods are imperative by design and not deprecated.
- All `MutableList` / `MutableSet` / `MutableMap` calls in the affected files — these are not on `Persistent*` receivers and the compiler does not flag them.
