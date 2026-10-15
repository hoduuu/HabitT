# 습관 다이어리 (Android 해빗 트래커) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 다이어리 느낌의 월 격자 해빗 트래커 Android 앱과, 습관 하나의 이번 달 격자를 보여주는 홈 화면 위젯을 만들어 서명된 APK로 설치한다.

**Architecture:** 단일 모듈 Kotlin 앱. 카드 모양은 `render` 패키지의 순수 좌표 계산(`CardLayout`)과 `android.graphics.Canvas` 렌더러(`CardRenderer`) 하나로 그려서, 앱(Compose `Canvas`의 nativeCanvas)과 위젯(비트맵 → `RemoteViews` ImageView)이 똑같은 모양을 공유한다. 데이터는 Room + DataStore, 화면 상태는 `MainViewModel` 하나.

**Tech Stack:** Kotlin 2.0.21, AGP 8.7.2, Gradle 8.10.2, Jetpack Compose (BOM 2024.10.01, Material3), Room 2.6.1 (KSP), DataStore Preferences 1.1.1, sh.calvin.reorderable 2.4.0, JUnit4 + Robolectric 4.14.1 + kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-10-15-habit-tracker-design.md`

## Global Constraints

- 패키지/applicationId: `com.habitdiary.app`, 앱 이름 `습관 다이어리`
- minSdk 26, compileSdk 35, targetSdk 35, Java/JVM target 17
- 라이트 테마만 (다크 모드 없음). 모든 UI 문구는 한국어.
- 격자: 7열, **일요일 시작**, 칸은 동그라미, 오늘=습관 색 2px 링, 미래 칸 탭 무반응
- 색 토큰: 페이지 `#fff8f0`, 종이 `#fffdf9`, 글자 `#5b4636`, 요일 `#c4ab97`, 빈 칸 `#f3ebe3`, 미래 칸 `#faf5ef`, 모눈 점 `#ead9c9`
- 습관 팔레트(순서 고정): `#ffb38a` `#ffd27a` `#9ccfa8` `#8fd3d0` `#a9c4f5` `#c7b2f0` `#f5a9c8` `#d9b99b`
- 폰트: Gaegu Regular/Bold, Gamja Flower (OFL) — `res/font`에 번들
- 카드 스타일: `INDEX_TAB`(C, 기본) / `MASKING_TAPE`(A), 설정에서 전역 1개
- 위젯: 보기 전용, 탭 → 앱 열기. 위젯 1개 = 습관 1개.
- 제외: 다크 모드, 년도 보기, 동기부여/통계 문구, 알림, 백업
- **경로 (이 PC):** JDK `%LOCALAPPDATA%\Programs\jdk-17`, Android SDK `%LOCALAPPDATA%\Android\Sdk`
- **Gradle 실행은 항상 PowerShell에서 이 형태로:**
  `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat <task>` (작업 디렉터리 `D:\claude\HabitTracker`)
- **adb:** `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" <args>`
- **안전 규칙:** 파일 다운로드(JDK, SDK, Gradle, 폰트)와 SDK 라이선스 동의는 실행 직전에 사용자에게 무엇을 어디서 받는지 알리고 승인받는다. 폰에 설치/삭제하기 전에도 사용자에게 확인한다.
- 커밋 메시지 끝에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`

## File Structure

```
HabitTracker/
├─ settings.gradle.kts, build.gradle.kts, gradle.properties, gradlew(.bat), gradle/wrapper/*
├─ local.properties            (gitignored, sdk.dir)
├─ keystore.properties         (gitignored, Task 11)
└─ app/
   ├─ build.gradle.kts
   └─ src/
      ├─ main/
      │  ├─ AndroidManifest.xml
      │  ├─ res/font/          gaegu_regular.ttf, gaegu_bold.ttf, gamja_flower.ttf
      │  ├─ res/values/strings.xml
      │  ├─ res/drawable/      ic_launcher_foreground.xml, widget_message_bg.xml
      │  ├─ res/mipmap-anydpi-v26/ic_launcher.xml
      │  ├─ res/layout/widget_card.xml
      │  ├─ res/xml/habit_widget_info.xml
      │  └─ java/com/habitdiary/app/
      │     ├─ HabitApp.kt              Application + AppContainer (수동 DI)
      │     ├─ MainActivity.kt
      │     ├─ calendar/MonthGrid.kt     달력 계산 (순수)
      │     ├─ style/CardStyle.kt        스타일 enum
      │     ├─ style/Palette.kt          색 토큰, 테이프 색 선택
      │     ├─ render/CardLayout.kt      디자인 단위 좌표·탭 판정 (순수)
      │     ├─ render/CardRenderer.kt    Canvas 그리기, 비트맵
      │     ├─ render/CardFonts.kt       폰트 로딩
      │     ├─ data/Habit.kt, Check.kt, HabitDao.kt, HabitDatabase.kt
      │     ├─ data/HabitStore.kt        인터페이스 + RoomHabitStore
      │     ├─ data/Settings.kt          SettingsStore + DataStoreSettings + WidgetBindings
      │     ├─ ui/MainViewModel.kt
      │     ├─ ui/Theme.kt               DiaryFont, Ink 등 Compose 값
      │     ├─ ui/HabitCard.kt           렌더러를 감싼 Compose 카드
      │     ├─ ui/MainScreen.kt          헤더 + 목록 + 순서 변경
      │     ├─ ui/HabitEditSheet.kt      편집 바텀시트 + 색 고르기
      │     ├─ ui/Emoji.kt               lastGrapheme
      │     ├─ ui/SettingsScreen.kt
      │     ├─ ui/HabitDiaryApp.kt       화면 전환 루트
      │     └─ widget/WidgetSizing.kt, WidgetUpdater.kt, HabitWidgetProvider.kt,
      │               WidgetConfigActivity.kt, MidnightScheduler.kt
      └─ test/java/com/habitdiary/app/  (각 Task의 테스트)
```

---

### Task 1: 개발 도구 설치 + 빈 Compose 앱 빌드

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `local.properties`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/java/com/habitdiary/app/MainActivity.kt`, `app/src/test/java/com/habitdiary/app/SanityTest.kt`
- Generate: `gradlew`, `gradlew.bat`, `gradle/wrapper/*`

**Interfaces:**
- Produces: 빌드 가능한 Gradle 프로젝트, 모든 이후 Task의 의존성(아래 `app/build.gradle.kts`에 전부 선언됨)

- [ ] **Step 1: 사용자 승인 받기**

다운로드 목록을 사용자에게 알리고 승인받는다:
- Eclipse Temurin JDK 17 (adoptium.net, 약 190MB)
- Android command-line tools (dl.google.com, 약 150MB) + SDK 패키지 `platform-tools`, `platforms;android-35`, `build-tools;35.0.0` (약 300MB)
- Gradle 8.10.2 배포판 (services.gradle.org, 약 130MB)
- Android SDK 라이선스 동의(`sdkmanager --licenses`)도 사용자 동의가 필요함을 함께 알린다.

- [ ] **Step 2: JDK 17 설치**

PowerShell:
```powershell
New-Item -ItemType Directory -Force "$env:LOCALAPPDATA\Programs" | Out-Null
curl.exe -L -o "$env:TEMP\jdk17.zip" "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"
Expand-Archive "$env:TEMP\jdk17.zip" "$env:LOCALAPPDATA\Programs\jdk17-tmp" -Force
Move-Item (Get-ChildItem "$env:LOCALAPPDATA\Programs\jdk17-tmp" -Directory | Select-Object -First 1).FullName "$env:LOCALAPPDATA\Programs\jdk-17"
Remove-Item -Recurse -Force "$env:LOCALAPPDATA\Programs\jdk17-tmp"
& "$env:LOCALAPPDATA\Programs\jdk-17\bin\java.exe" -version
```
Expected: `openjdk version "17.…"`

- [ ] **Step 3: Android SDK 설치**

```powershell
$sdk="$env:LOCALAPPDATA\Android\Sdk"
New-Item -ItemType Directory -Force "$sdk\cmdline-tools" | Out-Null
curl.exe -L -o "$env:TEMP\clt.zip" "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
Expand-Archive "$env:TEMP\clt.zip" "$sdk\cmdline-tools\tmp" -Force
Move-Item "$sdk\cmdline-tools\tmp\cmdline-tools" "$sdk\cmdline-tools\latest"
Remove-Item -Recurse -Force "$sdk\cmdline-tools\tmp"
$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"
# 라이선스 동의 — Step 1에서 사용자 승인을 받은 경우에만 실행
"y`ny`ny`ny`ny`ny`ny`ny`n" | & "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools" "platforms;android-35" "build-tools;35.0.0"
Test-Path "$sdk\platforms\android-35\android.jar"
```
Expected: 마지막 줄 `True`

- [ ] **Step 4: 프로젝트 파일 작성**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "HabitTracker"
include(":app")
```

`build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

`local.properties` (gitignored — 이미 `.gitignore`에 있음):
```properties
sdk.dir=C\:\\Users\\hyun\\AppData\\Local\\Android\\Sdk
```

`app/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.habitdiary.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.habitdiary.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("sh.calvin.reorderable:reorderable:2.4.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
```

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="@string/app_name"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">습관 다이어리</string>
</resources>
```

`app/src/main/java/com/habitdiary/app/MainActivity.kt`:
```kotlin
package com.habitdiary.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("습관 다이어리") }
    }
}
```

`app/src/test/java/com/habitdiary/app/SanityTest.kt`:
```kotlin
package com.habitdiary.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SanityTest {
    @Test
    fun arithmeticWorks() {
        assertEquals(4, 2 + 2)
    }
}
```

- [ ] **Step 5: Gradle wrapper 생성**

```powershell
curl.exe -L -o "$env:TEMP\gradle.zip" "https://services.gradle.org/distributions/gradle-8.10.2-bin.zip"
Expand-Archive "$env:TEMP\gradle.zip" "$env:TEMP\gradle-dist" -Force
$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"
& "$env:TEMP\gradle-dist\gradle-8.10.2\bin\gradle.bat" wrapper --gradle-version 8.10.2
Test-Path .\gradlew.bat
```
Expected: `True`

- [ ] **Step 6: 테스트 + 디버그 빌드**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`, `app\build\outputs\apk\debug\app-debug.apk` 존재

- [ ] **Step 7: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradlew gradlew.bat gradle app
git commit -m "chore: scaffold Android Compose project"
```

---

### Task 2: MonthGrid (달력 계산)

**Files:**
- Create: `app/src/main/java/com/habitdiary/app/calendar/MonthGrid.kt`
- Test: `app/src/test/java/com/habitdiary/app/calendar/MonthGridTest.kt`

**Interfaces:**
- Produces:
  - `enum class DayKind { PAST, TODAY, FUTURE }`
  - `data class MonthGrid(val month: YearMonth, val leadingBlanks: Int, val daysInMonth: Int, val rows: Int)`
  - `fun MonthGrid.kindOf(day: Int, today: LocalDate): DayKind`
  - `MonthGrid.of(month: YearMonth): MonthGrid`

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
package com.habitdiary.app.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthGridTest {
    @Test
    fun october2026_startsThursday_fiveRows() {
        val g = MonthGrid.of(YearMonth.of(2026, 10))
        assertEquals(4, g.leadingBlanks)
        assertEquals(31, g.daysInMonth)
        assertEquals(5, g.rows)
    }

    @Test
    fun february2026_startsSunday_fourRows() {
        val g = MonthGrid.of(YearMonth.of(2026, 2))
        assertEquals(0, g.leadingBlanks)
        assertEquals(28, g.daysInMonth)
        assertEquals(4, g.rows)
    }

    @Test
    fun august2026_startsSaturday_sixRows() {
        val g = MonthGrid.of(YearMonth.of(2026, 8))
        assertEquals(6, g.leadingBlanks)
        assertEquals(6, g.rows)
    }

    @Test
    fun february2027_monday_and_leapFebruary2028() {
        val feb27 = MonthGrid.of(YearMonth.of(2027, 2))
        assertEquals(1, feb27.leadingBlanks)
        assertEquals(28, feb27.daysInMonth)
        assertEquals(29, MonthGrid.of(YearMonth.of(2028, 2)).daysInMonth)
    }

    @Test
    fun kindOf_comparesWithToday() {
        val g = MonthGrid.of(YearMonth.of(2026, 10))
        val today = LocalDate.of(2026, 10, 14)
        assertEquals(DayKind.PAST, g.kindOf(13, today))
        assertEquals(DayKind.TODAY, g.kindOf(14, today))
        assertEquals(DayKind.FUTURE, g.kindOf(15, today))
        assertEquals(DayKind.PAST, MonthGrid.of(YearMonth.of(2026, 9)).kindOf(30, today))
        assertEquals(DayKind.FUTURE, MonthGrid.of(YearMonth.of(2026, 11)).kindOf(1, today))
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest --tests "com.habitdiary.app.calendar.*"`
Expected: 컴파일 실패 (`Unresolved reference: MonthGrid`)

- [ ] **Step 3: 구현**

```kotlin
package com.habitdiary.app.calendar

import java.time.LocalDate
import java.time.YearMonth

enum class DayKind { PAST, TODAY, FUTURE }

/** 일요일 시작 7열 달력 배치. leadingBlanks = 1일 앞 빈 칸 수. */
data class MonthGrid(
    val month: YearMonth,
    val leadingBlanks: Int,
    val daysInMonth: Int,
    val rows: Int,
) {
    fun kindOf(day: Int, today: LocalDate): DayKind {
        val date = month.atDay(day)
        return when {
            date.isBefore(today) -> DayKind.PAST
            date == today -> DayKind.TODAY
            else -> DayKind.FUTURE
        }
    }

    companion object {
        fun of(month: YearMonth): MonthGrid {
            // DayOfWeek.value: 월=1 … 일=7 → 일=0
            val leading = month.atDay(1).dayOfWeek.value % 7
            val days = month.lengthOfMonth()
            return MonthGrid(month, leading, days, (leading + days + 6) / 7)
        }
    }
}
```

- [ ] **Step 4: 통과 확인**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest --tests "com.habitdiary.app.calendar.*"`
Expected: `BUILD SUCCESSFUL`, 5 tests passed

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/habitdiary/app/calendar app/src/test/java/com/habitdiary/app/calendar
git commit -m "feat: add Sunday-first MonthGrid"
```

---

### Task 3: 스타일 토큰 + 폰트 + CardLayout (좌표 계산)

**Files:**
- Create: `app/src/main/java/com/habitdiary/app/style/CardStyle.kt`, `style/Palette.kt`, `render/CardLayout.kt`
- Create: `app/src/main/res/font/gaegu_regular.ttf`, `gaegu_bold.ttf`, `gamja_flower.ttf`, `docs/licenses/OFL-Gaegu.txt`, `docs/licenses/OFL-GamjaFlower.txt`
- Test: `app/src/test/java/com/habitdiary/app/style/PaletteTest.kt`, `app/src/test/java/com/habitdiary/app/render/CardLayoutTest.kt`

**Interfaces:**
- Consumes: `MonthGrid` (Task 2)
- Produces:
  - `enum class CardStyle { INDEX_TAB, MASKING_TAPE }`
  - `object Palette { PAGE, PAPER, INK, WEEKDAY, EMPTY, FUTURE, GRID_DOT, PAPER_SHADOW: Int; habitColors: IntArray; fun habitColor(index: Int): Int; fun tapeColorIndex(habitId: Long, habitColorIndex: Int): Int }`
  - `data class RectU(left, top, right, bottom: Float)` with `width, height, centerX, centerY, contains(x, y)`
  - `data class PointU(x: Float, y: Float)`
  - `data class CardLayout(style, grid, width, height, paper: RectU, tab: RectU?, tape: RectU?, emojiCenterX, emojiCenterY, emojiSize, titleX, titleBaseline, titleMaxWidth: Float, weekdayAnchors: List<PointU>, cells: List<RectU>)` with `fun dayAt(x: Float, y: Float): Int?` and `CardLayout.compute(style: CardStyle, grid: MonthGrid): CardLayout`
  - 좌표 단위는 "디자인 단위"(시안의 CSS px와 동일). 렌더 시 실제 크기에 맞춰 scale.

- [ ] **Step 1: 폰트 다운로드 (사용자 승인 후)**

사용자에게 "Google Fonts GitHub(github.com/google/fonts)에서 Gaegu 2개, Gamja Flower 1개 폰트(OFL)와 라이선스 파일을 받는다"고 알리고 승인받은 뒤:
```powershell
New-Item -ItemType Directory -Force app\src\main\res\font, docs\licenses | Out-Null
curl.exe -L -o app\src\main\res\font\gaegu_regular.ttf https://github.com/google/fonts/raw/main/ofl/gaegu/Gaegu-Regular.ttf
curl.exe -L -o app\src\main\res\font\gaegu_bold.ttf https://github.com/google/fonts/raw/main/ofl/gaegu/Gaegu-Bold.ttf
curl.exe -L -o app\src\main\res\font\gamja_flower.ttf https://github.com/google/fonts/raw/main/ofl/gamjaflower/GamjaFlower-Regular.ttf
curl.exe -L -o docs\licenses\OFL-Gaegu.txt https://github.com/google/fonts/raw/main/ofl/gaegu/OFL.txt
curl.exe -L -o docs\licenses\OFL-GamjaFlower.txt https://github.com/google/fonts/raw/main/ofl/gamjaflower/OFL.txt
Get-ChildItem app\src\main\res\font | Select-Object Name, Length
```
Expected: 세 파일 모두 크기 100KB 이상 (HTML 오류 페이지가 아님)

- [ ] **Step 2: 실패하는 테스트 작성**

`app/src/test/java/com/habitdiary/app/style/PaletteTest.kt`:
```kotlin
package com.habitdiary.app.style

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PaletteTest {
    @Test
    fun habitColor_wrapsIndex() {
        assertEquals(0xFFFFB38A.toInt(), Palette.habitColor(0))
        assertEquals(0xFFD9B99B.toInt(), Palette.habitColor(7))
        assertEquals(Palette.habitColor(0), Palette.habitColor(8))
    }

    @Test
    fun tapeColor_isDeterministic_andDiffersFromHabitColor() {
        for (id in 1L..40L) {
            for (c in 0 until 8) {
                val t = Palette.tapeColorIndex(id, c)
                assertNotEquals(c, t)
                assertEquals(t, Palette.tapeColorIndex(id, c))
            }
        }
    }
}
```

`app/src/test/java/com/habitdiary/app/render/CardLayoutTest.kt`:
```kotlin
package com.habitdiary.app.render

