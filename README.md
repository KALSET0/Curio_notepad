# Curiosity Notes

> **Capture curiosity now. Understand it later.**

Capture questions, ideas, and curiosities as they come to mind. Curio uses AI to organize and explain them, so you can understand them later.

Curiosity Notes is a personal Android application for quickly capturing questions, ideas, concepts, doubts, and topics that you want to understand later.

Instead of interrupting what you're doing to investigate something immediately, you can save the thought in a few seconds and let AI organize and explain it for you.

---

## 💡 Concept

We constantly encounter things we don't understand:

* "What exactly is quantum entanglement?"
* "Why does this happen?"
* "I have an idea for an app..."
* "I don't understand how this works."
* "I should research this later."
* "Is this claim actually true?"

Curiosity Notes is designed to capture these thoughts before they disappear.

The basic workflow is:

```text
Capture a thought
       ↓
Save it
       ↓
AI identifies what it is
       ↓
AI organizes and explains it
       ↓
Come back later
       ↓
Understand it
       ↓
Explore further
```

The goal is not to create another generic chatbot.

It is a **curiosity inbox**.

---

## 📲 Installation

Download the latest APK from [GitHub Releases](https://github.com/KALSET0/Curio_notepad/releases/latest) and install it on your Android device.

Android will warn that the app comes from outside Google Play — that is expected: Curio is distributed through GitHub rather than the Play Store. Allow “Install unknown apps” for your browser/files app once to proceed.

Curio needs no account and no API key to install or open:

1. Open Curio.
2. Optionally choose **Gemini** or **OpenRouter** and enter your own API key — or skip and use the built-in Mock AI.
3. Start capturing thoughts. Keys can be added, changed, or removed later in **Settings**.

Your keys are stored encrypted only on your device, and your notes never leave the phone except for the AI requests you trigger.

Found a bug? Report it in [Issues](https://github.com/KALSET0/Curio_notepad/issues).

---

## ✨ Features

### Note Capture

Create a note quickly without having to manually categorize it.

A note can contain:

* A question
* A concept
* An idea
* A confusion
* A topic
* A claim
* A reflection
* Anything else worth remembering

### 🤖 AI Classification

AI automatically determines what type of thought was captured.

Supported types:

```text
QUESTION
CONCEPT
IDEA
CONFUSION
TOPIC
CLAIM
REFLECTION
OTHER
```

### 📚 Structured Explanations

Different types of notes receive different response structures.

For example, a question can be organized into:

```text
Quick Answer
Explanation
Example
Key Points
Related Topics
Follow-up Questions
```

While an idea can receive:

```text
Interpretation
Possible Implementation
Potential Advantages
Potential Problems
Improvements
Next Steps
```

This allows the application to provide information that matches the user's intent instead of treating every note as a normal chat message.

### 📥 Inbox Organization

The inbox has **Inbox** and **Archived** tabs. Long-press a note to enter selection mode and archive, pin or delete several at once (the system back button cancels). Pinned notes stay on top and show a pin marker.

Inside a note you can also archive/unarchive and pin/unpin individually, below Delete.

### 🔍 Search & Filters

Local keyword search across titles, original thoughts, AI content and related topics — no network needed. Results can be narrowed with per-type filter chips, sorted newest/oldest, and are grouped by day (Today / Yesterday / date) with a fast-scroll scrollbar.

### 💬 Continue with AI

From any answered note, **Continue with AI** opens a conversation that already carries the note's context (original thought, type, summary, related topics). Follow-up suggestions double as tap-to-send starters. Conversations are persisted per note and shown in the note detail.

### 🔍 Web search (opt-in)

Settings → **Web search** lets the AI consult the internet via Tavily (`TAVILY_API_KEY`). A tiny gatekeeper call decides per note — and once at the start of each conversation — whether fresh facts are needed; at most one search runs per request. Sources the model cites are verified against the delivered URLs and shown as a tappable **Sources** section on the note. Search failures degrade to answering without sources, except a missing/rejected key which surfaces a friendly error.

### ⚙️ Settings & Themes

Choose the AI provider (**Mock AI**, **Gemini**, **OpenRouter**) with live key status, switch System / Light / Dark theme — dark is true-black AMOLED with a sky-blue accent — and read the about section.

---

## 🧠 AI Architecture

The application uses an AI provider abstraction.

```text
                     ┌── Mock AI
                     │
Application → AIProvider ── Gemini
                     │
                     └── OpenRouter
```

This allows AI providers to be replaced without changing the rest of the application.

### Mock AI

The project includes a local Mock AI provider for development.

It requires:

* No API key
* No account
* No internet connection
* No external service

This allows the application to be developed and tested before connecting a real AI provider.

### Gemini

Gemini is a real AI provider for the application. It uses schema-constrained JSON (`gemini-3.8-flash` by default, configurable in one place) so responses always parse into structured cards.

### OpenRouter

OpenRouter is a real AI provider speaking the OpenAI-compatible chat API. The default model is `openrouter/free` (free router with structured-output support); pin a specific `:free` model in one place if you prefer.

### API Keys

Every user supplies their own keys — the distributed app contains none.

In the app: first launch offers an optional AI setup (or skip it), and **Settings** permanently manages keys per provider — enter with show/hide, test the key, see a masked `..........A8F2` status, change or remove it anytime. Keys are encrypted on-device (Android Keystore) and excluded from auto-backup.

For developers building from source, keys can also come from git-ignored `local.properties` (or environment variables) and are compiled into `BuildConfig` as a fallback:

```properties
GEMINI_API_KEY=
OPENROUTER_API_KEY=   # free at https://openrouter.ai/keys
TAVILY_API_KEY=       # free at https://tavily.com — only needed for web search
```

Empty means unconfigured — the app keeps working with Mock AI, and selecting a keyless provider explains how to configure it in Settings instead of crashing. Keys must never be committed to the repository.

### Local AI with Ollama (Developer options)

The app can talk to Ollama running on your laptop over your own Wi-Fi (no internet involved, no key). The phone sends chat requests to `http://<laptop-ip>:11434`.

**On the laptop:**

1. Install Ollama from https://ollama.com and pull a model, e.g. `ollama pull qwen3:8b` (needs ~6 GB free RAM; otherwise try `qwen3:4b`).
2. Serve on the LAN (by default Ollama only listens on localhost):
   ```bash
   OLLAMA_HOST=0.0.0.0 ollama serve
   ```
3. Allow Ollama through the firewall (port 11434) and join the same Wi-Fi as the phone.
4. Find the laptop IP (`ipconfig` on Windows → IPv4, e.g. `192.168.1.10`).

**In the app:**

1. Settings → About → tap the version 5 times quickly to unlock Developer mode.
2. A new **Developer options** section appears above About: enter the Server URL (`http://192.168.1.10:11434`) and the Model (`qwen3:8b`), then **Test connection**.
3. Back in the AI provider section, pick **Local AI server**.

Turning Developer mode off while Ollama is active safely falls back to Mock AI. Qwen3 "thinking" traces (`<think>…</think>`) are stripped automatically from chat answers (and before JSON decoding); if a reply comes back empty because of it, the note shows a retryable error.

### Developer-mode generation info

With Developer mode on, every AI answer carries diagnostics in small blue letters: the provider and model (`Ollama · qwen3:8b`) plus the end-to-end generation time (`8.4 s`, gatekeeper and web search included). Notes show it under the AI response, conversations under each AI message, and the create screen shows the active provider above Save. The data is stored with the note/message rows, so it survives restarts.

---

## 🏗️ Technology Stack

The application is being built with:

* **Kotlin** 2.4.20
* **Jetpack Compose** (BOM 2026.09.00)
* **Material 3** (true-black dark theme)
* **Android SDK** (minSdk 26, targetSdk 36, compileSdk 37)
* **ViewModel**
* **Kotlin Coroutines** 1.11.0
* **Room** 2.8.5 (KSP)
* **Kotlin Serialization** 1.11.0
* **OkHttp** 5.5.0
* **DataStore Preferences** 1.2.1
* **Gradle Kotlin DSL** (AGP 9.4.1, Gradle 9.8.0)

The project aims to use modern Android development practices while avoiding unnecessary complexity.

---

## 📁 Project Structure

The current architecture is:

```text
app/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── database/   # Room DB v3 + migrations
│   │   └── entity/
│   │
│   ├── preferences/    # DataStore settings
│   └── repository/
│
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
│
├── ai/
│   ├── AIProvider.kt
│   ├── AIResponse.kt
│   ├── AiException.kt
│   ├── SwitchingAIProvider.kt
│   ├── prompts/        # centralized system prompt
│   └── providers/
│       ├── MockAIProvider.kt
│       ├── GeminiAIProvider.kt
│       └── OpenRouterAIProvider.kt
│
├── ui/
│   ├── home/
│   ├── note/
│   ├── conversation/
│   ├── search/
│   ├── settings/
│   └── components/
│
├── di/                 # manual AppContainer, no framework
│
└── MainActivity.kt
```

65+ unit tests live under `app/src/test`, with shared fakes in `testing/`.

---

## 🗃️ Local Storage

Notes are stored locally using Room.

A note contains:

```text
id
title
originalText
type
aiResponseJson
errorMessage
status
isArchived
isPinned
createdAt
updatedAt
```

Possible processing states include:

```text
PENDING
PROCESSING
ANSWERED
ERROR
```

The original thought is always preserved.

If AI processing fails, the note stays available with a user-friendly error and a retry action — it is never lost. Pinned notes sort first; archived notes leave the inbox and search but remain under the Archived tab.

---

## 🌐 Offline Behavior

Thoughts can always be captured without an internet connection:

```text
No Internet
    ↓
Capture note
    ↓
Save locally (Room)
    ↓
Pending
    ↓
Process when possible
```

On every launch the app resets notes stuck in `PROCESSING` (e.g. killed mid-flight) back to pending and enqueues unprocessed notes automatically.

AI processing itself requires an external provider unless a local AI implementation is added in the future.

---

## 🔐 Privacy & Security

Curio is local-first and needs no account, cloud sync, social features, payments, or backend.

Notes live in the on-device database; settings sync with Android backup. API keys are encrypted with the device Keystore, never logged, never shown in full, and excluded from backups.

API keys must never be stored directly in source code or committed to Git.

For developers, keys can come from git-ignored `local.properties` (see `local.properties.example`):

```properties
GEMINI_API_KEY=
OPENROUTER_API_KEY=
TAVILY_API_KEY=
```

> **Important:** A user-supplied key inside a client-side Android application cannot be made unextractable the way a server-side secret can. Curio protects keys against accidental exposure (encrypted storage, no logs, masked UI, backup exclusion) — that is the honest security bar for a distributed client app.

---

## 🚀 Getting Started

### Requirements

You will need:

* Windows, macOS, or Linux
* Android SDK
* JDK compatible with the project's Android/Gradle configuration
* An Android device or emulator
* Git

Android Studio is recommended for configuring the Android environment, although the project can also be edited using VS Code.

### Clone the repository

```bash
git clone https://github.com/KALSET0/Curio_notepad.git
cd Curio_notepad
```

### Configure AI keys (optional)

Copy `local.properties.example` to `local.properties` and fill in the keys you have. Empty means unconfigured — the app fully works with Mock AI:

```properties
GEMINI_API_KEY=
OPENROUTER_API_KEY=
TAVILY_API_KEY=
```

### Build the project

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

On macOS/Linux:

```bash
./gradlew assembleDebug
```

### Run tests

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Reports land in `app/build/reports/tests/testDebugUnitTest/index.html`.

### Run

The application can be launched on:

* An Android emulator
* A connected Android device

---

## 🧪 Development

Development is intentionally incremental.

The project was built in stages (all complete):

```text
Project Foundation ✅
        ↓
Local Notes ✅
        ↓
Note Creation ✅
        ↓
AI Architecture ✅
        ↓
Mock AI ✅
        ↓
Structured Responses ✅
        ↓
Gemini Integration ✅
        ↓
Centralized AI Prompt ✅
        ↓
Search ✅
        ↓
Continue with AI ✅
        ↓
Settings & Preferences ✅
        ↓
Reliability ✅
        ↓
UI Polish ✅
        ↓
Testing ✅
        ↓
Personal Release ✅
```

The application should remain functional after every stage.

---

## 🗺️ Roadmap

### MVP

* [x] Android project foundation
* [x] Jetpack Compose UI
* [x] Navigation
* [x] Local Room database (v3 + migrations)
* [x] Create notes
* [x] Edit notes
* [x] Delete notes (single + multi-select)
* [x] Archive / unarchive notes
* [x] Pin notes
* [x] Home/inbox (Inbox/Archived tabs)
* [x] Note detail
* [x] Note processing states
* [x] Mock AI provider
* [x] Automatic note classification
* [x] Structured AI responses
* [x] Gemini provider
* [x] OpenRouter provider
* [x] Local API-key configuration
* [x] In-app API key setup (Settings + first-run, encrypted on-device)
* [x] Centralized AI prompt
* [x] Search (type filters, sort, day groups, fast scroll)
* [x] Settings (provider, theme)
* [x] Light/dark themes (true-black dark)
* [x] Error handling (user-friendly, offline-safe)
* [x] Continue with AI

### Future

These features are intentionally outside the initial MVP:

* [ ] Tags
* [ ] Collections
* [ ] Favorites
* [ ] Learned status
* [ ] Semantic search
* [ ] Related-note discovery
* [ ] Topic clustering
* [ ] Knowledge graph
* [ ] Learning recommendations
* [ ] Spaced repetition
* [ ] Local AI models
* [ ] Cloud synchronization
* [ ] Home-screen widgets
* [ ] Notifications

---

## 🎯 Design Philosophy

Curiosity Notes should make capturing a thought almost effortless.

The application should be:

* **Fast**
* **Minimal**
* **Calm**
* **Easy to read**
* **Learning-focused**
* **Not chatbot-centric**

The most important UX principle is:

> **Capture the thought in seconds. Return to what you were doing. Understand it later.**

---

## 🛠️ Development Status

**Status: Public Release 1.0.0 — distributed through GitHub Releases, in daily use**

The MVP is complete and the app is used as a personal daily driver. New ideas go through the same lens: capture fast, keep the inbox calm, never lose a thought.

---

## 📄 License

MIT License.
