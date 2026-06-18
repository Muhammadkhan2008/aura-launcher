# Aura Launcher - Build Status

## Latest Build
- **Commit**: Latest on main/master branch
- **Status**: Auto-builds on every push via GitHub Actions
- **APK Location**: GitHub Actions → Artifacts → Aura-debug-apk (30 days retention)

## Build Command (Local)
```bash
gradle assembleDebug --no-daemon --stacktrace
```

## Recent Commits (Fixes Applied)
- 12bf35e: BUILD FIX - zIndex import, Button onClick lambda, pointerInput separation
- 7a38875: Phase 4 Part 2 Polish - bigger icons (72sp clock, 64dp dock)
- d833046: Premium UI - custom navbar (back/home/recents)
- c460f86: Fix onFocusEvent error
- fd4216c: Feature 10 - Search history

## Features Ready
✅ Voice Search (SpeechRecognizer)
✅ File Search (MediaStore)
✅ Backup/Restore (JSON + Storage Access Framework)
✅ Icon Packs (Nova-compatible)
✅ Premium Wallpapers (8 gradients)
✅ Weather Widget (Open-Meteo free API)
✅ Notification Badges
✅ App Categories/Folders
✅ Quick Settings
✅ Search History

## UI Polish
✅ Clock 72sp (premium)
✅ Weather widget with emoji
✅ Dock icons 64dp (bigger)
✅ Custom navbar (back/home/recents)
✅ Violet accent color (0xFF9D86FF)
✅ Smooth animations
✅ AI button enable/disable

## Download APK
1. Go to GitHub repo
2. Actions tab → Latest workflow
3. Download "Aura-debug-apk" artifact