import com.habitdiary.app.calendar.MonthGrid
import com.habitdiary.app.style.CardStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class CardLayoutTest {
    private val oct2026 = MonthGrid.of(YearMonth.of(2026, 10))   // 앞 빈칸 4, 5행
    private val feb2026 = MonthGrid.of(YearMonth.of(2026, 2))    // 앞 빈칸 0, 4행

    @Test
    fun indexTab_size() {
        val l = CardLayout.compute(CardStyle.INDEX_TAB, oct2026)
        assertEquals(320f, l.width, 0.01f)
        assertEquals(227f, l.height, 0.01f)          // 34 + 16 + 153 + 16 + 8
        assertEquals(31, l.cells.size)
        assertEquals(7, l.weekdayAnchors.size)
    }

    @Test
    fun indexTab_fourRowMonth_isShorter() {
        val l = CardLayout.compute(CardStyle.INDEX_TAB, feb2026)
        assertEquals(199f, l.height, 0.01f)          // 34 + 16 + 125 + 16 + 8
    }

    @Test
    fun indexTab_cellPositions() {
        val l = CardLayout.compute(CardStyle.INDEX_TAB, oct2026)
        assertEquals(RectU(218f, 69f, 240f, 91f), l.cells[0])      // 1일: 4열 0행
        assertEquals(RectU(274f, 181f, 296f, 203f), l.cells[30])   // 31일: 6열 4행
    }

    @Test
    fun maskingTape_sizeAndCells() {
        val l = CardLayout.compute(CardStyle.MASKING_TAPE, oct2026)
        assertEquals(320f, l.width, 0.01f)
        assertEquals(271f, l.height, 0.01f)          // 92 + 153 + 18 + 8
        assertEquals(RectU(177f, 111f, 199f, 133f), l.cells[0])
    }

    @Test
    fun dayAt_hitsCells_andMissesBlanks() {
        val l = CardLayout.compute(CardStyle.INDEX_TAB, oct2026)
        assertEquals(1, l.dayAt(229f, 80f))
        assertEquals(31, l.dayAt(285f, 192f))
        assertNull(l.dayAt(107f, 70f))    // 앞 빈 칸 자리
        assertNull(l.dayAt(5f, 5f))       // 카드 밖
    }
}
```

- [ ] **Step 3: 실패 확인**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest --tests "com.habitdiary.app.style.*" --tests "com.habitdiary.app.render.*"`
Expected: 컴파일 실패 (`Unresolved reference`)

- [ ] **Step 4: 구현**

`style/CardStyle.kt`:
```kotlin
package com.habitdiary.app.style

enum class CardStyle { INDEX_TAB, MASKING_TAPE }
```

`style/Palette.kt`:
```kotlin
package com.habitdiary.app.style

object Palette {
    val PAGE = 0xFFFFF8F0.toInt()
    val PAPER = 0xFFFFFDF9.toInt()
    val INK = 0xFF5B4636.toInt()
    val WEEKDAY = 0xFFC4AB97.toInt()
    val EMPTY = 0xFFF3EBE3.toInt()
    val FUTURE = 0xFFFAF5EF.toInt()
    val GRID_DOT = 0xFFEAD9C9.toInt()
    val PAPER_SHADOW = 0x1F966E50

    val habitColors = intArrayOf(
        0xFFFFB38A.toInt(), 0xFFFFD27A.toInt(), 0xFF9CCFA8.toInt(), 0xFF8FD3D0.toInt(),
        0xFFA9C4F5.toInt(), 0xFFC7B2F0.toInt(), 0xFFF5A9C8.toInt(), 0xFFD9B99B.toInt(),
    )

    fun habitColor(index: Int): Int = habitColors[index.mod(habitColors.size)]

    /** 마스킹테이프 색: 습관 id로 정해지되 습관 색과는 겹치지 않는 팔레트 인덱스. */
    fun tapeColorIndex(habitId: Long, habitColorIndex: Int): Int {
        val n = habitColors.size
        val i = habitId.mod(n.toLong()).toInt()
        return if (i == habitColorIndex.mod(n)) (i + 1) % n else i
    }
}
```

`render/CardLayout.kt`:
```kotlin
package com.habitdiary.app.render

import com.habitdiary.app.calendar.MonthGrid
import com.habitdiary.app.style.CardStyle
import kotlin.math.max

data class RectU(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

data class PointU(val x: Float, val y: Float)

/** 카드 한 장의 좌표 (디자인 단위). cells[i] = (i+1)일 칸. */
data class CardLayout(
    val style: CardStyle,
    val grid: MonthGrid,
    val width: Float,
    val height: Float,
    val paper: RectU,
    val tab: RectU?,
    val tape: RectU?,
    val emojiCenterX: Float,
    val emojiCenterY: Float,
    val emojiSize: Float,
    val titleX: Float,
    val titleBaseline: Float,
    val titleMaxWidth: Float,
    val weekdayAnchors: List<PointU>,
    val cells: List<RectU>,
) {
    fun dayAt(x: Float, y: Float): Int? {
        val i = cells.indexOfFirst { it.contains(x, y) }
        return if (i < 0) null else i + 1
    }

    companion object {
        const val CELL = 22f
        const val GAP = 6f
        const val PITCH = CELL + GAP
        const val HEADER = 19f            // 요일 글자 13 + 간격 6
        const val MARGIN = 8f
        const val PAPER_W = 304f
        const val GRID_W = 7 * CELL + 6 * GAP   // 190

        fun gridHeight(rows: Int): Float = HEADER + rows * CELL + (rows - 1) * GAP

        fun compute(style: CardStyle, grid: MonthGrid): CardLayout = when (style) {
            CardStyle.INDEX_TAB -> indexTab(grid)
            CardStyle.MASKING_TAPE -> maskingTape(grid)
        }

        private fun indexTab(grid: MonthGrid): CardLayout {
            val gridH = gridHeight(grid.rows)
            val contentH = max(64f, gridH)
            val paperTop = MARGIN + 26f
            val paper = RectU(MARGIN, paperTop, MARGIN + PAPER_W, paperTop + 16f + contentH + 16f)
            val tab = RectU(MARGIN + 16f, MARGIN, paper.right - 16f, paperTop)
            val gridLeft = MARGIN + 16f + 64f + 18f
            val gridTop = paperTop + 16f + (contentH - gridH) / 2f
            val (anchors, cells) = gridParts(grid, gridLeft, gridTop)
            return CardLayout(
                style = CardStyle.INDEX_TAB, grid = grid,
                width = MARGIN * 2 + PAPER_W, height = paper.bottom + MARGIN,
                paper = paper, tab = tab, tape = null,
                emojiCenterX = MARGIN + 16f + 32f, emojiCenterY = paperTop + 16f + contentH / 2f, emojiSize = 32f,
                titleX = tab.left + 16f, titleBaseline = MARGIN + 19f, titleMaxWidth = tab.width - 32f,
                weekdayAnchors = anchors, cells = cells,
            )
        }

        private fun maskingTape(grid: MonthGrid): CardLayout {
            val gridH = gridHeight(grid.rows)
            val paperTop = MARGIN + 10f
            val gridTop = paperTop + 22f + 40f + 12f
            val paper = RectU(MARGIN, paperTop, MARGIN + PAPER_W, gridTop + gridH + 18f)
            val tape = RectU(paper.centerX - 35f, MARGIN, paper.centerX + 35f, MARGIN + 20f)
            val gridLeft = MARGIN + (PAPER_W - GRID_W) / 2f
            val titleX = MARGIN + 18f + 40f + 10f
            val (anchors, cells) = gridParts(grid, gridLeft, gridTop)
            return CardLayout(
                style = CardStyle.MASKING_TAPE, grid = grid,
                width = MARGIN * 2 + PAPER_W, height = paper.bottom + MARGIN,
                paper = paper, tab = null, tape = tape,
                emojiCenterX = MARGIN + 18f + 20f, emojiCenterY = paperTop + 22f + 20f, emojiSize = 34f,
                titleX = titleX, titleBaseline = paperTop + 22f + 28f, titleMaxWidth = paper.right - 18f - titleX,
                weekdayAnchors = anchors, cells = cells,
            )
        }

        private fun gridParts(grid: MonthGrid, left: Float, top: Float): Pair<List<PointU>, List<RectU>> {
            val anchors = (0 until 7).map { PointU(left + it * PITCH + CELL / 2f, top + 11f) }
            val cells = (1..grid.daysInMonth).map { day ->
                val idx = grid.leadingBlanks + day - 1
                val l = left + (idx % 7) * PITCH
                val t = top + HEADER + (idx / 7) * PITCH
                RectU(l, t, l + CELL, t + CELL)
            }
            return anchors to cells
        }
    }
}
```

