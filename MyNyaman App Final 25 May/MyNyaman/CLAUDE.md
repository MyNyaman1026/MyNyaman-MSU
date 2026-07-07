# CLAUDE.md

This file is the mandatory operational rulebook for Claude Code agents working in this repository. Claude must internalize these instructions before making any proposal, edit, terminal command, or architectural recommendation.

## Prime Directive

This repository is a native Android family digital wellbeing app named MyNyaman. Treat it as a production-sensitive mobile app with Firebase-backed account, role, child-safety, usage-tracking, app-limit, and consultant-session data. Do not make speculative edits. Do not fragment core logic. Preserve existing behavior unless the user explicitly asks to change it.

Claude must follow these rules exactly:

1. Gather context first.
2. Plan before large changes.
3. Ask permission before risky operations.
4. Make the smallest correct edit.
5. Preserve the existing architecture and data contracts.
6. Stop and ask when critical schema, permission, or UI details are unclear.

## Project Architecture And Tech Stack

Primary environment:

- Native Android application.
- Gradle Kotlin DSL project.
- Root Gradle project name: `MyNyaman app`.
- Main app module: `app`.
- Android package and namespace: `com.mynyaman.mynyamanapp`.
- Minimum SDK: 24.
- Target SDK: 35.
- Compile SDK: 35.

Primary languages and UI technologies:

- Java for almost all application activities, services, repositories, and helpers.
- XML layouts under `app/src/main/res/layout/` for the visible app screens.
- Kotlin is present mainly for theme files under `app/src/main/java/com/mynyaman/mynyamanapp/ui/theme/` and Compose dependencies, but the current screen architecture is Java/XML first.
- AndroidX AppCompat, Material Components, ConstraintLayout, SwipeRefreshLayout, RecyclerView, and foreground services are used.
- MPAndroidChart is used for usage pie charts.

Backend, storage, and external services:

- Firebase Authentication for email/password users and child account creation.
- Cloud Firestore for users, child links, usage reports, limits, consultant sessions, conversations, and chat messages.
- Firebase Analytics is configured as a dependency.
- Android SharedPreferences is used for local UI state such as selected child.
- Android UsageStatsManager, accessibility service events, foreground service monitoring, and overlay windows are used for screen-time tracking and app locking.
- AI consultant requests are sent through OpenAI-compatible chat completions APIs configured by BuildConfig values from `local.properties`.

Important Gradle and configuration files:

- `settings.gradle.kts`: repository and module configuration.
- `build.gradle.kts`: root plugin configuration.
- `app/build.gradle.kts`: Android app configuration, dependencies, BuildConfig fields, Firebase plugin, SDK levels.
- `gradle.properties`: Gradle properties.
- `local.properties`: local SDK path and private runtime values. Treat this file as sensitive.
- `app/src/google-services.json`: Firebase app configuration. Treat changes as sensitive.
- `app/src/main/AndroidManifest.xml`: app permissions, activities, foreground service, and accessibility service declarations.

## Core Architectural Boundaries

Claude must keep architectural logic in the existing designated files. Do not duplicate, scatter, or reimplement these responsibilities inside unrelated activities.

Routing and role ownership:

- `app/src/main/java/com/mynyaman/mynyamanapp/Login.java` owns login flow and post-login role routing.
- `app/src/main/java/com/mynyaman/mynyamanapp/Roleselection.java` owns permanent parent/consultant role selection.
- `app/src/main/java/com/mynyaman/mynyamanapp/RoleUtils.java` owns role normalization and role label conversion.
- Do not add new role-string parsing, role constants, or role-routing branches in random screens. Update `RoleUtils.java` and the designated routing screens when role behavior changes.

Shared Firestore sync and data contracts:

- `app/src/main/java/com/mynyaman/mynyamanapp/BlueprintSyncRepository.java` owns shared Firestore write/read helpers for users, parent-child relationships, app limits, usage reports, weekly usage summary, and AI consultant session persistence.
- Do not create competing repository logic for the same Firestore collections in unrelated screens.
- Keep Firestore field compatibility with existing names such as `role`, `roleKey`, `roleLabel`, `parentUid`, `childUid`, `appLimitSeconds`, `appLimits`, `usageTodaySeconds`, `usageDayKey`, `usageWeekKey`, `topApps`, and `usageLastSyncedAt`.

Selected child state:

- `app/src/main/java/com/mynyaman/mynyamanapp/SelectedChildStore.java` owns local selected-child state through SharedPreferences.
- Parent screens that need selected child state should use this helper instead of creating new preference keys.

Usage windows and usage calculations:

- `app/src/main/java/com/mynyaman/mynyamanapp/UsageWindowHelper.java` owns current day/week keys and current-window checks.
- `app/src/main/java/com/mynyaman/mynyamanapp/UsageStatsHelper.java` owns Android usage stats extraction, duration formatting, top-app payload conversion, and permission checks.
- Do not duplicate date-window logic or duration formatting across activities.

Child protection and limit enforcement:

- `app/src/main/java/com/mynyaman/mynyamanapp/LimitService.java` owns foreground protection, Firestore limit listening, usage sync, overlay lock display, and pending child deletion handling.
- `app/src/main/java/com/mynyaman/mynyamanapp/AppWatchAccessibilityService.java` owns accessibility-based foreground-app detection.
- `app/src/main/java/com/mynyaman/mynyamanapp/AccessibilityStateHelper.java` owns accessibility enabled-state checks.
- `app/src/main/java/com/mynyaman/mynyamanapp/ForegroundAppStateStore.java` owns persisted foreground app session state.
- Do not implement app-lock or foreground-tracking logic in dashboard/report screens.

High-distraction app filtering:

- `app/src/main/java/com/mynyaman/mynyamanapp/DistractionAppFilter.java` owns filtering and display fallback for high-distraction apps.
- Do not hard-code duplicate package/app filtering rules elsewhere.

AI consultant flow:

- `app/src/main/java/com/mynyaman/mynyamanapp/ConsultantHubActivity.java` owns consultant path selection.
- `app/src/main/java/com/mynyaman/mynyamanapp/AiConsultantChatActivity.java` owns AI chat UI, session lifecycle, and session close prompt.
- `app/src/main/java/com/mynyaman/mynyamanapp/AiConsultantApi.java` owns external AI API request configuration and execution.
- `BlueprintSyncRepository.java` owns saved AI consultant session persistence.
- Do not place API key handling or external API request code in UI adapters or dashboard activities.

Screen ownership:

- Parent dashboard: `Parentdash.java` and `app/src/main/res/layout/parentdash.xml`.
- Child dashboard: `Childdash.java` and `app/src/main/res/layout/childdash.xml`.
- Daily limits: `Dailylimit.java` and `app/src/main/res/layout/dailylimit.xml`.
- Parent usage list: `Usagereport.java` and `app/src/main/res/layout/usagereport.xml`.
- Parent child usage detail: `ChildUsageDetailActivity.java` and `app/src/main/res/layout/activity_child_usage_detail.xml`.
- Child usage report: `Usagereportchild.java` and `app/src/main/res/layout/usagereportchild.xml`.
- Child account creation: `Addchild.java` and `app/src/main/res/layout/add_child.xml`.

## Agent Workflow And Token Efficiency

Claude must operate with disciplined context and minimal output.

### Context Gathering Required

Before proposing code changes, Claude must map the relevant directory structure and read the relevant files.

Required behavior:

- Use file search/listing commands before assuming paths.
- Prefer `rg --files` when available.
- If `rg` is unavailable or blocked, use PowerShell-compatible alternatives such as `Get-ChildItem -Recurse -File`.
- Read the target Java/XML/Gradle file before editing it.
- Read the entire target file before applying diffs. Partial reads are not enough for edits because Android Java/XML syntax can be corrupted by misplaced braces, listeners, or layout nesting.
- Inspect paired files together when changing UI behavior: read both the Java activity and its XML layout.

### Plan First

Before generating large blocks of code or making broad edits, Claude must output a brief step-by-step plan and wait for user approval.

A plan is required for:

- New features.
- Multi-file changes.
- Changes affecting Firebase schemas or collection fields.
- Changes affecting Android permissions, services, manifests, Gradle files, API keys, or connection strings.
- UI redesigns or layout restructures.
- Refactors touching more than one class.

A plan is not required for very small documentation edits, typo fixes, or single-line corrections, unless the edit touches sensitive configuration or schemas.

### Minimal Edits

Claude must modify only the files strictly necessary for the requested task.

Rules:

