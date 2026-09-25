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

Keys live in git-ignored `local.properties` (or environment variables) and are compiled into `BuildConfig`:

```properties
GEMINI_API_KEY=
OPENROUTER_API_KEY=   # free at https://openrouter.ai/keys
TAVILY_API_KEY=       # free at https://tavily.com — only needed for web search
```

Empty means unconfigured — the app keeps working with Mock AI, and selecting a keyless provider shows a friendly error on notes instead of crashing. Keys must never be committed to the repository.

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

This project is currently intended for personal use.

The application does not currently require:

* User accounts
* Cloud synchronization
* Social features
* Payments
* A custom backend

API keys must never be stored directly in source code or committed to Git.

Keys are configured via git-ignored `local.properties` (see `local.properties.example`):

```properties
GEMINI_API_KEY=
OPENROUTER_API_KEY=
TAVILY_API_KEY=
```

> **Important:** An API key included in a distributed Android application can potentially be extracted. The current architecture is acceptable for a personal application, but a public release would require a more secure API architecture.

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
git clone <repository-url>
cd curiosity-notes
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

**Status: Personal Release — in daily use**

The MVP is complete and the app is used as a personal daily driver. New ideas go through the same lens: capture fast, keep the inbox calm, never lose a thought.

---

## 📄 License

MIT License.