- [ ] **Step 5: 통과 확인**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest --tests "com.habitdiary.app.style.*" --tests "com.habitdiary.app.render.*"`
Expected: `BUILD SUCCESSFUL`, 7 tests passed

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/habitdiary/app/style app/src/main/java/com/habitdiary/app/render app/src/main/res/font docs/licenses app/src/test/java/com/habitdiary/app/style app/src/test/java/com/habitdiary/app/render
git commit -m "feat: add palette, bundled fonts, and card layout geometry"
```

---

### Task 4: CardRenderer (Canvas로 C/A 카드 그리기)

**Files:**
- Create: `app/src/main/java/com/habitdiary/app/render/CardFonts.kt`, `render/CardRenderer.kt`
- Test: `app/src/test/java/com/habitdiary/app/render/CardRendererTest.kt`

**Interfaces:**
- Consumes: `CardLayout`, `RectU`, `PointU` (Task 3), `Palette`, `CardStyle` (Task 3), `DayKind`, `MonthGrid.kindOf` (Task 2)
- Produces:
  - `data class CardFonts(val gaegu: Typeface, val gaeguBold: Typeface, val gamja: Typeface)` + `CardFonts.load(context: Context): CardFonts`
  - `data class CardContent(val habitId: Long, val name: String, val emoji: String, val colorIndex: Int, val doneDays: Set<Int>, val today: LocalDate)`
  - `class CardRenderer(fonts: CardFonts)` with `fun draw(canvas: Canvas, layout: CardLayout, content: CardContent)` (디자인 단위로 그림 — 호출자가 scale) and `fun renderBitmap(layout: CardLayout, content: CardContent, widthPx: Int, heightPx: Int): Bitmap`

- [ ] **Step 1: 실패하는 테스트 작성**

Robolectric 네이티브 그래픽으로 실제 픽셀을 검사하고, 눈으로 확인할 PNG도 남긴다.
```kotlin
package com.habitdiary.app.render

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import com.habitdiary.app.calendar.MonthGrid
import com.habitdiary.app.style.CardStyle
import com.habitdiary.app.style.Palette
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CardRendererTest {
    private val renderer = CardRenderer(CardFonts(Typeface.DEFAULT, Typeface.DEFAULT_BOLD, Typeface.DEFAULT))
    private val today = LocalDate.of(2026, 10, 14)
    private val content = CardContent(
        habitId = 2, name = "1시30분전 수면", emoji = "🌙", colorIndex = 0,
        doneDays = setOf(2, 3, 5, 6, 7, 10, 12, 13), today = today,
    )

    private fun render(style: CardStyle): Pair<CardLayout, Bitmap> {
        val layout = CardLayout.compute(style, MonthGrid.of(YearMonth.of(2026, 10)))
        val bmp = renderer.renderBitmap(layout, content, (layout.width * 2).toInt(), (layout.height * 2).toInt())
        File("build/render-preview").mkdirs()
        File("build/render-preview/${style.name}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return layout to bmp
    }

    private fun pixelAtDay(layout: CardLayout, bmp: Bitmap, day: Int): Int {
        val c = layout.cells[day - 1]
        return bmp.getPixel((c.centerX * 2).toInt(), (c.centerY * 2).toInt())
    }

    @Test
    fun indexTab_colorsCellsByState() {
        val (layout, bmp) = render(CardStyle.INDEX_TAB)
        assertEquals(Palette.habitColor(0), pixelAtDay(layout, bmp, 2))   // 완료
        assertEquals(Palette.EMPTY, pixelAtDay(layout, bmp, 1))            // 지난 미완료
        assertEquals(Palette.FUTURE, pixelAtDay(layout, bmp, 20))          // 미래
    }

    @Test
    fun maskingTape_colorsCellsByState() {
        val (layout, bmp) = render(CardStyle.MASKING_TAPE)
        // ±1° 기울임이 있어 중심 픽셀 색은 그대로지만 안티앨리어싱 허용 위해 RGB만 비교
        assertEquals(Color.rgb(0xFF, 0xB3, 0x8A) and 0xFFFFFF, pixelAtDay(layout, bmp, 2) and 0xFFFFFF)
        assertEquals(Palette.FUTURE and 0xFFFFFF, pixelAtDay(layout, bmp, 20) and 0xFFFFFF)
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest --tests "com.habitdiary.app.render.CardRendererTest"`
Expected: 컴파일 실패 (`Unresolved reference: CardRenderer`)

- [ ] **Step 3: 구현**

`render/CardFonts.kt`:
```kotlin
package com.habitdiary.app.render

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.habitdiary.app.R

data class CardFonts(val gaegu: Typeface, val gaeguBold: Typeface, val gamja: Typeface) {
    companion object {
        fun load(context: Context) = CardFonts(
            gaegu = ResourcesCompat.getFont(context, R.font.gaegu_regular) ?: Typeface.DEFAULT,
            gaeguBold = ResourcesCompat.getFont(context, R.font.gaegu_bold) ?: Typeface.DEFAULT_BOLD,
            gamja = ResourcesCompat.getFont(context, R.font.gamja_flower) ?: Typeface.DEFAULT,
        )
    }
}
```

`render/CardRenderer.kt`:
```kotlin
package com.habitdiary.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import com.habitdiary.app.calendar.DayKind
import com.habitdiary.app.style.CardStyle
import com.habitdiary.app.style.Palette
import java.time.LocalDate
import kotlin.math.min

data class CardContent(
    val habitId: Long,
    val name: String,
    val emoji: String,
    val colorIndex: Int,
    val doneDays: Set<Int>,
    val today: LocalDate,
)

class CardRenderer(private val fonts: CardFonts) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val dash = DashPathEffect(floatArrayOf(5f, 4f), 0f)

    /** layout의 디자인 단위 좌표 그대로 그린다. 호출자가 canvas.scale로 크기를 맞춘다. */
    fun draw(canvas: Canvas, layout: CardLayout, content: CardContent) {
        when (layout.style) {
            CardStyle.INDEX_TAB -> drawIndexTab(canvas, layout, content)
            CardStyle.MASKING_TAPE -> drawMaskingTape(canvas, layout, content)
        }
    }

    fun renderBitmap(layout: CardLayout, content: CardContent, widthPx: Int, heightPx: Int): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.scale(widthPx / layout.width, heightPx / layout.height)
        draw(canvas, layout, content)
        return bmp
    }

    private fun drawIndexTab(canvas: Canvas, layout: CardLayout, content: CardContent) {
        val color = Palette.habitColor(content.colorIndex)
        val tab = layout.tab!!
        val p = layout.paper

        setText(fonts.gamja, 19f, Color.WHITE, Paint.Align.LEFT)
        val title = ellipsize(content.name, layout.titleMaxWidth)
        val tabRight = tab.left + min(text.measureText(title) + 32f, tab.width)
        fill.color = color
        canvas.drawPath(roundPath(tab.left, tab.top, tabRight, tab.bottom, floatArrayOf(12f, 12f, 12f, 12f, 0f, 0f, 0f, 0f)), fill)
        canvas.drawText(title, layout.titleX, layout.titleBaseline, text)

        val paperPath = roundPath(p.left, p.top, p.right, p.bottom, floatArrayOf(4f, 4f, 18f, 18f, 18f, 18f, 18f, 18f))
        fill.color = Palette.PAPER
        canvas.drawPath(paperPath, fill)
        canvas.save()
        canvas.clipPath(paperPath)
        fill.color = Palette.GRID_DOT
        var y = p.top + 7f
        while (y < p.bottom) {
            var x = p.left + 7f
            while (x < p.right) {
                canvas.drawCircle(x, y, 1f, fill)
                x += 14f
            }
            y += 14f
        }
        canvas.restore()
        stroke.color = color
        stroke.strokeWidth = 2f
        stroke.pathEffect = null
        canvas.drawPath(paperPath, stroke)

        stroke.pathEffect = dash
        canvas.drawCircle(layout.emojiCenterX, layout.emojiCenterY, 31f, stroke)
        stroke.pathEffect = null
        drawEmoji(canvas, content.emoji, layout.emojiCenterX, layout.emojiCenterY, layout.emojiSize, -10f)

        drawGrid(canvas, layout, content, fonts.gamja)
    }

    private fun drawMaskingTape(canvas: Canvas, layout: CardLayout, content: CardContent) {
        val p = layout.paper
        val tape = layout.tape!!
        canvas.save()
        canvas.rotate(if (content.habitId % 2L == 0L) -1f else 1f, p.centerX, p.centerY)

        fill.color = Palette.PAPER
        fill.setShadowLayer(10f, 0f, 3f, Palette.PAPER_SHADOW)
        canvas.drawRoundRect(p.left, p.top, p.right, p.bottom, 6f, 6f, fill)
        fill.clearShadowLayer()

        canvas.save()
        canvas.rotate(-4f, tape.centerX, tape.centerY)
        fill.color = Palette.habitColor(Palette.tapeColorIndex(content.habitId, content.colorIndex))
        fill.alpha = 191
        canvas.drawRoundRect(tape.left, tape.top, tape.right, tape.bottom, 2f, 2f, fill)
        fill.alpha = 255
        canvas.restore()

        drawEmoji(canvas, content.emoji, layout.emojiCenterX, layout.emojiCenterY, layout.emojiSize, -8f)
        setText(fonts.gaeguBold, 24f, Palette.INK, Paint.Align.LEFT)
        canvas.drawText(ellipsize(content.name, layout.titleMaxWidth), layout.titleX, layout.titleBaseline, text)

        drawGrid(canvas, layout, content, fonts.gaegu)
        canvas.restore()
    }

    private fun drawGrid(canvas: Canvas, layout: CardLayout, content: CardContent, headerFont: Typeface) {
        setText(headerFont, 13f, Palette.WEEKDAY, Paint.Align.CENTER)
        WEEKDAYS.forEachIndexed { i, w ->
            val a = layout.weekdayAnchors[i]
            canvas.drawText(w, a.x, a.y, text)
        }
        val color = Palette.habitColor(content.colorIndex)
        layout.cells.forEachIndexed { i, r ->
            val day = i + 1
            val kind = layout.grid.kindOf(day, content.today)
            fill.color = when {
                day in content.doneDays -> color
                kind == DayKind.FUTURE -> Palette.FUTURE
                else -> Palette.EMPTY
            }
            canvas.drawCircle(r.centerX, r.centerY, r.width / 2f, fill)
            if (kind == DayKind.TODAY) {
                stroke.color = color
                stroke.strokeWidth = 2f
                stroke.pathEffect = null
                canvas.drawCircle(r.centerX, r.centerY, r.width / 2f + 1f, stroke)
            }
        }
    }

    private fun drawEmoji(canvas: Canvas, emoji: String, cx: Float, cy: Float, size: Float, degrees: Float) {
        setText(Typeface.DEFAULT, size, Color.BLACK, Paint.Align.CENTER)
        canvas.save()
        canvas.rotate(degrees, cx, cy)
        canvas.drawText(emoji, cx, cy - (text.ascent() + text.descent()) / 2f, text)
        canvas.restore()
    }

    private fun setText(typeface: Typeface, size: Float, color: Int, align: Paint.Align) {
        text.typeface = typeface
        text.textSize = size
        text.color = color
        text.textAlign = align
    }

    private fun ellipsize(s: String, maxWidth: Float): String =
        TextUtils.ellipsize(s, text, maxWidth, TextUtils.TruncateAt.END).toString()

    private fun roundPath(l: Float, t: Float, r: Float, b: Float, radii: FloatArray) =
        Path().apply { addRoundRect(RectF(l, t, r, b), radii, Path.Direction.CW) }

    private companion object {
        val WEEKDAYS = listOf("일", "월", "화", "수", "목", "금", "토")
    }
}
```

