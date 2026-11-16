# Module Map

Status: VERIFIED

## 1. Overview & Build Architecture

neatGram is structured as an Android multi-module project where the bulk of the implementation lives in a core Android library module (`:TMessagesProj`), while several application packaging modules (`:TMessagesProj_App*`) consume it to assemble standalone or store-specific APKs.

## 2. Module Breakdown

| Module | Plugin Type | Namespace | Responsibility | Status |
|--------|-------------|-----------|----------------|--------|
| `:TMessagesProj` | `com.android.library` | `org.telegram.messenger` | Core client library: UI, MTProto network stack, SQLite storage, media handling, and CMake JNI targets (`tmessages.49`, `twidget`). | VERIFIED |
| `:TMessagesProj_App` | `com.android.application` | `org.telegram.messenger.regular` | Primary APK assembly module for standalone direct distribution and Google Play. Builds the universal multi-ABI APK via flavor `afat`. | VERIFIED |
| `:TMessagesProj_AppStandalone` | `com.android.application` | `org.telegram.messenger.web` | Standalone direct-download packaging module with `.web` package suffix and in-app update checks. | VERIFIED |
| `:TMessagesProj_AppHuawei` | `com.android.application` | `org.telegram.messenger.huawei` | Packaging module targeting Huawei AppGallery with HMS Push/Location integration. | VERIFIED |
| `:TMessagesProj_AppHockeyApp` | `com.android.application` | `org.telegram.messenger.regular` | Beta testing distribution module with HockeyApp/AppCenter integration (`.beta` suffix). | VERIFIED |
| `:TMessagesProj_AppTests` | `com.android.application` | `org.telegram.messenger.test` | Android instrumentation and integration test harness. | VERIFIED |
| `:jlatexmath` | `java-library` | N/A | Formula and LaTeX rendering engine (`TMessagesProj/lib/jlatexmath`). | VERIFIED |
| `:TMessagesProj_Modules:media3:*` | `com.android.library` | Varies | Media3 / ExoPlayer fork (HLS, DASH, FFmpeg audio decoder, session management). | VERIFIED |

## 3. Autonomous APK Assembly (`afat` / `TMessagesProj_App`)

The standalone universal APK (containing all native ABIs: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`) is produced by:
- **Module**: `:TMessagesProj_App`
- **Product Flavor**: `afat` (dimension `minApi`)
- **Build Types**:
  - `release`: Produces `app.apk` without package suffix (`applicationId = APP_PACKAGE`). Uses `TMessagesProj/config/release/AndroidManifest_SDK23.xml`.
  - `standalone`: Produces `app.apk` with `.web` suffix (`applicationId = APP_PACKAGE + ".web"`). Uses `TMessagesProj/config/release/AndroidManifest_standalone.xml`.
  - `debug`: Produces debug build with `.beta` suffix (`applicationId = APP_PACKAGE + ".beta"`). Uses `TMessagesProj/config/debug/AndroidManifest_SDK23.xml`.

The target Gradle assembly task for the autonomous release APK is:
```bash
./gradlew :TMessagesProj_App:assembleAfatRelease
```

## 4. Packaging, Namespace, and Isolation Rules

- **Java Namespace**: Kept strictly as `org.telegram.messenger` to maintain 100% upstream source compatibility and prevent breaking imports, JNI symbol exports, or reflection.
- **Application ID**: Defined in `gradle.properties` (`APP_PACKAGE=org.telegram.neatgram`) and consumed by all app modules as `defaultConfig.applicationId = APP_PACKAGE`.
- **ContentProvider Authorities**: Formed via `${applicationId}.provider`, `${applicationId}.notification_image_provider`, and `${applicationId}.call_sound_provider` to ensure zero collision (`INSTALL_FAILED_CONFLICTING_PROVIDER`) with official Telegram.
- **AccountManager / SyncAdapter**: Account type configured as `org.telegram.neatgram` in `auth.xml`, `sync_contacts.xml`, `auth_menu.xml`, and `ContactsController.java`.
