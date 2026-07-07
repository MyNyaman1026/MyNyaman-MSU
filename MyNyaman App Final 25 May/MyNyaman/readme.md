# MyNyaman App

MyNyaman is an Android digital wellbeing and parental guidance app designed to help families manage children's screen time, review app usage, set daily limits, and access support around online safety topics such as cyberbullying, screen habits, and mental wellbeing.

The app uses role-based experiences for parents, children, and consultants. Parents can create and manage child accounts, monitor usage, and configure restrictions. Children can view their own usage and receive guidance. Consultant features provide AI-assisted and prototype human-support flows for digital wellbeing conversations.

## Project Overview

- Project name: MyNyaman app
- Android application ID: `com.mynyaman.mynyamanapp`
- Main module: `app`
- Primary platform: Android
- Main source code: `app/src/main/java/com/mynyaman/mynyamanapp/`
- UI layouts: `app/src/main/res/layout/`
- Assets and drawables: `app/src/main/res/drawable/`
- Firebase config location: `app/src/google-services.json`
- Build system: Gradle Kotlin DSL
- Primary app language: Java, with some Kotlin theme files

## App Purpose

The purpose of MyNyaman is to give families a practical tool for healthier device use. It combines parental controls, screen-time tracking, app limit enforcement, usage reports, and guidance support in one Android application.

The app is built around these goals:

- Help parents understand how much time children spend on apps.
- Let parents set daily app limits for distracting apps.
- Keep limits active on the child device through background monitoring.
- Give children visibility into their own screen-time habits.
- Support safe conversations about screen health, digital wellbeing, and cyberbullying.
- Store family, usage, limit, and consultant-session data in Firebase for syncing between parent and child experiences.

## Main User Roles

### Parent

The parent role is the main family-management role. A parent can add child accounts, select a child profile, open usage reports, set daily limits, save blocked sites, open profile screens, and access consultant support.

Important parent screens include:

- `Parentdash.java`: main parent dashboard.
- `Addchild.java`: child account creation and linking.
- `Dailylimit.java`: app time limit management.
- `Usagereport.java`: child usage list.
- `ChildUsageDetailActivity.java`: detailed child usage report.
- `Blocksite.java`: blocked website list.
- `Parentprofile.java`: parent profile.
- `ConsultantHubActivity.java`: consultant support entry point.

### Child

The child role is used on the child's device. The child dashboard prompts for required Android permissions, starts the protection service, shows usage features, and allows access to child profile, tutorial, and consultant support.

Important child screens and services include:

- `Childdash.java`: main child dashboard.
- `Usagereportchild.java`: child's own usage report with chart and app list.
- `ProfileeditChild.java`: child profile editing.
- `Tutorialchild.java`: child tutorial.
- `LimitService.java`: foreground protection and usage-sync service.
- `AppWatchAccessibilityService.java`: accessibility-based foreground app tracking.

### Consultant

The consultant role supports digital wellbeing guidance. The app includes a consultant dashboard plus AI and human-consultant flows.

Important consultant screens include:

- `Consultantdash.java`: consultant dashboard.
- `ConsultantHubActivity.java`: support path selection.
- `AiConsultantChatActivity.java`: AI consultant chat.
- `ConsultantSessionHistoryActivity.java`: saved session history.
- `HumanConsultantPrototypeActivity.java`: prototype human consultant path.

## Core Features

### 1. Account Registration and Login

MyNyaman uses Firebase Authentication for email/password accounts.

Key behavior:

- Users sign up with name, email, password, and confirm password.
- Passwords must contain at least 6 characters, one capital letter, one number, and one symbol.
- New users receive an email verification link.
- Login requires the Firebase email account to be verified.
- After login, the app reads the saved role from Firestore and routes the user to the parent, child, consultant, or role-selection screen.

Main files:

- `Signup.java`
- `Login.java`
- `Roleselection.java`
- `RoleUtils.java`
- `BlueprintSyncRepository.java`

### 2. Role-Based Navigation

The app supports normalized role keys: `parent`, `child`, and `consultant`. `RoleUtils.java` normalizes role values and converts them to legacy display labels when needed. `Login.java` uses this role information to route users to the correct dashboard.

### 3. Parent Dashboard