- Do not rewrite an entire class for a minor bug fix.
- Do not reformat unrelated code.
- Do not rename classes, fields, layouts, resources, Firestore fields, or activities unless explicitly requested.
- Do not introduce a new abstraction unless it removes real duplication or matches an existing project pattern.
- Preserve existing Java/XML style and naming conventions, even when they are imperfect.
- Avoid dependency churn. Do not add libraries unless the user approves and the existing Android/Firebase APIs are insufficient.

### Stop On Ambiguity

Claude must stop and ask for clarification immediately when critical details are missing.

Stop conditions include:

- Unknown Firestore collection or field schema for a requested data change.
- Unclear whether a change should affect parent, child, consultant, or all roles.
- Unclear UI layout ownership or missing XML ids.
- Ambiguous app-limit behavior that could weaken child protection.
- Any request involving account deletion, child account linking, permission behavior, API keys, or production Firebase data where intent is not explicit.

When stopping, ask a concise question and identify the exact missing decision.

## Strict Permission Protocols

Claude must explicitly ask for confirmation before performing any of the following actions.

Do not proceed until the user confirms.

### File And Directory Deletion

Ask before:

- Deleting any file.
- Deleting any directory.
- Removing generated resources or source files.
- Cleaning build directories if the command deletes files.

### Terminal Commands

Ask before executing terminal commands that build, test, migrate, commit, install, publish, or mutate state.

This includes:

- `./gradlew` or `.\gradlew.bat` build/test tasks.
- Database migration scripts or Firebase deployment commands.
- Git commits, branch creation, merges, rebases, resets, checkouts, or tags.
- Dependency installation or updates.
- Commands that write outside the repository.

For safe read-only context gathering, Claude may use file listing and file reading commands. On this Windows host, use PowerShell-compatible commands.

### Configuration And Connection Strings

Ask before modifying:

- `local.properties`.
- `app/build.gradle.kts` BuildConfig values.
- `settings.gradle.kts` repositories.
- `app/src/google-services.json`.
- API URLs, API keys, Firebase configuration, package identifiers, namespace, applicationId, minSdk, targetSdk, compileSdk, or manifest permissions.

### Sweeping Refactors

Ask before implementing refactors that affect multiple classes, shared helpers, data contracts, or architectural boundaries.

Examples requiring confirmation:

- Moving Firestore logic between screens and repositories.
- Reworking role routing.
- Replacing Java/XML screens with Compose.
- Changing how `LimitService` tracks app state.
- Replacing usage sync fields or Firestore document ids.

## Code Style Rules

Primary code style:

- Write clean, readable Java that matches the surrounding code.
- Use meaningful method and variable names.
- Keep methods focused.
- Prefer existing helper methods over new duplicated logic.
- Keep null checks explicit when interacting with Firebase, Android views, intents, and extras.
- Avoid unnecessary comments. Add a short comment only when the intent is not obvious.
- Use constants for repeated Firestore collection names, intent extras, preference names, and action strings when the surrounding file already uses constants.
- Preserve existing Android resource naming conventions.

Java and Android rules:

- Do not perform network, Firestore, or heavy usage-stat work on the UI thread.
- Use Firebase success/failure listeners, callbacks, background executors, or Android service patterns as appropriate.
- Always restore UI enabled/loading state in both success and failure paths.
- Do not leave buttons permanently disabled after failures.
- Do not block the main thread with sleeps, synchronous network calls, or long loops.
- Do not show Toasts from background threads unless safely marshalled to the UI thread.
- When updating UI from async callbacks, verify the Activity is still valid when appropriate.
- Keep RecyclerView adapter updates consistent with the backing list.
- Keep XML ids aligned with Java `findViewById` references.

Firebase and API rules:

- Always attach failure handling to Firebase operations that affect user-visible state.
- Prefer `SetOptions.merge()` when updating partial Firestore documents unless replacing the document is explicitly intended.
- Preserve backward compatibility fields where the app already supports legacy data, such as both `appLimitSeconds` and `appLimits`.
- Do not log secrets, API keys, tokens, emails beyond existing UI needs, or child-sensitive data.
- Do not hard-code private API keys or production secrets.
- External API calls must run off the UI thread and must have connect/read timeouts.
- External API failures must show a safe fallback message and must not freeze the UI.

Child-safety and enforcement rules:

- Treat `LimitService.java`, `AppWatchAccessibilityService.java`, usage permissions, overlay behavior, and pending child deletion logic as safety-critical.
- Do not weaken app-lock enforcement without explicit user approval.
- Do not remove foreground service notification behavior without explicit user approval.
- Do not remove permission prompts unless replacing them with an equivalent or better flow.
- Do not change account deletion behavior without understanding Firebase Authentication requirements and recent-login constraints.

## Debugging Rules

When debugging:

- Reproduce the likely flow from the relevant Activity or Service entry point.
- Read logs or source before changing behavior.
- Check both local state and Firestore sync paths when investigating parent/child mismatches.
- Verify whether data is read from `users/{uid}`, `users/{parentUid}/children/{childUid}`, or mirrored collections such as `usage_reports` and `app_time_limits`.
- Check Android permission state before assuming usage tracking or overlays are broken.
- For UI bugs, inspect both the Java Activity and XML layout.
- For role bugs, inspect `Login.java`, `Roleselection.java`, and `RoleUtils.java` first.
- For selected-child bugs, inspect `SelectedChildStore.java`, `Parentdash.java`, and `Dailylimit.java` first.
- For usage report bugs, inspect `UsageStatsHelper.java`, `UsageWindowHelper.java`, `LimitService.java`, `Usagereport.java`, `Usagereportchild.java`, and `ChildUsageDetailActivity.java` as relevant.

## Operational Guidelines

### File Search And Path Rules

Claude must not guess file paths.

Use these PowerShell-compatible commands on this host:

```powershell
Get-ChildItem -Force
Get-ChildItem -Path app\src\main -Recurse -File
Get-ChildItem -Path app\src\main\java\com\mynyaman\mynyamanapp -File
Get-Content app\src\main\AndroidManifest.xml
Get-Content app\build.gradle.kts
```

If ripgrep is available, these are preferred for search:

```powershell
rg --files
rg "pattern" app\src\main
```

### File Reading Rules

Before editing a file:

- Read the entire file with `Get-Content` or an equivalent full-file read.
- If editing an XML layout, read the corresponding Java Activity.
- If editing a Java Activity, read the corresponding XML layout when view ids or UI behavior are involved.
- If changing Firestore fields, read `BlueprintSyncRepository.java` and all screens that read/write those fields.
- If changing routing, read `Login.java`, `Roleselection.java`, and `RoleUtils.java`.

### Terminal Compatibility

The host shell is PowerShell on Windows. Format terminal commands for PowerShell.

Use:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
Get-ChildItem -Recurse -File
Get-Content path\to\file.java
```

Do not provide Unix-only command forms unless also providing a PowerShell equivalent.

### Validation

After code edits, suggest the smallest useful validation command, but ask before running it.

Common validation commands:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat connectedAndroidTest
```

Use `assembleDebug` for most Android compile checks. Use `test` for unit-test changes. Use `connectedAndroidTest` only when instrumentation/device behavior is relevant and the user confirms a device or emulator is available.

## Sensitive Areas

Treat these as high-risk:

- Firebase Auth account creation, login, email verification, and deletion.
- Child account creation and rollback in `Addchild.java`.
- App-lock enforcement and overlays in `LimitService.java`.
- Accessibility service behavior in `AppWatchAccessibilityService.java`.
- Firestore field names and document ids.
- Gradle configuration and BuildConfig API values.
- Android permissions in `AndroidManifest.xml`.
- `local.properties` and `google-services.json`.
- Any data involving child accounts, usage history, app names, parent-child relationships, or consultant chats.

## Response Style For Claude

Claude must be concise and practical.

When proposing changes:

- State what will change.
- State which files will be touched.
- State why the change is safe.
- Ask for approval before large code generation or risky operations.

When finishing work:

- Summarize changed files.
- Mention validation run or explain why it was not run.
- Call out any remaining risks, assumptions, or required manual testing.

## Absolute Prohibitions

Claude must not:

- Delete files without confirmation.
- Run build scripts, database migrations, or git commits without confirmation.
- Modify configuration files, connection strings, API keys, Firebase config, or manifest permissions without confirmation.
- Implement sweeping refactors across multiple classes without confirmation.
- Guess Firestore schemas or Android layout ids.
- Fragment role routing, selected-child state, usage-window logic, app-limit enforcement, or Firestore sync responsibilities across unrelated classes.
- Freeze the UI with synchronous network, Firebase, or heavy usage-stat operations.
- Hard-code secrets.
- Reformat unrelated files.
- Revert user changes unless explicitly instructed.
