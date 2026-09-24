# Curio_notepad
Capture questions, ideas, and curiosities as they come to mind. Curio uses AI to organize and explain them, so you can understand them later.

# Curiosity Notes

> **Capture curiosity now. Understand it later.**

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

---

## 🧠 AI Architecture

The application uses an AI provider abstraction.

```text
                    ┌── Mock AI
                    │
Application → AIProvider
                    │
                    └── Gemini
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

Gemini is the first real AI provider planned for the application.

The API key is configured locally and must never be committed to the repository.

---

## 🏗️ Technology Stack

The application is being built with:

* **Kotlin**
* **Jetpack Compose**
* **Material 3**
* **Android SDK**
* **ViewModel**
* **Kotlin Coroutines**
* **Room**
* **Kotlin Serialization**
* **Retrofit / OkHttp**

The project aims to use modern Android development practices while avoiding unnecessary complexity.

---

## 📁 Project Structure

The planned architecture is approximately:

```text
app/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── database/
│   │   └── entity/
│   │
│   ├── remote/
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
│   ├── prompts/
│   └── providers/
│       ├── MockAIProvider.kt
│       └── GeminiAIProvider.kt
│
├── ui/
│   ├── home/
│   ├── note/
│   ├── search/
│   ├── settings/
│   └── components/
│
└── MainActivity.kt
```

The structure may evolve as development continues.

---

## 🗃️ Local Storage

Notes are stored locally using Room.

A note contains information such as:

```text
id
title
originalText
type
aiResponse
status
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

If AI processing fails, the note should remain available rather than being lost.

---

## 🌐 Offline Behavior

The application should allow thoughts to be captured without an internet connection.

The intended behavior is:

```text
No Internet
    ↓
Capture note
    ↓
Save locally
    ↓
Pending
    ↓
Process when possible
```

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

Local configuration should be used instead, such as `local.properties` or another ignored development configuration.

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

### Build the project

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

On macOS/Linux:

```bash
./gradlew assembleDebug
```

### Run

The application can be launched on:

* An Android emulator
* A connected Android device

---

## 🧪 Development

Development is intentionally incremental.

The project is being built in stages:

```text
Project Foundation
       ↓
Local Notes
       ↓
Note Creation
       ↓
AI Architecture
       ↓
Mock AI
       ↓
Structured Responses
       ↓
Gemini Integration
       ↓
Search
       ↓
Continue with AI
       ↓
Polish & Testing
```

The application should remain functional after every stage.

---

## 🗺️ Roadmap

### MVP

* [ ] Android project foundation
* [ ] Jetpack Compose UI
* [ ] Navigation
* [ ] Local Room database
* [ ] Create notes
* [ ] Edit notes
* [ ] Delete notes
* [ ] Home/inbox
* [ ] Note detail
* [ ] Note processing states
* [ ] Mock AI provider
* [ ] Automatic note classification
* [ ] Structured AI responses
* [ ] Gemini provider
* [ ] Local Gemini configuration
* [ ] Search
* [ ] Settings
* [ ] Light/dark themes
* [ ] Error handling
* [ ] Continue with AI

### Future

These features are intentionally outside the initial MVP:

* [ ] Tags
* [ ] Collections
* [ ] Favorites
* [ ] Archive
* [ ] Learned status
* [ ] Semantic search
* [ ] Related-note discovery
* [ ] Topic clustering
* [ ] Knowledge graph
* [ ] Learning recommendations
* [ ] Spaced repetition
* [ ] Local AI models
* [ ] Cloud synchronization
* [ ] Additional AI providers
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

**Status: Early Development**

This project is currently being developed as a personal application.

The architecture and features may change significantly while the core idea is being tested.

---

## 📄 License

MIT License.
