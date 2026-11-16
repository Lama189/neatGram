# Subsystem Map: Search Bar RAM Pill Widget

Status: VERIFIED

## 1. Purpose

Provides a native, non-intrusive memory status indicator (RAM Pill) placed directly inside the main chat list search bar (`FragmentSearchField` within `DialogsActivity`). It displays an equalizer-like load indicator icon alongside current device RAM usage (`used / total` in tabular digits) with zero background battery impact and interactive memory cache clearing.

## 2. Entry Points & Key Classes

- **`org.telegram.neatgram.ui.RamInfoPillView`**: Isolated custom `View` drawing the rounded capsule, 3-column equalizer icon, and tabular text. Handles lifecycle updates, click interaction, and cache eviction.
- **`org.telegram.neatgram.config.NeatGramConfig`**: Central configuration provider managing `ramPillEnabled` flag with SharedPreferences backing.
- **`org.telegram.ui.Components.FragmentSearchField`**: Search bar container in the chat list header, hosting `RamInfoPillView` and dynamically adjusting `editText` padding and clip bounds to prevent text collision.
- **`org.telegram.ui.DialogsActivity`**: Instantiates `RamInfoPillView`, wires it into `FragmentSearchField`, forwards fragment lifecycle events (`onResume`, `onPause`, `onFragmentDestroy`), and controls visibility during search expansion/collapse.

## 3. Key Methods & Signatures

- `RamInfoPillView`:
  - `updateColors()`: Dynamically calculates background, icon, and text colors based on `Theme.isCurrentThemeDark()` or `Theme.ResourcesProvider`.
  - `updateMemoryInfo()`: Queries `ActivityManager.getMemoryInfo(memoryInfo)` without allocations on the draw path.
  - `shouldRunUpdates()`: Enforces that periodic 3000ms updates run strictly when the view is attached, visible, unpaused, and enabled.
  - `handlePillClick()`: Invokes `ImageLoader.getInstance().clearMemory()`, queries process PSS via `Debug.getPss()` in background queue, and shows a two-line `BulletinFactory` popup.
- `FragmentSearchField`:
  - `setRamInfoPillView(RamInfoPillView)`: Inserts the pill view with 6dp margins.
  - `checkUi_editTextPaddings()`: Adjusts `pEnd` to account for `RamInfoPillView` width when visible.
- `DialogsActivity`:
  - `onSearchExpand()` / `onSearchCollapse()` / `showSearch(...)`: Controls hiding and restoring the pill when searching chats.

## 4. Execution Flow & Lifecycle Constraints

1. **Initialization**: On creation of `DialogsActivity` for `DIALOGS_TYPE_DEFAULT`, `RamInfoPillView` is created and bound to `fragmentSearchField`.
2. **Periodic Updates**: Runs every 3000ms via `AndroidUtilities.runOnUIThread`. The callback is automatically paused and cancelled when:
   - Window visibility changes to non-visible (`onWindowVisibilityChanged`).
   - The view is detached from window (`onDetachedFromWindow`).
   - Fragment pauses (`DialogsActivity.onPause`).
   - Search mode expands (`onSearchExpand`, `showSearch`).
3. **Cache Eviction**: Touching the pill consumes the touch event (preventing the search keyboard from opening), evicts memory cache via `ImageLoader.getInstance().clearMemory()`, recalculates memory info, and shows process PSS in a Telegram Bulletin.

## 5. Verification Status

- Symbol verification against Telegram upstream sources: VERIFIED.
- Lifecycle & concurrency safety: VERIFIED.
- Zero allocation in `onDraw`: VERIFIED.