- [ ] **Step 4: 통과 확인 + PNG 눈으로 확인**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest --tests "com.habitdiary.app.render.CardRendererTest"`
Expected: `BUILD SUCCESSFUL`, 2 tests passed. 그다음 `app/build/render-preview/INDEX_TAB.png`, `MASKING_TAPE.png`를 Read 도구로 열어 시안(`.superpowers/brainstorm/647-1791989205/content/diary-layout-v2.html`의 C/A)과 비슷한지 확인한다. (테스트는 기본 폰트로 그리므로 손글씨가 아닌 건 정상)

Robolectric 네이티브 그래픽이 이 Windows 환경에서 초기화되지 않아 테스트가 렌더링과 무관한 오류(예: `UnsatisfiedLinkError`)로 실패하면, 이 테스트 클래스에 `@Ignore("native graphics unavailable on this host")`를 붙이고 그 사실을 보고한다 — 시각 확인은 Task 8 실기기 단계가 대신한다.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/habitdiary/app/render app/src/test/java/com/habitdiary/app/render
git commit -m "feat: draw index-tab and masking-tape cards on Canvas"
```

---

### Task 5: 데이터 계층 (Room + DataStore)

**Files:**
- Create: `app/src/main/java/com/habitdiary/app/data/Habit.kt`, `data/Check.kt`, `data/HabitDao.kt`, `data/HabitDatabase.kt`, `data/HabitStore.kt`, `data/Settings.kt`
- Test: `app/src/test/java/com/habitdiary/app/data/RoomHabitStoreTest.kt`, `app/src/test/java/com/habitdiary/app/data/SettingsTest.kt`

**Interfaces:**
- Consumes: `CardStyle` (Task 3)
- Produces:
  - `@Entity data class Habit(val id: Long = 0, val name: String, val emoji: String, val colorIndex: Int, val sortOrder: Int)`
  - `@Entity data class Check(val habitId: Long, val epochDay: Long)`
  - `interface HabitStore { observeHabits(): Flow<List<Habit>>; observeMonthChecks(month: YearMonth): Flow<Map<Long, Set<Int>>>; suspend getHabit(id: Long): Habit?; suspend doneDays(habitId: Long, month: YearMonth): Set<Int>; suspend toggle(habitId: Long, date: LocalDate); suspend addHabit(name: String, emoji: String, colorIndex: Int): Long; suspend updateHabit(habit: Habit); suspend deleteHabit(id: Long); suspend reorder(idsInOrder: List<Long>) }`
  - `class RoomHabitStore(dao: HabitDao, onChanged: () -> Unit = {}) : HabitStore` — 모든 변경 후 `onChanged()` 호출
  - `HabitDatabase.build(context): HabitDatabase`, `HabitDatabase.habitDao()`
  - `val Context.settingsDataStore: DataStore<Preferences>`
  - `interface SettingsStore { val cardStyle: Flow<CardStyle>; suspend fun setCardStyle(style: CardStyle) }`
  - `class DataStoreSettings(ds: DataStore<Preferences>, onChanged: () -> Unit = {}) : SettingsStore`
  - `class WidgetBindings(ds: DataStore<Preferences>) { suspend bind(appWidgetId: Int, habitId: Long); suspend habitIdFor(appWidgetId: Int): Long?; suspend unbind(appWidgetId: Int) }`

- [ ] **Step 1: 실패하는 테스트 작성**

`RoomHabitStoreTest.kt`:
```kotlin
package com.habitdiary.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomHabitStoreTest {
    private lateinit var db: HabitDatabase
    private lateinit var store: RoomHabitStore
    private var changes = 0
    private val oct = YearMonth.of(2026, 10)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HabitDatabase::class.java)
            .allowMainThreadQueries().build()
        store = RoomHabitStore(db.habitDao()) { changes++ }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun addHabit_appendsInOrder() = runTest {
        store.addHabit("수면", "🌙", 0)
        store.addHabit("밀크티", "🥤", 2)
        assertEquals(listOf("수면", "밀크티"), store.observeHabits().first().map { it.name })
        assertEquals(2, changes)
    }

    @Test
    fun toggle_addsThenRemoves_andScopesToMonth() = runTest {
        val id = store.addHabit("수면", "🌙", 0)
        store.toggle(id, LocalDate.of(2026, 10, 5))
        store.toggle(id, LocalDate.of(2026, 10, 6))
        store.toggle(id, LocalDate.of(2026, 9, 30))
        assertEquals(setOf(5, 6), store.doneDays(id, oct))
        assertEquals(mapOf(id to setOf(5, 6)), store.observeMonthChecks(oct).first())
        store.toggle(id, LocalDate.of(2026, 10, 5))
        assertEquals(setOf(6), store.doneDays(id, oct))
    }

    @Test
    fun deleteHabit_removesItsChecks() = runTest {
        val id = store.addHabit("수면", "🌙", 0)
        store.toggle(id, LocalDate.of(2026, 10, 5))
        store.deleteHabit(id)
        assertNull(store.getHabit(id))
        assertEquals(emptyMap<Long, Set<Int>>(), store.observeMonthChecks(oct).first())
    }

    @Test
    fun reorder_and_update() = runTest {
        val a = store.addHabit("A", "🅰", 0)
        val b = store.addHabit("B", "🅱", 1)
        store.reorder(listOf(b, a))
        assertEquals(listOf(b, a), store.observeHabits().first().map { it.id })
        store.updateHabit(store.getHabit(a)!!.copy(name = "A2"))
        assertEquals("A2", store.getHabit(a)!!.name)
    }
}
```

`SettingsTest.kt`:
```kotlin
package com.habitdiary.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.habitdiary.app.style.CardStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun cardStyle_defaultsToIndexTab_andPersists() = runTest {
        val ds = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "s.preferences_pb") }
        var changed = 0
        val settings = DataStoreSettings(ds) { changed++ }
        assertEquals(CardStyle.INDEX_TAB, settings.cardStyle.first())
        settings.setCardStyle(CardStyle.MASKING_TAPE)
        assertEquals(CardStyle.MASKING_TAPE, settings.cardStyle.first())
        assertEquals(1, changed)
    }

    @Test
    fun widgetBindings_bindLookupUnbind() = runTest {
        val ds = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "w.preferences_pb") }
        val bindings = WidgetBindings(ds)
        assertNull(bindings.habitIdFor(7))
        bindings.bind(7, 42L)
        assertEquals(42L, bindings.habitIdFor(7))
        bindings.unbind(7)
        assertNull(bindings.habitIdFor(7))
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest --tests "com.habitdiary.app.data.*"`
Expected: 컴파일 실패 (`Unresolved reference`)

- [ ] **Step 3: 구현**

`data/Habit.kt`:
```kotlin
package com.habitdiary.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val colorIndex: Int,
    val sortOrder: Int,
)
```

`data/Check.kt`:
```kotlin
package com.habitdiary.app.data

import androidx.room.Entity
import androidx.room.Index

/** habitId 습관을 epochDay(LocalDate.toEpochDay) 날짜에 완료함. */
@Entity(tableName = "checks", primaryKeys = ["habitId", "epochDay"], indices = [Index("habitId")])
data class Check(val habitId: Long, val epochDay: Long)
```

`data/HabitDao.kt`:
```kotlin
package com.habitdiary.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HabitDao {
    @Query("SELECT * FROM habits ORDER BY sortOrder, id")
    abstract fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    abstract suspend fun getHabit(id: Long): Habit?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM habits")
    abstract suspend fun maxSortOrder(): Int

    @Insert
    abstract suspend fun insert(habit: Habit): Long

    @Update
    abstract suspend fun update(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    abstract suspend fun deleteHabitRow(id: Long)

    @Query("DELETE FROM checks WHERE habitId = :id")
    abstract suspend fun deleteChecksOf(id: Long)

    @Transaction
    open suspend fun deleteHabitWithChecks(id: Long) {
        deleteChecksOf(id)
        deleteHabitRow(id)
    }

    @Query("UPDATE habits SET sortOrder = :order WHERE id = :id")
    abstract suspend fun setSortOrder(id: Long, order: Int)

    @Transaction
    open suspend fun reorder(ids: List<Long>) {
        ids.forEachIndexed { index, id -> setSortOrder(id, index) }
    }

    @Query("SELECT * FROM checks WHERE epochDay BETWEEN :from AND :to")
    abstract fun observeChecks(from: Long, to: Long): Flow<List<Check>>

    @Query("SELECT * FROM checks WHERE habitId = :habitId AND epochDay BETWEEN :from AND :to")
    abstract suspend fun getChecks(habitId: Long, from: Long, to: Long): List<Check>

    @Query("SELECT COUNT(*) FROM checks WHERE habitId = :habitId AND epochDay = :day")
    abstract suspend fun countCheck(habitId: Long, day: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCheck(check: Check)

    @Query("DELETE FROM checks WHERE habitId = :habitId AND epochDay = :day")
    abstract suspend fun deleteCheck(habitId: Long, day: Long)
}
```

`data/HabitDatabase.kt`:
```kotlin
package com.habitdiary.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Habit::class, Check::class], version = 1, exportSchema = false)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        fun build(context: Context): HabitDatabase =
            Room.databaseBuilder(context, HabitDatabase::class.java, "habits.db").build()
    }
}
```

`data/HabitStore.kt`:
```kotlin
package com.habitdiary.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

interface HabitStore {
    fun observeHabits(): Flow<List<Habit>>
    /** habitId → 그 달에 완료한 날(dayOfMonth) 집합. 완료가 없는 습관은 키가 없다. */
    fun observeMonthChecks(month: YearMonth): Flow<Map<Long, Set<Int>>>
    suspend fun getHabit(id: Long): Habit?
    suspend fun doneDays(habitId: Long, month: YearMonth): Set<Int>
    suspend fun toggle(habitId: Long, date: LocalDate)
    suspend fun addHabit(name: String, emoji: String, colorIndex: Int): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(id: Long)
    suspend fun reorder(idsInOrder: List<Long>)
}

class RoomHabitStore(
    private val dao: HabitDao,
    private val onChanged: () -> Unit = {},
) : HabitStore {
    override fun observeHabits() = dao.observeHabits()

    override fun observeMonthChecks(month: YearMonth): Flow<Map<Long, Set<Int>>> =
        dao.observeChecks(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay()).map { checks ->
            checks.groupBy({ it.habitId }, { LocalDate.ofEpochDay(it.epochDay).dayOfMonth })
                .mapValues { it.value.toSet() }
        }

    override suspend fun getHabit(id: Long) = dao.getHabit(id)

    override suspend fun doneDays(habitId: Long, month: YearMonth): Set<Int> =
        dao.getChecks(habitId, month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())
            .map { LocalDate.ofEpochDay(it.epochDay).dayOfMonth }.toSet()

    override suspend fun toggle(habitId: Long, date: LocalDate) {
        val day = date.toEpochDay()
        if (dao.countCheck(habitId, day) > 0) dao.deleteCheck(habitId, day)
        else dao.insertCheck(Check(habitId, day))
        onChanged()
    }

    override suspend fun addHabit(name: String, emoji: String, colorIndex: Int): Long {
        val id = dao.insert(Habit(name = name, emoji = emoji, colorIndex = colorIndex, sortOrder = dao.maxSortOrder() + 1))
        onChanged()
        return id
    }

    override suspend fun updateHabit(habit: Habit) {
        dao.update(habit)
        onChanged()
    }

    override suspend fun deleteHabit(id: Long) {
        dao.deleteHabitWithChecks(id)
        onChanged()
    }

    override suspend fun reorder(idsInOrder: List<Long>) {
        dao.reorder(idsInOrder)
        onChanged()
    }
}
```

`data/Settings.kt`:
```kotlin
package com.habitdiary.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.habitdiary.app.style.CardStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

interface SettingsStore {
    val cardStyle: Flow<CardStyle>
    suspend fun setCardStyle(style: CardStyle)
}

class DataStoreSettings(
    private val ds: DataStore<Preferences>,
    private val onChanged: () -> Unit = {},
) : SettingsStore {
    private val styleKey = stringPreferencesKey("card_style")

    override val cardStyle: Flow<CardStyle> = ds.data.map { prefs ->
        prefs[styleKey]?.let { name -> CardStyle.entries.firstOrNull { it.name == name } } ?: CardStyle.INDEX_TAB
    }

    override suspend fun setCardStyle(style: CardStyle) {
        ds.edit { it[styleKey] = style.name }
        onChanged()
    }
}

/** 홈 화면 위젯 id → 습관 id 연결. */
class WidgetBindings(private val ds: DataStore<Preferences>) {
    private fun key(appWidgetId: Int) = longPreferencesKey("widget_$appWidgetId")

    suspend fun bind(appWidgetId: Int, habitId: Long) {
        ds.edit { it[key(appWidgetId)] = habitId }
    }

    suspend fun habitIdFor(appWidgetId: Int): Long? = ds.data.first()[key(appWidgetId)]

    suspend fun unbind(appWidgetId: Int) {
        ds.edit { it.remove(key(appWidgetId)) }
    }
}
```

- [ ] **Step 4: 통과 확인**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest --tests "com.habitdiary.app.data.*"`
Expected: `BUILD SUCCESSFUL`, 6 tests passed

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/habitdiary/app/data app/src/test/java/com/habitdiary/app/data
git commit -m "feat: add Room habit store and DataStore settings"
```

---

### Task 6: MainViewModel

**Files:**
- Create: `app/src/main/java/com/habitdiary/app/ui/MainViewModel.kt`
- Test: `app/src/test/java/com/habitdiary/app/ui/MainViewModelTest.kt`