The parent dashboard is the main control center for parents.

Features include:

- Welcome message loaded from Firestore.
- Swipe-to-refresh for dashboard data.
- Child account chips for selecting the active child.
- Add child account shortcut.
- Daily limit shortcut.
- Usage report shortcut.
- Consultant support shortcut.
- Blocked sites shortcut.
- Tutorial shortcut.
- Parent profile and full menu navigation.

The dashboard stores the currently selected child locally through `SelectedChildStore.java`, so other screens can open with the same active child.

Main file: `Parentdash.java`

### 4. Child Account Creation

Parents can create linked child accounts from the app.

Key behavior:

- A parent can link up to 8 child accounts.
- The child account requires name, contact, email, and date of birth.
- The child email cannot be the same as the parent email.
- The app creates the child Firebase Authentication user through a secondary Firebase app instance so the parent stays signed in.
- A temporary password is generated internally.
- The child receives an email verification link and password setup/reset email.
- Firestore stores the child profile, the parent-child subcollection entry, and a `parent_child_relationships` document.
- If account linking or email setup fails, the app attempts rollback cleanup.

Main file: `Addchild.java`

### 5. Daily App Limits

Parents can set per-app daily limits for a selected child.

Key behavior:

- The screen loads child accounts from Firestore.
- Parents can switch between child chips.
- The app reads the child's synced `topApps` data.
- Only high-distraction apps are shown through `DistractionAppFilter.java`.
- Existing limits are read from `appLimitSeconds`, legacy `appLimits`, and mirrored `app_time_limits` documents.
- Limits can be enabled or disabled per app.
- Time can be configured in hours, minutes, and seconds.
- Saved limits are written to both the child user document and the parent's child subcollection.
- Limit records are mirrored into the `app_time_limits` collection.

Main files:

- `Dailylimit.java`
- `DistractionAppFilter.java`
- `BlueprintSyncRepository.java`

### 6. Child Protection and App Locking

The child device runs `LimitService.java` as a foreground service to keep app protection active.

Key behavior:

- Runs as a sticky foreground service with a persistent notification.
- Watches the currently active app.
- Reads app limits from the signed-in child user's Firestore document.
- Tracks daily usage using Android usage stats.
- Uses accessibility foreground-app events when available.
- Falls back to usage events if accessibility tracking is not enabled.
- Schedules exact lock checks based on remaining app time.
- Shows an overlay lock screen when an app has reached its limit.
- Sends the child device back to the home screen when a blocked app is over limit.
- Syncs usage totals, top apps, daily keys, and weekly keys to Firestore.
- Mirrors usage data to the parent child document when `parentUid` is available.
- Handles pending child account deletion by signing out and cleaning up the child account state.

Main files:

- `LimitService.java`
- `AppWatchAccessibilityService.java`
- `AccessibilityStateHelper.java`
- `ForegroundAppStateStore.java`
- `UsageStatsHelper.java`
- `UsageWindowHelper.java`

### 7. Usage Reports

MyNyaman provides usage reports for both parents and children.

Parent report features:

- Parent can see all linked children sorted by today's usage time.
- Each child row shows total usage and last sync time.
- Parent can open a detailed report for one child.
- Detailed report displays total daily usage, weekly usage, most used app, top apps, and a pie chart.
- The detail report reads synced child usage data from the parent's child subcollection.

Child report features:

- Child can view their own total daily usage.
- Child can view weekly usage and most-used app.
- Child can see app-by-app foreground time with icons and progress bars.
- Child report includes an MPAndroidChart pie chart.
- If usage access is missing, the screen prompts the child to open Android Usage Access settings.
- Usage is synced back to Firestore after local calculation.

Main files:

- `Usagereport.java`
- `ChildUsageDetailActivity.java`
- `Usagereportchild.java`
- `UsageStatsHelper.java`
- `UsageWindowHelper.java`
- `BlueprintSyncRepository.java`

### 8. AI Consultant Support

The AI consultant chat is designed for screen health, digital wellbeing, screen-time limits, mental health related to screen use, and cyberbullying topics.

Key behavior:

