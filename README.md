# Aura Launcher 🌌

Ek smart, beautiful Android launcher — **Nova ki speed + xOS ki beauty + AI ki smartness**.

> Android Studio ki zarurat nahi. Cloud (GitHub Actions) pe build hota hai.

---

## Ye kya hai?

Aura tumhare phone ka home screen replace karta hai. Abhi (Phase 1) ye kaam karta hai:
- ✅ Home screen ban jaata hai (default launcher set kar sakte ho)
- ✅ Saari installed apps dikhata hai (grid mein)
- ✅ App search bar
- ✅ App tap karke khol sakte ho
- ✅ Premium gradient look

Aage AI search, prediction, folders, themes aayenge (dekho `docs/ARCHITECTURE.md`).

---

## Build kaise karein (PC pe kuch bhaari nahi chahiye)

### Step 1 — GitHub pe repo banao
1. github.com pe ek nayi repository banao (naam: `aura-launcher`). Public rakho — free unlimited build.
2. Apne PC pe Aura folder mein terminal kholo aur ye chalao:

```bash
git init
git add .
git commit -m "Aura launcher Phase 1"
git branch -M main
git remote add origin https://github.com/TUMHARA-USERNAME/aura-launcher.git
git push -u origin main
```

### Step 2 — Cloud build apne aap chalega
- Push karte hi GitHub Actions APK banana shuru kar dega.
- GitHub pe jao → **Actions** tab → build chalti dikhegi (~3-5 min).

### Step 3 — APK download karo
- Build complete hone pe, us build pe click karo.
- Niche **Artifacts** mein `Aura-debug-apk` milega → download karo.
- Zip kholo → andar `app-debug.apk` hai.

### Step 4 — Phone pe install + Aura ko home banao
1. APK phone pe bhejo (USB/WhatsApp/Drive).
2. Install karo (Settings → "unknown sources se install" allow karna pad sakta hai).
3. Phone ka **home button** dabao → Android puchega "kaunsa launcher?" → **Aura** choose karo.
4. Ho gaya! Ab tumhara apna launcher chal raha hai. 🎉

> Wapas purane launcher pe jaana ho: Settings → Apps → Default apps → Home app → purana launcher.

---

## Project structure

Poori detail: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)

## Tech stack

Kotlin · Jetpack Compose · GitHub Actions (cloud build) · Groq (sirf AI feature ke liye, baad mein)

## Budget

Launcher ka 95% **bilkul free, bina kisi API ke** chalta hai. Sirf AI search/chat ke liye
Groq ki free API key lagegi. (Detail: ARCHITECTURE.md → section 3)