**Interfaces:**
- Consumes: `HabitStore`, `Habit`, `SettingsStore` (Task 5), `CardStyle` (Task 3)
- Produces:
  - `data class HabitRow(val habit: Habit, val doneDays: Set<Int>)`
  - `data class MainUiState(val month: YearMonth, val today: LocalDate, val style: CardStyle, val rows: List<HabitRow>, val loaded: Boolean)`
  - `class MainViewModel(store: HabitStore, settings: SettingsStore, clock: () -> LocalDate = { LocalDate.now() })` with `state: StateFlow<MainUiState>`, `previousMonth()`, `nextMonth()`, `refreshToday()`, `onDayTapped(habitId: Long, day: Int)`, `saveHabit(id: Long?, name: String, emoji: String, colorIndex: Int)`, `deleteHabit(id: Long)`, `reorder(ids: List<Long>)`, `setStyle(style: CardStyle)`
  - `MainViewModel.DEFAULT_EMOJI = "📝"`

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
package com.habitdiary.app.ui

import com.habitdiary.app.data.Habit
import com.habitdiary.app.data.HabitStore
import com.habitdiary.app.data.SettingsStore
import com.habitdiary.app.style.CardStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

private class FakeStore : HabitStore {
    val habits = MutableStateFlow<List<Habit>>(emptyList())
    val checks = MutableStateFlow<Set<Pair<Long, LocalDate>>>(emptySet())
    private var nextId = 1L

    override fun observeHabits(): Flow<List<Habit>> = habits
    override fun observeMonthChecks(month: YearMonth) = checks.map { all ->
        all.filter { YearMonth.from(it.second) == month }
            .groupBy({ it.first }, { it.second.dayOfMonth }).mapValues { it.value.toSet() }
    }
    override suspend fun getHabit(id: Long) = habits.value.firstOrNull { it.id == id }
    override suspend fun doneDays(habitId: Long, month: YearMonth) =
        checks.value.filter { it.first == habitId && YearMonth.from(it.second) == month }.map { it.second.dayOfMonth }.toSet()
    override suspend fun toggle(habitId: Long, date: LocalDate) {
        val key = habitId to date
        checks.value = if (key in checks.value) checks.value - key else checks.value + key
    }
    override suspend fun addHabit(name: String, emoji: String, colorIndex: Int): Long {
        val id = nextId++
        habits.value = habits.value + Habit(id, name, emoji, colorIndex, habits.value.size)
        return id
    }
    override suspend fun updateHabit(habit: Habit) {
        habits.value = habits.value.map { if (it.id == habit.id) habit else it }
    }
    override suspend fun deleteHabit(id: Long) {
        habits.value = habits.value.filterNot { it.id == id }
    }
    override suspend fun reorder(idsInOrder: List<Long>) {
        habits.value = idsInOrder.mapIndexed { i, id -> habits.value.first { it.id == id }.copy(sortOrder = i) }
    }
}

private class FakeSettings : SettingsStore {
    override val cardStyle = MutableStateFlow(CardStyle.INDEX_TAB)
    override suspend fun setCardStyle(style: CardStyle) { cardStyle.value = style }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val today = LocalDate.of(2026, 10, 14)
    private lateinit var store: FakeStore
    private lateinit var settings: FakeSettings
    private lateinit var vm: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        store = FakeStore()
        settings = FakeSettings()
        vm = MainViewModel(store, settings) { today }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsOnCurrentMonth_withRows() {
        vm.saveHabit(null, "수면", "🌙", 0)
        val s = vm.state.value
        assertEquals(YearMonth.of(2026, 10), s.month)
        assertEquals(today, s.today)
        assertEquals(listOf("수면"), s.rows.map { it.habit.name })
        assertEquals(true, s.loaded)
    }

    @Test
    fun tapPastAndToday_toggles_futureIgnored() {
        vm.saveHabit(null, "수면", "🌙", 0)
        val id = vm.state.value.rows.single().habit.id
        vm.onDayTapped(id, 13)
        vm.onDayTapped(id, 14)
        vm.onDayTapped(id, 15)
        assertEquals(setOf(13, 14), vm.state.value.rows.single().doneDays)
        vm.onDayTapped(id, 13)
        assertEquals(setOf(14), vm.state.value.rows.single().doneDays)
    }

    @Test
    fun monthNavigation_showsThatMonthsChecks() {
        vm.saveHabit(null, "수면", "🌙", 0)
        val id = vm.state.value.rows.single().habit.id
        vm.previousMonth()
        assertEquals(YearMonth.of(2026, 9), vm.state.value.month)
        vm.onDayTapped(id, 30)
        assertEquals(setOf(30), vm.state.value.rows.single().doneDays)
        vm.nextMonth()
        assertEquals(emptySet<Int>(), vm.state.value.rows.single().doneDays)
    }

    @Test
    fun saveHabit_trimsName_defaultsEmoji_ignoresBlank_updatesExisting() {
        vm.saveHabit(null, "   ", "🌙", 0)
        assertEquals(0, vm.state.value.rows.size)
        vm.saveHabit(null, "  수면  ", "", 3)
        val h = vm.state.value.rows.single().habit
        assertEquals("수면", h.name)
        assertEquals(MainViewModel.DEFAULT_EMOJI, h.emoji)
        vm.saveHabit(h.id, "잠", "🌙", 5)
        val updated = vm.state.value.rows.single().habit
        assertEquals("잠", updated.name)
        assertEquals("🌙", updated.emoji)
        assertEquals(5, updated.colorIndex)
    }

    @Test
    fun deleteReorderAndStyle() {
        vm.saveHabit(null, "A", "🅰", 0)
        vm.saveHabit(null, "B", "🅱", 1)
        val (a, b) = vm.state.value.rows.map { it.habit.id }
        vm.reorder(listOf(b, a))
        assertEquals(listOf("B", "A"), vm.state.value.rows.map { it.habit.name })
        vm.deleteHabit(b)
        assertEquals(listOf("A"), vm.state.value.rows.map { it.habit.name })
        vm.setStyle(CardStyle.MASKING_TAPE)
        assertEquals(CardStyle.MASKING_TAPE, vm.state.value.style)
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest --tests "com.habitdiary.app.ui.MainViewModelTest"`
Expected: 컴파일 실패 (`Unresolved reference: MainViewModel`)

- [ ] **Step 3: 구현**

```kotlin
package com.habitdiary.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitdiary.app.data.Habit
import com.habitdiary.app.data.HabitStore
import com.habitdiary.app.data.SettingsStore
import com.habitdiary.app.style.CardStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class HabitRow(val habit: Habit, val doneDays: Set<Int>)

data class MainUiState(
    val month: YearMonth,
    val today: LocalDate,
    val style: CardStyle,
    val rows: List<HabitRow>,
    val loaded: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val store: HabitStore,
    private val settings: SettingsStore,
    private val clock: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.from(clock()))
    private val today = MutableStateFlow(clock())

    // 달과 그 달의 체크를 한 쌍으로 묶어, 달을 넘길 때 이전 달 체크가 잠깐 섞여 보이지 않게 한다.
    private val monthChecks = month.flatMapLatest { m -> store.observeMonthChecks(m).map { m to it } }

    val state: StateFlow<MainUiState> =
        combine(monthChecks, today, settings.cardStyle, store.observeHabits()) { (m, checks), t, style, habits ->
            MainUiState(m, t, style, habits.map { HabitRow(it, checks[it.id] ?: emptySet()) }, loaded = true)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            MainUiState(month.value, today.value, CardStyle.INDEX_TAB, emptyList(), loaded = false),
        )

    fun previousMonth() { month.value = month.value.minusMonths(1) }

    fun nextMonth() { month.value = month.value.plusMonths(1) }

    /** 앱이 다시 보일 때 호출: 자정이 지났으면 오늘을 갱신한다. */
    fun refreshToday() { today.value = clock() }

    fun onDayTapped(habitId: Long, day: Int) {
        val date = month.value.atDay(day)
        if (date.isAfter(clock())) return
        viewModelScope.launch { store.toggle(habitId, date) }
    }

    fun saveHabit(id: Long?, name: String, emoji: String, colorIndex: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val e = emoji.ifBlank { DEFAULT_EMOJI }
        viewModelScope.launch {
            val existing = id?.let { store.getHabit(it) }
            if (existing == null) store.addHabit(trimmed, e, colorIndex)
            else store.updateHabit(existing.copy(name = trimmed, emoji = e, colorIndex = colorIndex))
        }
    }

    fun deleteHabit(id: Long) { viewModelScope.launch { store.deleteHabit(id) } }

    fun reorder(ids: List<Long>) { viewModelScope.launch { store.reorder(ids) } }

    fun setStyle(style: CardStyle) { viewModelScope.launch { settings.setCardStyle(style) } }

    companion object {
        const val DEFAULT_EMOJI = "📝"
    }
}
```

- [ ] **Step 4: 통과 확인**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest --tests "com.habitdiary.app.ui.MainViewModelTest"`
Expected: `BUILD SUCCESSFUL`, 5 tests passed

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/habitdiary/app/ui/MainViewModel.kt app/src/test/java/com/habitdiary/app/ui/MainViewModelTest.kt
git commit -m "feat: add MainViewModel with month navigation and habit editing"
```

---

### Task 7: 앱 껍데기 + 메인 화면 (카드 목록, 월 이동, 칸 탭)

**Files:**
- Create: `app/src/main/java/com/habitdiary/app/HabitApp.kt`, `ui/Theme.kt`, `ui/HabitCard.kt`, `ui/MainScreen.kt`, `ui/HabitDiaryApp.kt`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Modify: `app/src/main/java/com/habitdiary/app/MainActivity.kt` (전체 교체), `app/src/main/AndroidManifest.xml` (`<application>` 속성)

**Interfaces:**
- Consumes: `MainViewModel`, `MainUiState`, `HabitRow` (Task 6), `CardRenderer`, `CardFonts`, `CardContent`, `CardLayout` (Task 3·4), `MonthGrid` (Task 2), `RoomHabitStore`, `HabitDatabase`, `DataStoreSettings`, `WidgetBindings`, `settingsDataStore` (Task 5)
- Produces:
  - `class AppContainer(context: Context)` with `scope: CoroutineScope`, `store: HabitStore`, `settings: SettingsStore`, `bindings: WidgetBindings`, `renderer: CardRenderer` (lazy), private `changed()` (Task 10에서 위젯 갱신 연결)
  - `class HabitApp : Application` with `container: AppContainer`; 확장 프로퍼티 `val Context.container: AppContainer`
  - `ui/Theme.kt`: `val Ink: Color`, `val PageColor: Color`, `val DiaryFont: FontFamily`
  - `@Composable fun HabitCard(row: HabitRow, month: YearMonth, today: LocalDate, style: CardStyle, renderer: CardRenderer, onDayTap: (Int) -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit, onAdd: () -> Unit, onSettings: () -> Unit)`
  - `@Composable fun MainScreen(...)` (Task 8에서 시그니처 확장)
  - `@Composable fun HabitDiaryApp(vm: MainViewModel, renderer: CardRenderer)`

- [ ] **Step 1: AppContainer / HabitApp**

`HabitApp.kt`:
```kotlin
package com.habitdiary.app

import android.app.Application
import android.content.Context
import com.habitdiary.app.data.DataStoreSettings
import com.habitdiary.app.data.HabitDatabase
import com.habitdiary.app.data.HabitStore
import com.habitdiary.app.data.RoomHabitStore
import com.habitdiary.app.data.SettingsStore
import com.habitdiary.app.data.WidgetBindings
import com.habitdiary.app.data.settingsDataStore
import com.habitdiary.app.render.CardFonts
import com.habitdiary.app.render.CardRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val app = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val db = HabitDatabase.build(app)
    val store: HabitStore = RoomHabitStore(db.habitDao(), ::changed)
    val settings: SettingsStore = DataStoreSettings(app.settingsDataStore, ::changed)
    val bindings = WidgetBindings(app.settingsDataStore)
    val renderer: CardRenderer by lazy { CardRenderer(CardFonts.load(app)) }

    /** 데이터·설정이 바뀔 때 호출된다. 위젯 갱신은 Task 10에서 연결한다. */
    private fun changed() {}
}

class HabitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as HabitApp).container
```

- [ ] **Step 2: Theme, HabitCard, MonthHeader, MainScreen, HabitDiaryApp**

`ui/Theme.kt`:
```kotlin
package com.habitdiary.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.habitdiary.app.R
import com.habitdiary.app.style.Palette

val Ink = Color(Palette.INK)
val PageColor = Color(Palette.PAGE)
val PaperColor = Color(Palette.PAPER)
val DiaryFont = FontFamily(Font(R.font.gamja_flower))
```

`ui/HabitCard.kt`:
```kotlin
package com.habitdiary.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.habitdiary.app.calendar.MonthGrid
import com.habitdiary.app.render.CardContent
import com.habitdiary.app.render.CardLayout
import com.habitdiary.app.render.CardRenderer
import com.habitdiary.app.style.CardStyle
import java.time.LocalDate
import java.time.YearMonth

