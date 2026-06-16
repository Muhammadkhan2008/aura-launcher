# Aura Launcher — Architecture (Hinglish mein samjhaaya)

> Ek smart, beautiful Android launcher. Nova ki speed + xOS ki beauty + AI ki smartness.

---

## 1. Aura HOME SCREEN kaise banta hai? (sabse important sawaal)

Android mein launcher koi "special" app nahi hota. Ek **normal app** hi launcher ban jaata hai
agar wo manifest mein ye 2 lines de:

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.HOME" />     <!-- ye line -->
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

- Jab user **home button** dabaata hai, Android puchta hai: "kaun home screen dikhayega?"
- Agar 2+ apps HOME category dete hain, Android user se puchta hai "kaunsa launcher default banayein?"
- User "Aura" choose karega → ab home button dabane pe Aura khulega.

**Koi special permission ya API nahi chahiye iske liye.** Bas manifest ki entry. ✅
(Ye file already bana di hai: `app/src/main/AndroidManifest.xml`)

---

## 2. Apps ki list kaise aati hai? (koi API nahi chahiye)

Phone ki saari installed apps Android ka apna **PackageManager** deta hai — **bilkul offline**,
koi internet/API nahi.

```kotlin
val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
packageManager.queryIntentActivities(intent, 0)   // saari apps mil gayi
```

App khologe to: `getLaunchIntentForPackage(packageName)` → `startActivity()`.
(Ye logic file mein hai: `AppRepository.kt`)

---

## 3. Konsi cheezon ke liye API/internet chahiye? (tumhara budget sawaal)

| Feature | API chahiye? | Cost |
|---------|-------------|------|
| Home screen banna | ❌ Nahi | Free |
| App list + launch | ❌ Nahi (PackageManager) | Free |
| App icons | ❌ Nahi (phone se) | Free |
| Search apps | ❌ Nahi (local) | Free |
| Themes / wallpaper / grid | ❌ Nahi | Free |
| App hide / lock | ❌ Nahi | Free |
| Folders, gestures | ❌ Nahi | Free |
| **AI search / AI commands** | ✅ Haan | **Groq FREE tier** |
| **Smart app prediction** | ❌ Nahi (on-device) | Free |
| Weather widget (optional) | ✅ Haan | Free tier (Open-Meteo — no key) |

**Nateeja:** Launcher ka 95% kaam **bina kisi API ke, bilkul free** chalta hai.
Sirf "AI chat/search" ke liye Groq ki free API key lagegi — aur wahi tum use karna chahte ho. 👍

---

## 4. Tech Stack (kya use kar rahe hain aur kyun)

| Cheez | Kya | Kyun |
|-------|-----|------|
| Language | **Kotlin** | Android ki official, modern, safe language |
| UI | **Jetpack Compose** | Fast, smooth animations, kam code |
| Min Android | **7.0 (API 24)** | Purane phone bhi chalein |
| Icons/images | **Coil** | Halka image loader |
| AI (later) | **Groq API** | Free, super fast LLM |
| Smart predict (later) | **On-device logic** | Free, private, no server |
| Build | **GitHub Actions** | Cloud build — tumhara PC bach jaata hai |
| Local storage | **SharedPreferences / Room** | Settings & data offline rakhne ke liye |

---

## 5. Folder Structure

```
Aura/
├─ app/
│  ├─ build.gradle                 # app ki dependencies + config
│  └─ src/main/
│     ├─ AndroidManifest.xml       # ★ HOME category yahan (launcher banata hai)
│     ├─ java/com/aura/launcher/
│     │  ├─ MainActivity.kt        # home screen UI (Compose)
│     │  └─ AppRepository.kt       # apps list + launch (offline)
│     └─ res/                      # icon, theme, strings
├─ .github/workflows/build.yml     # ★ cloud APK builder
├─ build.gradle                    # project-level config
├─ settings.gradle
├─ gradle.properties
└─ docs/ARCHITECTURE.md            # ye file
```

---

## 6. Build & Test flow (tumhare halke PC ke liye)

```
[VS Code mein code likho]  ->  [GitHub pe push]  ->  [GitHub Actions cloud build]
                                                              |
                                              [APK download]  v
                                                              |
                                          [phone pe install + test]
```

Tumhare PC pe kuch bhaari install nahi hota. Bas code + browser.

---

## 7. Roadmap (phases)

**Phase 1 — Core (bana diya) ✅**
- Home screen banna
- App list, search, launch
- Gradient theme
- Cloud build setup

**Phase 2 — Customization (bana diya) ✅**
- App drawer (swipe up se khulta hai)
- Dock (favourite apps niche)
- Long-press menu (Add to dock / App info / Uninstall)
- Settings panel (grid columns 3-6)
- Back button fix (launcher home pe rehta hai, xOS wapas nahi aata)
- Real wallpaper (phone ka apna wallpaper dikhta hai)
- Smooth drawer slide animation

**Phase 3 — Beauty (xOS jaisa, bina bloat)**
- Material You dynamic colors
- Icon packs support
- Widgets (clock, weather)
- Aur smooth animations

**Phase 3 — Launcher Takeover ("kabza") ✅**
- "Set as default" banner + 1-tap RoleManager prompt
- Double-tap home se screen LOCK (Device Admin)
- Apna Recent apps row (UsageStats — system recents ka alternative)
- Swipe down se notifications panel
- Settings mein saari powers ka status + enable buttons

> NOTE: 3 hardware buttons (back/home/recents) aur system lock-screen
> ko KOI launcher reprogram/replace nahi kar sakta — wo Android SystemUI
> ke control mein hain (security). Aura wahi sab karta hai jo ek asli
> launcher (Nova/xOS-launcher) legally kar sakta hai.

**Phase 4 — Smartness (Aura ka USP) 🧠**
- On-device app prediction (time/habit based)
- AI universal search (Groq)
- Natural language commands
- Smart auto-folders

---

## 8. Privacy & Security

- Sirf **INTERNET** permission (woh bhi AI ke liye). Aur koi permission nahi.
- Koi data kisi server pe nahi jaata (sirf AI query Groq ko, jab user khud puche).
- App prediction **phone ke andar hi** hoti hai — private.
- Groq API key build mein hardcode NAHI karenge — user apni key settings mein daalega
  (ya BuildConfig/secret se aayegi). Key kabhi git pe nahi.
