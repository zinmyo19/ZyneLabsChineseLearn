# ZyneLabs Chinese Learn 📚

Learn **Mandarin Chinese** with audio pronunciation, flashcards and quizzes —
built for Burmese speakers, with **Chinese ↔ Burmese** translations.

## 📱 Get it

| Platform | How |
|---|---|
| **Android** | Download `ChineseLearn-1.0.apk` from [Releases](../../releases) and install |
| **iPhone / iPad / Web** | Open the web app → **Share → Add to Home Screen** for an app-like install: https://zinmyo19.github.io/ZyneLabsChineseLearn/ |

## ✨ Features

- **79 words & phrases** in 7 categories: greetings, numbers, family, food, daily phrases, time, feelings
- Each entry: 汉字 (simplified) + **pinyin with tone marks** + Burmese meaning
- 🔊 **Audio pronunciation** — tap any entry to hear it (Android TTS / browser speech)
- 🃏 **Flashcards** — tap to flip, shuffle, prev/next
- ✅ **Quiz** — 10 random multiple-choice questions with score
- 📚 **Dictionary** — search by Chinese, pinyin or Burmese
- 🌐 **Translate** (Android app) — free-text 中文 ↔ မြန်မာ translation (online)

## 🛠️ Android source

The APK is built with a manual no-Gradle pipeline (`kotlinc` + `aapt2` + `d8`):

- `src/main/AndroidManifest.xml`
- `src/main/java/com/zynelabs/chineselearn/ChineseData.kt` — vocabulary (79 entries)
- `src/main/java/com/zynelabs/chineselearn/MainActivity.kt` — all 5 tabs

Package: `com.zynelabs.chineselearn` · v1.0

---

Built with ZyneLabs 🤖 — free tier, no servers.