/** CardRenderer로 그린 카드. 탭 위치를 CardLayout.dayAt으로 날짜에 매핑한다. */
@Composable
fun HabitCard(
    row: HabitRow,
    month: YearMonth,
    today: LocalDate,
    style: CardStyle,
    renderer: CardRenderer,
    onDayTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = remember(style, month) { CardLayout.compute(style, MonthGrid.of(month)) }
    val content = CardContent(row.habit.id, row.habit.name, row.habit.emoji, row.habit.colorIndex, row.doneDays, today)
    val tap by rememberUpdatedState(onDayTap)
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(layout.width / layout.height)
            .pointerInput(layout) {
                detectTapGestures { pos ->
                    val s = size.width / layout.width
                    layout.dayAt(pos.x / s, pos.y / s)?.let { tap(it) }
                }
            },
    ) {
        val s = size.width / layout.width
        drawIntoCanvas { c ->
            val n = c.nativeCanvas
            n.save()
            n.scale(s, s)
            renderer.draw(n, layout, content)
            n.restore()
        }
    }
}
```

`ui/MainScreen.kt` (Task 7 버전 — Task 8에서 전체 교체):
```kotlin
package com.habitdiary.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitdiary.app.render.CardRenderer
import java.time.YearMonth

@Composable
fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit, onAdd: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)) {
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
            HeaderButton("‹", onPrev)
            Text(
                "${month.year % 100}년 ${month.monthValue}월",
                fontFamily = DiaryFont, fontSize = 30.sp, color = Ink,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            HeaderButton("›", onNext)
        }
        Row(Modifier.align(Alignment.CenterEnd)) {
            HeaderButton("＋", onAdd)
            HeaderButton("⚙", onSettings)
        }
    }
}

@Composable
private fun HeaderButton(label: String, onClick: () -> Unit) {
    Text(label, fontSize = 26.sp, color = Ink, modifier = Modifier.clickable(onClick = onClick).padding(8.dp))
}

@Composable
fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("＋를 눌러 첫 습관을 만들어요", fontFamily = DiaryFont, fontSize = 22.sp, color = Ink, textAlign = TextAlign.Center)
    }
}

@Composable
fun MainScreen(
    state: MainUiState,
    renderer: CardRenderer,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDayTap: (habitId: Long, day: Int) -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(PageColor).safeDrawingPadding()) {
        MonthHeader(state.month, onPrev, onNext, onAdd, onSettings)
        if (state.loaded && state.rows.isEmpty()) {
            EmptyState()
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.rows, key = { it.habit.id }) { row ->
                HabitCard(row, state.month, state.today, state.style, renderer, onDayTap = { d -> onDayTap(row.habit.id, d) })
            }
        }
    }
}
```

`ui/HabitDiaryApp.kt` (Task 7 버전):
```kotlin
package com.habitdiary.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habitdiary.app.render.CardRenderer

@Composable
fun HabitDiaryApp(vm: MainViewModel, renderer: CardRenderer) {
    val state by vm.state.collectAsStateWithLifecycle()
    MainScreen(
        state = state,
        renderer = renderer,
        onPrev = vm::previousMonth,
        onNext = vm::nextMonth,
        onDayTap = vm::onDayTapped,
        onAdd = {},
        onSettings = {},
    )
}
```

`MainActivity.kt` (전체 교체):
```kotlin
package com.habitdiary.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.habitdiary.app.ui.HabitDiaryApp
import com.habitdiary.app.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels {
        viewModelFactory { initializer { MainViewModel(container.store, container.settings) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { HabitDiaryApp(vm, container.renderer) }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshToday()
    }
}
```

- [ ] **Step 3: 앱 아이콘 + Manifest**

`res/drawable/ic_launcher_foreground.xml` (동그라미 3×3 격자, 일부 칸 채움):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFB38A" android:pathData="M40,40m-6,0a6,6 0,1 1,12 0a6,6 0,1 1,-12 0"/>
    <path android:fillColor="#FFB38A" android:pathData="M54,40m-6,0a6,6 0,1 1,12 0a6,6 0,1 1,-12 0"/>
    <path android:fillColor="#F3EBE3" android:pathData="M68,40m-6,0a6,6 0,1 1,12 0a6,6 0,1 1,-12 0"/>
    <path android:fillColor="#F3EBE3" android:pathData="M40,54m-6,0a6,6 0,1 1,12 0a6,6 0,1 1,-12 0"/>
    <path android:fillColor="#9CCFA8" android:pathData="M54,54m-6,0a6,6 0,1 1,12 0a6,6 0,1 1,-12 0"/>
    <path android:fillColor="#9CCFA8" android:pathData="M68,54m-6,0a6,6 0,1 1,12 0a6,6 0,1 1,-12 0"/>
    <path android:fillColor="#A9C4F5" android:pathData="M40,68m-6,0a6,6 0,1 1,12 0a6,6 0,1 1,-12 0"/>
    <path android:fillColor="#F3EBE3" android:pathData="M54,68m-6,0a6,6 0,1 1,12 0a6,6 0,1 1,-12 0"/>
    <path android:fillColor="#A9C4F5" android:pathData="M68,68m-6,0a6,6 0,1 1,12 0a6,6 0,1 1,-12 0"/>
</vector>
```

`res/mipmap-anydpi-v26/ic_launcher.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/launcher_bg" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

`res/values/strings.xml`에 색 리소스를 같이 두지 말고 `res/values/colors.xml` 생성:
```xml
<resources>
    <color name="launcher_bg">#FFF8F0</color>
</resources>
```

`AndroidManifest.xml`의 `<application>` 태그를 다음으로 교체:
```xml
    <application
        android:name=".HabitApp"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
```

- [ ] **Step 4: 전체 테스트 + 빌드**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL` (기존 테스트 전부 통과). 습관 추가 UI는 Task 8에서 생기므로 실기기 확인은 Task 8에서 한다.

- [ ] **Step 5: Commit**

```bash
git add app/src/main
git commit -m "feat: main screen with month header and tappable habit cards"
```

---

### Task 8: 습관 편집 시트 + 삭제 + 순서 변경 + 첫 실기기 확인

**Files:**
- Create: `app/src/main/java/com/habitdiary/app/ui/Emoji.kt`, `ui/HabitEditSheet.kt`
- Modify: `ui/MainScreen.kt` (`MainScreen` 함수 전체 교체 — `MonthHeader`, `HeaderButton`, `EmptyState`는 유지), `ui/HabitDiaryApp.kt` (전체 교체)
- Test: `app/src/test/java/com/habitdiary/app/ui/EmojiTest.kt`

**Interfaces:**
- Consumes: `MainViewModel.saveHabit/deleteHabit/reorder` (Task 6), `Habit` (Task 5), `Palette` (Task 3), `HabitCard`, `MonthHeader`, `EmptyState`, `DiaryFont`, `Ink`, `PaperColor` (Task 7)
- Produces:
  - `fun lastGrapheme(text: String): String`
  - `sealed interface EditTarget { data object New; data class Existing(val habit: Habit) }`
  - `@Composable fun HabitEditSheet(target: EditTarget, onSave: (name: String, emoji: String, colorIndex: Int) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit)`
  - `MainScreen(state, renderer, onPrev, onNext, onDayTap, onAdd, onSettings, onEdit: (Habit) -> Unit, onReorder: (List<Long>) -> Unit)`

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
package com.habitdiary.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmojiTest {
    @Test
    fun keepsOnlyLastTypedGrapheme() {
        assertEquals("", lastGrapheme(""))
        assertEquals("🌙", lastGrapheme("🌙"))
        assertEquals("📖", lastGrapheme("🌙📖"))
        assertEquals("🌙", lastGrapheme("a🌙"))
        assertEquals("👍🏽", lastGrapheme("🌙👍🏽"))          // 피부색 수식자 포함 한 글자
        assertEquals("👨‍👩‍👧", lastGrapheme("🌙👨‍👩‍👧"))     // ZWJ 가족 이모지 한 글자
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest --tests "com.habitdiary.app.ui.EmojiTest"`
Expected: 컴파일 실패 (`Unresolved reference: lastGrapheme`)

- [ ] **Step 3: Emoji.kt 구현 후 통과 확인**

```kotlin
package com.habitdiary.app.ui

import android.icu.text.BreakIterator

/** 마지막 글자(그래핌) 하나만 남긴다. 이모지 칸에 새 이모지를 치면 이전 것을 대체하게 하기 위함. */
fun lastGrapheme(text: String): String {
    if (text.isEmpty()) return ""
    val it = BreakIterator.getCharacterInstance()
    it.setText(text)
    val end = it.last()
    val start = it.previous()
    return if (start == BreakIterator.DONE) text else text.substring(start, end)
}
```

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest --tests "com.habitdiary.app.ui.EmojiTest"`
Expected: `BUILD SUCCESSFUL`, 1 test passed

- [ ] **Step 4: HabitEditSheet**

```kotlin
package com.habitdiary.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitdiary.app.data.Habit
import com.habitdiary.app.style.Palette

sealed interface EditTarget {
    data object New : EditTarget
    data class Existing(val habit: Habit) : EditTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditSheet(
    target: EditTarget,
    onSave: (name: String, emoji: String, colorIndex: Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val existing = (target as? EditTarget.Existing)?.habit
    var name by remember(target) { mutableStateOf(existing?.name ?: "") }
    var emoji by remember(target) { mutableStateOf(existing?.emoji ?: "") }
    var colorIndex by remember(target) { mutableIntStateOf(existing?.colorIndex ?: 0) }
    var confirmDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PaperColor) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(if (existing == null) "새 습관" else "습관 수정", fontFamily = DiaryFont, fontSize = 26.sp, color = Ink)
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("이름") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = emoji, onValueChange = { emoji = lastGrapheme(it) },
                label = { Text("이모지 (비우면 ${MainViewModel.DEFAULT_EMOJI})") }, singleLine = true,
                modifier = Modifier.width(200.dp),
            )
            ColorPicker(colorIndex) { colorIndex = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (existing != null) {
                    OutlinedButton(onClick = { confirmDelete = true }) { Text("삭제") }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onSave(name.trim(), emoji, colorIndex) }, enabled = name.isNotBlank()) { Text("저장") }
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("습관 삭제") },
            text = { Text("‘${existing.name}’ 습관과 모든 기록을 지울까요?") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("삭제") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Palette.habitColors.forEachIndexed { i, c ->
            Box(
                Modifier
                    .size(34.dp)
                    .border(if (i == selected) 3.dp else 0.dp, if (i == selected) Ink else Color.Transparent, CircleShape)
                    .padding(4.dp)
                    .background(Color(c), CircleShape)
                    .clickable { onSelect(i) },
            )
        }
    }
}
```

- [ ] **Step 5: MainScreen 교체 (순서 변경 포함)**

`ui/MainScreen.kt`의 `MainScreen` 함수를 아래로 교체하고, 파일 상단 import에 아래 항목을 추가한다 (`MonthHeader`, `HeaderButton`, `EmptyState`는 그대로 둔다):
```kotlin
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import com.habitdiary.app.data.Habit
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
```

```kotlin
@Composable
fun MainScreen(
    state: MainUiState,
    renderer: CardRenderer,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDayTap: (habitId: Long, day: Int) -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    onEdit: (Habit) -> Unit,
    onReorder: (List<Long>) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(PageColor).safeDrawingPadding()) {
        MonthHeader(state.month, onPrev, onNext, onAdd, onSettings)
        if (state.loaded && state.rows.isEmpty()) {
            EmptyState()
            return@Column
        }
        // 드래그 중에는 로컬 목록을 움직이고, 손을 떼면 한 번에 저장한다.
        var rows by remember(state.rows) { mutableStateOf(state.rows) }
        var moved by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            rows = rows.toMutableList().apply { add(to.index, removeAt(from.index)) }
            moved = true
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(rows, key = { it.habit.id }) { row ->
                ReorderableItem(reorderState, key = row.habit.id) { isDragging ->
                    HabitCard(
                        row, state.month, state.today, state.style, renderer,
                        onDayTap = { d -> onDayTap(row.habit.id, d) },
                        modifier = Modifier
                            .graphicsLayer {
                                val s = if (isDragging) 1.03f else 1f
                                scaleX = s
                                scaleY = s
                            }
                            .longPressDraggableHandle(
                                onDragStarted = { moved = false },
                                // 길게 누르고 움직이지 않았으면 편집, 움직였으면 새 순서 저장
                                onDragStopped = { if (moved) onReorder(rows.map { it.habit.id }) else onEdit(row.habit) },
                            ),
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 6: HabitDiaryApp 교체**

```kotlin
package com.habitdiary.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habitdiary.app.render.CardRenderer

@Composable
fun HabitDiaryApp(vm: MainViewModel, renderer: CardRenderer) {
    val state by vm.state.collectAsStateWithLifecycle()
    var editTarget by remember { mutableStateOf<EditTarget?>(null) }

    MainScreen(
        state = state,
        renderer = renderer,
        onPrev = vm::previousMonth,
        onNext = vm::nextMonth,
        onDayTap = vm::onDayTapped,
        onAdd = { editTarget = EditTarget.New },
        onSettings = {},
        onEdit = { editTarget = EditTarget.Existing(it) },
        onReorder = vm::reorder,
    )

    editTarget?.let { target ->
        val existingId = (target as? EditTarget.Existing)?.habit?.id
        HabitEditSheet(
            target = target,
            onSave = { name, emoji, color -> vm.saveHabit(existingId, name, emoji, color); editTarget = null },
            onDelete = { existingId?.let(vm::deleteHabit); editTarget = null },
            onDismiss = { editTarget = null },
        )
    }
}
```

- [ ] **Step 7: 테스트 + 빌드**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: 실기기 확인 (사용자 협조 필요)**

사용자에게 요청: 폰에서 개발자 옵션 → USB 디버깅 켜고 USB로 PC 연결, 폰에 뜨는 "USB 디버깅 허용" 수락. 그리고 디버그 APK 설치 승인을 받는다.
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.habitdiary.app/.MainActivity
```
확인 항목(사용자가 폰에서 하거나, 화면 캡처로 확인):
1. 빈 상태 문구 표시
2. ＋ → 이름/이모지/색 입력 → 저장 → 카드 등장 (손글씨 폰트, 시안 C 모양)
3. 지난 날/오늘 칸 탭 → 채워짐/비워짐, 미래 칸 탭 무반응
4. ‹ › 로 월 이동, 지난 달 칸도 수정 가능
5. 카드 길게 누르고 그대로 떼기 → 편집 시트, 길게 누르고 끌기 → 순서 변경 후 유지
6. 편집 시트에서 삭제 → 확인 → 카드 사라짐

화면 캡처:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" exec-out screencap -p > build\shot-main.png
```
`build\shot-main.png`를 Read 도구로 열어 레이아웃을 확인한다. 칸 탭과 길게 누르기 제스처가 서로 간섭하면(예: 길게 누를 때 칸이 토글됨) `HabitCard`의 `detectTapGestures`를 `onTap`만 쓰도록 유지한 채 원인을 조사해 고친다.

- [ ] **Step 9: Commit**

```bash
git add app/src/main app/src/test
git commit -m "feat: habit edit sheet, delete confirmation, and drag reorder"
```

---

### Task 9: 설정 화면 (카드 스타일 선택)

**Files:**
- Create: `app/src/main/java/com/habitdiary/app/ui/SettingsScreen.kt`
- Modify: `ui/HabitDiaryApp.kt` (전체 교체)

**Interfaces:**
- Consumes: `MainViewModel.setStyle`, `MainUiState.style/today` (Task 6), `HabitCard`, `HabitRow` (Task 6·7), `CardStyle`, `Palette` (Task 3)
- Produces: `@Composable fun SettingsScreen(current: CardStyle, renderer: CardRenderer, today: LocalDate, onSelect: (CardStyle) -> Unit, onBack: () -> Unit)`

- [ ] **Step 1: SettingsScreen**

```kotlin
package com.habitdiary.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.habitdiary.app.data.Habit
import com.habitdiary.app.render.CardRenderer
import com.habitdiary.app.style.CardStyle
import com.habitdiary.app.style.Palette
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun SettingsScreen(
    current: CardStyle,
    renderer: CardRenderer,
    today: LocalDate,
    onSelect: (CardStyle) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        Modifier
            .fillMaxSize()
            .background(PageColor)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 28.sp, color = Ink, modifier = Modifier.clickable(onClick = onBack).padding(8.dp))
            Text("설정", fontFamily = DiaryFont, fontSize = 30.sp, color = Ink)
        }
        Text("카드 스타일", fontFamily = DiaryFont, fontSize = 22.sp, color = Ink)
        StyleOption("C · 인덱스 탭", CardStyle.INDEX_TAB, current, renderer, today, onSelect)
        StyleOption("A · 마스킹테이프", CardStyle.MASKING_TAPE, current, renderer, today, onSelect)
    }
}

