# Implementation Plan - Onboarding, Permissions & Reality Check Refinement

This plan addresses UI issues in the onboarding flow, fixes a recurring default launcher permission bug, and modernizes the "Reality Check" dashboard to be more minimal.

## User Review Required

> [!IMPORTANT]
> The "Start Using JustU Launcher" button inside the final onboarding slide will be removed to avoid redundancy with the "Start" button in the navigation bar.

## Proposed Changes

---

### 1. Onboarding Flow Refinement
**Goal:** Fix button visibility on small screens and ensure content doesn't overlap with navigation.

#### [MODIFY] [OnboardingScreen.kt](file:///D:/Projects2026/JustULauncher/app/src/main/java/com/justu/launcher/ui/onboarding/OnboardingScreen.kt)
- Make `OnboardingSlide` scrollable to prevent content from going under the bottom buttons on small screens.
- Remove the redundant "Start Using JustU Launcher" button in `OnboardingFinalSlide`.
- Adjust padding and spacing to ensure a better fit for various screen sizes.

---

### 2. Default Launcher & Permission UI
**Goal:** Fix the "recurring permission prompt" bug and improve the UI of permission/TC dialogs.

#### [MODIFY] [HomeScreen.kt](file:///D:/Projects2026/JustULauncher/app/src/main/java/com/justu/launcher/ui/home/HomeScreen.kt)
- **Bug Fix:** Change `isDefaultLauncher` from a static `remember` to a state that re-evaluates when the app resumes (using `LifecycleEventEffect` or similar). This ensures the dialog disappears immediately after the user sets the launcher as default.
- **UI Improvement:** Modernize `TermsAndConditionsDialog` and `DefaultLauncherDialog` with better typography, spacing, and a more "minimal" aesthetic consistent with the rest of the app.
- Simplify the "Default Home Launcher" prompt to be less intrusive.

---

### 3. Reality Check (Usage Dashboard) Modernization
**Goal:** Remove app logos and background process mentions for a cleaner, minimal look.

#### [MODIFY] [RightScreen.kt](file:///D:/Projects2026/JustULauncher/app/src/main/java/com/justu/launcher/ui/home/RightScreen.kt)
- Remove `appIcon` display in the "Top Apps" list.
- Simplify the layout of top apps: remove the rank number or make it very subtle.
- Adjust the "Today vs Yesterday" chart to be even more minimal.

#### [MODIFY] [UsageRepository.kt](file:///D:/Projects2026/JustULauncher/app/src/main/java/com/justu/launcher/data/repository/UsageRepository.kt)
- Ensure usage calculation strictly reflects active foreground time (already implemented, but will double-check for any "background" noise).

---

## Verification Plan

### Automated Tests
- N/A (UI-centric changes)

### Manual Verification
1. **Onboarding:** Run the app on a small screen device (or emulator with small resolution) and verify that the "Next/Previous" buttons are always visible and content is scrollable.
2. **Default Launcher Bug:** Open the app, see the "Set as Default" dialog, click "Open Settings", set JustU as default, and return to the app. The dialog should be gone immediately.
3. **Reality Check:** Navigate to the right screen and verify the new minimal UI: no app icons, cleaner list, and accurate foreground time.
