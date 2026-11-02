# Branding & Customization Guide

How to rebrand this app for a different SACCO / MFI. Every change below
requires only Android Studio — no backend changes needed (the backend already
serves the SACCO name + logo from `CompanyInfo`; these are the *offline /
startup* assets the user sees before the API responds).

---

## 1. App Name

**File:** `app/src/main/res/values/strings.xml`

```xml
<!-- BEFORE -->
<string name="app_name">NODi</string>

<!-- AFTER — your SACCO name -->
<string name="app_name">My SACCO</string>
```

This changes the launcher label, the task-switcher name, and every layout
that references `@string/app_name` (splash screen, login screen title).

---

## 2. Package / Application ID

**File:** `app/build.gradle`

```groovy
// BEFORE
applicationId "com.nodimobilebanking"

// AFTER
applicationId "com.mysacco.app"
```

> **Also update** `namespace` in the same file to match.
> Then in Android Studio: **Refactor → Rename** the package folders under
> `app/src/main/java/com/nodimobilebanking/` to match the new ID
> (e.g. `com/mysacco/app/`).

---

## 3. Colors

**File:** `app/src/main/res/values/colors.xml`

The palette is organized into clear sections. The two values that control
90% of the visual feel are:

| Name            | Current         | Controls                                   |
|-----------------|-----------------|---------------------------------------------|
| `primary`       | `#1E2A8C` blue  | App bar, buttons, balance card, inputs focus |
| `accent`        | `#1E7C2E` green | Withdraw button, loan balance, success tint  |

To rebrand, change these two plus their `_dark` / `_light` / `_50` variants:

```xml
<color name="primary">#B22222</color>        <!-- Your brand red -->
<color name="primary_dark">#8B1A1A</color>
<color name="primary_light">#E05555</color>
<color name="primary_50">#FDECEA</color>

<color name="accent">#FF8C00</color>         <!-- Your brand orange -->
<color name="accent_dark">#CC7000</color>
<color name="accent_light">#FFB347</color>
<color name="accent_50">#FFF3E0</color>
```

Also update `input_stroke_focused` to match `primary`, and `promo_blue`
to match `primary` (used in carousel cards).

The **theme** file (`values/themes.xml`) reads from `@color/primary` etc.
so you do NOT need to edit `themes.xml` unless you want to change
corner radii, elevation, or text sizes.

### Status-bar color

Controlled by `primary_dark` — the system status bar tints to this value.

---

## 4. Logos & Launcher Icons

### Splash + Login logo

**Files to replace:**

| File                                          | Usage                |
|-----------------------------------------------|----------------------|
| `res/drawable/nodi_logo.png`                  | Splash & login logo  |
| `res/drawable/nodi_wordmark.png`              | (if used) text logo  |

Replace them with your own PNG files using the **same filenames**, or rename
and update the references in:

- `res/layout/activity_splash.xml` → `android:src="@drawable/nodi_logo"`
- `res/layout/activity_login.xml`  → `android:src="@drawable/nodi_logo"`

Recommended size: 200×200 px for the logo, transparent background.

### Launcher icon (home screen)

Use Android Studio's **Image Asset Studio**:

1. Right-click `res/` → **New → Image Asset**
2. Choose **Launcher Icons (Adaptive & Legacy)**
3. Upload your logo; adjust padding so it looks good in the round mask
4. Click **Next → Finish** — it generates all `mipmap-*` densities automatically

This overwrites:

```
res/mipmap-mdpi/ic_launcher.png
res/mipmap-hdpi/ic_launcher.png
res/mipmap-xhdpi/ic_launcher.png
res/mipmap-xxhdpi/ic_launcher.png
res/mipmap-xxxhdpi/ic_launcher.png
(…and the _round variants + foreground layers)
```

### Launcher icon background color

**File:** `res/values/ic_launcher_background.xml`

```xml
<!-- BEFORE -->
<color name="ic_launcher_background">#1E2A8C</color>

<!-- AFTER — your primary brand color -->
<color name="ic_launcher_background">#B22222</color>
```

---

## 5. Quick-action button colors (Dashboard)

The three action buttons on the dashboard use **inline** hex colors in
`res/layout/fragment_dashboard.xml`:

| Button       | Line                           | Default        |
|--------------|--------------------------------|----------------|
| Deposit      | `android:background="@color/primary"` | Brand blue     |
| Withdraw     | `android:background="@color/accent"`  | Brand green    |
| Apply Loan   | `android:background="#0D47A1"`        | Deep blue      |

Change the `Apply Loan` hex to your brand color if needed.

---

## 6. Onboarding screens

If your brand has a different onboarding flow or illustrations, update:

- `res/layout/activity_onboard_step1.xml`
- `res/layout/activity_onboard_step2.xml`
- `res/layout/activity_onboard_step3.xml`

Replace any drawable references (`nodi_logo`, etc.) and update the
text strings in those layouts or in `strings.xml`.

---

## 7. Backend-driven branding (runtime)

The backend's `/androidapi/company-info/` endpoint returns:

```json
{
  "company_name": "My SACCO",
  "company_address": "...",
  "company_contact": "...",
  "company_footer": "Powered by My SACCO MIS"
}
```

This is cached in `TokenManager` and used by:
- PDF statement headers (`PdfStatementGenerator`)
- `CompanyInfoManager` for dynamic footer text

To change the runtime branding, update `CompanyInfo` in Django Admin.

---

## 8. Carousel / promotion colors

Fallback carousel items use hard-coded hex colors in
`DashboardFragment.kt → setupCarousel()`:

```kotlin
val colors = listOf("#1B5E20", "#0D47A1", "#E65100", "#4A148C", "#880E4F")
```

Change these to your palette. When real promotions load from the API,
these are used as cycling backgrounds, so pick colors that contrast well
with white text.

---

## Summary — minimal rebrand checklist

1. `strings.xml` → change `app_name`
2. `colors.xml`  → change `primary`, `primary_dark`, `accent` + variants
3. Replace `res/drawable/nodi_logo.png` with your logo
4. Android Studio → Image Asset → generate new launcher icons
5. `build.gradle` → change `applicationId` + `namespace`
6. Refactor package name (`com.nodimobilebanking` → `com.yourcompany.app`)
7. Django Admin → update `CompanyInfo` name / logo / footer

That's it. Build and deploy.