@Composable
private fun StyleOption(
    label: String,
    style: CardStyle,
    current: CardStyle,
    renderer: CardRenderer,
    today: LocalDate,
    onSelect: (CardStyle) -> Unit,
) {
    val selected = style == current
    val shape = RoundedCornerShape(16.dp)
    val sample = HabitRow(
        Habit(id = 1, name = "1시30분전 수면", emoji = "🌙", colorIndex = 0, sortOrder = 0),
        setOf(1, 2, 3, 5, 6, 7, 10).filter { it < today.dayOfMonth }.toSet(),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(if (selected) 3.dp else 1.dp, if (selected) Color(Palette.habitColor(0)) else Color(0x22000000), shape)
            .clickable { onSelect(style) }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text((if (selected) "✔ " else "") + label, fontFamily = DiaryFont, fontSize = 20.sp, color = Ink)
        HabitCard(sample, YearMonth.from(today), today, style, renderer, onDayTap = {})
    }
}
```

- [ ] **Step 2: HabitDiaryApp 교체 (설정 화면 전환)**

```kotlin
package com.habitdiary.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habitdiary.app.render.CardRenderer

@Composable
fun HabitDiaryApp(vm: MainViewModel, renderer: CardRenderer) {
    val state by vm.state.collectAsStateWithLifecycle()
    var editTarget by remember { mutableStateOf<EditTarget?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            current = state.style,
            renderer = renderer,
            today = state.today,
            onSelect = vm::setStyle,
            onBack = { showSettings = false },
        )
        return
    }

    MainScreen(
        state = state,
        renderer = renderer,
        onPrev = vm::previousMonth,
        onNext = vm::nextMonth,
        onDayTap = vm::onDayTapped,
        onAdd = { editTarget = EditTarget.New },
        onSettings = { showSettings = true },
        onEdit = { editTarget = EditTarget.Existing(it) },
        onReorder = vm::reorder,
    )

    editTarget?.let { target ->
        val existingId = (target as? EditTarget.Existing)?.habit?.id
        HabitEditSheet(
            target = target,
            onSave = { name, emoji, color -> vm.saveHabit(existingId, name, emoji, color); editTarget = null },
            onDelete = { existingId?.let(vm::deleteHabit); editTarget = null },
            onDismiss = { editTarget = null },
        )
    }
}
```

- [ ] **Step 3: 테스트 + 빌드 + 실기기 확인**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`

폰이 연결돼 있으면 `adb install -r app\build\outputs\apk\debug\app-debug.apk` 후 확인: ⚙ → 두 스타일 미리보기 → A 선택 → 뒤로 → 메인 카드가 마스킹테이프 모양, 앱 재시작 후에도 유지. 캡처: `adb exec-out screencap -p > build\shot-settings.png`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main
git commit -m "feat: settings screen with card style picker"
```

---

### Task 10: 홈 화면 위젯

**Files:**
- Create: `app/src/main/java/com/habitdiary/app/widget/WidgetSizing.kt`, `widget/MidnightScheduler.kt`, `widget/WidgetUpdater.kt`, `widget/HabitWidgetProvider.kt` (`HabitWidgetProvider` + `MidnightReceiver`), `widget/WidgetConfigActivity.kt`
- Create: `app/src/main/res/layout/widget_card.xml`, `res/xml/habit_widget_info.xml`, `res/drawable/widget_message_bg.xml`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/AndroidManifest.xml` (전체 교체), `app/src/main/java/com/habitdiary/app/HabitApp.kt` (`AppContainer`에 `widgetUpdater` 추가, `changed()` 구현)
- Test: `app/src/test/java/com/habitdiary/app/widget/WidgetSizingTest.kt`, `app/src/test/java/com/habitdiary/app/widget/MidnightSchedulerTest.kt`

**Interfaces:**
- Consumes: `HabitStore.getHabit/doneDays/observeHabits`, `SettingsStore.cardStyle`, `WidgetBindings` (Task 5), `CardLayout`, `CardRenderer.renderBitmap`, `CardContent` (Task 3·4), `MonthGrid` (Task 2), `AppContainer`, `Context.container`, `MainActivity` (Task 7), `DiaryFont`, `Ink`, `PageColor` (Task 7), `Palette` (Task 3)
- Produces:
  - `data class BitmapSize(val width: Int, val height: Int)`; `WidgetSizing.bitmapSize(boxWidthDp: Int, boxHeightDp: Int, density: Float, layoutWidth: Float, layoutHeight: Float, maxPixels: Int = 1_000_000): BitmapSize`
  - `MidnightScheduler.nextTriggerMillis(now: ZonedDateTime): Long`, `MidnightScheduler.schedule(context: Context)`, `class MidnightReceiver : BroadcastReceiver` (HabitWidgetProvider.kt 안)
  - `class WidgetUpdater(context, store, settings, bindings, renderer: () -> CardRenderer, clock: () -> LocalDate = { LocalDate.now() })` with `suspend updateAll()`, `suspend update(appWidgetId: Int)`
  - `AppContainer.widgetUpdater: WidgetUpdater`

- [ ] **Step 1: 실패하는 테스트 작성**

`WidgetSizingTest.kt`:
```kotlin
package com.habitdiary.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSizingTest {
    @Test
    fun exactFit_atDensity1() {
        assertEquals(BitmapSize(320, 227), WidgetSizing.bitmapSize(320, 227, 1f, 320f, 227f))
    }

    @Test
    fun widerBox_isLimitedByHeight_keepingAspect() {
        assertEquals(BitmapSize(320, 227), WidgetSizing.bitmapSize(640, 227, 1f, 320f, 227f))
    }

    @Test
    fun density_scalesPixels() {
        assertEquals(BitmapSize(960, 681), WidgetSizing.bitmapSize(320, 227, 3f, 320f, 227f))
    }

    @Test
    fun hugeBox_isCappedByPixelBudget_keepingAspect() {
        val s = WidgetSizing.bitmapSize(640, 454, 3f, 320f, 227f)
        assertTrue(s.width * s.height <= 1_000_000)
        assertEquals(320f / 227f, s.width.toFloat() / s.height, 0.01f)
        assertTrue(s.width > 1000)
    }
}
```

`MidnightSchedulerTest.kt`:
```kotlin
package com.habitdiary.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class MidnightSchedulerTest {
    @Test
    fun triggersFiveSecondsAfterNextMidnight() {
        val zone = ZoneId.of("Asia/Seoul")
        val now = ZonedDateTime.of(2026, 10, 15, 23, 59, 0, 0, zone)
        val expected = ZonedDateTime.of(2026, 10, 16, 0, 0, 5, 0, zone).toInstant().toEpochMilli()
        assertEquals(expected, MidnightScheduler.nextTriggerMillis(now))
    }

    @Test
    fun justAfterMidnight_targetsFollowingMidnight() {
        val zone = ZoneId.of("Asia/Seoul")
        val now = ZonedDateTime.of(2026, 10, 16, 0, 0, 10, 0, zone)
        val expected = ZonedDateTime.of(2026, 10, 17, 0, 0, 5, 0, zone).toInstant().toEpochMilli()
        assertEquals(expected, MidnightScheduler.nextTriggerMillis(now))
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest --tests "com.habitdiary.app.widget.*"`
Expected: 컴파일 실패 (`Unresolved reference`)

- [ ] **Step 3: WidgetSizing + MidnightScheduler 구현 후 통과 확인**

`widget/WidgetSizing.kt`:
```kotlin
package com.habitdiary.app.widget

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class BitmapSize(val width: Int, val height: Int)

object WidgetSizing {
    /** 위젯 영역(dp)에 카드 비율을 유지하며 꽉 맞는 비트맵 크기. RemoteViews 비트맵 메모리 한도 때문에 픽셀 수를 제한한다. */
    fun bitmapSize(
        boxWidthDp: Int,
        boxHeightDp: Int,
        density: Float,
        layoutWidth: Float,
        layoutHeight: Float,
        maxPixels: Int = 1_000_000,
    ): BitmapSize {
        var s = min(boxWidthDp * density / layoutWidth, boxHeightDp * density / layoutHeight)
        val pixels = layoutWidth * s * layoutHeight * s
        if (pixels > maxPixels) s *= sqrt(maxPixels / pixels)
        return BitmapSize(max(1, (layoutWidth * s).toInt()), max(1, (layoutHeight * s).toInt()))
    }
}
```
(내림(`toInt`)이라 상한 적용 시 픽셀 수가 한도를 넘지 않는다.)

`widget/MidnightScheduler.kt`:
```kotlin
package com.habitdiary.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.ZonedDateTime

object MidnightScheduler {
    fun nextTriggerMillis(now: ZonedDateTime): Long =
        now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusSeconds(5).toInstant().toEpochMilli()

    /** 다음 자정 직후 위젯을 다시 그리도록 예약 (정확한 알람 권한 없이, 몇 분 늦을 수 있음). */
    fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 0, Intent(context, MidnightReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        am.set(AlarmManager.RTC, nextTriggerMillis(ZonedDateTime.now()), pi)
    }
}
```
Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest --tests "com.habitdiary.app.widget.*"`
Expected: `BUILD SUCCESSFUL`, 6 tests passed

- [ ] **Step 4: 위젯 리소스 + WidgetUpdater + Provider + 구성 Activity**

`res/values/strings.xml` (전체 교체):
```xml
<resources>
    <string name="app_name">습관 다이어리</string>
    <string name="widget_desc">습관 하나의 이번 달 기록</string>
    <string name="widget_missing">습관이 삭제됐어요\n탭해서 다시 고르기</string>