- Parents and children can open the consultant hub.
- The AI chat starts with a welcome message.
- Messages are sent to an OpenAI-compatible chat completions endpoint.
- The app supports Forge API configuration first, then DeepSeek as fallback.
- The AI system prompt restricts the consultant to family-safe digital wellbeing topics.
- On leaving the chat, the user is asked whether to end the session.
- Ended sessions are saved to Firestore as a conversation, consultant session, and chat messages.
- Session history can be viewed later.

Configuration comes from `local.properties` through generated `BuildConfig` fields:

- `BUILT_IN_FORGE_API_URL`
- `BUILT_IN_FORGE_API_KEY`
- `DEEPSEEK_API_URL`
- `DEEPSEEK_API_KEY`

Main files:

- `ConsultantHubActivity.java`
- `AiConsultantChatActivity.java`
- `AiConsultantApi.java`
- `ConsultantSessionHistoryActivity.java`
- `BlueprintSyncRepository.java`

### 9. Blocked Sites

The parent blocked-site screen lets a parent save website or domain entries in Firestore.

Current behavior:

- Parent enters a website or domain.
- The app stores entries in the parent's `blockedSites` field.
- Saved entries are displayed as a list.

Main file: `Blocksite.java`

Note: The current implementation stores the blocked-site list, but website blocking enforcement is not visible in the current Java implementation.

### 10. Profiles, Tutorials, and Menu

The app includes profile, tutorial, and menu pages for different user roles.

Relevant files include:

- `Parentprofile.java`
- `Userprofile.java`
- `ProfileeditChild.java`
- `Tutorial.java`
- `Tutorialchild.java`
- `FullMenu.java`
- `Start.java`
- `Splash.java`

## Firebase and Data Storage

The app uses Firebase Authentication and Cloud Firestore.

Important collections and fields visible in the code:

- `users`: stores user profile, role, email verification state, usage data, parent links, app limits, and child metadata.
- `users/{parentUid}/children/{childUid}`: stores the parent's linked child records and mirrored child usage data.
- `parent_child_relationships`: stores parent-child link records.
- `app_time_limits`: stores mirrored per-child, per-package app limit documents.
- `usage_reports`: stores daily child usage reports with week keys and top app data.
- `conversations`: stores saved consultant conversations.
- `consultant_sessions`: stores consultant session metadata.
- `chat_messages`: stores saved AI consultant chat messages.

Examples of synced usage fields:

- `usageDayKey`
- `usageWeekKey`
- `usageTodayMinutes`
- `usageTodaySeconds`
- `usageLastSyncedAt`
- `topApps`
- `usagePermissionGranted`

Examples of app limit fields:

- `appLimitSeconds`
- `appLimits`
- `limitSeconds`
- `limitMinutes`
- `isEnabled`
- `packageName`
- `appName`

## Android Permissions

The app requests permissions needed for monitoring and enforcement:

- `INTERNET`: Firebase and AI consultant API access.
- `PACKAGE_USAGE_STATS`: app usage tracking.
- `QUERY_ALL_PACKAGES`: installed app/package visibility.
- `POST_NOTIFICATIONS`: foreground service notification on newer Android versions.
- `SYSTEM_ALERT_WINDOW`: lock overlay display.
- `FOREGROUND_SERVICE`: background protection service.
- `FOREGROUND_SERVICE_DATA_SYNC`: foreground data sync service type.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: helps keep monitoring active.
- Accessibility service binding through `AppWatchAccessibilityService`.

The child dashboard prompts the user to grant notification, overlay, battery optimization, usage access, and accessibility-related permissions where needed.

## Technical Stack

- Android Gradle Plugin through version catalog aliases.
- Gradle Kotlin DSL.
- Java 11 source and target compatibility.
- Java activities with XML layouts.
- Firebase Authentication.
- Firebase Firestore.
- Firebase Analytics.
- AndroidX AppCompat.
- AndroidX ConstraintLayout.
- AndroidX SwipeRefreshLayout.
- Material Components.
- MPAndroidChart for pie charts.
- Some Jetpack Compose dependencies and Kotlin theme files are present, although the visible app screens are mostly Java/XML based.

## Build Configuration

The app module is configured in `app/build.gradle.kts`.

Important build settings:

- `namespace`: `com.mynyaman.mynyamanapp`
- `applicationId`: `com.mynyaman.mynyamanapp`
- `compileSdk`: 35
- `minSdk`: 24
- `targetSdk`: 35
- `versionCode`: 1
- `versionName`: `1.0`

BuildConfig values are read from `local.properties`:

- `BUILT_IN_FORGE_API_URL`, default `https://forge.manus.im`
- `BUILT_IN_FORGE_API_KEY`, default empty
- `DEEPSEEK_API_URL`, default `https://api.deepseek.com`
- `DEEPSEEK_API_KEY`, default empty
- `MYNYAMAN_WEB_BASE_URL`, default empty

## Project Structure

```text
.
|-- app/
|   |-- build.gradle.kts
|   |-- src/
|   |   |-- google-services.json
|   |   |-- main/
|   |   |   |-- AndroidManifest.xml
|   |   |   |-- java/com/mynyaman/mynyamanapp/
|   |   |   |-- res/drawable/
|   |   |   |-- res/layout/
|   |   |   |-- res/values/
|   |   |   |-- res/xml/
|   |   |-- androidTest/
|   |   |-- test/
|-- build.gradle.kts
|-- settings.gradle.kts
|-- gradle.properties
|-- gradlew
|-- gradlew.bat
|-- local.properties
|-- website_blueprint/
```

## Important Source Files

- `AndroidManifest.xml`: permissions, services, launcher activity, and activity registration.
- `Splash.java`: launcher entry point.
- `Start.java`: start/onboarding entry.
- `Login.java`: email/password sign-in and role routing.
- `Signup.java`: account registration and email verification.
- `Roleselection.java`: user role selection.
- `Parentdash.java`: parent dashboard.
- `Childdash.java`: child dashboard and permission prompts.
- `Consultantdash.java`: consultant dashboard.
- `Addchild.java`: child account creation and linking.
- `Dailylimit.java`: app limit setup.
- `LimitService.java`: foreground limit monitoring and lock enforcement.
- `AppWatchAccessibilityService.java`: accessibility foreground app monitoring.
- `Usagereport.java`: parent usage report list.
- `ChildUsageDetailActivity.java`: parent view of child usage details.
- `Usagereportchild.java`: child usage report.
- `AiConsultantChatActivity.java`: AI chat UI and session saving.
- `AiConsultantApi.java`: AI API request handling.
- `BlueprintSyncRepository.java`: shared Firestore sync helper methods.
- `RoleUtils.java`: role normalization helper.
- `UsageStatsHelper.java`: Android usage stats helper.
- `UsageWindowHelper.java`: current day/week usage window helper.

## Setup Notes

1. Open the project in Android Studio.
2. Confirm the Android SDK for compile SDK 35 is installed.
3. Confirm Firebase is configured through `app/src/google-services.json`.
4. Keep API keys in `local.properties`; do not commit secrets.
5. Optional AI consultant configuration can be added to `local.properties`:

```properties
BUILT_IN_FORGE_API_KEY=your_forge_key
BUILT_IN_FORGE_API_URL=https://forge.manus.im
DEEPSEEK_API_KEY=your_deepseek_key
DEEPSEEK_API_URL=https://api.deepseek.com
```

## Useful Commands

From the project root:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat connectedAndroidTest
```

For quick local validation after code edits:

```powershell
.\gradlew.bat assembleDebug
```

## Current Development Notes

- Several app files are currently modified in the working tree, including parent dashboard, limit service, usage report screens, and signup layout.
- The app uses both current second-based limits (`appLimitSeconds`) and older minute-based limits (`appLimits`) for compatibility.
- Blocked-site entries are stored, but enforcement is not visible in the current Java implementation.
- The protection system depends on Android permissions that must be granted on the child device for best results.
- Usage and limit data is synced in multiple Firestore locations so parent and child screens can read the same family state.

## Definition of Done for Future Changes

- The requested feature or fix is implemented in the smallest useful scope.
- Firebase reads and writes remain compatible with existing field names where possible.
- Role-based routing still works for parent, child, and consultant users.
- Child protection behavior is tested carefully when changing `LimitService.java`, `AppWatchAccessibilityService.java`, usage stats, or overlay logic.
- Relevant Gradle validation commands are run when possible.
- Any skipped validation is documented with the reason.
