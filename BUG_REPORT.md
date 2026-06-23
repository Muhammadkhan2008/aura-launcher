# AURA LAUNCHER - BUG REPORT & FEATURE TRACKER

## 🔴 CRITICAL BUGS FOUND

### Bug #1: Missing LockHelper Class
- **File**: MainActivity.kt (lines 209, 215, 223)
- **Issue**: `LockHelper.lockScreen(context)` called but class not defined
- **Status**: ❌ NOT FIXED
- **Fix**: Need to create LockHelper.kt

### Bug #2: Undefined 'hidden' Variable
- **File**: MainActivity.kt (line 470)
- **Issue**: `apps.filter { ... && it.packageName !in hidden }`
- **Status**: ❌ NOT FIXED
- **Fix**: Define hidden apps list in AuraPrefs or MainActivity

### Bug #3: Missing Coil Import
- **File**: AppComponents.kt (line 77)
- **Issue**: `coil.compose.AsyncImage()` used but not imported
- **Status**: ❌ NOT FIXED
- **Fix**: Add import for `coil.compose.AsyncImage`

### Bug #4: Missing FileResult & FileType
- **File**: MainActivity.kt (lines 795-820)
- **Issue**: FileResult and FileType data classes used but not defined
- **Status**: ❌ NOT FIXED
- **Fix**: Check if FileSearch.kt defines these

### Bug #5: Missing Import in MainActivity
- **File**: MainActivity.kt
- **Issue**: FileResult, FileType, FileSearch classes referenced but may not be imported
- **Status**: ❌ NOT FIXED

---

## 🟡 FEATURE REQUESTS / INCOMPLETE FEATURES

### Feature #1: Home Pages / Swipeable Screens
- **Status**: Mentioned in PHASE_5.1_FEATURES.md but not visible in MainActivity
- **Implementation**: Multiple home pages with HorizontalPager

### Feature #2: Floating Search Bubble
- **Status**: Used in MainActivity but may have bugs
- **File**: FloatingSearchBubble.kt - needs verification

### Feature #3: App Shortcuts (Long Press)
- **Status**: Need to verify implementation

### Feature #4: Quick Actions
- **Status**: Partially implemented

---

## 📋 VERIFICATION NEEDED

1. ✅ FloatingSearchBubble.kt - exists and should work
2. ✅ FileSearch.kt - exists, need to check FileResult definition
3. ❌ LockHelper.kt - MISSING
4. ✅ AuraPrefs.kt - exists
5. ✅ SettingsPanel.kt - exists

---

## ACTION ITEMS

1. **Create LockHelper.kt** - Handle screen lock
2. **Define 'hidden' variable** - For hidden apps functionality  
3. **Fix Coil import** - Add proper import in AppComponents.kt
4. **Verify FileResult class** - Check FileSearch.kt
5. **Test all features** - Ensure no runtime crashes