</resources>
```

`res/drawable/widget_message_bg.xml`:
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#FFFDF9" />
    <corners android:radius="18dp" />
    <stroke android:width="2dp" android:color="#EAD9C9" />
</shape>
```

`res/layout/widget_card.xml`:
```xml
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <ImageView
        android:id="@+id/widget_image"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:contentDescription="@string/widget_desc"
        android:scaleType="fitCenter" />
    <TextView
        android:id="@+id/widget_message"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@drawable/widget_message_bg"
        android:gravity="center"
        android:padding="12dp"
        android:textColor="#5B4636"
        android:textSize="14sp"
        android:visibility="gone" />
</FrameLayout>
```

`res/xml/habit_widget_info.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:configure="com.habitdiary.app.widget.WidgetConfigActivity"
    android:description="@string/widget_desc"
    android:initialLayout="@layout/widget_card"
    android:minWidth="180dp"
    android:minHeight="110dp"
    android:minResizeWidth="180dp"
    android:minResizeHeight="110dp"
    android:resizeMode="horizontal|vertical"
    android:targetCellWidth="3"
    android:targetCellHeight="2"
    android:updatePeriodMillis="0"
    android:widgetCategory="home_screen"
    android:widgetFeatures="reconfigurable" />
```

`widget/WidgetUpdater.kt`:
```kotlin
package com.habitdiary.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.habitdiary.app.MainActivity
import com.habitdiary.app.R
import com.habitdiary.app.calendar.MonthGrid
import com.habitdiary.app.data.HabitStore
import com.habitdiary.app.data.SettingsStore
import com.habitdiary.app.data.WidgetBindings
import com.habitdiary.app.render.CardContent
import com.habitdiary.app.render.CardLayout
import com.habitdiary.app.render.CardRenderer
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth

class WidgetUpdater(
    private val context: Context,
    private val store: HabitStore,
    private val settings: SettingsStore,
    private val bindings: WidgetBindings,
    private val renderer: () -> CardRenderer,
    private val clock: () -> LocalDate = { LocalDate.now() },
) {
    private val manager get() = AppWidgetManager.getInstance(context)

    suspend fun updateAll() {
        manager.getAppWidgetIds(ComponentName(context, HabitWidgetProvider::class.java)).forEach { update(it) }
        MidnightScheduler.schedule(context)
    }

    suspend fun update(appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_card)
        val habit = bindings.habitIdFor(appWidgetId)?.let { store.getHabit(it) }
        if (habit == null) {
            views.setViewVisibility(R.id.widget_image, View.GONE)
            views.setViewVisibility(R.id.widget_message, View.VISIBLE)
            views.setTextViewText(R.id.widget_message, context.getString(R.string.widget_missing))
            views.setOnClickPendingIntent(R.id.widget_root, configIntent(appWidgetId))
        } else {
            val today = clock()
            val month = YearMonth.from(today)
            val layout = CardLayout.compute(settings.cardStyle.first(), MonthGrid.of(month))
            val opts = manager.getAppWidgetOptions(appWidgetId)
            val wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: 250
            val hDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 } ?: 180
            val size = WidgetSizing.bitmapSize(wDp, hDp, context.resources.displayMetrics.density, layout.width, layout.height)
            val content = CardContent(habit.id, habit.name, habit.emoji, habit.colorIndex, store.doneDays(habit.id, month), today)
            views.setViewVisibility(R.id.widget_message, View.GONE)
            views.setViewVisibility(R.id.widget_image, View.VISIBLE)
            views.setImageViewBitmap(R.id.widget_image, renderer().renderBitmap(layout, content, size.width, size.height))
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent())
        }
        manager.updateAppWidget(appWidgetId, views)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun configIntent(appWidgetId: Int): PendingIntent = PendingIntent.getActivity(
        context, appWidgetId,
        Intent(context, WidgetConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
```

`widget/HabitWidgetProvider.kt`:
```kotlin
package com.habitdiary.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.habitdiary.app.container
import kotlinx.coroutines.launch

class HabitWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) = refresh(context)

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) =
        refresh(context)

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val c = context.container
        val pending = goAsync()
        c.scope.launch {
            try { appWidgetIds.forEach { c.bindings.unbind(it) } } finally { pending.finish() }
        }
    }

    private fun refresh(context: Context) {
        val c = context.container
        val pending = goAsync()
        c.scope.launch {
            try { c.widgetUpdater.updateAll() } finally { pending.finish() }
        }
    }
}

/** 자정 알람, 시간/시간대 변경 시 위젯 갱신. */
class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = context.container
        val pending = goAsync()
        c.scope.launch {
            try { c.widgetUpdater.updateAll() } finally { pending.finish() }
        }
    }
}
```

`widget/WidgetConfigActivity.kt`:
```kotlin
package com.habitdiary.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.habitdiary.app.MainActivity
import com.habitdiary.app.container
import com.habitdiary.app.data.Habit
import com.habitdiary.app.style.Palette
import com.habitdiary.app.ui.DiaryFont
import com.habitdiary.app.ui.Ink
import com.habitdiary.app.ui.PageColor
import com.habitdiary.app.ui.PaperColor
import kotlinx.coroutines.launch

/** 위젯을 놓을 때(또는 습관이 지워진 위젯을 탭할 때) 보여줄 습관을 고른다. */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, result)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val c = container
        enableEdgeToEdge()
        setContent {
            val habits by c.store.observeHabits().collectAsStateWithLifecycle(initialValue = null)
            HabitPicker(
                habits = habits,
                onPick = { habit ->
                    lifecycleScope.launch {
                        c.bindings.bind(appWidgetId, habit.id)
                        c.widgetUpdater.update(appWidgetId)
                        setResult(RESULT_OK, result)
                        finish()
                    }
                },
                onOpenApp = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                },
            )
        }
    }
}

@Composable
private fun HabitPicker(habits: List<Habit>?, onPick: (Habit) -> Unit, onOpenApp: () -> Unit) {
    Column(Modifier.fillMaxSize().background(PageColor).safeDrawingPadding().padding(16.dp)) {
        Text("위젯에 보여줄 습관", fontFamily = DiaryFont, fontSize = 28.sp, color = Ink, modifier = Modifier.padding(bottom = 16.dp))
        when {
            habits == null -> Unit
            habits.isEmpty() -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("먼저 앱에서 습관을 만들어 주세요", fontFamily = DiaryFont, fontSize = 20.sp, color = Ink)
                Button(onClick = onOpenApp, modifier = Modifier.padding(top = 16.dp)) { Text("앱 열기") }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(habits, key = { it.id }) { habit ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(PaperColor, RoundedCornerShape(14.dp))
                            .clickable { onPick(habit) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(14.dp).background(Color(Palette.habitColor(habit.colorIndex)), CircleShape))
                        Text(habit.emoji, fontSize = 24.sp)
                        Text(habit.name, fontFamily = DiaryFont, fontSize = 22.sp, color = Ink)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 5: AppContainer 연결 + Manifest 교체**

`HabitApp.kt`의 `AppContainer`에서 `changed()`를 아래로 바꾸고 `widgetUpdater`를 추가한다 (import `com.habitdiary.app.widget.WidgetUpdater`, `kotlinx.coroutines.launch` 추가):
```kotlin
    val widgetUpdater: WidgetUpdater by lazy { WidgetUpdater(app, store, settings, bindings, { renderer }) }

    /** 데이터·설정이 바뀌면 모든 위젯을 다시 그린다. */
    private fun changed() {
        scope.launch { widgetUpdater.updateAll() }
    }
```

`AndroidManifest.xml` (전체 교체):
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name=".HabitApp"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".widget.WidgetConfigActivity"
            android:exported="true"
            android:excludeFromRecents="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".widget.HabitWidgetProvider"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/habit_widget_info" />
        </receiver>

        <receiver
            android:name=".widget.MidnightReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.TIME_SET" />
                <action android:name="android.intent.action.TIMEZONE_CHANGED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

- [ ] **Step 6: 전체 테스트 + 빌드**

Run: `$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL` (Step 3의 위젯 테스트 6개 포함 전부 통과)

- [ ] **Step 7: 실기기 확인**

`adb install -r app\build\outputs\apk\debug\app-debug.apk` (사용자 승인 후). 사용자에게 다음을 요청하거나 함께 확인:
1. 홈 화면 길게 누르기 → 위젯 → 습관 다이어리 → 놓기 → 습관 고르기 화면 → 선택 → 카드가 위젯에 보임
2. 위젯 두 개를 놓고 각각 다른 습관 선택
3. 위젯 크기 늘리기/줄이기 → 최소 크기 아래로는 안 줄어들고, 늘리면 카드가 커짐 (31칸 다 보임)
4. 위젯 탭 → 앱 열림 → 칸 체크 → 홈으로 → 위젯에 반영
5. 설정에서 스타일 바꾸기 → 위젯도 바뀜
6. 위젯에 연결된 습관 삭제 → 위젯에 "습관이 삭제됐어요" → 탭 → 습관 고르기
7. 자정 넘김: `adb shell cmd alarm` 대신 폰 날짜를 하루 뒤로 바꾸고(TIME_SET 브로드캐스트) 위젯의 오늘 링이 다음 날로 이동하는지 확인 후 자동 날짜 설정으로 되돌린다 — 날짜 변경은 사용자가 직접 한다.

홈 화면 캡처: `adb exec-out screencap -p > build\shot-widget.png` → Read로 확인. 최소 크기에서 격자가 너무 작으면 `habit_widget_info.xml`의 `minWidth/minHeight/minResize*` 값을 키우고 다시 확인한다.

- [ ] **Step 8: Commit**

```bash
git add app/src/main app/src/test
git commit -m "feat: home screen widget showing one habit's month grid"
```

---

### Task 11: 서명된 release APK

**Files:**
- Modify: `app/build.gradle.kts` (서명 설정 추가)
- Create (gitignored): `keystore.properties`, 키스토어 `%USERPROFILE%\.android-keys\habitdiary.jks`

**Interfaces:**
- Consumes: 전체 앱 (Task 1–10)
- Produces: `app\build\outputs\apk\release\app-release.apk`, 복사본 `dist\habit-diary-1.0.apk`

- [ ] **Step 1: 키스토어 생성**

비밀번호는 무작위로 만들고 대화에 출력하지 않는다.
```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\.android-keys" | Out-Null
$pw = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 24 | ForEach-Object { [char]$_ })
& "$env:LOCALAPPDATA\Programs\jdk-17\bin\keytool.exe" -genkeypair -v -keystore "$env:USERPROFILE\.android-keys\habitdiary.jks" -alias habitdiary -keyalg RSA -keysize 2048 -validity 36500 -storepass $pw -keypass $pw -dname "CN=Habit Diary"
$store = ("$env:USERPROFILE\.android-keys\habitdiary.jks" -replace '\\', '/')
@"
storeFile=$store
storePassword=$pw
keyAlias=habitdiary
keyPassword=$pw
"@ | Set-Content -Encoding utf8 keystore.properties
git check-ignore keystore.properties
```
Expected: 마지막 줄 `keystore.properties` (git에서 제외됨 확인)

- [ ] **Step 2: 서명 설정**

`app/build.gradle.kts`의 **첫 줄**에 `import java.util.Properties`를 넣고, `plugins { }` 블록 바로 아래에 `keystoreProps`를 추가한다:
```kotlin
import java.util.Properties

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
```
`android { }` 블록 안 `buildTypes` 위에 추가:
```kotlin
    signingConfigs {
        create("release") {
            if (!keystoreProps.isEmpty) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
```
`buildTypes.release`를 다음으로 교체:
```kotlin
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
```

- [ ] **Step 3: 빌드 + 서명 검증**

```powershell
$env:JAVA_HOME="$env:LOCALAPPDATA\Programs\jdk-17"; .\gradlew.bat testDebugUnitTest assembleRelease
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk
New-Item -ItemType Directory -Force dist | Out-Null
Copy-Item app\build\outputs\apk\release\app-release.apk dist\habit-diary-1.0.apk
```
Expected: `BUILD SUCCESSFUL`, apksigner 출력에 `Signer #1 certificate DN: CN=Habit Diary`

- [ ] **Step 4: 폰에 설치 (사용자 승인 필요)**

디버그 APK와 서명이 달라 그대로 덮어쓸 수 없다. 사용자에게 "디버그 버전을 지우면 테스트로 넣은 습관 기록도 지워진다"고 알리고 승인받은 뒤:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" uninstall com.habitdiary.app
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install dist\habit-diary-1.0.apk
```
(USB 없이 하려면 `dist\habit-diary-1.0.apk`를 폰으로 옮겨 파일 앱에서 열어 설치 — "출처를 알 수 없는 앱 설치" 허용은 사용자가 직접 한다.)

사용자에게 알릴 것: `%USERPROFILE%\.android-keys\habitdiary.jks`와 `D:\claude\HabitTracker\keystore.properties`를 백업해 두라 — 잃어버리면 이후 업데이트를 덮어 설치할 수 없다.

- [ ] **Step 5: `.gitignore`에 dist 추가 + Commit**

```bash
echo "dist/" >> .gitignore
git add .gitignore app/build.gradle.kts
git commit -m "build: sign release APK"
```
